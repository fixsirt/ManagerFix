package ru.managerfix.commands;

import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /fly, /heal, /feed, /god, /workbench, /enderchest. Permission: managerfix.command.<name>
 */
public final class PlayerUtilityCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;

    public PlayerUtilityCommand(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        String perm = "managerfix.command." + cmd;
        if (!sender.hasPermission(perm)) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        switch (cmd) {
            case "fly" -> {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                Player p = (Player) sender;
                p.setAllowFlight(!p.getAllowFlight());
                p.setFlying(p.getAllowFlight());
                MessageUtil.send(plugin, sender, p.getAllowFlight() ? "fly.enabled" : "fly.disabled");
                return true;
            }
            case "heal" -> {
                Player target = args.length > 0 ? ru.managerfix.utils.NickResolver.resolve(args[0]) : (sender instanceof Player ? (Player) sender : null);
                if (target == null || !target.isOnline()) {
                    MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args.length > 0 ? args[0] : ""));
                    return true;
                }
                target.setHealth((float) target.getAttribute(Attribute.MAX_HEALTH).getValue());
                target.setFoodLevel(20);
                target.setSaturation(20f);
                target.setFireTicks(0);
                if (target.equals(sender)) {
                    MessageUtil.send(plugin, sender, "heal.healed");
                } else {
                    MessageUtil.send(plugin, sender, "heal.healed-target", Map.of("player", target.getName()));
                    MessageUtil.send(plugin, target, "heal.healed");
                }
                return true;
            }
            case "feed" -> {
                Player target = args.length > 0 ? ru.managerfix.utils.NickResolver.resolve(args[0]) : (sender instanceof Player ? (Player) sender : null);
                if (target == null || !target.isOnline()) {
                    MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args.length > 0 ? args[0] : ""));
                    return true;
                }
                target.setFoodLevel(20);
                target.setSaturation(20f);
                if (target.equals(sender)) {
                    MessageUtil.send(plugin, sender, "feed.fed");
                } else {
                    MessageUtil.send(plugin, sender, "feed.fed-target", Map.of("player", target.getName()));
                    MessageUtil.send(plugin, target, "feed.fed");
                }
                return true;
            }
            case "god" -> {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                Player p = (Player) sender;
                boolean invul = !p.isInvulnerable();
                p.setInvulnerable(invul);
                MessageUtil.send(plugin, sender, invul ? "god.enabled" : "god.disabled");
                return true;
            }
            case "workbench" -> {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                Player p = (Player) sender;
                org.bukkit.inventory.MenuType.CRAFTING.builder()
                        .checkReachable(false)
                        .location(p.getLocation())
                        .build(p)
                        .open();
                return true;
            }
            case "enderchest" -> {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                ((Player) sender).openInventory(((Player) sender).getEnderChest());
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (("heal".equals(cmd) || "feed".equals(cmd)) && args.length == 1) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        return Collections.emptyList();
    }
}
