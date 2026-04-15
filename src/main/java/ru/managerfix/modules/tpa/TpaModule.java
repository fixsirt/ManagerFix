package ru.managerfix.modules.tpa;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;

/**
 * TPA module: /tpa, /tpahere, /tpaccept, /tpadeny, /tpatoggle, /tpablacklist, GUI, delay, animation.
 */
public final class TpaModule extends AbstractModule {

    private static final String MODULE_NAME = "tpa";
    private static final String CONFIG_FILE = "tpa/config.yml";

    private FileConfiguration moduleConfig;
    private TpaConfig tpaConfig;
    private TpaService tpaService;
    private TpaListener tpaListener;
    private TpaGui tpaGui;
    private TpaRequestsGui tpaRequestsGui;

    public TpaModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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

        tpaConfig = new TpaConfig(moduleConfig);
        ru.managerfix.scheduler.TaskScheduler scheduler = null;
        if (plugin instanceof ManagerFix mf) {
            scheduler = mf.getScheduler();
        }
        if (scheduler == null) {
            scheduler = new ru.managerfix.scheduler.TaskScheduler(plugin);
        }
        
        // Получаем SqlTpaStorage если используется MySQL
        ru.managerfix.storage.SqlTpaStorage sqlTpaStorage = null;
        if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
            sqlTpaStorage = mf.getSqlTpaStorage();
        }
        
        tpaService = new TpaService(plugin, tpaConfig, scheduler, sqlTpaStorage);
        tpaListener = new TpaListener(plugin, tpaService);
        plugin.getServer().getPluginManager().registerEvents(tpaListener, plugin);

        if (plugin instanceof ManagerFix mf) {
            tpaGui = new TpaGui(mf, mf.getGuiManager(), tpaService);
            tpaRequestsGui = new TpaRequestsGui(mf, mf.getGuiManager(), tpaService, tpaGui);
            TpaCommand tpaCommand = new TpaCommand(mf, tpaService, mf.getGuiManager(), tpaGui, tpaRequestsGui);
            mf.getCommandManager().register("tpa", tpaCommand, tpaCommand);
            mf.getCommandManager().register("tpahere", tpaCommand, tpaCommand);
            mf.getCommandManager().register("tpaccept", tpaCommand, tpaCommand);
            mf.getCommandManager().register("tpdeny", tpaCommand, tpaCommand);
            mf.getCommandManager().register("tpadeny", tpaCommand, tpaCommand);
            mf.getCommandManager().register("tpatoggle", tpaCommand, tpaCommand);
            mf.getCommandManager().register("tpablacklist", tpaCommand, tpaCommand);
            mf.getCommandManager().register("tpareply", tpaCommand, tpaCommand);
        }
        LoggerUtil.debug("TPA module enabled.");
    }

    @Override
    protected void disable() {
        if (tpaListener != null) {
            tpaListener.stop();
            tpaListener = null;
        }
        if (tpaService != null) {
            tpaService.cleanup();
            tpaService = null;
        }
        tpaConfig = null;
        moduleConfig = null;
        tpaGui = null;
        tpaRequestsGui = null;
        LoggerUtil.debug("TPA module disabled.");
    }

    @Override
    public void clearData() {
        if (tpaService != null) {
            tpaService.cleanup();
        }
    }

    public TpaService getTpaService() {
        return tpaService;
    }

    public TpaConfig getTpaConfig() {
        return tpaConfig;
    }
}
