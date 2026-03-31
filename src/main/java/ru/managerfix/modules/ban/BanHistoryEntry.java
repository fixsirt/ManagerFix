package ru.managerfix.modules.ban;

import java.util.UUID;

public final class BanHistoryEntry {
    private final UUID targetUuid;
    private final String targetName;
    private final String action; // BAN, TEMPBAN, UNBAN
    private final String reason;
    private final String source;
    private final long createdAt;
    private final long expiresAt;

    public BanHistoryEntry(UUID targetUuid, String targetName, String action, String reason, String source, long createdAt, long expiresAt) {
        this.targetUuid = targetUuid;
        this.targetName = targetName != null ? targetName : "Unknown";
        this.action = action != null ? action : "BAN";
        this.reason = reason != null ? reason : "";
        this.source = source != null ? source : "Console";
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public String getAction() { return action; }
    public String getReason() { return reason; }
    public String getSource() { return source; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
}
