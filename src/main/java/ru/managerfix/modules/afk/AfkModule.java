package ru.managerfix.modules.afk;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.event.MFEventHandler;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.event.EventBus;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.service.ServiceRegistry;
import ru.managerfix.utils.MessageUtil;

import java.util.List;

/**
 * AFK module: /afk, auto-AFK by timeout, broadcast, block commands option, EventBus, profile metadata.
 */
public final class AfkModule extends AbstractModule {

    private static final String MODULE_NAME = "afk";
    private static final String CONFIG_FILE = "afk/config.yml";

    private FileConfiguration moduleConfig;
    private AfkManager afkManager;
    private AfkListener afkListener;
    private AfkBroadcastListener afkBroadcastListener;

    public AfkModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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

        ProfileManager profileManager = plugin instanceof ManagerFix mf ? mf.getProfileManager() : null;
        EventBus eventBus = plugin instanceof ManagerFix mf ? mf.getEventBus() : null;
        
        if (profileManager == null || eventBus == null) {
            LoggerUtil.warning("AFK module cannot start: ProfileManager or EventBus is null");
            return;
        }
        
        afkManager = new AfkManager(profileManager, eventBus);
        
        if (plugin instanceof ManagerFix mf) {
            AfkCommand afkCommand = new AfkCommand(mf, afkManager);
            mf.getCommandManager().register("afk", afkCommand, afkCommand);
            ru.managerfix.commands.TopCommand topCommand = new ru.managerfix.commands.TopCommand(mf);
            mf.getCommandManager().register("top", topCommand, topCommand);
            afkListener = new AfkListener(this, afkManager);
            plugin.getServer().getPluginManager().registerEvents(afkListener, plugin);
            afkBroadcastListener = new AfkBroadcastListener(mf, afkManager);
            eventBus.registerListener(afkBroadcastListener);
            plugin.getServer().getPluginManager().registerEvents(afkBroadcastListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(new AfkJoinListener(afkListener), plugin);
        }
        LoggerUtil.debug("AFK module enabled.");
    }

    @Override
    protected void disable() {
        if (plugin instanceof ManagerFix mf && mf.getEventBus() != null && afkBroadcastListener != null) {
            mf.getEventBus().unregisterListener(afkBroadcastListener);
        }
        afkBroadcastListener = null;
        afkListener = null;
        afkManager = null;
        moduleConfig = null;
        LoggerUtil.debug("AFK module disabled.");
    }

    public int getAfkTimeoutSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("afk-timeout-seconds", 300) : 300;
    }

    public boolean getBroadcastAfk() {
        return moduleConfig != null && moduleConfig.getBoolean("broadcast-afk", true);
    }

    public boolean getBlockCommandsWhileAfk() {
        return moduleConfig != null && moduleConfig.getBoolean("block-commands-while-afk", false);
    }

    public int getKickTimeoutSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("kick-timeout-seconds", 0) : 0;
    }

    public AfkManager getAfkManager() {
        return afkManager;
    }

    /**
     * Broadcasts AFK enter/leave to all players when config says so. EventBus + Bukkit listener.
     * Debounces: only one broadcast per enter per player (avoids duplicates on reload/multiple events).
     */
    public static final class AfkBroadcastListener implements Listener {
        private final ManagerFix plugin;
        private final AfkManager afkManager;
        private final java.util.Map<java.util.UUID, Boolean> lastBroadcastState = new java.util.concurrent.ConcurrentHashMap<>();

        public AfkBroadcastListener(ManagerFix plugin, AfkManager afkManager) {
            this.plugin = plugin;
            this.afkManager = afkManager;
        }

        @MFEventHandler
        public void onAfkEnter(ru.managerfix.event.AfkEnterEvent event) {
            AfkModule mod = plugin.getModuleManager().getEnabledModule("afk")
                .filter(m -> m instanceof AfkModule)
                .map(m -> (AfkModule) m)
                .orElse(null);
            if (mod == null || !mod.getBroadcastAfk()) return;
            java.util.UUID uuid = event.getPlayer().getUniqueId();
            if (Boolean.TRUE.equals(lastBroadcastState.put(uuid, true))) return; // already broadcast enter
            MessageUtil.broadcast(plugin, "afk.broadcast-enter", java.util.Map.of("player", event.getPlayer().getName()));
        }

        @MFEventHandler
        public void onAfkLeave(ru.managerfix.event.AfkLeaveEvent event) {
            AfkModule mod = plugin.getModuleManager().getEnabledModule("afk")
                .filter(m -> m instanceof AfkModule)
                .map(m -> (AfkModule) m)
                .orElse(null);
            if (mod == null || !mod.getBroadcastAfk()) return;
            java.util.UUID uuid = event.getPlayer().getUniqueId();
            lastBroadcastState.put(uuid, false);
            MessageUtil.broadcast(plugin, "afk.broadcast-leave", java.util.Map.of("player", event.getPlayer().getName()));
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent event) {
            lastBroadcastState.remove(event.getPlayer().getUniqueId());
        }
    }

    private static final class AfkJoinListener implements Listener {
        private final AfkListener afkListener;

        AfkJoinListener(AfkListener afkListener) {
            this.afkListener = afkListener;
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            if (afkListener != null) {
                afkListener.startAfkCheck(event.getPlayer());
            }
        }
    }
}
