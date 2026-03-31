package ru.managerfix.modules.rtp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.ManagerFix;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.scheduler.TaskScheduler;
import ru.managerfix.utils.MessageUtil;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class RtpService {

    private static final String PERM_BYPASS_COOLDOWN = "managerfix.rtp.bypass.cooldown";
    private static final Random RANDOM = new Random();
    private static final int MAX_ATTEMPTS = 60;

    private final ManagerFix plugin;
    private final int cooldownSeconds;
    private final FileConfiguration config;
    private final double nearTeleportCost;

    RtpService(ManagerFix plugin, int cooldownSeconds, FileConfiguration config) {
        this.plugin = plugin;
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.config = config;
        this.nearTeleportCost = config != null ? config.getDouble("costs.near-player", 1000.0) : 1000.0;
    }

    void randomTeleport(Player player, int radius) {
        if (!checkAndSetCooldownStart(player)) return;
        World world = player.getWorld();
        Location center = world.getSpawnLocation();
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dist = RANDOM.nextDouble() * radius;
        int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
        int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
        int y = world.getHighestBlockYAt(x, z);
        Location dest = new Location(world, x + 0.5, y + 1, z + 0.5, player.getLocation().getYaw(), 0f);
        var tpaMod = plugin.getModuleManager().getEnabledModule("tpa")
            .filter(m -> m instanceof ru.managerfix.modules.tpa.TpaModule)
            .map(m -> (ru.managerfix.modules.tpa.TpaModule) m)
            .orElse(null);
        if (tpaMod != null && tpaMod.getTpaService() != null) {
            applyCooldown(player);
            tpaMod.getTpaService().scheduleTeleport(player, dest, () -> cancelCooldownIfJustStarted(player));
        } else {
            applyCooldown(player);
            if (plugin.getEventBus() != null) {
                plugin.getEventBus().callEvent(new ru.managerfix.event.RTPEvent(player, player.getLocation(), dest));
            }
            player.teleport(dest);
            player.sendMessage(MessageUtil.parse(getMsg("success", "<#FAA300>Вы были телепортированы.</#FAA300>")));
        }
    }

    void teleportToRandomNearbyPlayer(Player player, int radius) {
        if (!checkAndSetCooldownStart(player)) return;
        List<Player> candidates = new ArrayList<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) candidates.add(p);
        }
        if (candidates.isEmpty()) {
            cancelCooldownIfJustStarted(player);
            player.sendMessage(MessageUtil.parse(getMsg("no-targets", "<#FF4D00>Нет других игроков для телепорта.</#FF4D00>")));
            return;
        }
        Player target = candidates.get(RANDOM.nextInt(candidates.size()));
        if (!chargeEconomy(player, nearTeleportCost)) {
            cancelCooldownIfJustStarted(player);
            return;
        }
        Location around = target.getLocation().clone();
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double dist = RANDOM.nextDouble() * radius;
        int x = around.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
        int z = around.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
        int y = around.getWorld().getHighestBlockYAt(x, z);
        Location dest = new Location(around.getWorld(), x + 0.5, y + 1, z + 0.5, player.getLocation().getYaw(), 0f);
        var tpaMod = plugin.getModuleManager().getEnabledModule("tpa")
            .filter(m -> m instanceof ru.managerfix.modules.tpa.TpaModule)
            .map(m -> (ru.managerfix.modules.tpa.TpaModule) m)
            .orElse(null);
        if (tpaMod != null && tpaMod.getTpaService() != null) {
            applyCooldown(player);
            tpaMod.getTpaService().scheduleTeleport(player, dest, () -> {
                cancelCooldownIfJustStarted(player);
                refundEconomy(player, nearTeleportCost);
            });
        } else {
            applyCooldown(player);
            if (plugin.getEventBus() != null) {
                plugin.getEventBus().callEvent(new ru.managerfix.event.RTPEvent(player, player.getLocation(), dest));
            }
            player.teleport(dest);
            player.sendMessage(MessageUtil.parse(getMsg("success", "<#FAA300>Вы были телепортированы.</#FAA300>")));
        }
    }

    private boolean checkAndSetCooldownStart(Player player) {
        if (cooldownSeconds <= 0 || player.hasPermission(PERM_BYPASS_COOLDOWN) || player.hasPermission("managerfix.bypass.cooldown")) {
            return true;
        }
        ProfileManager pm = plugin.getProfileManager();
        PlayerProfile profile = pm.getProfile(player);
        if (profile.hasCooldown("rtp")) {
            long rem = profile.getCooldownRemaining("rtp");
            String msg = getMsg("cooldown", "<#FF4D00>Подождите {seconds} сек. перед следующим телепортом.</#FF4D00>").replace("{seconds}", String.valueOf((rem + 999) / 1000));
            player.sendMessage(MessageUtil.parse(msg));
            return false;
        }
        profile.setCooldown("rtp", 1L);
        return true;
    }

    private void cancelCooldownIfJustStarted(Player player) {
        if (cooldownSeconds <= 0 || player.hasPermission(PERM_BYPASS_COOLDOWN) || player.hasPermission("managerfix.bypass.cooldown")) {
            return;
        }
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        profile.clearCooldown("rtp");
    }

    private void applyCooldown(Player player) {
        if (cooldownSeconds <= 0 || player.hasPermission(PERM_BYPASS_COOLDOWN) || player.hasPermission("managerfix.bypass.cooldown")) {
            return;
        }
        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        profile.setCooldown("rtp", cooldownSeconds * 1000L);
    }

    private Location findSafe(World world, Location origin, int radius) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = RANDOM.nextDouble() * radius;
            int x = origin.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
            int z = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
            int y = world.getHighestBlockYAt(x, z);
            if (y <= 0) continue;
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            Block ground = world.getBlockAt(x, y - 1, z);
            if (isSafe(feet) && isSafe(head) && isSolid(ground) && !isLiquid(feet) && !isLiquid(head) && !isLiquid(ground)) {
                return new Location(world, x + 0.5, y, z + 0.5, RANDOM.nextFloat() * 360f, 0f);
            }
        }
        return null;
    }

    private static boolean isSafe(Block block) {
        Material t = block.getType();
        return t.isAir() || !t.isSolid();
    }

    private static boolean isSolid(Block block) {
        return block.getType().isSolid();
    }

    private static boolean isLiquid(Block block) {
        return block.getType() == Material.WATER || block.getType() == Material.LAVA;
    }

    private String getMsg(String key, String def) {
        if (config == null) return def;
        String path = "messages." + key;
        String v = config.getString(path);
        return v != null ? v : def;
    }

    public double getNearTeleportCost() {
        return nearTeleportCost;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean chargeEconomy(Player player, double amount) {
        try {
            if (amount <= 0) return true;
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                player.sendMessage(MessageUtil.parse(getMsg("eco-unavailable", "<#FF4D00>Экономика недоступна.</#FF4D00>")));
                return false;
            }
            var reg = plugin.getServer().getServicesManager()
                    .getRegistration((Class) Class.forName("net.milkbowl.vault.economy.Economy"));
            if (reg == null) {
                player.sendMessage(MessageUtil.parse(getMsg("eco-unavailable", "<#FF4D00>Экономика недоступна.</#FF4D00>")));
                return false;
            }
            Object econ = reg.getProvider();
            // Try has(OfflinePlayer,double)
            java.lang.reflect.Method has = null;
            java.lang.reflect.Method withdraw = null;
            try {
                has = econ.getClass().getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
                withdraw = econ.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            } catch (NoSuchMethodException ignored) {
            }
            if (has != null && withdraw != null) {
                boolean enough = (boolean) has.invoke(econ, player, amount);
                if (!enough) {
                    String msg = getMsg("insufficient-funds", "<#FF4D00>Недостаточно средств: требуется {amount}.</#FF4D00>")
                            .replace("{amount}", String.valueOf((int) amount));
                    player.sendMessage(MessageUtil.parse(msg));
                    return false;
                }
                Object resp = withdraw.invoke(econ, player, amount);
                java.lang.reflect.Method ok = resp.getClass().getMethod("transactionSuccess");
                boolean success = (boolean) ok.invoke(resp);
                if (!success) {
                    player.sendMessage(MessageUtil.parse(getMsg("eco-unavailable", "<#FF4D00>Экономика недоступна.</#FF4D00>")));
                }
                return success;
            }
            // Fallback: use getBalance(OfflinePlayer) + withdrawPlayer(OfflinePlayer,double)
            java.lang.reflect.Method getBalance = econ.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            double bal = ((Number) getBalance.invoke(econ, player)).doubleValue();
            if (bal + 1e-6 < amount) {
                String msg = getMsg("insufficient-funds", "<#FF4D00>Недостаточно средств: требуется {amount}.</#FF4D00>")
                        .replace("{amount}", String.valueOf((int) amount));
                player.sendMessage(MessageUtil.parse(msg));
                return false;
            }
            Object resp = econ.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(econ, player, amount);
            java.lang.reflect.Method ok = resp.getClass().getMethod("transactionSuccess");
            boolean success = (boolean) ok.invoke(resp);
            if (!success) {
                player.sendMessage(MessageUtil.parse(getMsg("eco-unavailable", "<#FF4D00>Экономика недоступна.</#FF4D00>")));
            }
            return success;
        } catch (Throwable t) {
            player.sendMessage(MessageUtil.parse(getMsg("eco-unavailable", "<#FF4D00>Экономика недоступна.</#FF4D00>")));
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void refundEconomy(Player player, double amount) {
        try {
            if (amount <= 0) return;
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
            var reg = plugin.getServer().getServicesManager()
                    .getRegistration((Class) Class.forName("net.milkbowl.vault.economy.Economy"));
            if (reg == null) return;
            Object econ = reg.getProvider();
            try {
                Object resp = econ.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class)
                        .invoke(econ, player, amount);
                // ignore response
            } catch (NoSuchMethodException ignored) {
                // Best-effort: if deposit method signature differs, silently ignore
            }
        } catch (Throwable ignored) {
        }
    }
}
