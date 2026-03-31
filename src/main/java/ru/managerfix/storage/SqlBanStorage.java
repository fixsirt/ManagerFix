package ru.managerfix.storage;

import ru.managerfix.database.DatabaseManager;
import ru.managerfix.modules.ban.BanRecord;
import ru.managerfix.modules.ban.BanStorage;
import ru.managerfix.scheduler.TaskScheduler;

import java.sql.*;
import java.util.*;

/**
 * SQL-based ban storage.
 */
public final class SqlBanStorage implements BanStorage {

    private static final String SELECT = "SELECT * FROM bans WHERE uuid = ?";
    private static final String INSERT = "INSERT INTO bans (uuid, name, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), reason = VALUES(reason), source = VALUES(source), created_at = VALUES(created_at), expires_at = VALUES(expires_at)";
    private static final String DELETE = "DELETE FROM bans WHERE uuid = ?";
    private static final String LIST = "SELECT * FROM bans";

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;
    private final Map<UUID, BanRecord> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public SqlBanStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
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
            List<BanRecord> loaded = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(LIST)) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    String reason = rs.getString("reason");
                    String source = rs.getString("source");
                    long createdAt = rs.getLong("created_at");
                    long expiresAt = rs.getLong("expires_at");
                    BanRecord record = new BanRecord(uuid, name, reason, source, createdAt, expiresAt);
                    loaded.add(record);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> {
                cache.clear();
                for (BanRecord r : loaded) {
                    cache.put(r.getTargetUuid(), r);
                }
            });
        });
    }

    @Override
    public Optional<BanRecord> getBan(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    @Override
    public void addBanAsync(BanRecord record, Runnable onDone) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setString(1, record.getTargetUuid().toString());
                ps.setString(2, record.getTargetName());
                ps.setString(3, record.getReason());
                ps.setString(4, record.getSource());
                ps.setLong(5, record.getCreatedAt());
                ps.setLong(6, record.getExpiresAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // Обновляем кэш и вызываем onDone асинхронно
            cache.put(record.getTargetUuid(), record);
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    @Override
    public void removeBanAsync(UUID uuid, Runnable onDone) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                // Сразу удаляем из кэша после успешного удаления из БД
                cache.remove(uuid);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (onDone != null) {
                scheduler.runSync(onDone);
            }
        });
    }

    @Override
    public void getBansAsync(java.util.function.Consumer<List<BanRecord>> callback) {
        scheduler.runAsync(() -> {
            List<BanRecord> bans = new ArrayList<>(cache.values());
            scheduler.runSync(() -> callback.accept(bans));
        });
    }
}
