package ru.managerfix.modules.tab;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Runs tab updates at configurable interval. Uses TabRenderer; updates only changed header/footer/names.
 */
public final class TabTask {

    private final TabModule module;
    private final TabRenderer renderer;
    private BukkitTask task;

    public TabTask(TabModule module, TabRenderer renderer) {
        this.module = module;
        this.renderer = renderer;
    }

    public void start() {
        stop();
        long interval = module.getTabConfig().getUpdateIntervalTicks();
        if (interval <= 0) return;
        task = module.getPlugin().getServer().getScheduler().runTaskTimer(
                module.getPlugin(),
                () -> renderer.updateAll(),
                interval,
                interval
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            renderer.invalidate(player);
        }
    }
}
