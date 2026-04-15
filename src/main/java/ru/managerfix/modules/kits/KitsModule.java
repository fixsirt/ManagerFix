package ru.managerfix.modules.kits;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.KitsCommand;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;
import java.util.Optional;

/**
 * Kits module: /kit [name], /kits (GUI), /kit create <name> (admin). Cooldowns, per-kit permissions.
 */
public final class KitsModule extends AbstractModule {

    private static final String MODULE_NAME = "kits";
    private static final String CONFIG_FILE = "kits/config.yml";

    private FileConfiguration moduleConfig;
    private KitStorage storage;
    private KitManager kitManager;

    public KitsModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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
        moduleConfig = configManager.getModuleConfig(CONFIG_FILE);

        // Инициализация команд
        initCommandConfig(MODULE_NAME);

        // Использовать SQL или YAML хранилище в зависимости от конфигурации
        if (plugin instanceof ManagerFix mf && (mf.isMySqlStorage() || mf.isSqliteStorage())) {
            storage = mf.getSqlKitStorage();
        } else {
            storage = new YamlKitStorage(plugin);
        }
        storage.init();
        
        ProfileManager profileManager = plugin instanceof ManagerFix mf ? mf.getProfileManager() : null;
        if (profileManager != null) {
            kitManager = new KitManager(storage, profileManager, plugin);
            LoggerUtil.debug("[Kits] Calling reload()...");
            kitManager.reload();
            List<String> names = kitManager.getKitNames();
            LoggerUtil.info("[Kits] Loaded " + names.size() + " kit(s) from storage: " + names);
        } else {
            plugin.getLogger().severe("[Kits] ProfileManager is null!");
        }
        if (plugin instanceof ManagerFix mf) {
            KitsCommand kitsCommand = new KitsCommand(mf, mf.getGuiManager());
            EditKitsCommand editKitsCommand = new EditKitsCommand(mf, this);
            
            mf.getCommandManager().register("kit", kitsCommand, kitsCommand);
            mf.getCommandManager().register("kits", kitsCommand, kitsCommand);
            mf.getCommandManager().register("editkits", editKitsCommand, editKitsCommand);
        }
        LoggerUtil.debug("Kits module enabled.");
    }

    @Override
    protected void disable() {
        if (storage != null) {
            storage.shutdown();
        }
        storage = null;
        kitManager = null;
        moduleConfig = null;
        LoggerUtil.debug("Kits module disabled.");
    }

    public int getDefaultCooldownSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("default-cooldown", 86400) : 86400;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    @Override
    public Optional<String> getAdminStats() {
        if (kitManager == null) return Optional.empty();
        return Optional.of("Kits: " + kitManager.getKitNames().size());
    }
}
