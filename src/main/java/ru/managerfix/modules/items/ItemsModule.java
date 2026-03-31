package ru.managerfix.modules.items;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.FileManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

import java.io.File;
import java.util.List;

/**
 * Модуль Items: команда /i для манипуляции предметами.
 * Поддерживает name, lore, amount, enchant, attribute, save, give, reload.
 * Конфиг: modules/items/config.yml, Данные: data/items.yml
 */
public final class ItemsModule extends AbstractModule {

    private static final String MODULE_NAME = "items";
    private static final String CONFIG_FILE = "items/config.yml";

    private FileConfiguration moduleConfig;
    private FileConfiguration savedConfig;
    private File savedFile;

    public ItemsModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        super(plugin, configManager, serviceRegistry);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    protected void enable() {
        LoggerUtil.debug("Модуль Items включён.");
        
        // Инициализация команд
        initCommandConfig(MODULE_NAME);
        
        moduleConfig = configManager.getModuleConfig(CONFIG_FILE);

        // Использовать SQL или YAML хранилище в зависимости от конфигурации
        if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
            // SQL хранилище будет инициализировано в ManagerFix
            savedConfig = null;
            savedFile = null;
        } else {
            // Загружаем data/items.yml для сохранённых предметов
            FileManager fm = plugin instanceof ManagerFix
                    ? ((ManagerFix) plugin).getFileManager()
                    : null;
            if (fm != null) {
                savedFile = new File(fm.getDataFolderStorage(), "items.yml");
            } else {
                savedFile = new File(plugin.getDataFolder(), "data/items.yml");
            }
            savedFile.getParentFile().mkdirs();
            if (!savedFile.exists()) {
                try {
                    savedFile.createNewFile();
                } catch (Exception e) {
                    LoggerUtil.log(java.util.logging.Level.WARNING, "Could not create items.yml", e);
                }
            }
            savedConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(savedFile);
        }

        if (moduleConfig == null) {
            LoggerUtil.debug("items/config.yml не загружен — используются значения по умолчанию");
        }

        if (plugin instanceof ManagerFix mf) {
            ItemsCommand itemsCommand = new ItemsCommand(mf, this);
            org.bukkit.command.PluginCommand cmd = plugin.getCommand("i");
            if (cmd != null) {
                cmd.setExecutor(itemsCommand);
                cmd.setTabCompleter(itemsCommand);
                LoggerUtil.debug("Команда /i зарегистрирована.");
            } else {
                LoggerUtil.debug("Команда /i не найдена в plugin.yml.");
            }
        }
    }

    @Override
    protected void disable() {
        saveSavedConfig();
        LoggerUtil.debug("Модуль Items выключен.");
        moduleConfig = null;
        savedConfig = null;
        savedFile = null;
    }

    public FileConfiguration getModuleConfig() {
        return moduleConfig;
    }

    public FileConfiguration getSavedConfig() {
        return savedConfig;
    }

    public void saveConfig() {
        if (moduleConfig != null) {
            configManager.saveModuleConfig(CONFIG_FILE, moduleConfig);
        }
    }

    public void saveSavedConfig() {
        if (savedConfig != null && savedFile != null) {
            try {
                savedConfig.save(savedFile);
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save items.yml", e);
            }
        }
        // Для SQL сохранение происходит асинхронно в SqlItemsStorage
    }

    public void reloadConfig() {
        moduleConfig = configManager.getModuleConfig(CONFIG_FILE);
        if (savedFile != null) {
            savedConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(savedFile);
        }
    }

    public boolean isSqlStorage() {
        return plugin instanceof ManagerFix mf && mf.isMySqlStorage();
    }
}