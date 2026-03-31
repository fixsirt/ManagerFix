package ru.managerfix.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import ru.managerfix.database.DatabaseManager;
import ru.managerfix.modules.kits.KitData;
import ru.managerfix.modules.kits.KitStorage;
import ru.managerfix.scheduler.TaskScheduler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * SQL-based kit storage.
 */
public final class SqlKitStorage implements KitStorage {

    private static final String SELECT = "SELECT * FROM kits WHERE name = ?";
    private static final String INSERT = "INSERT INTO kits (name, cooldown, permission, items, priority, one_time, icon_material) VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE cooldown = VALUES(cooldown), permission = VALUES(permission), items = VALUES(items), priority = VALUES(priority), one_time = VALUES(one_time), icon_material = VALUES(icon_material)";
    private static final String DELETE = "DELETE FROM kits WHERE name = ?";
    private static final String LIST = "SELECT name FROM kits ORDER BY priority ASC, name ASC";

    private static final Gson GSON = new Gson();
    private static final Type ITEM_STACK_LIST_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;
    private final Map<String, KitData> cache = new LinkedHashMap<>();

    public SqlKitStorage(DatabaseManager databaseManager, TaskScheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        loadAllKitsSync();
    }

    @Override
    public void shutdown() {
        cache.clear();
    }

    private void loadAllKitsSync() {
        // Загружаем все киты синхронно (в текущем потоке) при инициализации
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(LIST);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                Optional<KitData> kit = loadKitSync(name);
                kit.ifPresent(k -> cache.put(name.toLowerCase(), k));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAllKits() {
        scheduler.runAsync(() -> {
            Map<String, KitData> loaded = new HashMap<>();
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(LIST);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    Optional<KitData> kit = loadKitSync(name);
                    kit.ifPresent(k -> loaded.put(name, k));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> cache.putAll(loaded));
        });
    }

    @Override
    public Optional<KitData> loadKit(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(cache.get(name.toLowerCase()));
    }

    private Optional<KitData> loadKitSync(String name) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                int cooldown = rs.getInt("cooldown");
                String permission = rs.getString("permission");
                String itemsJson = rs.getString("items");
                int priority = rs.getInt("priority");
                boolean oneTime = rs.getBoolean("one_time");
                String iconMaterial = rs.getString("icon_material");
                List<ItemStack> items = parseItems(itemsJson);
                KitData kit = new KitData(name, cooldown, permission, items);
                kit.setPriority(priority);
                kit.setOneTime(oneTime);
                if (iconMaterial != null) {
                    kit.setIconMaterial(iconMaterial);
                }
                return Optional.of(kit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void saveKit(KitData kit) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT)) {
                ps.setString(1, kit.getName());
                ps.setInt(2, kit.getCooldownSeconds());
                ps.setString(3, kit.getPermission());
                String itemsJson = serializeItems(kit.getItems());
                ps.setString(4, itemsJson);
                ps.setInt(5, kit.getPriority());
                ps.setBoolean(6, kit.isOneTime());
                ps.setString(7, kit.getIconMaterial() != null ? kit.getIconMaterial() : "");
                int rows = ps.executeUpdate();
                System.out.println("[SqlKitStorage] Saved kit '" + kit.getName() + "' to DB (rows=" + rows + ")");
            } catch (SQLException e) {
                System.err.println("[SqlKitStorage] Failed to save kit '" + kit.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
            scheduler.runSync(() -> cache.put(kit.getName().toLowerCase(), kit));
        });
    }

    @Override
    public void deleteKit(String name) {
        scheduler.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE)) {
                ps.setString(1, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            scheduler.runSync(() -> cache.remove(name.toLowerCase()));
        });
    }

    @Override
    public List<String> listKitNames() {
        // Загружаем список из БД если кэш пуст
        if (cache.isEmpty()) {
            try (Connection conn = databaseManager.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(LIST)) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    loadKitSync(name).ifPresent(kit -> cache.put(name.toLowerCase(), kit));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>(cache.keySet());
    }

    private List<ItemStack> parseItems(String base64) {
        if (base64 == null || base64.isEmpty()) return Collections.emptyList();
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 GZIPInputStream gis = new GZIPInputStream(bais);
                 BukkitObjectInputStream bois = new BukkitObjectInputStream(gis)) {
                List<ItemStack> items = new ArrayList<>();
                int count = bois.readInt();
                for (int i = 0; i < count; i++) {
                    Object obj = bois.readObject();
                    if (obj instanceof ItemStack) {
                        items.add((ItemStack) obj);
                    }
                }
                return items;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String serializeItems(List<ItemStack> items) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(baos);
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(gos);
            boos.writeInt(items != null ? items.size() : 0);
            if (items != null) {
                for (ItemStack stack : items) {
                    boos.writeObject(stack);
                }
            }
            boos.close(); // закрывает gos → вызывает finish() у GZIP и сбрасывает все данные в baos
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
