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
            sender.sendMessage(MessageUtil.parse("<#FF3366>У вас нет прав для использования этой команды!"));
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
            player.sendMessage(MessageUtil.parse("<#F0F4F8>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</#F0F4F8>"));
            player.sendMessage(MessageUtil.parse("<#FF3366>Чат был очищен администратором <#F0F4F8>" + sender.getName() + "</#F0F4F8></#FF3366>"));
            player.sendMessage(MessageUtil.parse("<#F0F4F8>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</#F0F4F8>"));
            clearedCount++;
        }

        // Сообщение отправителю
        sender.sendMessage(MessageUtil.parse("<#00C8FF>Чат очищен! Очищено чатов: <#F0F4F8>" + clearedCount + "</#F0F4F8>"));

        // Логирование в консоль
        Bukkit.getConsoleSender().sendMessage(MessageUtil.parse(
            "<#FF3366>[ClearChat]</#FF3366> Чат очищен игроком <#F0F4F8>" + sender.getName() + "</#F0F4F8>"
        ));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
