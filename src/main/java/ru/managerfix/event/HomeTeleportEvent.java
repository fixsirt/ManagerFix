package ru.managerfix.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Fired when a player teleports to a home.
 */
public class HomeTeleportEvent extends ManagerFixEvent {

    private final Player player;
    private final String homeName;
    private final Location location;

    public HomeTeleportEvent(Player player, String homeName, Location location) {
        this.player = player;
        this.homeName = homeName;
        this.location = location;
    }

    public Player getPlayer() {
        return player;
    }

    public String getHomeName() {
        return homeName;
    }

    public Location getLocation() {
        return location;
    }
}
