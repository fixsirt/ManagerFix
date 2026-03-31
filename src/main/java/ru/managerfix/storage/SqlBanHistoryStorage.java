package ru.managerfix.storage;

import ru.managerfix.database.DatabaseManager;
import ru.managerfix.modules.ban.BanHistoryEntry;
import ru.managerfix.scheduler.TaskScheduler;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * SQL-based ban history storage.
 */
public final class SqlBanHistoryStorage {

    private static final String INSERT = "INSERT INTO ban_history (uuid, name, action, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_UUIDS = "SELECT * FROM ban_history WHERE uuid IN (%s)";

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;

    public SqlBanHistoryStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    public void init() {
        // Таблица создаётся в DatabaseManager
    }

    public void shutdown() {
        // Нет кэша
    }

    /**
     * Добавляет запись в историю банов.
     */
    public void appendAsync(BanHistoryEntry entry) {
        if (entry == null) return;
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setString(1, entry.getTargetUuid().toString());
                ps.setString(2, entry.getTargetName());
                ps.setString(3, entry.getAction());
                ps.setString(4, entry.getReason());
                ps.setString(5, entry.getSource());
                ps.setLong(6, entry.getCreatedAt());
                ps.setLong(7, entry.getExpiresAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Получает историю банов для указанных UUID.
     */
    public void getHistoryAsync(Set<UUID> uuids, Consumer<List<BanHistoryEntry>> callback) {
        scheduler.runAsync(() -> {
            List<BanHistoryEntry> result = new ArrayList<>();

            if (uuids == null || uuids.isEmpty()) {
                scheduler.runSync(() -> callback.accept(result));
                return;
            }

            try (Connection conn = databaseManager.getConnection()) {
                // Формируем запрос с нужным количеством плейсхолдеров
                String inSql = String.join(",", Collections.nCopies(uuids.size(), "?"));
                String sql = String.format(SELECT_BY_UUIDS, inSql);

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    int i = 1;
                    for (UUID uuid : uuids) {
                        ps.setString(i++, uuid.toString());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            String name = rs.getString("name");
                            String action = rs.getString("action");
                            String reason = rs.getString("reason");
                            String source = rs.getString("source");
                            long createdAt = rs.getLong("created_at");
                            long expiresAt = rs.getLong("expires_at");
                            result.add(new BanHistoryEntry(uuid, name, action, reason, source, createdAt, expiresAt));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            List<BanHistoryEntry> finalResult = Collections.unmodifiableList(result);
            scheduler.runSync(() -> callback.accept(finalResult));
        });
    }
}
