package ru.managerfix.event;

import ru.managerfix.profile.PlayerProfile;

/**
 * Fired when a profile is saved (async save completed).
 */
public class ProfileSaveEvent extends ManagerFixEvent {

    private final PlayerProfile profile;

    public ProfileSaveEvent(PlayerProfile profile) {
        this.profile = profile;
    }

    public PlayerProfile getProfile() {
        return profile;
    }
}
