package ru.managerfix.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Fired when a player's nickname is changed (oldNick → newNick).
 * If changedBy != null, the change was made by an admin (/nickadmin).
 */
public class PlayerNickChangeEvent extends ManagerFixEvent {

    private final UUID target;
    private final String oldNick;
    private final String newNick;
    private final UUID changedBy;

    public PlayerNickChangeEvent(UUID target, String oldNick, String newNick, UUID changedBy) {
        this.target = target;
        this.oldNick = oldNick;
        this.newNick = newNick;
        this.changedBy = changedBy;
    }

    public UUID getTarget() {
        return target;
    }

    /** Convenience: returns online player for target, or null if offline. */
    public Player getPlayer() {
        return Bukkit.getPlayer(target);
    }

    public String getOldNick() {
        return oldNick;
    }

    public String getNewNick() {
        return newNick;
    }

    /** Who changed the nick (admin UUID), or null if changed by self (/nick). */
    public UUID getChangedBy() {
        return changedBy;
    }

    public boolean isChangedByAdmin() {
        return changedBy != null;
    }
}
