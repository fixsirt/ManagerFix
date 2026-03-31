package ru.managerfix.modules.tab;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;

/**
 * Tab module: multi-line header/footer (MiniMessage, HEX, PlaceholderAPI), player format,
 * AFK format, LuckPerms sort, configurable update interval. Uses TabConfig and TabRenderer.
 * Does not overwrite scoreboard; does not conflict with NamesModule (uses {displayName} as Component).
 */
public final class TabModule extends AbstractModule implements Listener {

    private static final String MODULE_NAME = "tab";
    private static final String CONFIG_FILE = "tab/config.yml";

    private TabConfig tabConfig;
    private TabRenderer tabRenderer;
    private LuckPermsSortService sortService;
    private TabTask tabTask;

    public TabModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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
        FileConfiguration moduleConfig = configManager.getModuleConfig(CONFIG_FILE);
        tabConfig = new TabConfig(moduleConfig);
        sortService = new LuckPermsSortService();
        tabRenderer = new TabRenderer(tabConfig, this, sortService);
        tabTask = new TabTask(this, tabRenderer);
        tabTask.start();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (sortService.isAvailable()) {
            LoggerUtil.debug("Tab module enabled (LuckPerms sort available).");
        } else {
            LoggerUtil.debug("Tab module enabled.");
        }
    }

    @Override
    protected void disable() {
        if (tabTask != null) {
            tabTask.stop();
            tabTask = null;
        }
        if (tabRenderer != null) {
            tabRenderer.cleanupAllTeams();
            tabRenderer = null;
        }
        sortService = null;
        tabConfig = null;
        LoggerUtil.debug("Tab module disabled.");
    }

    public TabConfig getTabConfig() {
        return tabConfig;
    }

    public TabRenderer getTabRenderer() {
        return tabRenderer;
    }

    public LuckPermsSortService getSortService() {
        return sortService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (tabRenderer != null) tabRenderer.invalidate(event.getPlayer());
    }

    /** After respawn, NamesModule resets playerListName to pure nick.
     *  Invalidate Tab cache so the next tick re-applies the formatted name with prefix. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (tabRenderer != null) tabRenderer.invalidate(event.getPlayer());
    }

    /** When leaving spectator or changing gamemode, NamesModule may touch playerListName.
     *  Invalidate and immediately rebuild formatted tab name to restore prefix. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (tabRenderer == null) return;
        // Немедленно инвалидируем кеш, а обновление перенесём на следующий тик,
        // чтобы NamesModule успел вернуть displayName после выхода из SPECTATOR.
        var player = event.getPlayer();
        tabRenderer.invalidate(player);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (tabRenderer != null && player.isOnline()) {
                tabRenderer.updateAll();
            }
        });
    }
}
