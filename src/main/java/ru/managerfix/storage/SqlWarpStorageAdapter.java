package ru.managerfix.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import ru.managerfix.database.DatabaseManager;
import ru.managerfix.scheduler.TaskScheduler;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * SQL-based warp storage. All operations run async via TaskScheduler. Supports both MySQL and SQLite.
 */
public final class SqlWarpStorageAdapter implements WarpStorageAdapter {

    private static final String SELECT_ALL = "SELECT name, world, x, y, z, yaw, pitch FROM warps";
    private static final String DELETE_ALL = "DELETE FROM warps";
    private static final String INSERT_MYSQL = "INSERT INTO warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)";
    private static final String INSERT_SQLITE = "INSERT INTO warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;
    private String insertSql;

    public SqlWarpStorageAdapter(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        this.insertSql = "SQLITE".equalsIgnoreCase(databaseManager.getStorageType()) ? INSERT_SQLITE : INSERT_MYSQL;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void loadWarpsAsync(Consumer<Map<String, Location>> callback) {
        scheduler.runAsync(() -> {
            Map<String, Location> warps = loadWarpsSync();
            scheduler.runSync(() -> callback.accept(warps));
        });
    }

    private Map<String, Location> loadWarpsSync() {
        Map<String, Location> warps = new ConcurrentHashMap<>();
        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String worldName = rs.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                warps.put(name.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
            }
        } catch (SQLException e) {
            ru.managerfix.core.LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to load warps", e);
        }
        return warps;
    }

    @Override
    public void saveWarpsAsync(Map<String, Location> warps, Runnable onDone) {
        scheduler.runAsync(() -> {
            saveWarpsSync(warps != null ? warps : Map.of());
            if (onDone != null) {
                scheduler.runSync(onDone);
            }
        });
    }

    private void saveWarpsSync(Map<String, Location> warps) {
        try (Connection conn = databaseManager.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(DELETE_ALL);
            }
            if (!warps.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (Map.Entry<String, Location> e : warps.entrySet()) {
                        Location loc = e.getValue();
                        ps.setString(1, e.getKey().toLowerCase());
                        ps.setString(2, loc.getWorld() != null ? loc.getWorld().getName() : "world");
                        ps.setDouble(3, loc.getX());
                        ps.setDouble(4, loc.getY());
                        ps.setDouble(5, loc.getZ());
                        ps.setFloat(6, loc.getYaw());
                        ps.setFloat(7, loc.getPitch());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
        } catch (SQLException e) {
            ru.managerfix.core.LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save warps", e);
        }
    }
}
