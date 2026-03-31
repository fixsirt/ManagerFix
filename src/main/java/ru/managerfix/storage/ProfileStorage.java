package ru.managerfix.storage;

import ru.managerfix.profile.PlayerProfile;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Storage for player profiles. All operations are async from the caller's perspective.
 */
public interface ProfileStorage extends DataStorage {

    /**
     * Loads a profile asynchronously. Callback is invoked with the result (empty if not found).
     */
    void loadProfileAsync(UUID uuid, Consumer<Optional<PlayerProfile>> callback);

    /**
     * Saves a profile asynchronously. onDone is invoked when save completes (optional).
     */
    void saveProfileAsync(PlayerProfile profile, Runnable onDone);

    /**
     * Finds UUIDs of profiles where metadata contains key=value (string equality).
     * Runs asynchronously and returns matching UUIDs (may be empty).
     */
    default void findUuidsByMetadataEquals(String key, String value, Consumer<java.util.Set<UUID>> callback) {
        callback.accept(java.util.Collections.emptySet());
    }
}
