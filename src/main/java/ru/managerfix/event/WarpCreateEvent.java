package ru.managerfix.event;

import org.bukkit.Location;

/**
 * Fired when a warp is created or updated.
 */
public class WarpCreateEvent extends ManagerFixEvent {

    private final String name;
    private final Location location;

    public WarpCreateEvent(String name, Location location) {
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }
}
