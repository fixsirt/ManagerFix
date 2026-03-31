package ru.managerfix.modules.announcer;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rotates announcement messages by interval. Supports PlaceholderAPI per player.
 */
public final class AnnouncerTask {

    private final JavaPlugin plugin;
    private final AnnouncerModule module;
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final AtomicInteger index = new AtomicInteger(0);
    private BukkitTask task;

    public AnnouncerTask(JavaPlugin plugin, AnnouncerModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    public void reloadMessages(List<String> list) {
        messages.clear();
        if (list != null) messages.addAll(list);
    }

    public void start() {
        stop();
        reloadMessages(module.getMessages());
        if (messages.isEmpty()) return;
        long intervalTicks = module.getIntervalSeconds() * 20L;
        if (intervalTicks <= 0) return;
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::broadcastNext,
                intervalTicks,
                intervalTicks
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void broadcastNext() {
        if (messages.isEmpty()) return;
        int i = index.getAndIncrement() % messages.size();
        if (i < 0) i = 0;
        String raw = messages.get(i);
        String type = module.getBroadcastType();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String withPapi = MessageUtil.setPlaceholders(player, raw);
            Component component = MessageUtil.parse(withPapi);
            if ("ACTION_BAR".equalsIgnoreCase(type)) {
                player.sendActionBar(component);
            } else {
                player.sendMessage(component);
            }
        }
    }
}
