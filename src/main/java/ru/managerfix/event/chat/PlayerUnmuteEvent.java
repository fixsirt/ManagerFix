package ru.managerfix.event.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Событие размута игрока.
 */
public class PlayerUnmuteEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final UUID playerUuid;
    private final String playerName;
    
    public PlayerUnmuteEvent(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
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
    
    // Вспомогательный класс для создания события с Player
    public static class WithPlayer extends PlayerUnmuteEvent {
        private final org.bukkit.entity.Player player;
        
        public WithPlayer(org.bukkit.entity.Player player) {
            super(player.getUniqueId(), player.getName());
            this.player = player;
        }
        
        public org.bukkit.entity.Player getPlayer() {
            return player;
        }
    }
}
