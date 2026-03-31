package ru.managerfix.event;

/**
 * Base class for all ManagerFix internal events.
 * Events are dispatched synchronously via EventBus.
 */
public class ManagerFixEvent {

    private final long timestamp = System.currentTimeMillis();

    public long getTimestamp() {
        return timestamp;
    }
}
