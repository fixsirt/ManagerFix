package ru.managerfix.storage;

import org.bukkit.Location;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Storage adapter for warps. All operations are async from the caller's perspective.
 */
public interface WarpStorageAdapter extends DataStorage {

    /**
     * Loads all warps asynchronously. Callback receives name -> location map (may be empty).
     */
    void loadWarpsAsync(Consumer<Map<String, Location>> callback);

    /**
     * Saves all warps asynchronously. onDone is invoked when save completes (optional).
     */
    void saveWarpsAsync(Map<String, Location> warps, Runnable onDone);
}
