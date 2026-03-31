package ru.managerfix.modules.ban;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.FileManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.scheduler.TaskScheduler;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * YAML-based ban storage: data/bans.yml. Async via TaskScheduler.
 */
public final class YamlBanStorage implements BanStorage {

    private final JavaPlugin plugin;
    private final TaskScheduler scheduler;
    private File file;
    private FileConfiguration config;

    public YamlBanStorage(JavaPlugin plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
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
    }

    @Override
    public void shutdown() {
        config = null;
        file = null;
    }

    @Override
    public Optional<BanRecord> getBan(UUID uuid) {
        if (config == null || uuid == null) return Optional.empty();
        String path = "bans." + uuid.toString();
        if (!config.contains(path)) return Optional.empty();
        String name = config.getString(path + ".name", "Unknown");
        String reason = config.getString(path + ".reason", "");
        String source = config.getString(path + ".source", "Console");
        long createdAt = config.getLong(path + ".createdAt", 0);
        long expiresAt = config.getLong(path + ".expiresAt", 0);
        BanRecord r = new BanRecord(uuid, name, reason, source, createdAt, expiresAt);
        if (r.isExpired()) return Optional.empty();
        return Optional.of(r);
    }

    @Override
    public void addBanAsync(BanRecord record, Runnable onDone) {
        scheduler.runAsync(() -> {
            if (config == null) { if (onDone != null) scheduler.runSync(onDone); return; }
            String path = "bans." + record.getTargetUuid().toString();
            config.set(path + ".name", record.getTargetName());
            config.set(path + ".reason", record.getReason());
            config.set(path + ".source", record.getSource());
            config.set(path + ".createdAt", record.getCreatedAt());
            config.set(path + ".expiresAt", record.getExpiresAt());
            try {
                config.save(file);
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save bans.yml", e);
            }
            if (onDone != null) scheduler.runSync(onDone);
        });
    }

    @Override
    public void removeBanAsync(UUID uuid, Runnable onDone) {
        scheduler.runAsync(() -> {
            if (config != null && uuid != null) {
                config.set("bans." + uuid.toString(), null);
                try { config.save(file); } catch (Exception e) { LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save bans.yml", e); }
            }
            if (onDone != null) scheduler.runSync(onDone);
        });
    }

    @Override
    public void getBansAsync(Consumer<List<BanRecord>> callback) {
        scheduler.runAsync(() -> {
            List<BanRecord> list = new ArrayList<>();
            if (config != null && config.contains("bans")) {
                for (String key : config.getConfigurationSection("bans").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        getBan(uuid).ifPresent(list::add);
                    } catch (Exception ignored) {}
                }
            }
            scheduler.runSync(() -> callback.accept(list));
        });
    }
}
