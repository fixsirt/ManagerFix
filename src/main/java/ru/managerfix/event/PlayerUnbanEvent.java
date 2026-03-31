package ru.managerfix.event;

import java.util.UUID;

/**
 * Fired when a player is unbanned.
 */
public class PlayerUnbanEvent extends ManagerFixEvent {

    private final UUID targetUuid;
    private final String targetName;
    private final String source;

    public PlayerUnbanEvent(UUID targetUuid, String targetName, String source) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.source = source;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getSource() {
        return source;
    }
}
