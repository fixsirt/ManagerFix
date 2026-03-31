package ru.managerfix.modules.ban;

import ru.managerfix.storage.SqlBanHistoryStorage;

import java.util.*;
import java.util.function.Consumer;

/**
 * Wrapper для адаптации SqlBanHistoryStorage к интерфейсу BanHistoryStorage.
 */
public final class SqlBanHistoryStorageWrapper implements BanHistoryStorage {

    private final SqlBanHistoryStorage storage;

    public SqlBanHistoryStorageWrapper(SqlBanHistoryStorage storage) {
        this.storage = storage;
    }

    @Override
    public void init() {
        storage.init();
    }

    @Override
    public void appendAsync(BanHistoryEntry entry) {
        storage.appendAsync(entry);
    }

    @Override
    public void getHistoryAsync(Set<UUID> uuids, Consumer<List<BanHistoryEntry>> callback) {
        storage.getHistoryAsync(uuids, callback);
    }
}
