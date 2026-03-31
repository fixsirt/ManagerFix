package ru.managerfix.event;

import ru.managerfix.profile.PlayerProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * Fired when a profile is loaded (async load completed).
 */
public class ProfileLoadEvent extends ManagerFixEvent {

    private final UUID uuid;
    private final Optional<PlayerProfile> profile;

    public ProfileLoadEvent(UUID uuid, Optional<PlayerProfile> profile) {
        this.uuid = uuid;
        this.profile = profile;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Optional<PlayerProfile> getProfile() {
        return profile;
    }
}
