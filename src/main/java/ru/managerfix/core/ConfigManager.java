package ru.managerfix.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages main config and module enable/disable state from config.yml.
 */
public final class ConfigManager {

    private final JavaPlugin plugin;
    private final FileManager fileManager;
    private FileConfiguration mainConfig;
    private final Map<String, Boolean> moduleStates = new HashMap<>();

    private static final String MODULES_PATH = "modules";

    public ConfigManager(JavaPlugin plugin, FileManager fileManager) {
        this.plugin = plugin;
        this.fileManager = fileManager;
    }

    /**
     * Loads main config and creates default if missing.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        loadModuleStates();
    }

    private void loadModuleStates() {
        moduleStates.clear();
        if (mainConfig.contains(MODULES_PATH) && mainConfig.isConfigurationSection(MODULES_PATH)) {
            for (String key : mainConfig.getConfigurationSection(MODULES_PATH).getKeys(false)) {
                moduleStates.put(key.toLowerCase(), mainConfig.getBoolean(MODULES_PATH + "." + key, true));
            }
        }
    }

    /**
     * Returns whether the module with given name is enabled in config.
     */
    public boolean isModuleEnabled(String moduleName) {
        return moduleStates.getOrDefault(moduleName.toLowerCase(), false);
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public FileConfiguration getModuleConfig(String moduleFileName) {
        return fileManager.getModuleConfig(moduleFileName);
    }

    public void saveModuleConfig(String moduleFileName, FileConfiguration config) {
        fileManager.saveModuleConfig(moduleFileName, config);
    }

    public String getDefaultLanguage() {
        return mainConfig.getString("settings.default-language", "ru");
    }

    public boolean isDebug() {
        return mainConfig.getBoolean("settings.debug", false);
    }

    public int getProfileAutosaveMinutes() {
        return mainConfig.getInt("settings.profile-autosave-minutes", 10);
    }

    public int getWarpCooldownSeconds() {
        return mainConfig.getInt("settings.warp-cooldown-seconds", 5);
    }

    public int getGuiAnimationTicks() {
        return mainConfig.getInt("settings.gui-animation-ticks", 20);
    }

    /**
     * Returns storage type: YAML or MYSQL.
     */
    public String getStorageType() {
        return mainConfig.getString("storage.type", "YAML").toUpperCase();
    }

    /**
     * Sets storage type (YAML or MYSQL) in memory.
     */
    public void setStorageType(String type) {
        mainConfig.set("storage.type", type.toUpperCase());
        plugin.saveConfig();
    }

    /**
     * Returns whether MySQL storage is configured.
     */
    public boolean isMySqlStorage() {
        return "MYSQL".equals(getStorageType());
    }

    /**
     * Sets module enabled state in config and saves config.yml.
     */
    public void setModuleEnabled(String moduleName, boolean enabled) {
        moduleName = moduleName.toLowerCase();
        mainConfig.set(MODULES_PATH + "." + moduleName, enabled);
        moduleStates.put(moduleName, enabled);
        plugin.saveConfig();
    }

    /**
     * Returns whether cluster (Redis) is enabled.
     */
    public boolean isClusterEnabled() {
        return mainConfig.getBoolean("cluster.enabled", false);
    }

    /**
     * Returns server ID for cluster; messages from this server are ignored when received.
     */
    public String getServerId() {
        return mainConfig.getString("cluster.server-id", "server-1");
    }

    public String getClusterRedisHost() {
        return mainConfig.getString("cluster.redis.host", "localhost");
    }

    public int getClusterRedisPort() {
        return mainConfig.getInt("cluster.redis.port", 6379);
    }

    public String getClusterRedisPassword() {
        return mainConfig.getString("cluster.redis.password", "");
    }

    public String getClusterChannelPrefix() {
        return mainConfig.getString("cluster.redis.channel-prefix", "managerfix");
    }

    /**
     * Reloads main config and module states (used by /managerfix reload).
     */
    public void reload() {
        load();
    }
}
