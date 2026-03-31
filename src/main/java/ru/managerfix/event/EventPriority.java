package ru.managerfix.event;

/**
 * Priority for event handlers. LOW runs first, HIGH runs last.
 */
public enum EventPriority {
    LOW(0),
    NORMAL(1),
    HIGH(2);

    private final int order;

    EventPriority(int order) {
        this.order = order;
    }

    /**
     * Returns the order value for sorting (lower = runs first).
     */
    public int getOrder() {
        return order;
    }
}
