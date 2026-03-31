package ru.managerfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /invsee, /ecsee, /vanish, /sudo. Permissions: managerfix.command.<name>
 */
public final class AdminCommands implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;

    public AdminCommands(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (!sender.hasPermission("managerfix.command." + cmd)) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        switch (cmd) {
            case "vanish" -> {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                return toggleVanish((Player) sender);
            }
            case "invsee" -> {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                if (args.length < 1) {
                    MessageUtil.send(plugin, sender, "invsee.usage");
                    return true;
                }

                // Ищем онлайн игрока
                Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
                if (target == null || !target.isOnline()) {
                    MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args[0]));
                    return true;
                }

                // Открываем инвентарь
                ((Player) sender).openInventory(target.getInventory());
                MessageUtil.send(plugin, sender, "invsee.opened", Map.of("player", target.getName()));
                return true;
            }
            case "ecsee" -> {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                if (args.length < 1) return true;

                // Ищем онлайн игрока
                Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
                if (target == null || !target.isOnline()) {
                    MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args[0]));
                    return true;
                }

                // Открываем эндерсундук
                ((Player) sender).openInventory(target.getEnderChest());
                MessageUtil.send(plugin, sender, "ecsee.opened", Map.of("player", target.getName()));
                return true;
            }
            case "sudo" -> {
                if (args.length < 2) {
                    MessageUtil.send(plugin, sender, "sudo.usage");
                    return true;
                }
                Player targetSudo = ru.managerfix.utils.NickResolver.resolve(args[0]);
                if (targetSudo == null || !targetSudo.isOnline()) {
                    MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args[0]));
                    return true;
                }
                String commandLine = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Bukkit.dispatchCommand(targetSudo, commandLine);
                MessageUtil.send(plugin, sender, "sudo.executed", Map.of("player", targetSudo.getName()));
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean toggleVanish(Player player) {
        boolean vanished = isVanished(player);
        setVanished(player, !vanished);
        MessageUtil.send(plugin, player, vanished ? "vanish.disabled" : "vanish.enabled");
        return true;
    }

    public static boolean isVanished(Player player) {
        return player.hasMetadata("managerfix_vanish") && player.getMetadata("managerfix_vanish").get(0).asBoolean();
    }

    public static void setVanished(Player player, boolean vanish) {
        player.setMetadata("managerfix_vanish", new org.bukkit.metadata.FixedMetadataValue(
                Bukkit.getPluginManager().getPlugin("ManagerFix"), vanish));
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (vanish) {
                other.hidePlayer(Bukkit.getPluginManager().getPlugin("ManagerFix"), player);
            } else {
                other.showPlayer(Bukkit.getPluginManager().getPlugin("ManagerFix"), player);
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (("invsee".equals(cmd) || "ecsee".equals(cmd) || "sudo".equals(cmd)) && args.length == 1) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        if ("sudo".equals(cmd) && args.length >= 2) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
