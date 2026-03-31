package ru.managerfix.modules.ban;

import java.util.*;
import java.util.function.Consumer;

/**
 * Interface for ban history storage.
 */
public interface BanHistoryStorage {

    void init();

    void appendAsync(BanHistoryEntry entry);

    void getHistoryAsync(Set<UUID> uuids, Consumer<List<BanHistoryEntry>> callback);
}
