package ru.managerfix.api.chat;

import org.bukkit.entity.Player;

/**
 * Обработчик сообщений.
 */
@FunctionalInterface
public interface MessageHandler {
    
    /**
     * Вызывается при отправке сообщения.
     * @param player отправитель
     * @param message сообщение
     * @return результат проверки
     */
    ChatFilterResult onMessage(Player player, String message);
}
