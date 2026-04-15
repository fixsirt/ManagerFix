package ru.managerfix.modules.tpa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.ManagerFix;

import java.util.UUID;
import ru.managerfix.utils.MessageUtil;

/**
 * Cleans up requests and pending teleports on quit; cancels teleport on move &gt; 0.2 blocks.
 */
public final class TpaListener implements Listener {

    private static final double MOVE_THRESHOLD = 0.2;

    private final JavaPlugin plugin;
    private final TpaService service;
    private BukkitTask expireTask;

    public TpaListener(JavaPlugin plugin, TpaService service) {
        this.plugin = plugin;
        this.service = service;
        startExpireTask();
    }

    private void startExpireTask() {
        if (expireTask != null) expireTask.cancel();
        expireTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> service.cleanupExpired(), 100L, 100L);
    }

    public void stop() {
        if (expireTask != null) {
            expireTask.cancel();
            expireTask = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        service.removeRequestsByTarget(uuid);
        service.removeRequestsBySender(uuid);
        service.cancelPendingTeleport(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        if (!service.hasPendingTeleport(player.getUniqueId())) return;
        var startOpt = service.getPendingTeleportStart(player.getUniqueId());
        if (startOpt.isEmpty()) return;
        if (startOpt.get().getWorld() == null || !startOpt.get().getWorld().equals(player.getWorld())) return;
        if (startOpt.get().distance(event.getTo()) <= MOVE_THRESHOLD) return;
        service.cancelPendingTeleport(player.getUniqueId());
        player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("cancelled-move", "<#FF3366>Телепортация отменена: вы сдвинулись.")));
    }
}
