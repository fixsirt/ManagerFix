package ru.managerfix.modules.other;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Map;
import java.util.UUID;

public final class OtherListener implements Listener {

    private static final String FOOD_GOD_KEY = "other.foodgod";

    private final OtherModule module;
    private final BackService backService;
    private final VanishService vanishService;
    private final ProfileManager profileManager;
    private final Map<String, String> aliasMap;

    public OtherListener(OtherModule module, BackService backService, VanishService vanishService,
                         ProfileManager profileManager) {
        this.module = module;
        this.backService = backService;
        this.vanishService = vanishService;
        this.profileManager = profileManager;
        this.aliasMap = module.getOtherConfig().getAliasToCommandMap();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandAlias(PlayerCommandPreprocessEvent event) {
        if (aliasMap.isEmpty()) return;
        String msg = event.getMessage();
        if (msg == null || msg.length() < 2 || msg.charAt(0) != '/') return;
        String[] parts = msg.substring(1).split(" ");
        if (parts.length == 0) return;
        String cmd = parts[0].toLowerCase();
        String target = aliasMap.get(cmd);
        if (target == null || target.isEmpty()) return;
        String rest = msg.substring(1 + parts[0].length());
        event.setMessage("/" + target + rest);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) return;
        if (from.getWorld().equals(to.getWorld()) && from.distanceSquared(to) < 1e-6) return;
        backService.setBackLocation(event.getPlayer().getUniqueId(), from);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (p != null && p.getWorld() != null) {
            backService.setDeathLocation(p.getUniqueId(), p.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (!module.isFrozen(p.getUniqueId())) return;
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (module.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (module.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!module.isFoodGod(p.getUniqueId())) return;
        event.setCancelled(true);
        p.setFoodLevel(20);
        p.setSaturation(20f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChatLock(io.papermc.paper.event.player.AsyncChatEvent event) {
        if (!module.isChatLocked()) return;
        Player p = event.getPlayer();
        if (p.hasPermission("managerfix.other.chatlock")) {
            return;
        }
        event.setCancelled(true);
        p.sendMessage(MessageUtil.parse("<red>Чат закрыт.</red>"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        vanishService.applyForOnline(player);
        if (profileManager != null && module.getOtherConfig().isFoodGodPersist()) {
            PlayerProfile profile = profileManager.getProfile(player);
            Object val = profile.getMetadata(FOOD_GOD_KEY).orElse(false);
            if (val instanceof Boolean b && b) {
                module.setFoodGod(player.getUniqueId(), true);
                player.setFoodLevel(20);
                player.setSaturation(20f);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        boolean vanished = vanishService.isVanished(player);
        backService.clear(player.getUniqueId());
        vanishService.clear(player.getUniqueId());
        if (module.getOtherConfig().isVanishHideJoinQuit() && vanished) {
            event.quitMessage((net.kyori.adventure.text.Component) null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinMessage(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (module.getOtherConfig().isVanishHideJoinQuit() && vanishService.isVanished(player)) {
            event.joinMessage((net.kyori.adventure.text.Component) null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvseeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof InvseeHolder holder)) return;
        Player target = Bukkit.getPlayer(holder.getTargetUuid());
        if (target == null || !target.isOnline()) {
            event.setCancelled(true);
            return;
        }
        if (!viewer.hasPermission("managerfix.other.invsee.modify")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInvseeClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        if (!(event.getInventory().getHolder() instanceof InvseeHolder holder)) return;
        Player target = Bukkit.getPlayer(holder.getTargetUuid());
        if (target == null || !target.isOnline()) return;
        if (!viewer.hasPermission("managerfix.other.invsee.modify")) return;
        org.bukkit.inventory.Inventory inv = event.getInventory();
        org.bukkit.inventory.ItemStack[] contents = new org.bukkit.inventory.ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = inv.getItem(i);
        }
        target.getInventory().setContents(contents);
        org.bukkit.inventory.ItemStack[] armor = new org.bukkit.inventory.ItemStack[4];
        armor[0] = inv.getItem(36);
        armor[1] = inv.getItem(37);
        armor[2] = inv.getItem(38);
        armor[3] = inv.getItem(39);
        target.getInventory().setArmorContents(armor);
        target.getInventory().setItemInOffHand(inv.getItem(40));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        org.bukkit.entity.Player damager = null;
        if (event.getDamager() instanceof org.bukkit.entity.Player p) {
            damager = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj
                && proj.getShooter() instanceof org.bukkit.entity.Player p) {
            damager = p;
        }
        if (damager == null) return;
        if (module.isGod(damager.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (damager.getGameMode() != org.bukkit.GameMode.CREATIVE
                && damager.getGameMode() != org.bukkit.GameMode.SPECTATOR
                && damager.getAllowFlight()) {
            damager.setAllowFlight(false);
            damager.setFlying(false);
            damager.sendMessage(MessageUtil.parse("<red>Полёт выключен.</red>"));
        }
        if (victim.getGameMode() != org.bukkit.GameMode.CREATIVE
                && victim.getGameMode() != org.bukkit.GameMode.SPECTATOR
                && victim.getAllowFlight()) {
            victim.setAllowFlight(false);
            victim.setFlying(false);
            victim.sendMessage(MessageUtil.parse("<red>Полёт выключен.</red>"));
        }
    }
}
