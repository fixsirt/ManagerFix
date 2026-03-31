package ru.managerfix.event;

/**
 * Fired when a warp is deleted.
 */
public class WarpDeleteEvent extends ManagerFixEvent {

    private final String name;

    public WarpDeleteEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
