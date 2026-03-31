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

/**
 * Оптимизированное YAML хранилище мутов с кэшированием и отложенной записью.
 * - In-memory кэш для быстрого доступа
 * - Отложенная запись на диск (debounce 5 секунд)
 * - Thread-safe операции
 */
public final class CachedYamlMuteStorage implements MuteStorage {

    private final JavaPlugin plugin;
    private final TaskScheduler scheduler;
    private final ConcurrentHashMap<UUID, MuteRecord> muteCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService writeScheduler;
    private final Object saveLock = new Object();
    
    private File file;
    private FileConfiguration config;
    private volatile boolean dirty = false;
    private volatile boolean shutdown = false;

    public CachedYamlMuteStorage(JavaPlugin plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.writeScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MuteStorage-Writer");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void init() {
        FileManager fm = plugin instanceof ru.managerfix.ManagerFix m ? m.getFileManager() : null;
        if (fm != null) {
            file = new File(fm.getDataFolderStorage(), "mutes.yml");
        } else {
            file = new File(plugin.getDataFolder(), "data/mutes.yml");
        }
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception e) { LoggerUtil.log(java.util.logging.Level.WARNING, "Could not create mutes.yml", e); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadCache();
    }

    private void loadCache() {
        if (config == null || !config.contains("mutes")) return;
        
        for (String key : config.getConfigurationSection("mutes").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "mutes." + key;
                String name = config.getString(path + ".name", "Unknown");
                String reason = config.getString(path + ".reason", "");
                String source = config.getString(path + ".source", "Console");
                long createdAt = config.getLong(path + ".createdAt", 0);
                long expiresAt = config.getLong(path + ".expiresAt", 0);
                
                MuteRecord record = new MuteRecord(uuid, name, reason, source, createdAt, expiresAt);
                if (!record.isExpired()) {
                    muteCache.put(uuid, record);
                }
            } catch (Exception ignored) {}
        }
        
        LoggerUtil.debug("Loaded " + muteCache.size() + " active mutes from cache");
    }

    @Override
    public void shutdown() {
        shutdown = true;
        saveImmediately();
        writeScheduler.shutdown();
        try {
            if (!writeScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                writeScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            writeScheduler.shutdownNow();
        }
        muteCache.clear();
        config = null;
        file = null;
    }

    @Override
    public Optional<MuteRecord> getMute(UUID uuid) {
        if (uuid == null) return Optional.empty();
        MuteRecord record = muteCache.get(uuid);
        if (record == null) return Optional.empty();
        if (record.isExpired()) {
            muteCache.remove(uuid);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void addMuteAsync(MuteRecord record, Runnable onDone) {
        if (shutdown) {
            if (onDone != null) scheduler.runSync(onDone);
            return;
        }
        
        muteCache.put(record.getTargetUuid(), record);
        markDirty();
        
        if (onDone != null) scheduler.runSync(onDone);
    }

    @Override
    public void removeMuteAsync(UUID uuid, Runnable onDone) {
        if (shutdown) {
            if (onDone != null) scheduler.runSync(onDone);
            return;
        }
        
        muteCache.remove(uuid);
        if (config != null) {
            config.set("mutes." + uuid.toString(), null);
        }
        markDirty();
        
        if (onDone != null) scheduler.runSync(onDone);
    }

    private void markDirty() {
        if (shutdown) return;
        dirty = true;
        writeScheduler.schedule(this::saveIfDirty, 5, TimeUnit.SECONDS);
    }

    private void saveIfDirty() {
        if (dirty && !shutdown) {
            saveImmediately();
        }
    }

    private void saveImmediately() {
        synchronized (saveLock) {
            if (!dirty || config == null || file == null) return;
            
            long start = System.nanoTime();
            try {
                config.set("mutes", null);
                for (MuteRecord record : muteCache.values()) {
                    if (record.isExpired()) continue;
                    String path = "mutes." + record.getTargetUuid().toString();
                    config.set(path + ".name", record.getTargetName());
                    config.set(path + ".reason", record.getReason());
                    config.set(path + ".source", record.getSource());
                    config.set(path + ".createdAt", record.getCreatedAt());
                    config.set(path + ".expiresAt", record.getExpiresAt());
                }
                
                config.save(file);
                dirty = false;
                
                long elapsed = System.nanoTime() - start;
                LoggerUtil.debug("MuteStorage saved in " + (elapsed / 1_000_000) + " ms");
            } catch (IOException e) {
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save mutes.yml", e);
            }
        }
    }

    /**
     * Возвращает количество мутов в кэше.
     */
    public int getCachedMuteCount() {
        return muteCache.size();
    }
}
