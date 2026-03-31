package ru.managerfix.modules.warps;

import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;

/**
 * Modern Warps module for Paper 1.21.4.
 */
public final class WarpsModule extends AbstractModule {

    private static final String MODULE_NAME = "warps";

    private WarpConfig warpConfig;
    private WarpsDataStorage warpsDataStorage;
    private WarpService warpService;
    private WarpGui warpGui;
    private WarpEditGui warpEditGui;

    public WarpsModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        super(plugin, configManager, serviceRegistry);
    }

    @Override
    public String getName() {
        return "warps";
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    protected void enable() {
        LoggerUtil.debug("[WarpsModule] Enabling modern warps...");

        // Инициализация команд
        initCommandConfig(MODULE_NAME);

        this.warpConfig = new WarpConfig(plugin);
        this.warpsDataStorage = new WarpsDataStorage(plugin);
        this.warpsDataStorage.init();
        this.warpService = new WarpService(plugin, warpConfig, warpsDataStorage);
        this.warpEditGui = new WarpEditGui(warpsDataStorage);
        this.warpGui = new WarpGui(warpConfig, warpService, warpEditGui, warpsDataStorage);

        if (plugin instanceof ManagerFix mf) {
            WarpCommand warpCommand = new WarpCommand(mf, warpConfig, warpsDataStorage, warpEditGui, warpService);
            mf.getCommandManager().register("warp", warpCommand, warpCommand);
            mf.getCommandManager().register("warps", warpCommand, warpCommand);
            mf.getCommandManager().register("setwarp", warpCommand, warpCommand);
            mf.getCommandManager().register("delwarp", warpCommand, warpCommand);
            mf.getCommandManager().register("editwarp", warpCommand, warpCommand);
        }

        LoggerUtil.debug("Warps module (modern) enabled.");
    }

    public WarpConfig getConfig() {
        return warpConfig;
    }

    public WarpsDataStorage getDataStorage() {
        return warpsDataStorage;
    }

    @Override
    protected void disable() {
        if (warpsDataStorage != null) {
            warpsDataStorage.shutdown();
        }
        LoggerUtil.debug("Warps module disabled.");
    }

    @Override
    protected void reload() {
        if (warpConfig != null) {
            warpConfig.reload();
        }
        if (warpsDataStorage != null) {
            warpsDataStorage.init();
        }
    }
}
