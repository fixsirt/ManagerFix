package ru.managerfix.modules.names;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.event.MFEventHandler;
import ru.managerfix.event.ProfileLoadEvent;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.UUID;

/**
 * Applies nick on join/ProfileLoad; removes display on quit;
 * handles admin chat nick input; re-mounts TextDisplay on dismount.
 */
public final class NamesListener implements Listener {

    private final NamesModule module;
    private final ProfileManager profileManager;

    public NamesListener(NamesModule module, ProfileManager profileManager) {
        this.module = module;
        this.profileManager = profileManager;
    }

    // ========================= Join =========================

    /**
     * LOW priority: apply nick from cached profile immediately (before other handlers).
     * Also triggers async profile load for fresh data.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Try cached profile — apply nick immediately so displayName is set
        // before ChatJoinQuitDeathListener (HIGH) formats the join message.
        profileManager.getCachedProfile(player.getUniqueId()).ifPresentOrElse(profile -> {
            Object nick = profile.getMetadata(NamesModule.getNickMetadataKey()).orElse(null);
            if (nick instanceof String s && !s.isEmpty()) {
                // Проверяем globalNickHidden
                if (module.isGlobalNickHidden()) {
                    module.hidePlayerNametag(player);
                } else {
                    module.applyNick(player, s);
                }
                LoggerUtil.debug("[Names] Applied cached nick on join for " + player.getName());
            } else {
                if (!module.isGlobalNickHidden()) {
                    module.applyDefaultDisplay(player);
                } else {
                    module.hidePlayerNametag(player);
                }
            }
        }, () -> {
            if (!module.isGlobalNickHidden()) {
                module.applyDefaultDisplay(player);
            } else {
                module.hidePlayerNametag(player);
            }
        });

        // Trigger full async profile load (handles first-time joins & stale cache)
        profileManager.getProfile(player);

        // If player joins in SPECTATOR, ensure no nametag is shown
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            module.removeNickDisplay(player.getUniqueId());
        }
    }

    // ========================= Quit =========================

    /**
     * MONITOR priority: runs after all other quit handlers have formatted messages.
     * Removes the TextDisplay entity and cleans up pending state.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Выполняем в главном потоке но максимально быстро
        module.removeNickDisplay(uuid);
        module.clearPendingNickTarget(uuid);
        module.cleanupTeam(uuid);
    }

    // ========================= Profile load (may be async) =========================

    /**
     * Fired when profile finishes loading.
     * Applies or resets nick. Ensures main thread for entity operations.
     */
    @MFEventHandler
    public void onProfileLoad(ProfileLoadEvent event) {
        UUID uuid = event.getUuid();
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        event.getProfile().ifPresent(profile -> {
            Object nick = profile.getMetadata(NamesModule.getNickMetadataKey()).orElse(null);
            Runnable task = () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) return;
                if (nick instanceof String s && !s.isEmpty()) {
                    // Проверяем globalNickHidden
                    if (module.isGlobalNickHidden()) {
                        module.hidePlayerNametag(p);
                    } else {
                        module.applyNick(p, s);
                    }
                } else {
                    if (!module.isGlobalNickHidden()) {
                        module.applyDefaultDisplay(p);
                    } else {
                        module.hidePlayerNametag(p);
                    }
                }
            };
            // Entity operations (TextDisplay spawn/remove) must run on the main thread
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(module.getPlugin(), task);
            }
        });
    }

    // ========================= Death: remove TextDisplay =========================

    /**
     * On death: remove TextDisplay so it doesn't stay at the death location.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (module.hasNickDisplay(player.getUniqueId())) {
            module.removeNickDisplay(player.getUniqueId());
        }
    }

    // ========================= Respawn: re-create TextDisplay =========================

    /**
     * On respawn: re-create TextDisplay with the stored nick so the nametag appears again.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Schedule for 1 tick later so the player entity is fully ready
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            if (!player.isOnline()) return;
            profileManager.getCachedProfile(player.getUniqueId()).ifPresent(profile -> {
                Object nick = profile.getMetadata(NamesModule.getNickMetadataKey()).orElse(null);
                if (nick instanceof String s && !s.isEmpty()) {
                    // Проверяем globalNickHidden
                    if (module.isGlobalNickHidden()) {
                        module.hidePlayerNametag(player);
                    } else {
                        module.applyNick(player, s);
                    }
                    LoggerUtil.debug("[Names] Re-applied nick after respawn for " + player.getName());
                } else {
                    if (!module.isGlobalNickHidden()) {
                        module.applyDefaultDisplay(player);
                    } else {
                        module.hidePlayerNametag(player);
                    }
                }
            });
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                module.removeNickDisplay(player.getUniqueId());
            }
        }, 1L);
    }

    // ========================= Teleport/portals: move TextDisplay =========================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!module.hasNickDisplay(player.getUniqueId())) return;
        if (event.getTo() == null) return;
        if (event.getFrom().getWorld() == event.getTo().getWorld()
                && event.getFrom().distanceSquared(event.getTo()) < 0.0001) {
            return;
        }

        module.removeNickDisplay(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            if (!player.isOnline()) return;
            profileManager.getCachedProfile(player.getUniqueId()).ifPresent(profile -> {
                Object nick = profile.getMetadata(NamesModule.getNickMetadataKey()).orElse(null);
                if (nick instanceof String s && !s.isEmpty()) {
                    // Проверяем globalNickHidden
                    if (module.isGlobalNickHidden()) {
                        module.hidePlayerNametag(player);
                    } else {
                        module.applyNick(player, s);
                    }
                } else {
                    if (!module.isGlobalNickHidden()) {
                        module.applyDefaultDisplay(player);
                    } else {
                        module.hidePlayerNametag(player);
                    }
                }
            });
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                module.removeNickDisplay(player.getUniqueId());
            }
        }, 1L);
    }

    // ========================= Dismount: re-add TextDisplay =========================

    /**
     * When the TextDisplay is dismounted from a player (e.g. player enters vehicle,
     * takes damage, etc.), re-mount it after 1 tick.
     * Does NOT re-mount if the player is dead (handled by death/respawn events).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        Entity dismounted = event.getDismounted();
        if (!(dismounted instanceof Player player)) return;
        if (!module.hasNickDisplay(player.getUniqueId())) return;

        // Don't re-mount if the player is dead — death handler will remove it,
        // respawn handler will re-create it
        if (player.isDead()) return;

        // Schedule re-mount on next tick (the dismount is still being processed)
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            if (player.isOnline() && !player.isDead()) {
                module.remountNickDisplay(player);
            }
        }, 1L);
    }

    // ========================= GameMode change: hide in spectator =========================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getNewGameMode() == org.bukkit.GameMode.SPECTATOR) {
            module.removeNickDisplay(player.getUniqueId());
            return;
        }
        // Leaving spectator: re-apply display after a tick
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            if (!player.isOnline()) return;
            profileManager.getCachedProfile(player.getUniqueId()).ifPresent(profile -> {
                Object nick = profile.getMetadata(NamesModule.getNickMetadataKey()).orElse(null);
                if (nick instanceof String s && !s.isEmpty()) {
                    module.applyNick(player, s);
                } else {
                    module.applyDefaultDisplay(player);
                }
            });
        }, 1L);
    }

    // ========================= Admin chat nick input =========================

    /** When admin with pending nick target types in chat, use message as new nick. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        UUID adminUuid = event.getPlayer().getUniqueId();
        UUID targetUuid = module.getPendingNickTarget(adminUuid);
        if (targetUuid == null) return;

        event.setCancelled(true);
        Player admin = event.getPlayer();
        String rawNick = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        module.clearPendingNickTarget(adminUuid);

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), admin, "names.target-offline");
            return;
        }

        int maxLen = module.getMaxLength();
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(module.parseNickToComponent(rawNick));
        if (plain.length() > maxLen) {
            MessageUtil.send(module.getPlugin(), admin, "names.too-long",
                    java.util.Map.of("max", String.valueOf(maxLen)));
            return;
        }

        String finalRawNick = rawNick.isEmpty() ? null : rawNick;
        if (module.getPlugin() instanceof ru.managerfix.ManagerFix mf) {
            mf.getScheduler().runSync(() -> {
                Player t = Bukkit.getPlayer(targetUuid);
                if (t != null && t.isOnline()) {
                    module.setNick(t, profileManager, finalRawNick, admin.getUniqueId(), () -> {
                        mf.getGuiManager().sendConfirmation(admin, "Ник изменён");
                        LoggerUtil.debug("[Names] Admin " + admin.getName()
                                + " changed nick of " + t.getName()
                                + " via chat to " + (finalRawNick != null ? finalRawNick : "(reset)"));
                    });
                }
            });
        }
    }
}
