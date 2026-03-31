package ru.managerfix.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Manages plugin data folder structure and file operations.
 * Ensures plugins/ManagerFix/, modules/, data/, lang/ exist on startup.
 */
public final class FileManager {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final File modulesFolder;
    private final File dataFolderStorage;
    private final File playersFolder;
    private final File langFolder;

    public FileManager(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        this.dataFolder = plugin.getDataFolder();
        this.modulesFolder = new File(dataFolder, "modules");
        this.dataFolderStorage = new File(dataFolder, "data");
        this.playersFolder = new File(dataFolderStorage, "players");
        this.langFolder = new File(dataFolder, "lang");
    }

    /**
     * Creates plugin folder structure if it does not exist.
     */
    public void setupFolders() {
        createFolderIfNotExists(dataFolder);
        createFolderIfNotExists(modulesFolder);
        createFolderIfNotExists(dataFolderStorage);
        createFolderIfNotExists(playersFolder);
        createFolderIfNotExists(langFolder);
    }

    private void createFolderIfNotExists(File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            LoggerUtil.severe("Could not create folder: " + folder.getPath());
        }
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public File getModulesFolder() {
        return modulesFolder;
    }

    public File getDataFolderStorage() {
        return dataFolderStorage;
    }

    public File getPlayersFolder() {
        return playersFolder;
    }

    public File getLangFolder() {
        return langFolder;
    }

    /**
     * Gets or creates a module config file (e.g. modules/warps/config.yml).
     */
    public FileConfiguration getModuleConfig(String fileName) {
        File file;
        String resourcePath;

        // Поддержка путей вида "warps/config.yml" или "warps.yml"
        if (fileName.contains("/")) {
            file = new File(modulesFolder, fileName);
            resourcePath = "modules/" + fileName;
        } else {
            // Для обратной совместимости: warps.yml -> modules/warps/config.yml
            String moduleName = fileName.replace(".yml", "");
            file = new File(modulesFolder, moduleName + "/config.yml");
            resourcePath = "modules/" + moduleName + "/config.yml";
        }

        if (!file.exists()) {
            // Создаём родительскую папку если нужно
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            LoggerUtil.debug("Saving resource: " + resourcePath + " to " + file.getPath());
            plugin.saveResource(resourcePath, false);
            if (!file.exists()) {
                LoggerUtil.warning("Failed to save resource: " + resourcePath);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Saves a config to a file in modules folder.
     */
    public void saveModuleConfig(String fileName, FileConfiguration config) {
        File file;
        
        // Поддержка путей вида "warps/config.yml" или "warps.yml"
        if (fileName.contains("/")) {
            file = new File(modulesFolder, fileName);
        } else {
            String moduleName = fileName.replace(".yml", "");
            file = new File(modulesFolder, moduleName + "/config.yml");
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save " + fileName, e);
        }
    }

    /**
     * Gets a file from data/ folder (for custom data storage).
     */
    public File getDataFile(String fileName) {
        return new File(dataFolderStorage, fileName);
    }

    /**
     * Gets or creates a lang file (e.g. lang/ru.yml).
     */
    public FileConfiguration getLangFile(String locale) {
        File file = new File(langFolder, locale + ".yml");
        if (!file.exists()) {
            // Создаём родительскую папку если нужно
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            plugin.saveResource("lang/" + locale + ".yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
