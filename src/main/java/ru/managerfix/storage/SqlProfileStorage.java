package ru.managerfix.storage;

import ru.managerfix.database.DatabaseManager;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.scheduler.TaskScheduler;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * SQL-based profile storage. All operations run async via TaskScheduler.
 */
public final class SqlProfileStorage implements ProfileStorage {

    private static final String SELECT = "SELECT uuid, name, last_activity, metadata, cooldowns, last_ip FROM profiles WHERE uuid = ?";
    private static final String SELECT_HOMES = "SELECT name, world, x, y, z, yaw, pitch FROM homes WHERE uuid = ?";
    private static final String UPSERT = "INSERT INTO profiles (uuid, name, last_activity, metadata, cooldowns, last_ip) VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), last_activity = VALUES(last_activity), metadata = VALUES(metadata), cooldowns = VALUES(cooldowns), last_ip = VALUES(last_ip)";
    private static final String INSERT_HOME = "INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)";
    private static final String DELETE_HOME = "DELETE FROM homes WHERE uuid = ? AND name = ?";

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;

    public SqlProfileStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        // Tables created by DatabaseManager
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void loadProfileAsync(UUID uuid, Consumer<Optional<PlayerProfile>> callback) {
        scheduler.runAsync(() -> {
            Optional<PlayerProfile> result = loadProfileSync(uuid);
            scheduler.runSync(() -> callback.accept(result));
        });
    }

    private Optional<PlayerProfile> loadProfileSync(UUID uuid) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                PlayerProfile profile = new PlayerProfile(uuid);
                profile.setLastActivity(rs.getLong("last_activity"));
                profile.setLastIpAddress(rs.getString("last_ip"));

                // Загружаем homes из отдельной таблицы
                Map<String, String> homesMap = loadHomesSync(conn, uuid);
                profile.setHomesMap(homesMap);

