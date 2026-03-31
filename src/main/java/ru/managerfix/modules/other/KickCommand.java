package ru.managerfix.modules.other;

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
 * Команда /kick - Кик игрока
 */
public final class KickCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public KickCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("managerfix.command.kick")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtil.parse("<red>Использование: /kick <игрок> [причина]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtil.parse("<red>Игрок не найден!"));
            return true;
        }

        if (sender instanceof Player) {
            if (!((Player) sender).canSee(target)) {
                sender.sendMessage(MessageUtil.parse("<red>Вы не можете кикнуть этого игрока!"));
                return true;
            }
        }

        String reason = args.length > 1
            ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
            : "Неизвестно";

        // Кикаем игрока
        String kickReason = "§cВас кикнул §f" + sender.getName() + "§r\n§7Причина: " + reason;
        target.kickPlayer(kickReason);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    suggestions.add(player.getName());
                }
            }
            
            return suggestions;
        }
        return Collections.emptyList();
    }
}
