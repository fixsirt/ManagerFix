package ru.managerfix.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.scheduler.TaskScheduler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * YAML-based profile storage: data/players/<uuid>.yml.
 * All load/save run async via TaskScheduler.
 */
public final class YamlProfileStorage implements ProfileStorage {

    private final JavaPlugin plugin;
    private final File playersFolder;
    private final TaskScheduler scheduler;

    public YamlProfileStorage(JavaPlugin plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "data/players");
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void loadProfileAsync(UUID uuid, Consumer<Optional<PlayerProfile>> callback) {
        scheduler.runAsync(() -> {
            Optional<PlayerProfile> result = loadProfileSync(uuid);
            scheduler.runSync(() -> callback.accept(result));
        });
    }

    private Optional<PlayerProfile> loadProfileSync(UUID uuid) {
        File file = new File(playersFolder, uuid + ".yml");
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            PlayerProfile profile = new PlayerProfile(uuid);
            profile.setLastActivity(cfg.getLong("lastActivity", System.currentTimeMillis()));
            if (cfg.contains("metadata") && cfg.isConfigurationSection("metadata")) {
                Map<String, Object> meta = new HashMap<>();
                ConfigurationSection section = cfg.getConfigurationSection("metadata");
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        meta.put(key, section.get(key));
                    }
                }
                profile.setMetadataFromSnapshot(meta);
            }
            if (cfg.contains("cooldowns") && cfg.isList("cooldowns")) {
                List<?> list = cfg.getList("cooldowns");
                if (list != null) {
                    Map<String, Long> cooldowns = new HashMap<>();
                    for (Object o : list) {
                        if (o instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> entry = (Map<String, Object>) o;
                            Object k = entry.get("key");
                            Object v = entry.get("expiresAt");
                            if (k instanceof String key && v instanceof Number n) {
                                cooldowns.put(key, n.longValue());
                            }
                        }
                    }
                    profile.setCooldownsFromSnapshot(cooldowns);
                }
            }
            if (cfg.contains("homes") && cfg.isConfigurationSection("homes")) {
                Map<String, String> homesMap = new HashMap<>();
                ConfigurationSection homesSection = cfg.getConfigurationSection("homes");
                if (homesSection != null) {
                    for (String key : homesSection.getKeys(false)) {
                        String val = homesSection.getString(key);
                        if (val != null) homesMap.put(key, val);
                    }
                }
                profile.setHomesMap(homesMap);
            }
            return Optional.of(profile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to load profile " + uuid, e);
            return Optional.empty();
        }
    }

    @Override
    public void saveProfileAsync(PlayerProfile profile, Runnable onDone) {
        scheduler.runAsync(() -> {
            saveProfileSync(profile);
            if (onDone != null) {
                scheduler.runSync(onDone);
            }
        });
    }

    private void saveProfileSync(PlayerProfile profile) {
        File file = new File(playersFolder, profile.getUuid() + ".yml");
        file.getParentFile().mkdirs();
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("uuid", profile.getUuid().toString());
        cfg.set("lastActivity", profile.getLastActivity());
        Map<String, Object> meta = profile.getMetadataSnapshot();
        if (!meta.isEmpty()) {
            ConfigurationSection section = cfg.createSection("metadata");
            meta.forEach(section::set);
        }
        Map<String, Long> cooldowns = profile.getCooldownsSnapshot();
        if (!cooldowns.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            cooldowns.forEach((key, expiresAt) ->
                    list.add(Map.of("key", key, "expiresAt", expiresAt)));
            cfg.set("cooldowns", list);
        }
        Map<String, String> homesMap = profile.getHomesMap();
        if (!homesMap.isEmpty()) {
            ConfigurationSection homesSection = cfg.createSection("homes");
            homesMap.forEach(homesSection::set);
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save profile " + profile.getUuid(), e);
        }
    }

    @Override
    public void findUuidsByMetadataEquals(String key, String value, java.util.function.Consumer<java.util.Set<java.util.UUID>> callback) {
        scheduler.runAsync(() -> {
            java.util.Set<java.util.UUID> result = new java.util.HashSet<>();
            File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                        if (cfg.contains("metadata." + key)) {
                            String v = cfg.getString("metadata." + key);
                            if (v != null && v.equals(value)) {
                                String uuidStr = cfg.getString("uuid");
                                if (uuidStr == null) {
                                    // derive from filename
                                    String name = file.getName();
                                    uuidStr = name.substring(0, name.length() - 4);
                                }
                                try {
                                    result.add(java.util.UUID.fromString(uuidStr));
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            java.util.Set<java.util.UUID> finalResult = java.util.Collections.unmodifiableSet(result);
            scheduler.runSync(() -> callback.accept(finalResult));
        });
    }
}
