package ru.managerfix.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.database.DatabaseManager;
import ru.managerfix.modules.ban.BanRecord;
import ru.managerfix.modules.ban.MuteRecord;
import ru.managerfix.modules.kits.KitData;
import ru.managerfix.modules.warps.Warp;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.storage.SqlProfileStorage;
import ru.managerfix.storage.YamlProfileStorage;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Автоматическая миграция данных между YAML и MySQL.
 * Работает в обе стороны: YAML → MySQL и MySQL → YAML.
 */
public final class MigrationManager {

    private final ManagerFix plugin;
    private final FileManager fileManager;
    private final DatabaseManager databaseManager;
    private final String currentStorageType;
    private final boolean isSqlite;

    public MigrationManager(ManagerFix plugin, FileManager fileManager, DatabaseManager databaseManager, String storageType) {
        this.plugin = plugin;
        this.fileManager = fileManager;
        this.databaseManager = databaseManager;
        this.currentStorageType = storageType.toUpperCase();
        this.isSqlite = "SQLITE".equalsIgnoreCase(storageType);
    }

    /**
     * Запускает миграцию если нужно.
     */
    public void migrateIfNeeded() {
        File migrationFlag = new File(plugin.getDataFolder(), "data/migration_done.flag");
        
        // Если миграция уже была выполнена в этой сессии - пропускаем
        if (migrationFlag.exists()) {
            LoggerUtil.debug("Migration already done in this session.");
            return;
        }

        String lastStorageType = readLastStorageType();
        
        if (lastStorageType == null) {
            // Первый запуск - запоминаем тип
            writeLastStorageType(currentStorageType);
            LoggerUtil.info("Storage type initialized: " + currentStorageType);
            return;
        }

        if (!lastStorageType.equals(currentStorageType)) {
            // Тип хранилища изменился - нужна миграция
            LoggerUtil.info("Storage type changed: " + lastStorageType + " → " + currentStorageType);
            LoggerUtil.info("Starting data migration...");
            
            try {
                if ("MYSQL".equals(currentStorageType) || "SQLITE".equals(currentStorageType)) {
                    if (hasDataInYaml()) {
                        migrateYamlToSql();
                    } else {
                        LoggerUtil.info("No YAML data to migrate.");
                    }
                } else {
                    if (hasDataInSql()) {
                        migrateSqlToYaml();
                    } else {
                        LoggerUtil.info("No SQL data to migrate.");
                    }
                }
                
                writeLastStorageType(currentStorageType);
                migrationFlag.createNewFile();
                LoggerUtil.info("Migration completed successfully!");
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Migration failed!", e);
            }
        } else {
            LoggerUtil.debug("Storage type unchanged: " + currentStorageType);
        }
    }

