package ru.managerfix.profile;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player profile: metadata, last activity, cooldowns, homes.
 * Thread-safe for async load/save.
 */
public final class PlayerProfile {

    private static final String HOME_SEP = ",";

    private final UUID uuid;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private volatile long lastActivity;
    private volatile String lastIpAddress;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    /** Home name -> "world,x,y,z,yaw,pitch" */
    private final Map<String, String> homes = new ConcurrentHashMap<>();
    /** True if profile was modified in memory (e.g. nick set) before async load completed. */
    private volatile boolean modified;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.lastActivity = System.currentTimeMillis();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public void touchActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    public String getLastIpAddress() {
        return lastIpAddress;
    }

    public void setLastIpAddress(String lastIpAddress) {
        this.lastIpAddress = lastIpAddress;
        this.modified = true;
    }

    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    public void setMetadata(String key, Object value) {
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
        modified = true;
    }

    public Map<String, Object> getMetadataSnapshot() {
        return new ConcurrentHashMap<>(metadata);
    }

    public void setMetadataFromSnapshot(Map<String, Object> snapshot) {
        metadata.clear();
        if (snapshot != null) {
            metadata.putAll(snapshot);
        }
    }

    /**
     * Merges data from a loaded profile into this one. Current metadata wins over loaded (preserves in-memory changes like nick).
     */
    public void mergeFrom(PlayerProfile loaded) {
        if (loaded == null) return;
        Map<String, Object> merged = new HashMap<>(loaded.getMetadataSnapshot());
        merged.putAll(getMetadataSnapshot());
        setMetadataFromSnapshot(merged);
        setHomesMap(loaded.getHomesMap());
        setCooldownsFromSnapshot(loaded.getCooldownsSnapshot());
        setLastActivity(loaded.getLastActivity());
    }

    /**
     * Returns true if cooldown for key is still active (expiresAt > now).
     */
    public boolean hasCooldown(String key) {
        Long expiresAt = cooldowns.get(key);
        if (expiresAt == null) return false;
        if (System.currentTimeMillis() >= expiresAt) {
            cooldowns.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Returns remaining cooldown in milliseconds, or 0 if none.
     */
    public long getCooldownRemaining(String key) {
        Long expiresAt = cooldowns.get(key);
        if (expiresAt == null) return 0;
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key);
            return 0;
        }
        return remaining;
    }

    /**
     * Sets cooldown for key. millis = duration from now.
     */
    public void setCooldown(String key, long millis) {
        if (millis <= 0) {
            cooldowns.remove(key);
            return;
        }
        cooldowns.put(key, System.currentTimeMillis() + millis);
        modified = true;
    }

    public void clearCooldown(String key) {
        cooldowns.remove(key);
    }

    public Map<String, Long> getCooldownsSnapshot() {
        long now = System.currentTimeMillis();
        Map<String, Long> out = new ConcurrentHashMap<>();
        cooldowns.forEach((k, v) -> {
            if (v > now) out.put(k, v);
        });
        return out;
    }

    public void setCooldownsFromSnapshot(Map<String, Long> expiresAtByKey) {
        cooldowns.clear();
        if (expiresAtByKey != null) {
            long now = System.currentTimeMillis();
            expiresAtByKey.forEach((k, v) -> {
                if (v > now) cooldowns.put(k, v);
            });
        }
    }

    // --- Homes (stored via getHomesMap/setHomesMap by storage) ---

    public Collection<String> getHomeNames() {
        return Collections.unmodifiableSet(new HashSet<>(homes.keySet()));
    }

    public Optional<Location> getHome(String name) {
        String raw = homes.get(name == null ? null : name.toLowerCase());
        if (raw == null || raw.isEmpty()) return Optional.empty();
        return parseLocation(raw);
    }

    public void setHome(String name, Location loc) {
        if (name == null || name.isEmpty()) return;
        if (loc == null || loc.getWorld() == null) return;
        homes.put(name.toLowerCase(), formatLocation(loc));
        modified = true;
    }

    public boolean removeHome(String name) {
        boolean removed = name != null && homes.remove(name.toLowerCase()) != null;
        if (removed) modified = true;
        return removed;
    }

    public int getHomesCount() {
        return homes.size();
    }

    /** For storage layer: save/load homes map. Value format "world,x,y,z,yaw,pitch". */
    public Map<String, String> getHomesMap() {
        return new HashMap<>(homes);
    }

    public void setHomesMap(Map<String, String> map) {
        homes.clear();
        if (map != null) homes.putAll(map);
    }

    private static String formatLocation(Location loc) {
        return loc.getWorld().getName() + HOME_SEP
                + loc.getX() + HOME_SEP + loc.getY() + HOME_SEP + loc.getZ() + HOME_SEP
                + loc.getYaw() + HOME_SEP + loc.getPitch();
    }

    private static Optional<Location> parseLocation(String raw) {
        String[] parts = raw.split(HOME_SEP, -1);
        if (parts.length < 6) return Optional.empty();
        try {
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) return Optional.empty();
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);
            return Optional.of(new Location(w, x, y, z, yaw, pitch));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
