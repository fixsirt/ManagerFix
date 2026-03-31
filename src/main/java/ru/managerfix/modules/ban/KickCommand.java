package ru.managerfix.modules.ban;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Команда /kick - кик игрока с сервера.
 */
public final class KickCommand implements CommandExecutor, TabCompleter {

    private final BanModule banModule;

    public KickCommand(BanModule banModule) {
        this.banModule = banModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.ban.kick")) {
            sender.sendMessage(MessageUtil.parse("<red>У вас нет прав для использования этой команды!"));
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

        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "Кик администратором";

        // Кик игрока
        target.kick(net.kyori.adventure.text.Component.text("Вы были кикнуты!\nПричина: " + reason));

        // Сообщение в чат
        if (banModule.isBroadcastBans()) {
            String broadcastMsg = "<#FAA300>⬤ КИК</#FAA300> <#FFFFFF>" + target.getName() + "</#FFFFFF> <#E0E0E0>был кикнут игроком</#E0E0E0> <#FFFFFF>" + sender.getName() + "</#FFFFFF> <#E0E0E0>—</#E0E0E0> <#FF4D00>" + reason + "</#FF4D00>";
            Bukkit.getServer().sendMessage(MessageUtil.parse(broadcastMsg));
        }

        sender.sendMessage(MessageUtil.parse("<#FAA300>Игрок <#FFFFFF>" + target.getName() + "</#FFFFFF> был кикнут!"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
