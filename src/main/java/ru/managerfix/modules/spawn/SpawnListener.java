package ru.managerfix.modules.spawn;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class SpawnListener implements Listener {

    private final SpawnConfig config;
    private final SpawnService spawnService;

    public SpawnListener(SpawnConfig config, SpawnService spawnService) {
        this.config = config;
        this.spawnService = spawnService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location spawnLoc = config.getSpawnLocation();
        if (spawnLoc == null) return;

        if (config.isSpawnOnJoin()) {
            if (config.isSpawnFirstJoinOnly()) {
                if (!player.hasPlayedBefore()) {
                    player.teleport(spawnLoc);
                }
            } else {
                player.teleport(spawnLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (config.isSpawnOnDeath()) {
            Location spawnLoc = config.getSpawnLocation();
            if (spawnLoc != null) {
                event.setRespawnLocation(spawnLoc);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (config.isCancelOnDamage()) {
                spawnService.cancelTeleport(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spawnService.cancelTeleport(event.getPlayer().getUniqueId());
    }
}
