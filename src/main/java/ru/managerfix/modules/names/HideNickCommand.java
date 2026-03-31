package ru.managerfix.modules.names;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import java.util.*;

/**
 * Команда /hidenick - Скрыть/показать ники и префиксы над головами ВСЕХ игроков
 */
public final class HideNickCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;
    private final NamesModule namesModule;

    public HideNickCommand(ManagerFix plugin, NamesModule namesModule) {
        this.plugin = plugin;
        this.namesModule = namesModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("managerfix.command.hidenick")) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        // Переключаем глобальное состояние
        boolean currentlyHidden = namesModule.isGlobalNickHidden();
        boolean newHidden = !currentlyHidden;

        namesModule.setGlobalNickHidden(newHidden);

        // Обновляем всем игрокам
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (newHidden) {
                namesModule.hidePlayerNametag(player);
            } else {
                namesModule.showPlayerNametag(player);
            }
        }

        if (sender instanceof Player player) {
            if (newHidden) {
                player.sendMessage(MessageUtil.parse("<green>✓ Ник и префиксы ВСЕХ игроков СКРЫТЫ"));
            } else {
                player.sendMessage(MessageUtil.parse("<green>✓ Ник и префиксы ВСЕХ игроков ПОКАЗАНЫ"));
            }
        } else {
            if (newHidden) {
                sender.sendMessage(MessageUtil.parse("<green>✓ Ник и префиксы ВСЕХ игроков СКРЫТЫ"));
            } else {
                sender.sendMessage(MessageUtil.parse("<green>✓ Ник и префиксы ВСЕХ игроков ПОКАЗАНЫ"));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
