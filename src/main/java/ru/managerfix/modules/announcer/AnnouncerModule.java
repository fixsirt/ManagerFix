package ru.managerfix.modules.announcer;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Announcer module: timer-based broadcast messages, PlaceholderAPI, toggle via GUI (main menu).
 */
public final class AnnouncerModule extends AbstractModule {

    private static final String MODULE_NAME = "announcer";
    private static final String CONFIG_FILE = "announcer/config.yml";

    private FileConfiguration moduleConfig;
    private AnnouncerTask announcerTask;

    public AnnouncerModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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
        announcerTask = new AnnouncerTask(plugin, this);
        announcerTask.start();
        LoggerUtil.debug("Announcer module enabled.");
    }

    @Override
    protected void disable() {
        if (announcerTask != null) {
            announcerTask.stop();
            announcerTask = null;
        }
        moduleConfig = null;
        LoggerUtil.debug("Announcer module disabled.");
    }

    @Override
    protected void reload() {
        if (announcerTask != null) {
            announcerTask.reloadMessages(getMessages());
        }
    }

    public int getIntervalSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("interval-seconds", 300) : 300;
    }

    public List<String> getMessages() {
        if (moduleConfig == null || !moduleConfig.contains("messages")) return Collections.emptyList();
        return moduleConfig.getStringList("messages").stream().filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList());
    }

    public String getBroadcastType() {
        return moduleConfig != null ? moduleConfig.getString("broadcast-type", "CHAT") : "CHAT";
    }
}
