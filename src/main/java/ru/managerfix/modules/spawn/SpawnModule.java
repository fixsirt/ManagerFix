package ru.managerfix.modules.spawn;

import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

public class SpawnModule extends AbstractModule {

    private static final String MODULE_NAME = "spawn";
    
    private SpawnConfig spawnConfig;
    private SpawnService spawnService;
    private SpawnEditGui spawnEditGui;

    public SpawnModule(ManagerFix plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        super(plugin, configManager, serviceRegistry);
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    protected void enable() {
        LoggerUtil.debug("[SpawnModule] Enabling...");
        
        // Инициализация команд
        initCommandConfig(MODULE_NAME);
        
        this.spawnConfig = new SpawnConfig(plugin);
        this.spawnService = new SpawnService(plugin, spawnConfig);
        this.spawnEditGui = new SpawnEditGui(spawnConfig);

        SpawnCommand spawnCommand = new SpawnCommand(spawnService, spawnConfig, spawnEditGui);
        
        if (plugin instanceof ManagerFix mf) {
            LoggerUtil.debug("[SpawnModule] Registering commands...");
            mf.getCommandManager().register("spawn", spawnCommand, spawnCommand);
            mf.getCommandManager().register("setspawn", spawnCommand, spawnCommand);
            mf.getCommandManager().register("editspawn", spawnCommand, spawnCommand);
        } else {
            plugin.getLogger().severe("[SpawnModule] plugin is NOT an instance of ManagerFix!");
        }

        plugin.getServer().getPluginManager().registerEvents(new SpawnListener(spawnConfig, spawnService), plugin);

        LoggerUtil.debug("Spawn module enabled.");
    }

    @Override
    protected void disable() {
        LoggerUtil.debug("Spawn module disabled.");
    }

    @Override
    protected void reload() {
        if (spawnConfig != null) {
            spawnConfig.reload();
        }
    }
}