                String metaStr = rs.getString("metadata");
                if (metaStr != null && !metaStr.isEmpty()) {
                    Map<String, Object> meta = parseMetadata(metaStr);
                    // Удаляем homes из metadata если там есть
                    meta.remove("homes");
                    profile.setMetadataFromSnapshot(meta);
                }
                String cooldownsStr = rs.getString("cooldowns");
                if (cooldownsStr != null && !cooldownsStr.isEmpty()) {
                    profile.setCooldownsFromSnapshot(parseCooldowns(cooldownsStr));
                }
                return Optional.of(profile);
            }
        } catch (SQLException e) {
            ru.managerfix.core.LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to load profile " + uuid, e);
            return Optional.empty();
        }
    }

    private Map<String, String> loadHomesSync(Connection conn, UUID uuid) throws SQLException {
        Map<String, String> homesMap = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_HOMES)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String world = rs.getString("world");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float yaw = rs.getFloat("yaw");
                    float pitch = rs.getFloat("pitch");
                    // Формат: world,x,y,z,yaw,pitch
                    String location = world + "," + x + "," + y + "," + z + "," + yaw + "," + pitch;
                    homesMap.put(name, location);
                }
            }
        }
        return homesMap;
    }

    @Override
    public void saveProfileAsync(PlayerProfile profile, Runnable onDone) {
        scheduler.runAsync(() -> {
            saveProfileSync(profile);
            if (onDone != null) {
                scheduler.runSync(onDone);
            }
        });
    }

    private void saveProfileSync(PlayerProfile profile) {
        try (Connection conn = databaseManager.getConnection()) {
            // Получаем ник из кэша или оставляем null (не блокируем сервер вызовом Bukkit.getOfflinePlayer)
            String playerName = null;
            
            // Сохраняем профиль
            try (PreparedStatement ps = conn.prepareStatement(UPSERT)) {
                ps.setString(1, profile.getUuid().toString());
                ps.setString(2, playerName);
                ps.setLong(3, profile.getLastActivity());
                ps.setString(4, profile.getLastIpAddress() != null ? profile.getLastIpAddress() : "");
                Map<String, Object> meta = new HashMap<>(profile.getMetadataSnapshot());
                // Не сохраняем homes в metadata - они в отдельной таблице
                meta.remove("homes");
                ps.setString(5, serializeMetadata(meta));
                ps.setString(6, serializeCooldowns(profile.getCooldownsSnapshot()));
                ps.executeUpdate();
            }

            // Сохраняем homes в отдельную таблицу
            saveHomesSync(conn, profile);

        } catch (SQLException e) {
            ru.managerfix.core.LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save profile " + profile.getUuid(), e);
        }
    }

    private void saveHomesSync(Connection conn, PlayerProfile profile) throws SQLException {
        UUID uuid = profile.getUuid();
        Map<String, String> currentHomes = profile.getHomesMap();
        
        // Получаем текущие дома из БД
        Map<String, String> dbHomes = loadHomesSync(conn, uuid);
        
        // Удаляем дома которых нет в профиле
        for (String homeName : dbHomes.keySet()) {
            if (!currentHomes.containsKey(homeName)) {
                try (PreparedStatement ps = conn.prepareStatement(DELETE_HOME)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, homeName);
                    ps.executeUpdate();
                }
            }
        }
        
        // Добавляем/обновляем дома из профиля
        try (PreparedStatement ps = conn.prepareStatement(INSERT_HOME)) {
            for (Map.Entry<String, String> entry : currentHomes.entrySet()) {
                String homeName = entry.getKey();
                String location = entry.getValue();
                String[] parts = location.split(",");
                if (parts.length >= 4) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, homeName);
                    ps.setString(3, parts[0].trim()); // world
                    ps.setDouble(4, Double.parseDouble(parts[1].trim())); // x
                    ps.setDouble(5, Double.parseDouble(parts[2].trim())); // y
                    ps.setDouble(6, Double.parseDouble(parts[3].trim())); // z
                    ps.setFloat(7, parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0f); // yaw
                    ps.setFloat(8, parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0f); // pitch
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private static Map<String, Object> parseMetadata(String s) {
        Map<String, Object> out = new HashMap<>();
        for (String line : s.split("\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0) {
                out.put(line.substring(0, tab), line.substring(tab + 1));
            }
        }
        return out;
    }

    private static String serializeMetadata(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        meta.forEach((k, v) -> sb.append(k).append('\t').append(v != null ? v.toString() : "").append('\n'));
        return sb.toString();
    }

    private static Map<String, Long> parseCooldowns(String s) {
        Map<String, Long> out = new HashMap<>();
        long now = System.currentTimeMillis();
        for (String line : s.split("\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0) {
                try {
                    long expiresAt = Long.parseLong(line.substring(tab + 1).trim());
                    if (expiresAt > now) {
                        out.put(line.substring(0, tab), expiresAt);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return out;
    }

    private static String serializeCooldowns(Map<String, Long> cooldowns) {
        if (cooldowns == null || cooldowns.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        cooldowns.forEach((k, v) -> sb.append(k).append('\t').append(v).append('\n'));
        return sb.toString();
    }

    private static Map<String, String> parseHomesMap(String s) {
        Map<String, String> out = new HashMap<>();
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                out.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        return out;
    }

    private static String serializeHomes(Map<String, String> homes) {
        if (homes == null || homes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        homes.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(';');
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }

    @Override
    public void findUuidsByMetadataEquals(String key, String value, java.util.function.Consumer<java.util.Set<java.util.UUID>> callback) {
        scheduler.runAsync(() -> {
            java.util.Set<java.util.UUID> result = new java.util.HashSet<>();
            String like = "%" + key + "\t" + value + "%";
            String sql = "SELECT uuid FROM profiles WHERE metadata LIKE ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, like);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uuidStr = rs.getString("uuid");
                        try {
                            result.add(java.util.UUID.fromString(uuidStr));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (SQLException ignored) {
            }
            java.util.Set<java.util.UUID> finalResult = java.util.Collections.unmodifiableSet(result);
            scheduler.runSync(() -> callback.accept(finalResult));
        });
    }
}
