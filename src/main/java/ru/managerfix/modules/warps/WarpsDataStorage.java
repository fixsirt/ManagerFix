package ru.managerfix.modules.warps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.FileManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.database.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * YAML-based warps data storage: data/warps.yml
 * Также поддерживает SQL через DatabaseManager
 */
public final class WarpsDataStorage {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    private final Map<String, Warp> warps = new HashMap<>();
    private final DatabaseManager databaseManager;
    private final boolean useSql;

    public WarpsDataStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Проверяем используется ли SQL
        boolean isMySql = false;
        DatabaseManager dbManager = null;
        if (plugin instanceof ManagerFix mf) {
            isMySql = mf.isMySqlStorage();
            dbManager = mf.getDatabaseManager();
        }
        
        this.useSql = isMySql;
        this.databaseManager = dbManager;
    }

    public void init() {
        FileManager fm = plugin instanceof ru.managerfix.ManagerFix
                ? ((ru.managerfix.ManagerFix) plugin).getFileManager()
                : null;
        if (fm != null) {
            configFile = new File(fm.getDataFolderStorage(), "warps.yml");
        } else {
            configFile = new File(plugin.getDataFolder(), "data/warps.yml");
        }
        configFile.getParentFile().mkdirs();
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.WARNING, "Could not create warps.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadWarps();
    }

    public void shutdown() {
        if (!useSql) {
            save();
        }
        config = null;
        configFile = null;
        warps.clear();
    }

    private void loadWarps() {
        warps.clear();

        // В SQL-режиме загружаем из базы данных
        if (useSql && databaseManager != null) {
            loadWarpsFromSql();
            return;
        }

        // YAML-режим
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection ws = section.getConfigurationSection(key);
            if (ws == null) continue;

            String worldName = ws.getString("world");
            if (worldName == null || Bukkit.getWorld(worldName) == null) continue;

            Location loc = new Location(
                    Bukkit.getWorld(worldName),
                    ws.getDouble("x"),
                    ws.getDouble("y"),
                    ws.getDouble("z"),
                    (float) ws.getDouble("yaw"),
                    (float) ws.getDouble("pitch")
            );

            Warp warp = new Warp(key, loc);
            warp.setPermission(ws.getString("permission", ""));
            warp.setCategory(ws.getString("category", "default"));
            warp.setIcon(Material.valueOf(ws.getString("icon", "ENDER_PEARL")));
            warp.setSlot(ws.getInt("slot", -1));
            warp.setDescription(ws.getStringList("description"));
            warp.setTeleportDelay(ws.getInt("teleport-delay", 5));
            warp.setEnabled(ws.getBoolean("enabled", true));
            warp.setHidden(ws.getBoolean("hidden", false));
            String ownerUuid = ws.getString("owner");
            if (ownerUuid != null && !ownerUuid.isEmpty()) {
                try {
                    warp.setOwner(java.util.UUID.fromString(ownerUuid));
                } catch (IllegalArgumentException ignored) {}
            }

            warps.put(key.toLowerCase(), warp);
        }
    }

    private void loadWarpsFromSql() {
        try (Connection conn = databaseManager.getConnection();
             java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(
                     "SELECT name, world, x, y, z, yaw, pitch, permission, category, icon, slot, description, teleport_delay, enabled, hidden, owner FROM warps")) {
            while (rs.next()) {
                String key = rs.getString("name");
                String worldName = rs.getString("world");
                if (worldName == null || Bukkit.getWorld(worldName) == null) {
                    LoggerUtil.log(java.util.logging.Level.WARNING, "Warp '" + key + "' skipped: world '" + worldName + "' not loaded", null);
                    continue;
                }
                Location loc = new Location(
                        Bukkit.getWorld(worldName),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
                Warp warp = new Warp(key, loc);
                warp.setPermission(rs.getString("permission") != null ? rs.getString("permission") : "");
                warp.setCategory(rs.getString("category") != null ? rs.getString("category") : "default");
                try { warp.setIcon(Material.valueOf(rs.getString("icon"))); } catch (Exception e) { warp.setIcon(Material.ENDER_PEARL); }
                warp.setSlot(rs.getInt("slot"));
                String desc = rs.getString("description");
                if (desc != null && !desc.isEmpty()) {
                    warp.setDescription(java.util.Arrays.asList(desc.split("\n")));
                }
                warp.setTeleportDelay(rs.getInt("teleport_delay"));
                warp.setEnabled(rs.getBoolean("enabled"));
                warp.setHidden(rs.getBoolean("hidden"));
                String ownerStr = rs.getString("owner");
                if (ownerStr != null && !ownerStr.isEmpty()) {
                    try { warp.setOwner(java.util.UUID.fromString(ownerStr)); } catch (IllegalArgumentException ignored) {}
                }
                warps.put(key.toLowerCase(), warp);
            }
        } catch (java.sql.SQLException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Failed to load warps from SQL", e);
        }
    }

    public void saveWarp(Warp warp) {
        warps.put(warp.getName().toLowerCase(), warp);
        
        if (useSql && databaseManager != null) {
            // Сохраняем в SQL
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO warps (name, world, x, y, z, yaw, pitch, permission, category, icon, slot, description, teleport_delay, enabled, hidden, owner) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), " +
                         "yaw = VALUES(yaw), pitch = VALUES(pitch), permission = VALUES(permission), category = VALUES(category), " +
                         "icon = VALUES(icon), slot = VALUES(slot), description = VALUES(description), teleport_delay = VALUES(teleport_delay), " +
                         "enabled = VALUES(enabled), hidden = VALUES(hidden), owner = VALUES(owner)")) {
                    
                    ps.setString(1, warp.getName().toLowerCase());
                    ps.setString(2, warp.getLocation().getWorld().getName());
                    ps.setDouble(3, warp.getLocation().getX());
                    ps.setDouble(4, warp.getLocation().getY());
                    ps.setDouble(5, warp.getLocation().getZ());
                    ps.setFloat(6, warp.getLocation().getYaw());
                    ps.setFloat(7, warp.getLocation().getPitch());
                    ps.setString(8, warp.getPermission());
                    ps.setString(9, warp.getCategory());
                    ps.setString(10, warp.getIcon().name());
                    ps.setInt(11, warp.getSlot());
                    ps.setString(12, warp.getDescription() != null ? String.join("\n", warp.getDescription()) : "");
                    ps.setInt(13, warp.getTeleportDelay());
                    ps.setBoolean(14, warp.isEnabled());
                    ps.setBoolean(15, warp.isHidden());
                    ps.setString(16, warp.getOwner() != null ? warp.getOwner().toString() : null);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save warp to SQL", e);
                }
            });
        } else {
            // Сохраняем в YAML
            String path = "warps." + warp.getName();
            config.set(path + ".world", warp.getLocation().getWorld().getName());
            config.set(path + ".x", warp.getLocation().getX());
            config.set(path + ".y", warp.getLocation().getY());
            config.set(path + ".z", warp.getLocation().getZ());
            config.set(path + ".yaw", warp.getLocation().getYaw());
            config.set(path + ".pitch", warp.getLocation().getPitch());
            config.set(path + ".permission", warp.getPermission());
            config.set(path + ".category", warp.getCategory());
            config.set(path + ".icon", warp.getIcon().name());
            config.set(path + ".slot", warp.getSlot());
            config.set(path + ".description", warp.getDescription());
            config.set(path + ".teleport-delay", warp.getTeleportDelay());
            config.set(path + ".enabled", warp.isEnabled());
            config.set(path + ".hidden", warp.isHidden());
            config.set(path + ".owner", warp.getOwner() != null ? warp.getOwner().toString() : null);
            save();
        }
    }

    public void deleteWarp(String name) {
        if (useSql && databaseManager != null) {
            // Удаляем из SQL
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM warps WHERE name = ?")) {
                    ps.setString(1, name.toLowerCase());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not delete warp from SQL", e);
                }
            });
        } else {
            // Удаляем из YAML
            config.set("warps." + name, null);
            save();
        }
        warps.remove(name.toLowerCase());
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save warps.yml", e);
        }
    }

    public Map<String, Warp> getWarps() {
        return warps;
    }

    public Warp getWarp(String name) {
        return warps.get(name.toLowerCase());
    }
}
