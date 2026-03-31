package ru.managerfix.modules.ban;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.scheduler.TaskScheduler;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * YAML storage for ban history events: data/ban_history.yml
 */
public final class YamlBanHistoryStorage implements BanHistoryStorage {
    private final JavaPlugin plugin;
    private final TaskScheduler scheduler;
    private File file;
    private FileConfiguration config;

    public YamlBanHistoryStorage(JavaPlugin plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void init() {
        file = new File(plugin.getDataFolder(), "data/ban_history.yml");
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception e) { LoggerUtil.log(java.util.logging.Level.WARNING, "Could not create ban_history.yml", e); }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void appendAsync(BanHistoryEntry entry) {
        if (entry == null) return;
        scheduler.runAsync(() -> {
            try {
                if (config == null) init();
                List<Map<?, ?>> raw = config.getMapList("entries");
                List<Map<String, Object>> list = new ArrayList<>();
                if (raw != null) {
                    for (Map<?, ?> m : raw) {
                        Map<String, Object> converted = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> e : m.entrySet()) {
                            if (e.getKey() != null) converted.put(String.valueOf(e.getKey()), e.getValue());
                        }
                        list.add(converted);
                    }
                }
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("uuid", entry.getTargetUuid().toString());
                map.put("name", entry.getTargetName());
                map.put("action", entry.getAction());
                map.put("reason", entry.getReason());
                map.put("source", entry.getSource());
                map.put("createdAt", entry.getCreatedAt());
                map.put("expiresAt", entry.getExpiresAt());
                list.add(map);
                config.set("entries", list);
                config.save(file);
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to append ban history", e);
            }
        });
    }

    @Override
    public void getHistoryAsync(Set<UUID> uuids, Consumer<List<BanHistoryEntry>> callback) {
        scheduler.runAsync(() -> {
            List<BanHistoryEntry> out = new ArrayList<>();
            try {
                if (config == null) init();
                List<Map<?, ?>> list = config.getMapList("entries");
                if (list != null) {
                    for (Map<?, ?> m : list) {
                        try {
                            String uuidStr = Objects.toString(m.get("uuid"), null);
                            if (uuidStr == null) continue;
                            UUID uuid = UUID.fromString(uuidStr);
                            if (uuids != null && !uuids.isEmpty() && !uuids.contains(uuid)) continue;
                            String name = Objects.toString(m.get("name"), "Unknown");
                            String action = Objects.toString(m.get("action"), "BAN");
                            String reason = Objects.toString(m.get("reason"), "");
                            String source = Objects.toString(m.get("source"), "Console");
                            long createdAt = toLong(m.get("createdAt"));
                            long expiresAt = toLong(m.get("expiresAt"));
                            out.add(new BanHistoryEntry(uuid, name, action, reason, source, createdAt, expiresAt));
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to read ban history", e);
            }
            List<BanHistoryEntry> finalOut = Collections.unmodifiableList(out);
            scheduler.runSync(() -> callback.accept(finalOut));
        });
    }

    private static long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception ignored) { return 0L; }
    }
}
