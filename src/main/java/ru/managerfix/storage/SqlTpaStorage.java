package ru.managerfix.storage;

import ru.managerfix.database.DatabaseManager;
import ru.managerfix.scheduler.TaskScheduler;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL-based TPA data storage (toggle, blacklist).
 */
public final class SqlTpaStorage {

    private static final String SELECT_TOGGLE = "SELECT enabled FROM tpa_data WHERE uuid = ?";
    private static final String SELECT_BLACKLIST = "SELECT blacklisted_uuid FROM tpa_blacklist WHERE owner_uuid = ?";
    private static final String INSERT_TOGGLE = "INSERT INTO tpa_data (uuid, enabled) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE enabled = VALUES(enabled)";
    private static final String INSERT_BLACKLIST = "INSERT INTO tpa_blacklist (owner_uuid, blacklisted_uuid) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE blacklisted_uuid = VALUES(blacklisted_uuid)";
    private static final String DELETE_BLACKLIST = "DELETE FROM tpa_blacklist WHERE owner_uuid = ? AND blacklisted_uuid = ?";
    private static final String DELETE_BLACKLIST_ALL = "DELETE FROM tpa_blacklist WHERE owner_uuid = ?";

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;

    /** Player UUID -> enabled (true = accept requests, false = disabled) */
    private final Map<UUID, Boolean> toggleAccept = new ConcurrentHashMap<>();
    /** Owner UUID -> set of blacklisted player UUIDs */
    private final Map<UUID, Set<UUID>> blacklist = new ConcurrentHashMap<>();

    public SqlTpaStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    public void init() {
        loadAllData();
    }

    public void shutdown() {
        toggleAccept.clear();
        blacklist.clear();
    }

    private void loadAllData() {
        scheduler.runAsync(() -> {
            Map<UUID, Boolean> loadedToggle = new HashMap<>();
            Map<UUID, Set<UUID>> loadedBlacklist = new HashMap<>();

            try (Connection conn = databaseManager.getConnection();
                 Statement st = conn.createStatement()) {

                // Загружаем toggle
                try (ResultSet rs = st.executeQuery("SELECT uuid, enabled FROM tpa_data")) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        boolean enabled = rs.getBoolean("enabled");
                        loadedToggle.put(uuid, enabled);
                    }
                }

                // Загружаем blacklist
                try (ResultSet rs = st.executeQuery("SELECT owner_uuid, blacklisted_uuid FROM tpa_blacklist")) {
                    while (rs.next()) {
                        UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                        UUID blacklisted = UUID.fromString(rs.getString("blacklisted_uuid"));
                        loadedBlacklist.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(blacklisted);
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            scheduler.runSync(() -> {
                toggleAccept.putAll(loadedToggle);
                blacklist.putAll(loadedBlacklist);
            });
        });
    }

    // === Toggle ===

    public boolean isAcceptEnabled(UUID player) {
        return toggleAccept.getOrDefault(player, true);
    }

    public void setAcceptEnabled(UUID player, boolean enabled) {
        toggleAccept.put(player, enabled);
        saveToggleAsync(player, enabled);
    }

    private void saveToggleAsync(UUID player, boolean enabled) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_TOGGLE)) {
                ps.setString(1, player.toString());
                ps.setBoolean(2, enabled);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // === Blacklist ===

    public boolean isBlacklisted(UUID owner, UUID player) {
        Set<UUID> set = blacklist.get(owner);
        return set != null && set.contains(player);
    }

    public void addToBlacklist(UUID owner, UUID player) {
        blacklist.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(player);
        saveBlacklistAsync(owner, player);
    }

    public void removeFromBlacklist(UUID owner, UUID player) {
        Set<UUID> set = blacklist.get(owner);
        if (set != null) {
            set.remove(player);
            if (set.isEmpty()) blacklist.remove(owner);
            deleteBlacklistAsync(owner, player);
        }
    }

    public Set<UUID> getBlacklist(UUID owner) {
        Set<UUID> set = blacklist.get(owner);
        return set == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(set));
    }

    private void saveBlacklistAsync(UUID owner, UUID player) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_BLACKLIST)) {
                ps.setString(1, owner.toString());
                ps.setString(2, player.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteBlacklistAsync(UUID owner, UUID player) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE_BLACKLIST)) {
                ps.setString(1, owner.toString());
                ps.setString(2, player.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
