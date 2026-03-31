package ru.managerfix.modules.ban;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.FileManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.scheduler.TaskScheduler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Оптимизированное YAML хранилище банов с кэшированием и отложенной записью.
 * - In-memory кэш для быстрого доступа
 * - Отложенная запись на диск (debounce 5 секунд)
 * - Thread-safe операции
 */
public final class CachedYamlBanStorage implements BanStorage {

    private final JavaPlugin plugin;
    private final TaskScheduler scheduler;
    private final ConcurrentHashMap<UUID, BanRecord> banCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService writeScheduler;
    private final Object saveLock = new Object();
    
    private File file;
    private FileConfiguration config;
    private volatile boolean dirty = false;
    private volatile boolean shutdown = false;

    public CachedYamlBanStorage(JavaPlugin plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        // Отложенная запись: пул из 1 потока
        this.writeScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BanStorage-Writer");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void init() {
        FileManager fm = plugin instanceof ru.managerfix.ManagerFix m ? m.getFileManager() : null;
        if (fm != null) {
            file = new File(fm.getDataFolderStorage(), "bans.yml");
        } else {
            file = new File(plugin.getDataFolder(), "data/bans.yml");
        }
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception e) { LoggerUtil.log(java.util.logging.Level.WARNING, "Could not create bans.yml", e); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadCache();
    }

    /**
     * Загружает данные из YAML в кэш при старте.
     */
    private void loadCache() {
        if (config == null || !config.contains("bans")) return;
        
        for (String key : config.getConfigurationSection("bans").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "bans." + key;
                String name = config.getString(path + ".name", "Unknown");
                String reason = config.getString(path + ".reason", "");
                String source = config.getString(path + ".source", "Console");
                long createdAt = config.getLong(path + ".createdAt", 0);
                long expiresAt = config.getLong(path + ".expiresAt", 0);
                
                BanRecord record = new BanRecord(uuid, name, reason, source, createdAt, expiresAt);
                if (!record.isExpired()) {
                    banCache.put(uuid, record);
                }
            } catch (Exception ignored) {}
        }
        
        LoggerUtil.debug("Loaded " + banCache.size() + " active bans from cache");
    }

    @Override
    public void shutdown() {
        shutdown = true;
        // Сохраняем всё перед закрытием
        saveImmediately();
        writeScheduler.shutdown();
        try {
            if (!writeScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                writeScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            writeScheduler.shutdownNow();
        }
        banCache.clear();
        config = null;
        file = null;
    }

    @Override
    public Optional<BanRecord> getBan(UUID uuid) {
        if (uuid == null) return Optional.empty();
        BanRecord record = banCache.get(uuid);
        if (record == null) return Optional.empty();
        if (record.isExpired()) {
            banCache.remove(uuid);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void addBanAsync(BanRecord record, Runnable onDone) {
        if (shutdown) {
            if (onDone != null) scheduler.runSync(onDone);
            return;
        }
        
        // Добавляем в кэш сразу
        banCache.put(record.getTargetUuid(), record);
        markDirty();
        
        if (onDone != null) scheduler.runSync(onDone);
    }

    @Override
    public void removeBanAsync(UUID uuid, Runnable onDone) {
        if (shutdown) {
            if (onDone != null) scheduler.runSync(onDone);
            return;
        }
        
        // Удаляем из кэша сразу
        banCache.remove(uuid);
        if (config != null) {
            config.set("bans." + uuid.toString(), null);
        }
        markDirty();
        
        if (onDone != null) scheduler.runSync(onDone);
    }

    @Override
    public void getBansAsync(Consumer<List<BanRecord>> callback) {
        List<BanRecord> list = new ArrayList<>(banCache.values());
        scheduler.runSync(() -> callback.accept(list));
    }

    /**
     * Помечает данные как изменённые и планирует отложенную запись.
     */
    private void markDirty() {
        if (shutdown) return;
        dirty = true;
        // Планируем запись через 5 секунд, если не было новых изменений
        writeScheduler.schedule(this::saveIfDirty, 5, TimeUnit.SECONDS);
    }

    private void saveIfDirty() {
        if (dirty && !shutdown) {
            saveImmediately();
        }
    }

    /**
     * Немедленное сохранение на диск.
     */
    private void saveImmediately() {
        synchronized (saveLock) {
            if (!dirty || config == null || file == null) return;
            
            long start = System.nanoTime();
            try {
                // Обновляем конфиг из кэша
                config.set("bans", null); // Очищаем секцию
                for (BanRecord record : banCache.values()) {
                    if (record.isExpired()) continue;
                    String path = "bans." + record.getTargetUuid().toString();
                    config.set(path + ".name", record.getTargetName());
                    config.set(path + ".reason", record.getReason());
                    config.set(path + ".source", record.getSource());
                    config.set(path + ".createdAt", record.getCreatedAt());
                    config.set(path + ".expiresAt", record.getExpiresAt());
                }
                
                config.save(file);
                dirty = false;
                
                long elapsed = System.nanoTime() - start;
                LoggerUtil.debug("BanStorage saved in " + (elapsed / 1_000_000) + " ms");
            } catch (IOException e) {
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save bans.yml", e);
            }
        }
    }

    /**
     * Возвращает количество банов в кэше.
     */
    public int getCachedBanCount() {
        return banCache.size();
    }
}
