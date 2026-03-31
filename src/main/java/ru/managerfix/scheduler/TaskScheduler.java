package ru.managerfix.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * Centralized async/sync task runner. Use instead of raw Bukkit.getScheduler().
 */
public final class TaskScheduler {

    private final JavaPlugin plugin;

    public TaskScheduler(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    /**
     * Runs task asynchronously (off main thread).
     */
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Runs task on main thread (next tick).
     */
    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Runs task on main thread after delayTicks.
     */
    public BukkitTask runLater(long delayTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Runs task asynchronously after delayTicks.
     */
    public BukkitTask runLaterAsync(long delayTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    /**
     * Runs task on main thread every periodTicks, first run after delayTicks.
     */
    public BukkitTask runTimer(long delayTicks, long periodTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Runs task asynchronously every periodTicks, first run after delayTicks.
     */
    public BukkitTask runTimerAsync(long delayTicks, long periodTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }
}
