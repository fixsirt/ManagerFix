package ru.managerfix.event.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Событие отправки сообщения игроком.
 */
public class PlayerChatEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final UUID playerUuid;
    private final String playerName;
    private Component message;
    private String channel;
    private final Set<UUID> recipients = ConcurrentHashMap.newKeySet();
    private boolean cancelled = false;
    
    public PlayerChatEvent(Player player, Component message) {
        this.playerUuid = player.getUniqueId();
        this.playerName = player.getName();
        this.message = message;
        this.channel = "global";
        for (Player p : Bukkit.getOnlinePlayers()) {
            recipients.add(p.getUniqueId());
        }
    }
    
    public Player getPlayer() {
        return Bukkit.getPlayer(playerUuid);
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    /**
     * Получить сообщение.
     */
    public Component getMessage() {
        return message;
    }
    
    /**
     * Установить сообщение.
     */
    public void setMessage(Component message) {
        this.message = message;
    }
    
    /**
     * Получить канал чата.
     */
    public String getChannel() {
        return channel;
    }
    
    /**
     * Установить канал чата.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    /**
     * Получить получателей сообщения.
     */
    public Set<UUID> getRecipients() {
        return recipients;
    }
    
    /**
     * Добавить получателя.
     */
    public void addRecipient(UUID player) {
        recipients.add(player);
    }
    
    /**
     * Удалить получателя.
     */
    public void removeRecipient(UUID player) {
        recipients.remove(player);
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
