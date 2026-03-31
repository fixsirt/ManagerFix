package ru.managerfix.event.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Событие мута игрока.
 */
public class PlayerMuteEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final UUID playerUuid;
    private final String playerName;
    private String reason;
    private final String source;
    private long duration;
    private boolean cancelled = false;
    
    public PlayerMuteEvent(UUID playerUuid, String playerName, String reason, String source, long duration) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.reason = reason;
        this.source = source;
        this.duration = duration;
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
     * Причина мута.
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Установить причину.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    /**
     * Кто замутил.
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Длительность в мс (0 = навсегда).
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * Установить длительность.
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    // Вспомогательный класс для создания события с Player
    public static class WithPlayer extends PlayerMuteEvent {
        private final org.bukkit.entity.Player player;
        
        public WithPlayer(org.bukkit.entity.Player player, String reason, String source, long duration) {
            super(player.getUniqueId(), player.getName(), reason, source, duration);
            this.player = player;
        }
        
        public org.bukkit.entity.Player getPlayer() {
            return player;
        }
    }
}
