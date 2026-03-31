package ru.managerfix.modules.other;

import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BackService {

    private final Map<UUID, Location> lastBack = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastDeath = new ConcurrentHashMap<>();

    public void setBackLocation(UUID uuid, Location loc) {
        if (uuid == null || loc == null || loc.getWorld() == null) return;
        lastBack.put(uuid, loc.clone());
    }

    public void setDeathLocation(UUID uuid, Location loc) {
        if (uuid == null || loc == null || loc.getWorld() == null) return;
        lastDeath.put(uuid, loc.clone());
    }

    public Location getBackLocation(UUID uuid) {
        Location loc = uuid != null ? lastBack.get(uuid) : null;
        return loc != null ? loc.clone() : null;
    }

    public Location getDeathLocation(UUID uuid) {
        Location loc = uuid != null ? lastDeath.get(uuid) : null;
        return loc != null ? loc.clone() : null;
    }

    public void clear(UUID uuid) {
        if (uuid == null) return;
        lastBack.remove(uuid);
        lastDeath.remove(uuid);
    }
}
