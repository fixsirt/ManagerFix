package ru.managerfix.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Fired when a player uses RTP (before or after teleport).
 */
public class RTPEvent extends ManagerFixEvent {

    private final Player player;
    private final Location from;
    private final Location to;

    public RTPEvent(Player player, Location from, Location to) {
        this.player = player;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }
}
