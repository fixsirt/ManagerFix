package ru.managerfix.modules.other;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.modules.tpa.TpaModule;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TeleportCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public TeleportCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if ("pull".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.pull")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (!module.checkAndApplyCooldown(player, "pull", "managerfix.bypass.cooldown")) {
                return true;
            }
            return pull(player, args);
        }
        if ("push".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.push")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (!module.checkAndApplyCooldown(player, "push", "managerfix.bypass.cooldown")) {
                return true;
            }
            return push(player, args);
        }
        if ("tp".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.tp")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (!module.checkAndApplyCooldown(player, "tp", "managerfix.bypass.cooldown")) {
                return true;
            }
            if (args.length < 1) {
                MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/tp to <name> | /tp here <name> | /tp location <x> <y> <z> | /tp top"));
                return true;
            }
            String sub = args[0].toLowerCase();
            if ("to".equals(sub)) {
                if (args.length < 2) {
                    MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/tp to <name>"));
                    return true;
                }
                return tpTo(player, args[1]);
            }
            if ("here".equals(sub)) {
                if (args.length < 2) {
                    MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/tp here <name>"));
                    return true;
                }
                return tpHere(player, args[1]);
            }
            if ("top".equals(sub)) {
                return tpTop(player);
            }
            if ("location".equals(sub)) {
                if (!sender.hasPermission("managerfix.other.tp.location")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                if (args.length < 4) {
                    MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/tp location <x> <y> <z>"));
                    return true;
                }
                return tpLocation(player, args[1], args[2], args[3]);
            }
        }
        return false;
    }

    private boolean pull(Player sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/pull <name>"));
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        Location from = target.getLocation();
        module.getBackService().setBackLocation(target.getUniqueId(), from);
        teleport(target, sender.getLocation());
        module.logAdminAction("[Other] " + sender.getName() + " pulled " + target.getName());
        return true;
    }

    private boolean push(Player sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/push <name>"));
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        Location from = sender.getLocation();
        module.getBackService().setBackLocation(sender.getUniqueId(), from);
        teleport(sender, target.getLocation());
        module.logAdminAction("[Other] " + sender.getName() + " pushed to " + target.getName());
        return true;
    }

    private boolean tpTo(Player sender, String targetName) {
        Player target = ru.managerfix.utils.NickResolver.resolve(targetName);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", targetName));
            return true;
        }
        module.getBackService().setBackLocation(sender.getUniqueId(), sender.getLocation());
        teleport(sender, target.getLocation());
        module.logAdminAction("[Other] " + sender.getName() + " teleported to " + target.getName());
        return true;
    }

    private boolean tpHere(Player sender, String targetName) {
        Player target = ru.managerfix.utils.NickResolver.resolve(targetName);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", targetName));
            return true;
        }
        module.getBackService().setBackLocation(target.getUniqueId(), target.getLocation());
        teleport(target, sender.getLocation());
        module.logAdminAction("[Other] " + sender.getName() + " teleported " + target.getName() + " here");
        return true;
    }

    private boolean tpLocation(Player sender, String xRaw, String yRaw, String zRaw) {
        try {
            double x = Double.parseDouble(xRaw);
            double y = Double.parseDouble(yRaw);
            double z = Double.parseDouble(zRaw);
            Location loc = new Location(sender.getWorld(), x + 0.5, y, z + 0.5, sender.getLocation().getYaw(), sender.getLocation().getPitch());
            module.getBackService().setBackLocation(sender.getUniqueId(), sender.getLocation());
            teleport(sender, loc);
            module.logAdminAction("[Other] " + sender.getName() + " teleported to location " + x + "," + y + "," + z);
            return true;
        } catch (NumberFormatException e) {
            MessageUtil.send(module.getPlugin(), sender, "other.tp.invalid-location");
            return true;
        }
    }

    private boolean tpTop(Player sender) {
        var world = sender.getWorld();
        var loc = sender.getLocation();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        var highest = world.getHighestBlockAt(x, z);
        Location dest = highest.getLocation().add(0.5, 1, 0.5);
        dest.setYaw(loc.getYaw());
        dest.setPitch(loc.getPitch());
        module.getBackService().setBackLocation(sender.getUniqueId(), sender.getLocation());
        teleport(sender, dest);
        module.logAdminAction("[Other] " + sender.getName() + " teleported to top");
        return true;
    }

    private void teleport(Player player, Location dest) {
        if (player == null || dest == null || dest.getWorld() == null) return;
        if (module.getPlugin() instanceof ManagerFix mf) {
            var tpaMod = mf.getModuleManager().getEnabledModule("tpa")
                .filter(m -> m instanceof TpaModule)
                .map(m -> (TpaModule) m)
                .orElse(null);
            if (tpaMod != null && tpaMod.getTpaService() != null) {
                tpaMod.getTpaService().teleportInstant(player, dest);
                return;
            }
        }
        player.teleport(dest);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if ("tp".equals(cmd)) {
            if (args.length == 1) {
                return List.of("to", "here", "location", "top").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .toList();
            }
            if (args.length == 2 && ("to".equalsIgnoreCase(args[0]) || "here".equalsIgnoreCase(args[0]))) {
                return ru.managerfix.utils.NickResolver.tabComplete(args[1], sender instanceof Player p ? p.getUniqueId() : null);
            }
        }
        if ("pull".equals(cmd) || "push".equals(cmd)) {
            if (args.length == 1) {
                return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
            }
        }
        return Collections.emptyList();
    }
}
