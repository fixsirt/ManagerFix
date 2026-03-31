package ru.managerfix.modules.afk;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;

/**
 * /afk — toggle AFK state. Broadcast and EventBus handled by AfkManager.
 */
public final class AfkCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;
    private final AfkManager afkManager;

    public AfkCommand(ManagerFix plugin, AfkManager afkManager) {
        this.plugin = plugin;
        this.afkManager = afkManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        try {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            Player player = (Player) sender;
            if (!CommandManager.checkCommandPermission(sender, "afk", "managerfix.afk.use", plugin)) return true;
            afkManager.toggleAfk(player);
            if (afkManager.isAfk(player)) {
                MessageUtil.send(plugin, player, "afk.enter");
            } else {
                MessageUtil.send(plugin, player, "afk.leave");
            }
            return true;
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Error in /afk command", e);
            sender.sendMessage("§cПроизошла ошибка при выполнении команды. Проверьте консоль.");
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
