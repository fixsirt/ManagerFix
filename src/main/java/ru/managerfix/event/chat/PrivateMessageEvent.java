package ru.managerfix.event.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Событие отправки личного сообщения.
 */
public class PrivateMessageEvent extends Event implements Cancellable {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final Player sender;
    private Component message;
    private final Player target;
    private boolean cancelled = false;
    
    public PrivateMessageEvent(@NotNull Player sender, Player target, Component message) {
        this.sender = sender;
        this.message = message;
        this.target = target;
    }
    
    public Player getSender() {
        return sender;
    }
    
    public Component getMessage() {
        return message;
    }
    
    public void setMessage(Component message) {
        this.message = message;
    }
    
    public Player getTarget() {
        return target;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
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
