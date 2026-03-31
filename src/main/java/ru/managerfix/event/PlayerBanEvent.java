package ru.managerfix.event;

import java.util.UUID;

/**
 * Fired when a player is banned.
 */
public class PlayerBanEvent extends ManagerFixEvent {

    private final UUID targetUuid;
    private final String targetName;
    private final String reason;
    private final String source;
    private final long expiresAt; // 0 = permanent

    public PlayerBanEvent(UUID targetUuid, String targetName, String reason, String source, long expiresAt) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reason = reason;
        this.source = source;
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

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt <= 0;
    }
}
