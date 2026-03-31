package ru.managerfix.modules.ban;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage for bans (YAML or SQL). All operations should be async from caller.
 */
public interface BanStorage {

    Optional<BanRecord> getBan(UUID uuid);

    void addBanAsync(BanRecord record, Runnable onDone);

    void removeBanAsync(UUID uuid, Runnable onDone);

    void getBansAsync(java.util.function.Consumer<List<BanRecord>> callback);

    void init();

    void shutdown();
}
