package ru.managerfix.modules.ban;

import ru.managerfix.scheduler.TaskScheduler;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MuteManager {
    private final MuteStorage storage;
    private final TaskScheduler scheduler;
    private final ConcurrentMap<UUID, MuteRecord> cache = new ConcurrentHashMap<>();

    public MuteManager(MuteStorage storage, TaskScheduler scheduler) {
        this.storage = storage;
        this.scheduler = scheduler;
    }

    public Optional<MuteRecord> getMute(UUID uuid) {
        MuteRecord cached = cache.get(uuid);
        if (cached != null) {
            if (cached.isExpired()) {
                cache.remove(uuid);
                return Optional.empty();
            }
            return Optional.of(cached);
        }
        // Загружаем из БД и проверяем на истечение
        Optional<MuteRecord> opt = storage.getMute(uuid);
        if (opt.isPresent()) {
            MuteRecord record = opt.get();
            if (record.isExpired()) {
                // Мут истёк - не сохраняем в кэш
                return Optional.empty();
            }
            cache.put(uuid, record);
        }
        return opt;
    }

    public boolean isMuted(UUID uuid) {
        return getMute(uuid).isPresent();
    }

    public void mute(UUID uuid, String name, String reason, String source, long expiresAt, Runnable onDone) {
        long now = System.currentTimeMillis();
        MuteRecord r = new MuteRecord(uuid, name, reason, source, now, expiresAt);
        cache.put(uuid, r);
        storage.addMuteAsync(r, onDone);
    }

    public void unmute(UUID uuid, Runnable onDone) {
        // Сначала удаляем из кэша
        cache.remove(uuid);
        // Затем удаляем из БД
        storage.removeMuteAsync(uuid, onDone);
    }
}
