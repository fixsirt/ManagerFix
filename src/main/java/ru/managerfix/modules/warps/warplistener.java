package ru.managerfix.modules.warps;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class WarpListener implements Listener {

    private final WarpService warpService;

    WarpListener(WarpService warpService) {
        this.warpService = warpService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player) {
            warpService.cancelTeleport(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        warpService.cancelTeleport(event.getPlayer().getUniqueId());
    }
}

