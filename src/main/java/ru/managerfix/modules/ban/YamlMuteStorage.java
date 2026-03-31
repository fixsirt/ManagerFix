package ru.managerfix.modules.ban;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.FileManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.scheduler.TaskScheduler;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public final class YamlMuteStorage implements MuteStorage {
    private final JavaPlugin plugin;
    private final TaskScheduler scheduler;
    private File file;
    private FileConfiguration config;

    public YamlMuteStorage(JavaPlugin plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        FileManager fm = plugin instanceof ru.managerfix.ManagerFix
                ? ((ru.managerfix.ManagerFix) plugin).getFileManager()
                : null;
        if (fm != null) {
            file = new File(fm.getDataFolderStorage(), "mutes.yml");
        } else {
            file = new File(plugin.getDataFolder(), "data/mutes.yml");
        }
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.WARNING, "Could not create mutes.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void shutdown() {
        config = null;
        file = null;
    }

    @Override
    public Optional<MuteRecord> getMute(UUID uuid) {
        if (config == null || uuid == null) return Optional.empty();
        String path = "mutes." + uuid;
        if (!config.contains(path)) return Optional.empty();
        String name = config.getString(path + ".name", "Unknown");
        String reason = config.getString(path + ".reason", "");
        String source = config.getString(path + ".source", "Console");
        long createdAt = config.getLong(path + ".createdAt", 0);
        long expiresAt = config.getLong(path + ".expiresAt", 0);
        MuteRecord r = new MuteRecord(uuid, name, reason, source, createdAt, expiresAt);
        if (r.isExpired()) return Optional.empty();
        return Optional.of(r);
    }

    @Override
    public void addMuteAsync(MuteRecord record, Runnable onDone) {
        scheduler.runAsync(() -> {
            if (config == null || record == null) return;
            String path = "mutes." + record.getTargetUuid();
            config.set(path + ".name", record.getTargetName());
            config.set(path + ".reason", record.getReason());
            config.set(path + ".source", record.getSource());
            config.set(path + ".createdAt", record.getCreatedAt());
            config.set(path + ".expiresAt", record.getExpiresAt());
            try {
                config.save(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (onDone != null) scheduler.runSync(onDone);
        });
    }

    @Override
    public void removeMuteAsync(UUID uuid, Runnable onDone) {
        scheduler.runAsync(() -> {
            if (config == null || uuid == null) return;
            String path = "mutes." + uuid;
            config.set(path, null);
            try {
                config.save(file);
            } catch (Exception ignored) {}
            if (onDone != null) scheduler.runSync(onDone);
        });
    }
}
