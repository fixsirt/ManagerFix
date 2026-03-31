package ru.managerfix.modules.ban;

import java.util.List;
import java.util.Optional;

/**
 * Хранилище для IP-банов. Все операции должны быть асинхронными.
 */
public interface BanIpStorage {

    Optional<BanIpRecord> getBan(String ipAddress);

    void addBanAsync(BanIpRecord record, Runnable onDone);

    void removeBanAsync(String ipAddress, Runnable onDone);

    void getBansAsync(java.util.function.Consumer<List<BanIpRecord>> callback);

    void init();

    void shutdown();
}
