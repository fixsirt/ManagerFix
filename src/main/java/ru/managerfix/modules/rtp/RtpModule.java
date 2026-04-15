package ru.managerfix.modules.rtp;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;

/**
 * RTP module: /rtp — async safe random location, cooldown, loading GUI.
 */
public final class RtpModule extends AbstractModule {

    private static final String MODULE_NAME = "rtp";
    private static final String CONFIG_FILE = "rtp/config.yml";

    private FileConfiguration moduleConfig;
    private RtpService rtpService;
    private RtpGui rtpGui;

    public RtpModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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
        
        if (plugin instanceof ManagerFix mf) {
            int cd = moduleConfig != null ? moduleConfig.getInt("cooldown", 300) : 300;
            rtpService = new RtpService(mf, cd, moduleConfig);
            rtpGui = new RtpGui(mf, mf.getGuiManager(), mf.getUIThemeManager(), mf.getGuiTemplate(), rtpService);
            RtpCommand rtpCommand = new RtpCommand(mf, rtpGui);
            mf.getCommandManager().register("rtp", rtpCommand, rtpCommand);
        }
        LoggerUtil.debug("RTP module enabled.");
    }

    @Override
    protected void disable() {
        moduleConfig = null;
        rtpService = null;
        rtpGui = null;
        LoggerUtil.debug("RTP module disabled.");
    }

    public int getMinDistance() {
        return moduleConfig != null ? moduleConfig.getInt("min-distance", 1000) : 1000;
    }

    public int getMaxDistance() {
        return moduleConfig != null ? moduleConfig.getInt("max-distance", 10000) : 10000;
    }

    public int getCooldownSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("cooldown", 300) : 300;
    }

    public int getNearRtpMin() {
        return moduleConfig != null ? moduleConfig.getInt("near-rtp.min", 600) : 600;
    }

    public int getNearRtpMax() {
        return moduleConfig != null ? moduleConfig.getInt("near-rtp.max", 1000) : 1000;
    }

    public int getFarRtpMin() {
        return moduleConfig != null ? moduleConfig.getInt("far-rtp.min", 4000) : 4000;
    }

    public int getFarRtpMax() {
        return moduleConfig != null ? moduleConfig.getInt("far-rtp.max", 5000) : 5000;
    }

    public int getPlayerRadiusMin() {
        return moduleConfig != null ? moduleConfig.getInt("player-radius.min", 30) : 30;
    }

    public int getPlayerRadiusMax() {
        return moduleConfig != null ? moduleConfig.getInt("player-radius.max", 80) : 80;
    }
}
