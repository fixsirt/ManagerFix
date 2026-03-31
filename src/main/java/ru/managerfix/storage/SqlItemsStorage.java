package ru.managerfix.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.database.DatabaseManager;
import ru.managerfix.scheduler.TaskScheduler;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

/**
 * SQL-based saved items storage.
 */
public final class SqlItemsStorage {

    private static final String SELECT = "SELECT * FROM saved_items WHERE name = ?";
    private static final String INSERT = "INSERT INTO saved_items (name, data) VALUES (?, ?) " +
            "ON DUPLICATE KEY UPDATE data = VALUES(data)";
    private static final String DELETE = "DELETE FROM saved_items WHERE name = ?";
    private static final String LIST = "SELECT name, data FROM saved_items";

    private static final Gson GSON = new Gson();
    private static final Type ITEM_STACK_MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;
    private final Map<String, Map<String, Object>> cache = new HashMap<>();

    public SqlItemsStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    public void init() {
        loadAllItems();
    }

    public void shutdown() {
        cache.clear();
    }

    private void loadAllItems() {
        scheduler.runAsync(() -> {
            Map<String, Map<String, Object>> loaded = new HashMap<>();
            try (Connection conn = databaseManager.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(LIST)) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String dataJson = rs.getString("data");
                    Map<String, Object> data = parseData(dataJson);
                    if (!data.isEmpty()) {
                        loaded.put(name, data);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> cache.putAll(loaded));
        });
    }

    public void getItemAsync(String name, java.util.function.Consumer<Optional<Map<String, Object>>> callback) {
        // Сначала проверяем кэш
        Map<String, Object> cached = cache.get(name);
        if (cached != null) {
            scheduler.runSync(() -> callback.accept(Optional.of(cached)));
            return;
        }
        
        // Если нет в кэше — загружаем из SQL
        scheduler.runAsync(() -> {
            Optional<Map<String, Object>> result = loadItemSync(name);
            scheduler.runSync(() -> callback.accept(result));
        });
    }

    private Optional<Map<String, Object>> loadItemSync(String name) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("data");
                    if (json != null && !json.isEmpty()) {
                        Map<String, Object> data = GSON.fromJson(json, ITEM_STACK_MAP_TYPE);
                        cache.put(name, data); // Кэшируем
                        return Optional.of(data);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void saveItemAsync(String name, Map<String, Object> data, Runnable onDone) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setString(1, name);
                String json = GSON.toJson(data);
                ps.setString(2, json);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> {
                // Обновляем кэш сразу после сохранения
                cache.put(name, data);
                if (onDone != null) onDone.run();
            });
        });
    }
    
    /**
     * Сохраняет предмет и возвращает CompletableFuture для цепочки действий.
     */
    public void saveItemAndGetAsync(String name, Map<String, Object> data, Runnable onSuccess) {
        saveItemAsync(name, data, onSuccess);
    }

    public void deleteItemAsync(String name, Runnable onDone) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setString(1, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> {
                cache.remove(name);
                if (onDone != null) onDone.run();
            });
        });
    }

    public List<String> listNames() {
        return new ArrayList<>(cache.keySet());
    }

    private Map<String, Object> parseData(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyMap();
        try {
            return GSON.fromJson(json, ITEM_STACK_MAP_TYPE);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
