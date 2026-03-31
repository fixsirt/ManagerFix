package ru.managerfix.api.chat;

/**
 * Результат фильтрации сообщения.
 */
public enum ChatFilterResult {
    /**
     * Сообщение разрешено.
     */
    ALLOWED,
    
    /**
     * Сообщение заблокировано.
     */
    BLOCKED,
    
    /**
     * Сообщение требует модерации.
     */
    NEEDS_MODERATION
}
