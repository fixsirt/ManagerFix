package ru.managerfix.modules.warps;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.event.EventBus;
import ru.managerfix.event.WarpCreateEvent;
import ru.managerfix.event.WarpDeleteEvent;
import ru.managerfix.scheduler.TaskScheduler;
import ru.managerfix.storage.WarpStorageAdapter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory warp cache with persistence via WarpStorageAdapter.
 * Fires WarpCreateEvent and WarpDeleteEvent via EventBus when configured.
 */
public final class WarpStorage {

    private final JavaPlugin plugin;
    private final WarpStorageAdapter adapter;
    private final TaskScheduler scheduler;
    private final EventBus eventBus;
    private final Map<String, Location> warps = new ConcurrentHashMap<>();

    public WarpStorage(JavaPlugin plugin, WarpStorageAdapter adapter, TaskScheduler scheduler, EventBus eventBus) {
        this.plugin = plugin;
        this.adapter = adapter;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
    }

    public Optional<Location> getWarp(String name) {
        return Optional.ofNullable(warps.get(name.toLowerCase()));
    }

    public void setWarp(String name, Location location) {
        String key = name.toLowerCase();
        warps.put(key, location);
        if (eventBus != null) {
            eventBus.callEvent(new WarpCreateEvent(name, location));
        }
    }

    public boolean removeWarp(String name) {
        String key = name.toLowerCase();
        boolean removed = warps.remove(key) != null;
        if (removed && eventBus != null) {
            eventBus.callEvent(new WarpDeleteEvent(name));
        }
        return removed;
    }

    /**
     * Loads warps from adapter asynchronously. onLoaded runs on main thread when done.
     */
    public void loadAsync(Runnable onLoaded) {
        adapter.loadWarpsAsync(warpsMap -> {
            if (scheduler != null) {
                scheduler.runSync(() -> {
                    warps.clear();
                    warps.putAll(warpsMap != null ? warpsMap : Map.of());
                    if (onLoaded != null) onLoaded.run();
                });
            } else {
                warps.clear();
                warps.putAll(warpsMap != null ? warpsMap : Map.of());
                if (onLoaded != null) onLoaded.run();
            }
        });
    }

    /**
     * Saves to adapter asynchronously. Call after setWarp/removeWarp.
     */
    public void saveAsync(TaskScheduler taskScheduler) {
        Map<String, Location> snapshot = new HashMap<>(warps);
        adapter.saveWarpsAsync(snapshot, null);
    }

    public Collection<String> getWarpNames() {
        return Collections.unmodifiableCollection(new TreeSet<>(warps.keySet()));
    }

    public List<String> getWarpNamesList() {
        return new ArrayList<>(new TreeSet<>(warps.keySet()));
    }

    public Map<String, Location> getWarps() {
        return new HashMap<>(warps);
    }

    public int size() {
        return warps.size();
    }

    /**
     * Flushes cache to storage (async). Call on module disable.
     */
    public void save() {
        adapter.saveWarpsAsync(new HashMap<>(warps), null);
    }
}
