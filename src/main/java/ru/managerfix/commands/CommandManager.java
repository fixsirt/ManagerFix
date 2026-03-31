package ru.managerfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.Objects;

/**
 * Centralized command registration. Commands use permission managerfix.<module>.<command>.
 */
public final class CommandManager {

    private final JavaPlugin plugin;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().severe("[CommandManager] Command '" + name + "' not found in plugin.yml!");
            return;
        }
        cmd.setExecutor(executor);
        if (tabCompleter != null) {
            cmd.setTabCompleter(tabCompleter);
        }
    }

    public void register(String name, CommandExecutor executor) {
        register(name, executor, null);
    }

    /**
     * Checks permission; if missing, sends message from lang and returns false.
     */
    public static boolean checkPermission(CommandSender sender, String permission, JavaPlugin plugin) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        MessageUtil.send(plugin, sender, "no-permission");
        return false;
    }

    /**
     * Checks command permission: managerfix.command.&lt;name&gt; OR fallback. If both missing, sends no-permission and returns false.
     */
    public static boolean checkCommandPermission(CommandSender sender, String commandName, String fallbackPermission, JavaPlugin plugin) {
        String cmdPerm = "managerfix.command." + commandName.toLowerCase();
        if (sender.hasPermission(cmdPerm) || (fallbackPermission != null && sender.hasPermission(fallbackPermission))) {
            return true;
        }
        MessageUtil.send(plugin, sender, "no-permission");
        return false;
    }

    /**
     * Checks if sender is player; if not, sends message from lang and returns false.
     */
    public static boolean checkPlayer(CommandSender sender, JavaPlugin plugin) {
        if (sender instanceof org.bukkit.entity.Player) {
            return true;
        }
        MessageUtil.send(plugin, sender, "player-only");
        return false;
    }
}
