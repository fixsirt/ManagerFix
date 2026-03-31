package ru.managerfix.storage;

import ru.managerfix.database.DatabaseManager;
import ru.managerfix.modules.ban.MuteRecord;
import ru.managerfix.modules.ban.MuteStorage;
import ru.managerfix.scheduler.TaskScheduler;

import java.sql.*;
import java.util.*;

/**
 * SQL-based mute storage.
 */
public final class SqlMuteStorage implements MuteStorage {

    private static final String SELECT = "SELECT * FROM mutes WHERE uuid = ?";
    private static final String INSERT = "INSERT INTO mutes (uuid, name, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), reason = VALUES(reason), source = VALUES(source), created_at = VALUES(created_at), expires_at = VALUES(expires_at)";
    private static final String DELETE = "DELETE FROM mutes WHERE uuid = ?";

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;
    private final Map<UUID, MuteRecord> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public SqlMuteStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        loadAllMutes();
    }

    @Override
    public void shutdown() {
        cache.clear();
    }

    private void loadAllMutes() {
        scheduler.runAsync(() -> {
            Map<UUID, MuteRecord> loaded = new HashMap<>();
            long now = System.currentTimeMillis();
            try (Connection conn = databaseManager.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM mutes")) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    String reason = rs.getString("reason");
                    String source = rs.getString("source");
                    long createdAt = rs.getLong("created_at");
                    long expiresAt = rs.getLong("expires_at");
                    // Загружаем только НЕ истёкшие муты
                    if (expiresAt == 0 || expiresAt > now) {
                        MuteRecord record = new MuteRecord(uuid, name, reason, source, createdAt, expiresAt);
                        loaded.put(uuid, record);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> {
                cache.clear();  // Очищаем кэш перед загрузкой
                cache.putAll(loaded);
            });
        });
    }

    @Override
    public Optional<MuteRecord> getMute(UUID uuid) {
        // Сначала проверяем кэш
        MuteRecord cached = cache.get(uuid);
        if (cached != null) {
            return Optional.of(cached);
        }
        // Если нет в кэше, загружаем из БД
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String reason = rs.getString("reason");
                    String source = rs.getString("source");
                    long createdAt = rs.getLong("created_at");
                    long expiresAt = rs.getLong("expires_at");
                    MuteRecord record = new MuteRecord(uuid, name, reason, source, createdAt, expiresAt);
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void addMuteAsync(MuteRecord record, Runnable onDone) {
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
            scheduler.runSync(() -> {
                cache.put(record.getTargetUuid(), record);
                if (onDone != null) onDone.run();
            });
        });
    }

    @Override
    public void removeMuteAsync(UUID uuid, Runnable onDone) {
        scheduler.runAsync(() -> {
            int rowsAffected = 0;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setString(1, uuid.toString());
                rowsAffected = ps.executeUpdate();
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
}
