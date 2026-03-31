package ru.managerfix.modules.ban;

import java.util.UUID;

/**
 * Single ban record: target, reason, source, expiry.
 */
public final class BanRecord {

    private final UUID targetUuid;
    private final String targetName;
    private final String reason;
    private final String source;
    private final long createdAt;
    private final long expiresAt; // 0 = permanent

    public BanRecord(UUID targetUuid, String targetName, String reason, String source, long createdAt, long expiresAt) {
        this.targetUuid = targetUuid;
        this.targetName = targetName != null ? targetName : "Unknown";
        this.reason = reason != null ? reason : "";
        this.source = source != null ? source : "Console";
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getReason() {
        return reason;
    }

    public String getSource() {
        return source;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt <= 0;
    }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
    }
}
