package ru.managerfix.modules.tpa;

import java.util.UUID;

/**
 * A single TPA request: from requests to teleport to/from target.
 */
public final class TpaRequest {

    private final UUID from;
    private final UUID to;
    private final long expiresAt;
    private final boolean tpaHere; // if true, "from" wants "to" to teleport to "from"

    public TpaRequest(UUID from, UUID to, long expiresAt, boolean tpaHere) {
        this.from = from;
        this.to = to;
        this.expiresAt = expiresAt;
        this.tpaHere = tpaHere;
    }

    public UUID getFrom() {
        return from;
    }

    public UUID getTo() {
        return to;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isTpaHere() {
        return tpaHere;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
