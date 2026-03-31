package ru.managerfix.modules.chat;

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

/**
 * Команда /clearchat - очищает чат для всех игроков.
 */
public final class ChatClearCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Проверка прав
        if (!sender.hasPermission("managerfix.command.clearchat")) {
            sender.sendMessage(MessageUtil.parse("<#C0280F>У вас нет прав для использования этой команды!"));
            return true;
        }

        // Очищаем чат всем игрокам
        int clearedCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Отправляем 150 пустых строк для очистки чата
            for (int i = 0; i < 150; i++) {
                player.sendMessage(" ");
            }
            // Разделитель
            player.sendMessage(MessageUtil.parse("<#1A120B>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</#1A120B>"));
            player.sendMessage(MessageUtil.parse("<#FF4D00>Чат был очищен администратором <#FFFFFF>" + sender.getName() + "</#FFFFFF></#FF4D00>"));
            player.sendMessage(MessageUtil.parse("<#1A120B>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</#1A120B>"));
            clearedCount++;
        }

        // Сообщение отправителю
        sender.sendMessage(MessageUtil.parse("<#FAA300>Чат очищен! Очищено чатов: <#FFFFFF>" + clearedCount + "</#FFFFFF>"));

        // Логирование в консоль
        Bukkit.getConsoleSender().sendMessage(MessageUtil.parse(
            "<#FF4D00>[ClearChat]</#FF4D00> Чат очищен игроком <#FFFFFF>" + sender.getName() + "</#FFFFFF>"
        ));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
