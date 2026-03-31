package ru.managerfix.modules.afk;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.core.LoggerUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto-AFK on no movement; block commands if config says so; clear AFK on quit.
 */
public final class AfkListener implements Listener {

    private final AfkModule module;
    private final AfkManager afkManager;
    private final Map<UUID, Long> lastMoveTime = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> afkCheckTasks = new ConcurrentHashMap<>();

    public AfkListener(AfkModule module, AfkManager afkManager) {
        this.module = module;
        this.afkManager = afkManager;
    }

    public void startAfkCheck(Player player) {
        cancelAfkCheck(player);
        int timeoutSec = module.getAfkTimeoutSeconds();
        if (timeoutSec <= 0) return;
        long intervalTicks = 20L;
        BukkitTask task = module.getPlugin().getServer().getScheduler().runTaskTimer(
                module.getPlugin(),
                () -> {
                    if (!player.isOnline()) {
                        cancelAfkCheck(player);
                        return;
                    }
                    if (afkManager == null) {
                        cancelAfkCheck(player);
                        return;
                    }
                    if (afkManager.isAfk(player)) return;
                    long last = lastMoveTime.getOrDefault(player.getUniqueId(), 0L);
                    if (last > 0 && System.currentTimeMillis() - last >= timeoutSec * 1000L) {
                        afkManager.setAfk(player, true);
                    }
                },
                intervalTicks,
                intervalTicks
        );
        afkCheckTasks.put(player.getUniqueId(), task);
    }

    public void cancelAfkCheck(Player player) {
        BukkitTask task = afkCheckTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (afkManager == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        lastMoveTime.put(player.getUniqueId(), System.currentTimeMillis());
        if (afkManager.isAfk(player)) {
            afkManager.setAfk(player, false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (afkManager == null) return;
        if (!module.getBlockCommandsWhileAfk()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("managerfix.afk.bypass")) return;
        if (!afkManager.isAfk(player)) return;
        String cmd = event.getMessage().split("\\s+")[0].replaceFirst("/", "").toLowerCase();
        if ("afk".equals(cmd)) return;
        event.setCancelled(true);
        ru.managerfix.utils.MessageUtil.send(module.getPlugin(), player, "afk.blocked-command");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelAfkCheck(event.getPlayer());
        lastMoveTime.remove(event.getPlayer().getUniqueId());
    }
}
