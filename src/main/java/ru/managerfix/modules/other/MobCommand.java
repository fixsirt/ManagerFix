package ru.managerfix.modules.other;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class MobCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public MobCommand(OtherModule module) {
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
        if ("killmob".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.killmob")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (!module.checkAndApplyCooldown(player, "killmob", "managerfix.bypass.cooldown")) {
                return true;
            }
            if (args.length < 2) {
                MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/killmob <type> <radius>"));
                return true;
            }
            EntityType type = parseType(args[0]);
            if (type == null) {
                MessageUtil.send(module.getPlugin(), sender, "other.mob.invalid-type");
                return true;
            }
            double radius = parseDouble(args[1], 0);
            if (radius <= 0) {
                MessageUtil.send(module.getPlugin(), sender, "other.mob.invalid-radius");
                return true;
            }
            int killed = 0;
            for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
                if (e instanceof LivingEntity le && le.getType() == type && !(le instanceof Player)) {
                    le.remove();
                    killed++;
                }
            }
            MessageUtil.send(module.getPlugin(), sender, "other.mob.killed", Map.of("count", String.valueOf(killed)));
            module.logAdminAction("[Other] " + sender.getName() + " killed " + killed + " " + type + " in radius " + radius);
            return true;
        }
        if ("spawnmob".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.spawnmob")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (!module.checkAndApplyCooldown(player, "spawnmob", "managerfix.bypass.cooldown")) {
                return true;
            }
            if (args.length < 2) {
                MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/spawnmob <type> <amount>"));
                return true;
            }
            EntityType type = parseType(args[0]);
            if (type == null || !type.isSpawnable() || !type.isAlive()) {
                MessageUtil.send(module.getPlugin(), sender, "other.mob.invalid-type");
                return true;
            }
            int amount = (int) Math.max(1, parseDouble(args[1], 1));
            Location loc = player.getLocation();
            for (int i = 0; i < amount; i++) {
                player.getWorld().spawnEntity(loc, type);
            }
            MessageUtil.send(module.getPlugin(), sender, "other.mob.spawned", Map.of("count", String.valueOf(amount)));
            module.logAdminAction("[Other] " + sender.getName() + " spawned " + amount + " " + type);
            return true;
        }
        return false;
    }

    private EntityType parseType(String raw) {
        try {
            return EntityType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private double parseDouble(String raw, double def) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toUpperCase();
            return Stream.of(EntityType.values())
                    .filter(EntityType::isAlive)
                    .map(EntityType::name)
                    .filter(n -> n.startsWith(prefix))
                    .map(String::toLowerCase)
                    .toList();
        }
        return Collections.emptyList();
    }
}
