package ru.managerfix.modules.ban;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import ru.managerfix.event.EventBus;
import ru.managerfix.event.PlayerBanEvent;
import ru.managerfix.event.PlayerUnbanEvent;
import ru.managerfix.scheduler.TaskScheduler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ban management: storage + EventBus. Caches active bans in memory.
 */
public final class BanManager {

    private final BanStorage storage;
    private final EventBus eventBus;
    private final TaskScheduler scheduler;
    private final BanHistoryStorage historyStorage;
    private final java.util.Map<UUID, BanRecord> cache = new ConcurrentHashMap<>();

    public BanManager(BanStorage storage, EventBus eventBus, TaskScheduler scheduler, BanHistoryStorage historyStorage) {
        this.storage = storage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.historyStorage = historyStorage;
    }

    public Optional<BanRecord> getBan(UUID uuid) {
        BanRecord cached = cache.get(uuid);
        if (cached != null) {
            if (cached.isExpired()) {
                cache.remove(uuid);
                return Optional.empty();
            }
            return Optional.of(cached);
        }
        Optional<BanRecord> opt = storage.getBan(uuid);
        opt.ifPresent(r -> cache.put(uuid, r));
        return opt;
    }

    public void ban(UUID targetUuid, String targetName, String reason, String source, long expiresAt, Runnable onDone) {
        long now = System.currentTimeMillis();
        BanRecord record = new BanRecord(targetUuid, targetName, reason, source, now, expiresAt);
        cache.put(targetUuid, record);
        
        // Выполняем всё асинхронно
        storage.addBanAsync(record, () -> {
            // Событие и история тоже асинхронно
            if (eventBus != null) {
                eventBus.callEvent(new PlayerBanEvent(targetUuid, targetName, reason, source, expiresAt));
            }
            if (historyStorage != null) {
                String action = expiresAt > 0 ? "TEMPBAN" : "BAN";
                historyStorage.appendAsync(new BanHistoryEntry(targetUuid, targetName, action, reason, source, now, expiresAt));
            }
            // Возвращаемся в главный поток только для кика и чата
            if (onDone != null) {
                // onDone уже запускается в главном потоке из BanCommand
                onDone.run();
            }
        });
    }

    /**
     * Синхронный бан (для использования внутри IP-бана).
     */
    public void banSync(UUID targetUuid, String targetName, String reason, String source, long expiresAt) {
        long now = System.currentTimeMillis();
        BanRecord record = new BanRecord(targetUuid, targetName, reason, source, now, expiresAt);
        cache.put(targetUuid, record);
        storage.addBanAsync(record, () -> {
            if (eventBus != null) {
                eventBus.callEvent(new PlayerBanEvent(targetUuid, targetName, reason, source, expiresAt));
            }
            if (historyStorage != null) {
                String action = expiresAt > 0 ? "TEMPBAN" : "BAN";
                historyStorage.appendAsync(new BanHistoryEntry(targetUuid, targetName, action, reason, source, now, expiresAt));
            }
        });
    }
    
    /**
     * Добавить бан в кэш (для пакетного бана).
     */
    public void addToCache(BanRecord record) {
        cache.put(record.getTargetUuid(), record);
    }

    public void unban(UUID targetUuid, String targetName, String source, Runnable onDone) {
        // Сначала удаляем из кэша
        cache.remove(targetUuid);
        // Затем удаляем из БД
        storage.removeBanAsync(targetUuid, () -> {
            // Запись в истории после успешного удаления
            if (eventBus != null) {
                eventBus.callEvent(new PlayerUnbanEvent(targetUuid, targetName, source));
            }
            if (historyStorage != null) {
                long now = System.currentTimeMillis();
                historyStorage.appendAsync(new BanHistoryEntry(targetUuid, targetName, "UNBAN", "", source, now, 0));
            }
            if (onDone != null) onDone.run();
        });
    }

    public void getBansAsync(java.util.function.Consumer<List<BanRecord>> callback) {
        storage.getBansAsync(callback);
    }

    /** Cached ban count (may not include all from storage until loaded). */
    public int getCachedBanCount() {
        return cache.size();
    }

    public BanStorage getStorage() {
        return storage;
    }
}
