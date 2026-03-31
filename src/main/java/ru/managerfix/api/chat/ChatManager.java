package ru.managerfix.api.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API для работы с чатом.
 * Предоставляет методы для отправки сообщений, управления каналами, мутами и т.д.
 */
public interface ChatManager {
    
    // ==================== Отправка сообщений ====================
    
    /**
     * Отправить сообщение в глобальный чат.
     * @param message сообщение
     */
    void sendGlobalMessage(Component message);
    
    /**
     * Отправить сообщение в локальный чат (с радиусом).
     * @param sender отправитель
     * @param message сообщение
     * @param radius радиус в блоках
     */
    void sendLocalMessage(Player sender, Component message, int radius);
    
    /**
     * Отправить личное сообщение.
     * @param sender отправитель
     * @param target получатель
     * @param message сообщение
     */
    void sendPrivateMessage(Player sender, Player target, Component message);
    
    /**
     * Отправить сообщение в канал.
     * @param sender отправитель
     * @param channel имя канала
     * @param message сообщение
     */
    void sendChannelMessage(Player sender, String channel, Component message);
    
    /**
     * Отправить сообщение всем игрокам с правом.
     * @param message сообщение
     * @param permission требуемое право
     */
    void broadcast(Component message, String permission);
    
    /**
     * Отправить сообщение только операторам.
     * @param message сообщение
     */
    void sendToOps(Component message);
    
    // ==================== Форматирование ====================
    
    /**
     * Отформатировать сообщение с использованием MiniMessage.
     * @param message исходное сообщение
     * @return отформатированное Component
     */
    Component formatMessage(String message);
    
    /**
     * Применить формат чата к сообщению.
     * @param sender отправитель
     * @param message сообщение
     * @param format формат чата
     * @return отформатированное сообщение
     */
    Component applyChatFormat(Player sender, Component message, ChatFormat format);
    
    // ==================== Фильтры ====================
    
    /**
     * Проверить сообщение на запрещённые слова.
     * @param sender отправитель
     * @param message сообщение
     * @return результат проверки
     */
    ChatFilterResult filterMessage(Player sender, String message);
    
    /**
     * Проверить, является ли сообщение спамом.
     * @param sender отправитель
     * @param message сообщение
     * @return true если спам
     */
    boolean isSpam(Player sender, String message);
    
    /**
     * Проверить на капс.
     * @param message сообщение
     * @return true если капс
     */
    boolean isCapsSpam(String message);
    
    /**
     * Проверить на рекламу.
     * @param message сообщение
     * @return true если реклама
     */
    boolean containsAds(String message);
    
    // ==================== Обработчики ====================
    
    /**
     * Зарегистрировать обработчик сообщений.
     * @param handler обработчик
     */
    void registerMessageHandler(MessageHandler handler);
    
    /**
     * Удалить обработчик сообщений.
     * @param handler обработчик
     */
    void unregisterMessageHandler(MessageHandler handler);
    
    /**
     * Зарегистрировать формат чата.
     * @param name имя формата
     * @param format формат
     */
    void registerChatFormat(String name, ChatFormat format);
    
    // ==================== Каналы ====================
    
    /**
     * Создать канал чата.
     * @param name имя канала
     * @param permission право доступа
     */
    void createChannel(String name, String permission);
    
    /**
     * Вступить в канал.
     * @param player игрок
     * @param channel имя канала
     */
    void joinChannel(Player player, String channel);
    
    /**
     * Покинуть канал.
     * @param player игрок
     * @param channel имя канала
     */
    void leaveChannel(Player player, String channel);
    
    /**
     * Получить список каналов игрока.
     * @param player игрок
     * @return список имён каналов
     */
    List<String> getPlayerChannels(Player player);
    
    /**
     * Установить активный канал для игрока.
     * @param player игрок
     * @param channel имя канала
     */
    void setActiveChannel(Player player, String channel);
    
    /**
     * Получить активный канал игрока.
     * @param player игрок
     * @return имя канала или null
     */
    String getActiveChannel(Player player);
    
    // ==================== Игнор-листы ====================
    
    /**
     * Добавить игрока в игнор-лист.
     * @param player игрок
     * @param ignored кого игнорировать
     */
    void addToIgnore(Player player, UUID ignored);
    
    /**
     * Удалить из игнор-листа.
     * @param player игрок
     * @param ignored кого удалить
     */
    void removeFromIgnore(Player player, UUID ignored);
    
    /**
     * Проверить, игнорирует ли игрок другого.
     * @param player игрок
     * @param ignored кого проверяем
     * @return true если игнорирует
     */
    boolean isIgnoring(Player player, UUID ignored);
    
    /**
     * Получить список игнорируемых игроков.
     * @param player игрок
     * @return список UUID
     */
    List<UUID> getIgnoredPlayers(Player player);
    
    // ==================== Mute ====================
    
    /**
     * Проверить, замучен ли игрок.
     * @param player игрок
     * @return true если замучен
     */
    boolean isMuted(Player player);
    
    /**
     * Получить информацию о муте.
     * @param player игрок
     * @return информация о муте
     */
    Optional<MuteInfo> getMuteInfo(Player player);
    
    /**
     * Замутить игрока.
     * @param player игрок
     * @param reason причина
     * @param source кто замутил
     * @param duration длительность в мс (0 = навсегда)
     */
    void mute(Player player, String reason, String source, long duration);
    
    /**
     * Размутить игрока.
     * @param player игрок
     */
    void unmute(Player player);
    
    // ==================== Утилиты ====================
    
    /**
     * Очистить чат для всех игроков.
     */
    void clearChatForAll();
    
    /**
     * Очистить чат для конкретного игрока.
     * @param player игрок
     */
    void clearChatFor(Player player);
    
    /**
     * Заблокировать чат (только админы могут писать).
     * @param locked true для блокировки
     */
    void setChatLocked(boolean locked);
    
    /**
     * Проверить, заблокирован ли чат.
     * @return true если заблокирован
     */
    boolean isChatLocked();
}
