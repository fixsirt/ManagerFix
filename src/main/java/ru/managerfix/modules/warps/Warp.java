package ru.managerfix.modules.warps;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;

/**
 * Data model for a Warp.
 */
public class Warp {
    private final String name;
    private Location location;
    private String permission;
    private String category;
    private Material icon;
    private int slot;
    private List<String> description;
    private int teleportDelay;
    private boolean enabled;
    private boolean hidden;
    private java.util.UUID owner;

    public Warp(String name, Location location) {
        this.name = name;
        this.location = location;
        this.permission = "";
        this.category = "default";
        this.icon = Material.ENDER_PEARL;
        this.slot = -1;
        this.description = List.of("<#E0E0E0>Точка варпа: " + name);
        this.teleportDelay = 5;
        this.enabled = true;
        this.hidden = false;
        this.owner = null;
    }

    // Getters and Setters
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description; }
    public int getTeleportDelay() { return teleportDelay; }
    public void setTeleportDelay(int teleportDelay) { this.teleportDelay = teleportDelay; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public java.util.UUID getOwner() { return owner; }
    public void setOwner(java.util.UUID owner) { this.owner = owner; }
}
