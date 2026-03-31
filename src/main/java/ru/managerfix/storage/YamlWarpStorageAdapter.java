package ru.managerfix.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.scheduler.TaskScheduler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * YAML-based warp storage: data/warps.yml.
 * All load/save run async via TaskScheduler.
 */
public final class YamlWarpStorageAdapter implements WarpStorageAdapter {

    private final JavaPlugin plugin;
    private final File file;
    private final TaskScheduler scheduler;

    public YamlWarpStorageAdapter(JavaPlugin plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/warps.yml");
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        file.getParentFile().mkdirs();
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void loadWarpsAsync(Consumer<Map<String, Location>> callback) {
        scheduler.runAsync(() -> {
            Map<String, Location> warps = loadWarpsSync();
            scheduler.runSync(() -> callback.accept(warps));
        });
    }

    private Map<String, Location> loadWarpsSync() {
        Map<String, Location> warps = new ConcurrentHashMap<>();
        if (!file.exists()) {
            return warps;
        }
        try {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection section = cfg.getConfigurationSection("warps");
            if (section == null) return warps;
            for (String key : section.getKeys(false)) {
                String path = "warps." + key;
                World world = Bukkit.getWorld(Objects.requireNonNull(cfg.getString(path + ".world")));
                if (world == null) continue;
                double x = cfg.getDouble(path + ".x");
                double y = cfg.getDouble(path + ".y");
                double z = cfg.getDouble(path + ".z");
                float yaw = (float) cfg.getDouble(path + ".yaw", 0);
                float pitch = (float) cfg.getDouble(path + ".pitch", 0);
                warps.put(key.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
            }
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to load warps", e);
        }
        return warps;
    }

    @Override
    public void saveWarpsAsync(Map<String, Location> warps, Runnable onDone) {
        scheduler.runAsync(() -> {
            saveWarpsSync(warps != null ? warps : Map.of());
            if (onDone != null) {
                scheduler.runSync(onDone);
            }
        });
    }

    private void saveWarpsSync(Map<String, Location> warps) {
        file.getParentFile().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Location> e : warps.entrySet()) {
            String path = "warps." + e.getKey();
            Location loc = e.getValue();
            cfg.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
            cfg.set(path + ".x", loc.getX());
            cfg.set(path + ".y", loc.getY());
            cfg.set(path + ".z", loc.getZ());
            cfg.set(path + ".yaw", loc.getYaw());
            cfg.set(path + ".pitch", loc.getPitch());
        }
        try {
            cfg.save(file);
        } catch (IOException ex) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save warps.yml", ex);
        }
    }
}