    /**
     * Проверяет, есть ли данные в YAML файлах.
     */
    public boolean hasDataInYaml() {
        File dataFolder = fileManager.getDataFolderStorage();

        // Проверяем warps
        File warpsFile = new File(dataFolder, "warps.yml");
        if (warpsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
            if (config.contains("warps") && !config.getConfigurationSection("warps").getKeys(false).isEmpty()) {
                return true;
            }
        }

        // Проверяем kits
        File kitsFile = new File(dataFolder, "kits.yml");
        if (kitsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
            if (config.contains("kits") && !config.getConfigurationSection("kits").getKeys(false).isEmpty()) {
                return true;
            }
        }

        // Проверяем bans
        File bansFile = new File(dataFolder, "bans.yml");
        if (bansFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(bansFile);
            if (config.contains("bans") && !config.getConfigurationSection("bans").getKeys(false).isEmpty()) {
                return true;
            }
        }

        // Проверяем mutes
        File mutesFile = new File(dataFolder, "mutes.yml");
        if (mutesFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(mutesFile);
            if (config.contains("mutes") && !config.getConfigurationSection("mutes").getKeys(false).isEmpty()) {
                return true;
            }
        }

        // Проверяем items
        File itemsFile = new File(dataFolder, "items.yml");
        if (itemsFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
            if (config.contains("saved") && !config.getConfigurationSection("saved").getKeys(false).isEmpty()) {
                return true;
            }
        }

        // Проверяем tpa-data
        File tpaDataFile = new File(dataFolder, "tpa-data.yml");
        if (tpaDataFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(tpaDataFile);
            if (!config.getKeys(false).isEmpty()) {
                return true;
            }
        }

        // Проверяем ban_history
        File banHistoryFile = new File(dataFolder, "ban_history.yml");
        if (banHistoryFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(banHistoryFile);
            if (config.contains("entries")) {
                List<?> entries = config.getList("entries");
                if (entries != null && !entries.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Проверяет, есть ли данные в SQL (MySQL или SQLite).
     */
    @SuppressWarnings("unchecked")
    public boolean hasDataInSql() {
        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement()) {
            
            // Проверяем количество записей в каждой таблице
            String[] tables = {"warps", "kits", "bans", "mutes", "saved_items", "ip_bans", "ban_history"};
            for (String table : tables) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                } catch (SQLException ignored) {
                    // Таблица может не существовать
                }
            }
        } catch (SQLException e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to check SQL data", e);
        }
        return false;
    }

    public void migrateYamlToSql() throws SQLException {
        LoggerUtil.info("========================================");
        LoggerUtil.info("Starting YAML to MySQL migration...");
        LoggerUtil.info("Data folder: " + fileManager.getDataFolderStorage().getAbsolutePath());
        LoggerUtil.info("========================================");

        // Миграция варпов
        migrateWarpsYamlToSql();

        // Миграция китов
        migrateKitsYamlToSql();

        // Миграция банов
        migrateBansYamlToSql();

        // Миграция мутов
        migrateMutesYamlToSql();

        // Миграция предметов
        migrateItemsYamlToSql();

        // Миграция профилей (включая homes)
        migrateProfilesYamlToSql();

        // Миграция спавна
        migrateSpawnYamlToSql();

        // Миграция TPA данных
        migrateTpaDataYamlToSql();

        // Миграция истории банов
        migrateBanHistoryYamlToSql();
        
        LoggerUtil.info("========================================");
        LoggerUtil.info("Migration from YAML to MySQL completed.");
        LoggerUtil.info("========================================");
    }

    /**
     * Очищает все SQL таблицы перед миграцией.
     */
    public void clearSqlTables() throws SQLException {
        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement()) {
            
            boolean isSqlite = "SQLITE".equalsIgnoreCase(databaseManager.getStorageType());
            
            if (!isSqlite) {
                st.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // SQLite использует DELETE FROM вместо TRUNCATE TABLE
            String clearQuery = isSqlite ? "DELETE FROM %s" : "TRUNCATE TABLE %s";
            
            st.execute(String.format(clearQuery, "ban_history"));
            st.execute(String.format(clearQuery, "tpa_blacklist"));
            st.execute(String.format(clearQuery, "tpa_data"));
            st.execute(String.format(clearQuery, "saved_items"));
            st.execute(String.format(clearQuery, "mutes"));
            st.execute(String.format(clearQuery, "bans"));
            st.execute(String.format(clearQuery, "ip_bans"));
            st.execute(String.format(clearQuery, "homes"));
            st.execute(String.format(clearQuery, "kits"));
            st.execute(String.format(clearQuery, "warps"));
            st.execute(String.format(clearQuery, "profiles"));
            st.execute(String.format(clearQuery, "spawn"));
            
            if (!isSqlite) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            
            LoggerUtil.info("SQL tables cleared.");
        }
    }

    public void migrateSqlToYaml() {
        LoggerUtil.info("Migrating from MySQL to YAML...");

        // Миграция варпов
        migrateWarpsSqlToYaml();

        // Миграция китов
        migrateKitsSqlToYaml();

        // Миграция банов
        migrateBansSqlToYaml();

        // Миграция мутов
        migrateMutesSqlToYaml();

        // Миграция предметов
        migrateItemsSqlToYaml();

        // Миграция профилей
        migrateProfilesSqlToYaml();

        // Миграция спавна
        migrateSpawnSqlToYaml();

        // Миграция TPA данных
        migrateTpaDataSqlToYaml();

        // Миграция истории банов
        migrateBanHistorySqlToYaml();
    }

    // ==================== Warps ====================

    private void migrateWarpsYamlToSql() throws SQLException {
        File warpsFile = new File(fileManager.getDataFolderStorage(), "warps.yml");
        LoggerUtil.info("Checking warps file: " + warpsFile.getAbsolutePath() + " (exists: " + warpsFile.exists() + ")");
        
        if (!warpsFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);
        if (!config.contains("warps")) return;

        String sql;
        if (isSqlite) {
            sql = "INSERT INTO warps (name, world, x, y, z, yaw, pitch, permission, category, icon, slot, description, teleport_delay, enabled, hidden, owner) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, " +
                    "yaw = excluded.yaw, pitch = excluded.pitch, permission = excluded.permission, category = excluded.category, " +
                    "icon = excluded.icon, slot = excluded.slot, description = excluded.description, teleport_delay = excluded.teleport_delay, " +
                    "enabled = excluded.enabled, hidden = excluded.hidden, owner = excluded.owner";
        } else {
            sql = "INSERT INTO warps (name, world, x, y, z, yaw, pitch, permission, category, icon, slot, description, teleport_delay, enabled, hidden, owner) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), " +
                    "yaw = VALUES(yaw), pitch = VALUES(pitch), permission = VALUES(permission), category = VALUES(category), " +
                    "icon = VALUES(icon), slot = VALUES(slot), description = VALUES(description), teleport_delay = VALUES(teleport_delay), " +
                    "enabled = VALUES(enabled), hidden = VALUES(hidden), owner = VALUES(owner)";
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String name : config.getConfigurationSection("warps").getKeys(false)) {
                String path = "warps." + name;
                ps.setString(1, name);
                ps.setString(2, config.getString(path + ".world", "world"));
                ps.setDouble(3, config.getDouble(path + ".x", 0));
                ps.setDouble(4, config.getDouble(path + ".y", 64));
                ps.setDouble(5, config.getDouble(path + ".z", 0));
                ps.setFloat(6, (float) config.getDouble(path + ".yaw", 0));
                ps.setFloat(7, (float) config.getDouble(path + ".pitch", 0));
                ps.setString(8, config.getString(path + ".permission", ""));
                ps.setString(9, config.getString(path + ".category", "default"));
                ps.setString(10, config.getString(path + ".icon", "ENDER_PEARL"));
                ps.setInt(11, config.getInt(path + ".slot", -1));
                ps.setString(12, config.getStringList(path + ".description").stream().reduce((a, b) -> a + "\n" + b).orElse(""));
                ps.setInt(13, config.getInt(path + ".teleport-delay", 5));
                ps.setBoolean(14, config.getBoolean(path + ".enabled", true));
                ps.setBoolean(15, config.getBoolean(path + ".hidden", false));
                ps.setString(16, config.getString(path + ".owner", null));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        LoggerUtil.info("Migrated warps to SQL.");
    }

    private void migrateWarpsSqlToYaml() {
        File warpsFile = new File(fileManager.getDataFolderStorage(), "warps.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(warpsFile);

        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM warps")) {

            while (rs.next()) {
                String name = rs.getString("name");
                String path = "warps." + name;
                config.set(path + ".world", rs.getString("world"));
                config.set(path + ".x", rs.getDouble("x"));
                config.set(path + ".y", rs.getDouble("y"));
                config.set(path + ".z", rs.getDouble("z"));
                config.set(path + ".yaw", rs.getFloat("yaw"));
                config.set(path + ".pitch", rs.getFloat("pitch"));
                config.set(path + ".permission", rs.getString("permission"));
                config.set(path + ".category", rs.getString("category"));
                config.set(path + ".icon", rs.getString("icon"));
                config.set(path + ".slot", rs.getInt("slot"));
                String desc = rs.getString("description");
                if (desc != null) {
                    config.set(path + ".description", Arrays.asList(desc.split("\n")));
                }
                config.set(path + ".teleport-delay", rs.getInt("teleport_delay"));
                config.set(path + ".enabled", rs.getBoolean("enabled"));
                config.set(path + ".hidden", rs.getBoolean("hidden"));
                config.set(path + ".owner", rs.getString("owner"));
            }
            config.save(warpsFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate warps to YAML", e);
        }
        LoggerUtil.info("Migrated warps to YAML.");
    }

    // ==================== Kits ====================

    private void migrateKitsYamlToSql() throws SQLException {
        File kitsFile = new File(fileManager.getDataFolderStorage(), "kits.yml");
        if (!kitsFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        if (!config.contains("kits")) return;

        String sql = isSqlite
                ? "INSERT INTO kits (name, cooldown, permission, items) VALUES (?, ?, ?, ?) ON CONFLICT(name) DO UPDATE SET cooldown = excluded.cooldown, permission = excluded.permission, items = excluded.items"
                : "INSERT INTO kits (name, cooldown, permission, items) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE cooldown = VALUES(cooldown), permission = VALUES(permission), items = VALUES(items)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String name : config.getConfigurationSection("kits").getKeys(false)) {
                String path = "kits." + name;
                ps.setString(1, name);
                ps.setInt(2, config.getInt(path + ".cooldown", 86400));
                ps.setString(3, config.getString(path + ".permission", "managerfix.kits.kit." + name.toLowerCase()));

                // Сериализуем предметы через Bukkit
                List<?> itemsList = config.getList(path + ".items");
                String itemsJson = itemsList != null ? serializeItemList(itemsList) : "[]";
                ps.setString(4, itemsJson);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        LoggerUtil.info("Migrated kits to SQL.");
    }

    /**
     * Сериализует список предметов в JSON (без Gson для ItemStack).
     */
    private String serializeItemList(List<?> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : items) {
            if (!first) sb.append(",");
            first = false;
            
            if (item instanceof org.bukkit.inventory.ItemStack itemStack) {
                // Сериализуем через Bukkit
                sb.append(itemStack.serializeAsBytes().toString());
            } else if (item instanceof Map) {
                // Уже сериализованный предмет
                sb.append(item.toString());
            } else {
                sb.append("{}");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void migrateKitsSqlToYaml() {
        File kitsFile = new File(fileManager.getDataFolderStorage(), "kits.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);

        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM kits")) {

            while (rs.next()) {
                String name = rs.getString("name");
                String path = "kits." + name;
                config.set(path + ".cooldown", rs.getInt("cooldown"));
                config.set(path + ".permission", rs.getString("permission"));

                String itemsJson = rs.getString("items");
                // При миграции SQL->YAML просто оставляем как есть
                // Kits будут загружены из YAML при старте
            }
            config.save(kitsFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate kits to YAML", e);
        }
        LoggerUtil.info("Migrated kits to YAML (metadata only).");
    }

    // ==================== Bans ====================

    private void migrateBansYamlToSql() throws SQLException {
        File bansFile = new File(fileManager.getDataFolderStorage(), "bans.yml");
        if (!bansFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(bansFile);
        if (!config.contains("bans")) return;

        String sql = isSqlite
                ? "INSERT INTO bans (uuid, name, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, reason = excluded.reason, source = excluded.source, created_at = excluded.created_at, expires_at = excluded.expires_at"
                : "INSERT INTO bans (uuid, name, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name), reason = VALUES(reason), source = VALUES(source), created_at = VALUES(created_at), expires_at = VALUES(expires_at)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String uuidStr : config.getConfigurationSection("bans").getKeys(false)) {
                String path = "bans." + uuidStr;
                ps.setString(1, uuidStr);
                ps.setString(2, config.getString(path + ".name", "Unknown"));
                ps.setString(3, config.getString(path + ".reason", "No reason"));
                ps.setString(4, config.getString(path + ".source", "Console"));
                ps.setLong(5, config.getLong(path + ".created-at", System.currentTimeMillis()));
                ps.setLong(6, config.getLong(path + ".expires-at", -1));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        LoggerUtil.info("Migrated bans to SQL.");
    }

    private void migrateBansSqlToYaml() {
        File bansFile = new File(fileManager.getDataFolderStorage(), "bans.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(bansFile);

        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM bans")) {

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String path = "bans." + uuid;
                config.set(path + ".name", rs.getString("name"));
                config.set(path + ".reason", rs.getString("reason"));
                config.set(path + ".source", rs.getString("source"));
                config.set(path + ".created-at", rs.getLong("created_at"));
                config.set(path + ".expires-at", rs.getLong("expires_at"));
            }
            config.save(bansFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate bans to YAML", e);
        }
        LoggerUtil.info("Migrated bans to YAML.");
    }

    // ==================== Mutes ====================

    private void migrateMutesYamlToSql() throws SQLException {
        File mutesFile = new File(fileManager.getDataFolderStorage(), "mutes.yml");
        if (!mutesFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(mutesFile);
        if (!config.contains("mutes")) return;

        String sql = isSqlite
                ? "INSERT INTO mutes (uuid, name, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, reason = excluded.reason, source = excluded.source, created_at = excluded.created_at, expires_at = excluded.expires_at"
                : "INSERT INTO mutes (uuid, name, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name), reason = VALUES(reason), source = VALUES(source), created_at = VALUES(created_at), expires_at = VALUES(expires_at)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String uuidStr : config.getConfigurationSection("mutes").getKeys(false)) {
                String path = "mutes." + uuidStr;
                ps.setString(1, uuidStr);
                ps.setString(2, config.getString(path + ".name", "Unknown"));
                ps.setString(3, config.getString(path + ".reason", "No reason"));
                ps.setString(4, config.getString(path + ".source", "Console"));
                ps.setLong(5, config.getLong(path + ".created-at", System.currentTimeMillis()));
                ps.setLong(6, config.getLong(path + ".expires-at", -1));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        LoggerUtil.info("Migrated mutes to SQL.");
    }

    private void migrateMutesSqlToYaml() {
        File mutesFile = new File(fileManager.getDataFolderStorage(), "mutes.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(mutesFile);

        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM mutes")) {

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String path = "mutes." + uuid;
                config.set(path + ".name", rs.getString("name"));
                config.set(path + ".reason", rs.getString("reason"));
                config.set(path + ".source", rs.getString("source"));
                config.set(path + ".created-at", rs.getLong("created_at"));
                config.set(path + ".expires-at", rs.getLong("expires_at"));
            }
            config.save(mutesFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate mutes to YAML", e);
        }
        LoggerUtil.info("Migrated mutes to YAML.");
    }

    // ==================== Items ====================

    private void migrateItemsYamlToSql() throws SQLException {
        File itemsFile = new File(fileManager.getDataFolderStorage(), "items.yml");
        if (!itemsFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        if (!config.contains("saved")) return;

        String sql = isSqlite
                ? "INSERT INTO saved_items (name, data) VALUES (?, ?) ON CONFLICT(name) DO UPDATE SET data = excluded.data"
                : "INSERT INTO saved_items (name, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data = VALUES(data)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String name : config.getConfigurationSection("saved").getKeys(false)) {
                String path = "saved." + name;
                // Сериализуем через YAML
                org.bukkit.configuration.file.YamlConfiguration yamlData = 
                    new org.bukkit.configuration.file.YamlConfiguration();
                for (String key : config.getConfigurationSection(path).getKeys(false)) {
                    yamlData.set(key, config.get(path + "." + key));
                }
                String yaml = yamlData.saveToString();
                
                ps.setString(1, name);
                ps.setString(2, yaml);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        LoggerUtil.info("Migrated items to SQL.");
    }

    @SuppressWarnings("unchecked")
    private void migrateItemsSqlToYaml() {
        File itemsFile = new File(fileManager.getDataFolderStorage(), "items.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);

        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM saved_items")) {

            while (rs.next()) {
                String name = rs.getString("name");
                String dataStr = rs.getString("data");

                if (dataStr != null && !dataStr.isEmpty()) {
                    try {
                        // Загружаем YAML из строки
                        org.bukkit.configuration.file.YamlConfiguration yamlData = 
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                                new java.io.StringReader(dataStr));
                        
                        for (String key : yamlData.getKeys(false)) {
                            config.set("saved." + name + "." + key, yamlData.get(key));
                        }
                    } catch (Exception e) {
                        LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to deserialize item", e);
                    }
                }
            }
            config.save(itemsFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate items to YAML", e);
        }
        LoggerUtil.info("Migrated items to YAML.");
    }

    // ==================== Profiles ====================

    public void migrateProfilesYamlToSql() {
        // Миграция профилей из data/players/<uuid>.yml
        File playersFolder = new File(fileManager.getDataFolderStorage(), "players");
        if (!playersFolder.exists()) {
            LoggerUtil.info("Players folder does not exist: " + playersFolder.getAbsolutePath());
            return;
        }
        
        File[] yamlFiles = playersFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) {
            LoggerUtil.info("No player profiles found in: " + playersFolder.getAbsolutePath());
            return;
        }
        
        LoggerUtil.info("Found " + yamlFiles.length + " player profile(s) to migrate.");

        String sql = isSqlite
                ? "INSERT INTO profiles (uuid, last_activity, metadata, cooldowns) VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET last_activity = excluded.last_activity, metadata = excluded.metadata, cooldowns = excluded.cooldowns"
                : "INSERT INTO profiles (uuid, last_activity, metadata, cooldowns) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_activity = VALUES(last_activity), metadata = VALUES(metadata), cooldowns = VALUES(cooldowns)";
        String homeSql = isSqlite
                ? "INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(uuid, name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch"
                : "INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)";

        int migrated = 0;
        int homesMigrated = 0;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             PreparedStatement homePs = conn.prepareStatement(homeSql)) {

            for (File playerFile : Objects.requireNonNull(playersFolder.listFiles((d, name) -> name.endsWith(".yml")))) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                    String uuidStr = config.getString("uuid");
                    if (uuidStr == null) {
                        // Пытаемся получить UUID из имени файла
                        String fileName = playerFile.getName().replace(".yml", "");
                        try {
                            UUID.fromString(fileName);
                            uuidStr = fileName;
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                    }

                    UUID uuid = UUID.fromString(uuidStr);
                    long lastActivity = config.getLong("lastActivity", System.currentTimeMillis());
                    
                    // Metadata
                    Map<?, ?> metadata = config.getMapList("metadata").stream()
                        .collect(java.util.stream.Collectors.toMap(
                            m -> String.valueOf(((Map<?, ?>)m).get("key")),
                            m -> ((Map<?, ?>)m).get("value")
                        ));
                    String metadataJson = metadata.isEmpty() ? null : new com.google.gson.Gson().toJson(metadata);

                    // Cooldowns
                    List<?> cooldowns = config.getList("cooldowns");
                    String cooldownsJson = cooldowns != null ? new com.google.gson.Gson().toJson(cooldowns) : null;

                    ps.setString(1, uuidStr);
                    ps.setLong(2, lastActivity);
                    ps.setString(3, metadataJson);
                    ps.setString(4, cooldownsJson);
                    ps.addBatch();
                    migrated++;

                    // Homes
                    ConfigurationSection homesSection = config.getConfigurationSection("homes");
                    Map<?, ?> homes = null;
                    
                    if (homesSection != null) {
                        homes = homesSection.getValues(false);
                    }
                    
                    // Пробуем разные форматы хранения homes
                    if (homes == null && config.contains("homes")) {
                        // Homes могут храниться как Map<String, String>
                        try {
                            ConfigurationSection hs = config.getConfigurationSection("homes");
                            if (hs != null) {
                                homes = hs.getValues(false);
                            }
                        } catch (Exception e) {
                            // Игнорируем, если нет homes
                        }
                    }
                    
                    if (homes != null && !homes.isEmpty()) {
                        for (Map.Entry<?, ?> entry : homes.entrySet()) {
                            String homeName = String.valueOf(entry.getKey());
                            Object homeValue = entry.getValue();
                            String homeData = homeValue != null ? String.valueOf(homeValue) : null;
                            
                            if (homeData == null) continue;
                            
                            // Парсим location string: world,x,y,z,yaw,pitch (через запятую!)
                            String[] parts = homeData.split(",");
                            if (parts.length >= 4) {
                                homePs.setString(1, uuidStr);
                                homePs.setString(2, homeName);
                                homePs.setString(3, parts[0].trim()); // world
                                try {
                                    homePs.setDouble(4, Double.parseDouble(parts[1].trim())); // x
                                    homePs.setDouble(5, Double.parseDouble(parts[2].trim())); // y
                                    homePs.setDouble(6, Double.parseDouble(parts[3].trim())); // z
                                    homePs.setFloat(7, parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0f); // yaw
                                    homePs.setFloat(8, parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0f); // pitch
                                    homePs.addBatch();
                                    homesMigrated++;
                                } catch (NumberFormatException e) {
                                    LoggerUtil.log(java.util.logging.Level.WARNING, 
                                        "Failed to parse home location: " + homeData + " for " + uuidStr, e);
                                }
                            } else {
                                LoggerUtil.log(java.util.logging.Level.WARNING, 
                                    "Invalid home format (expected world,x,y,z): " + homeData, null);
                            }
                        }
                    }

                } catch (Exception e) {
                    LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate profile: " + playerFile.getName(), e);
                }
            }

            ps.executeBatch();
            homePs.executeBatch();

        } catch (SQLException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Error migrating profiles", e);
        }

        LoggerUtil.info("Migrated " + migrated + " profiles and " + homesMigrated + " homes to SQL.");
    }

    public void migrateProfilesSqlToYaml() {
        // При миграции на YAML профили будут сохраняться в data/players/
        LoggerUtil.info("Profiles will be saved to YAML on player logout.");
    }

    // ==================== Spawn ====================

    private void migrateSpawnYamlToSql() {
        File spawnConfigFile = new File(fileManager.getDataFolderStorage().getParentFile(), "modules/spawn/config.yml");
        if (!spawnConfigFile.exists()) {
            spawnConfigFile = new File(fileManager.getDataFolderStorage(), "modules/spawn/config.yml");
        }
        if (!spawnConfigFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(spawnConfigFile);
        
        // Проверяем, есть ли координаты спавна
        if (!config.contains("spawn-location")) return;

        String world = config.getString("spawn-location.world", "world");
        double x = config.getDouble("spawn-location.x", 0);
        double y = config.getDouble("spawn-location.y", 64);
        double z = config.getDouble("spawn-location.z", 0);
        float yaw = (float) config.getDouble("spawn-location.yaw", 0);
        float pitch = (float) config.getDouble("spawn-location.pitch", 0);

        // Сохраняем в таблицу spawn (если существует) или в profiles как metadata
        // Для простоты сохраняем в специальную таблицу или оставляем в config
        LoggerUtil.info("Spawn location found: " + world + "," + x + "," + y + "," + z);
        // Spawn остаётся в config.yml (не мигрируется в БД)
    }

    private void migrateSpawnSqlToYaml() {
        // Spawn не мигрируется, остаётся в config.yml
    }

    // ==================== TPA Data ====================

    private void migrateTpaDataYamlToSql() throws SQLException {
        File tpaDataFile = new File(fileManager.getDataFolderStorage(), "tpa-data.yml");
        if (!tpaDataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(tpaDataFile);
        if (config.getKeys(false).isEmpty()) return;

        String insertToggle = isSqlite
                ? "INSERT INTO tpa_data (uuid, enabled) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET enabled = excluded.enabled"
                : "INSERT INTO tpa_data (uuid, enabled) VALUES (?, ?) ON DUPLICATE KEY UPDATE enabled = VALUES(enabled)";
        String insertBlacklist = isSqlite
                ? "INSERT INTO tpa_blacklist (owner_uuid, blacklisted_uuid) VALUES (?, ?) ON CONFLICT(owner_uuid, blacklisted_uuid) DO UPDATE SET blacklisted_uuid = excluded.blacklisted_uuid"
                : "INSERT INTO tpa_blacklist (owner_uuid, blacklisted_uuid) VALUES (?, ?) ON DUPLICATE KEY UPDATE blacklisted_uuid = VALUES(blacklisted_uuid)";

        try (Connection conn = databaseManager.getConnection()) {
            try (PreparedStatement psToggle = conn.prepareStatement(insertToggle);
                 PreparedStatement psBlacklist = conn.prepareStatement(insertBlacklist)) {

                for (String uuidStr : config.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        
                        // Миграция toggle
                        if (config.contains(uuidStr + ".toggle")) {
                            boolean enabled = config.getBoolean(uuidStr + ".toggle", true);
                            psToggle.setString(1, uuidStr);
                            psToggle.setBoolean(2, enabled);
                            psToggle.addBatch();
                        }

                        // Миграция blacklist
                        if (config.contains(uuidStr + ".blacklist")) {
                            List<String> blacklist = config.getStringList(uuidStr + ".blacklist");
                            for (String blUuid : blacklist) {
                                try {
                                    UUID.fromString(blUuid); // Validate UUID
                                    psBlacklist.setString(1, uuidStr);
                                    psBlacklist.setString(2, blUuid);
                                    psBlacklist.addBatch();
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                psToggle.executeBatch();
                psBlacklist.executeBatch();
            }
        }
        LoggerUtil.info("Migrated TPA data to SQL.");
    }

    private void migrateTpaDataSqlToYaml() {
        File tpaDataFile = new File(fileManager.getDataFolderStorage(), "tpa-data.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(tpaDataFile);

        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement()) {

            // Загружаем toggle
            try (ResultSet rs = st.executeQuery("SELECT uuid, enabled FROM tpa_data")) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    boolean enabled = rs.getBoolean("enabled");
                    config.set(uuid + ".toggle", enabled);
                }
            }

            // Загружаем blacklist
            try (ResultSet rs = st.executeQuery("SELECT owner_uuid, blacklisted_uuid FROM tpa_blacklist")) {
                while (rs.next()) {
                    String owner = rs.getString("owner_uuid");
                    String blacklisted = rs.getString("blacklisted_uuid");
                    List<String> blacklist = config.getStringList(owner + ".blacklist");
                    if (!blacklist.contains(blacklisted)) {
                        blacklist.add(blacklisted);
                        config.set(owner + ".blacklist", blacklist);
                    }
                }
            }

            config.save(tpaDataFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate TPA data to YAML", e);
        }
        LoggerUtil.info("Migrated TPA data to YAML.");
    }

    // ==================== Ban History ====================

    private void migrateBanHistoryYamlToSql() throws SQLException {
        File banHistoryFile = new File(fileManager.getDataFolderStorage(), "ban_history.yml");
        if (!banHistoryFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(banHistoryFile);
        if (!config.contains("entries")) return;

        String sql = "INSERT INTO ban_history (uuid, name, action, reason, source, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            List<Map<?, ?>> entries = config.getMapList("entries");
            for (Map<?, ?> entry : entries) {
                try {
                    String uuidStr = Objects.toString(entry.get("uuid"), null);
                    if (uuidStr == null) continue;
                    
                    ps.setString(1, uuidStr);
                    ps.setString(2, Objects.toString(entry.get("name"), "Unknown"));
                    ps.setString(3, Objects.toString(entry.get("action"), "BAN"));
                    ps.setString(4, Objects.toString(entry.get("reason"), ""));
                    ps.setString(5, Objects.toString(entry.get("source"), "Console"));
                    ps.setLong(6, toLong(entry.get("createdAt")));
                    ps.setLong(7, toLong(entry.get("expiresAt")));
                    ps.addBatch();
                } catch (Exception ignored) {
                }
            }
            ps.executeBatch();
        }
        LoggerUtil.info("Migrated ban history to SQL.");
    }

    private void migrateBanHistorySqlToYaml() {
        File banHistoryFile = new File(fileManager.getDataFolderStorage(), "ban_history.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(banHistoryFile);

        try (Connection conn = databaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ban_history")) {

            List<Map<String, Object>> entries = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("uuid", rs.getString("uuid"));
                entry.put("name", rs.getString("name"));
                entry.put("action", rs.getString("action"));
                entry.put("reason", rs.getString("reason"));
                entry.put("source", rs.getString("source"));
                entry.put("createdAt", rs.getLong("created_at"));
                entry.put("expiresAt", rs.getLong("expires_at"));
                entries.add(entry);
            }
            config.set("entries", entries);
            config.save(banHistoryFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to migrate ban history to YAML", e);
        }
        LoggerUtil.info("Migrated ban history to YAML.");
    }

    private static long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception ignored) { return 0L; }
    }

    // ==================== Storage Type Tracking ====================

    private String readLastStorageType() {
        File file = new File(plugin.getDataFolder(), "data/last_storage_type.txt");
        if (!file.exists()) return null;
        try {
            return java.nio.file.Files.readString(file.toPath()).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeLastStorageType(String type) {
        File file = new File(plugin.getDataFolder(), "data/last_storage_type.txt");
        file.getParentFile().mkdirs();
        try {
            java.nio.file.Files.writeString(file.toPath(), type);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to write storage type", e);
        }
    }
}
