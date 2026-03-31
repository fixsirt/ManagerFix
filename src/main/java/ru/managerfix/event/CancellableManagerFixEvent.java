package ru.managerfix.event;

/**
 * Event that can be cancelled. Listeners may call setCancelled(true).
 */
public class CancellableManagerFixEvent extends ManagerFixEvent {

    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
