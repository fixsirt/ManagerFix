package ru.managerfix.storage;

import ru.managerfix.database.DatabaseManager;
import ru.managerfix.modules.ban.BanIpRecord;
import ru.managerfix.modules.ban.BanIpStorage;
import ru.managerfix.scheduler.TaskScheduler;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL-based IP ban storage.
 */
public final class SqlBanIpStorage implements BanIpStorage {

    private static final String SELECT = "SELECT * FROM ip_bans WHERE ip_address = ?";
    private static final String SELECT_BY_UUID = "SELECT * FROM ip_bans WHERE uuid = ?";
    private static final String INSERT = "INSERT INTO ip_bans (uuid, ip_address, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), reason = VALUES(reason), source = VALUES(source), created_at = VALUES(created_at), expires_at = VALUES(expires_at)";
    private static final String DELETE = "DELETE FROM ip_bans WHERE ip_address = ?";
    private static final String DELETE_BY_UUID = "DELETE FROM ip_bans WHERE uuid = ?";
    private static final String LIST = "SELECT * FROM ip_bans";

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;
    private final Map<String, BanIpRecord> cache = new ConcurrentHashMap<>();

    public SqlBanIpStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        loadAllBans();
    }

    @Override
    public void shutdown() {
        cache.clear();
    }

    private void loadAllBans() {
        scheduler.runAsync(() -> {
            List<BanIpRecord> loaded = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(LIST)) {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    UUID uuid = null;
                    try { uuid = UUID.fromString(uuidStr); } catch (Exception ignored) {}
                    String ipAddress = rs.getString("ip_address");
                    String reason = rs.getString("reason");
                    String source = rs.getString("source");
                    long createdAt = rs.getLong("created_at");
                    long expiresAt = rs.getLong("expires_at");
                    BanIpRecord record = new BanIpRecord(uuid, ipAddress, reason, source, createdAt, expiresAt);
                    loaded.add(record);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> {
                cache.clear();
                for (BanIpRecord r : loaded) {
                    cache.put(r.getIpAddress(), r);
                }
            });
        });
    }

    @Override
    public Optional<BanIpRecord> getBan(String ipAddress) {
        // Сначала проверяем кэш
        BanIpRecord cached = cache.get(ipAddress);
        if (cached != null) {
            if (cached.isExpired()) {
                cache.remove(ipAddress);
                return Optional.empty();
            }
            return Optional.of(cached);
        }
        
        // Если в кэше нет, проверяем БД напрямую
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT)) {
            ps.setString(1, ipAddress);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    UUID uuid = null;
                    try { uuid = UUID.fromString(uuidStr); } catch (Exception ignored) {}
                    String ip = rs.getString("ip_address");
                    String reason = rs.getString("reason");
                    String source = rs.getString("source");
                    long createdAt = rs.getLong("created_at");
                    long expiresAt = rs.getLong("expires_at");
                    BanIpRecord record = new BanIpRecord(uuid, ip, reason, source, createdAt, expiresAt);
                    // Добавляем в кэш
                    cache.put(ipAddress, record);
                    if (record.isExpired()) {
                        return Optional.empty();
                    }
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    /**
     * Получить бан по UUID.
     */
    public Optional<BanIpRecord> getBanByUuid(UUID uuid) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_UUID)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ip = rs.getString("ip_address");
                    String reason = rs.getString("reason");
                    String source = rs.getString("source");
                    long createdAt = rs.getLong("created_at");
                    long expiresAt = rs.getLong("expires_at");
                    BanIpRecord record = new BanIpRecord(uuid, ip, reason, source, createdAt, expiresAt);
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void addBanAsync(BanIpRecord record, Runnable onDone) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setString(1, record.getTargetUuid().toString());
                ps.setString(2, record.getIpAddress());
                ps.setString(3, record.getReason());
                ps.setString(4, record.getSource());
                ps.setLong(5, record.getCreatedAt());
                ps.setLong(6, record.getExpiresAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> {
                cache.put(record.getIpAddress(), record);
                if (onDone != null) onDone.run();
            });
        });
    }

    @Override
    public void removeBanAsync(String ipAddress, Runnable onDone) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setString(1, ipAddress);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> {
                cache.remove(ipAddress);
                if (onDone != null) onDone.run();
            });
        });
    }

    @Override
    public void getBansAsync(java.util.function.Consumer<List<BanIpRecord>> callback) {
        scheduler.runAsync(() -> {
            List<BanIpRecord> bans = new ArrayList<>(cache.values());
            scheduler.runSync(() -> callback.accept(bans));
        });
    }

    /**
     * Получить все UUID, забаненные по указанному IP.
     */
    public List<UUID> getBannedUuidsByIp(String ipAddress) {
        List<UUID> uuids = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM bans WHERE ip_address = ?")) {
            ps.setString(1, ipAddress);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        uuids.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException e) {
                        // Игнорируем некорректные UUID
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return uuids;
    }
}
