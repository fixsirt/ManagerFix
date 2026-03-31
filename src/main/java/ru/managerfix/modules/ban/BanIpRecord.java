package ru.managerfix.modules.ban;

import java.util.UUID;

/**
 * Запись IP-бана: IP, причина, создатель, срок действия.
 */
public final class BanIpRecord {

    private final UUID targetUuid;
    private final String ipAddress;
    private final String reason;
    private final String source;
    private final long createdAt;
    private final long expiresAt; // 0 = навсегда

    public BanIpRecord(UUID targetUuid, String ipAddress, String reason, String source, long createdAt, long expiresAt) {
        this.targetUuid = targetUuid;
        this.ipAddress = ipAddress;
        this.reason = reason != null ? reason : "";
        this.source = source != null ? source : "Console";
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getIpAddress() {
        return ipAddress;
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
