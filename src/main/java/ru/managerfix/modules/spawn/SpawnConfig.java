package ru.managerfix.modules.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.database.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SpawnConfig {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final DatabaseManager databaseManager;
    private final boolean useSql;

    private Location spawnLocation;

    // Settings
    private int teleportDelaySeconds;
    private boolean cancelOnMove;
    private boolean cancelOnDamage;
    private boolean spawnOnJoin;
    private boolean spawnOnDeath;
    private boolean spawnFirstJoinOnly;
    private boolean safeTeleport;

    // Animation
    private boolean animationEnabled;
    private Particle particles;
    private Particle secondaryParticles;
    private Sound sound;
    private float volume;
    private float pitch;
    private boolean bossbarCountdown;
    private boolean titleCountdown;

    public SpawnConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "modules/spawn/config.yml");
        
        // Проверяем используется ли SQL (MySQL или SQLite)
        boolean useSqlStorage = false;
        DatabaseManager dbManager = null;
        if (plugin instanceof ManagerFix mf) {
            useSqlStorage = mf.isMySqlStorage() || mf.isSqliteStorage();
            dbManager = mf.getDatabaseManager();
        }
        
        this.useSql = useSqlStorage;
        this.databaseManager = dbManager;
        
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("modules/spawn/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadValues();
    }

    private void loadValues() {
        // Загружаем спавн из MySQL если используется
        if (useSql && databaseManager != null) {
            loadSpawnFromSql();
        } else {
            // Загружаем из YAML
            String worldName = config.getString("world");
            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                this.spawnLocation = new Location(
                        Bukkit.getWorld(worldName),
                        config.getDouble("x"),
                        config.getDouble("y"),
                        config.getDouble("z"),
                        (float) config.getDouble("yaw"),
                        (float) config.getDouble("pitch")
                );
            } else {
                this.spawnLocation = null;
            }
        }

        this.teleportDelaySeconds = config.getInt("settings.teleport-delay-seconds", 5);
        this.cancelOnMove = config.getBoolean("settings.cancel-on-move", true);
        this.cancelOnDamage = config.getBoolean("settings.cancel-on-damage", true);
        this.spawnOnJoin = config.getBoolean("settings.spawn-on-join", false);
        this.spawnOnDeath = config.getBoolean("settings.spawn-on-death", false);
        this.spawnFirstJoinOnly = config.getBoolean("settings.spawn-first-join-only", false);
        this.safeTeleport = config.getBoolean("settings.safe-teleport", true);

        this.animationEnabled = config.getBoolean("animation.enabled", true);
        this.particles = parseParticle(config.getString("animation.particles"), Particle.PORTAL);
        this.secondaryParticles = parseParticle(config.getString("animation.secondary-particles"), Particle.ENCHANT);
        this.sound = parseSound(config.getString("animation.sound"), Sound.ENTITY_ENDERMAN_TELEPORT);
        this.volume = (float) config.getDouble("animation.volume", 1.0);
        this.pitch = (float) config.getDouble("animation.pitch", 1.0);
        this.bossbarCountdown = config.getBoolean("animation.bossbar-countdown", true);
        this.titleCountdown = config.getBoolean("animation.title-countdown", true);
    }
    
    private void loadSpawnFromSql() {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM spawn WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String worldName = rs.getString("world");
                if (worldName != null && Bukkit.getWorld(worldName) != null) {
                    this.spawnLocation = new Location(
                            Bukkit.getWorld(worldName),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load spawn location from SQL: " + e.getMessage());
        }
    }

    private Particle parseParticle(String name, Particle def) {
        if (name == null) return def;
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    @SuppressWarnings("deprecation")
    private Sound parseSound(String name, Sound def) {
        if (name == null) return def;
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    public void setSpawnLocation(Location loc) {
        this.spawnLocation = loc;
        
        if (useSql && databaseManager != null) {
            // Сохраняем в SQL
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String sql;
                if ("SQLITE".equalsIgnoreCase(databaseManager.getStorageType())) {
                    sql = "INSERT OR REPLACE INTO spawn (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?)";
                } else {
                    sql = "INSERT INTO spawn (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?) " +
                          "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), " +
                          "z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)";
                }
                try (Connection conn = databaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, loc.getWorld().getName());
                    ps.setDouble(2, loc.getX());
                    ps.setDouble(3, loc.getY());
                    ps.setDouble(4, loc.getZ());
                    ps.setFloat(5, loc.getYaw());
                    ps.setFloat(6, loc.getPitch());
                    ps.executeUpdate();
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not save spawn to SQL: " + e.getMessage());
                }
            });
        } else {
            // Сохраняем в YAML
            config.set("world", loc.getWorld().getName());
            config.set("x", loc.getX());
            config.set("y", loc.getY());
            config.set("z", loc.getZ());
            config.set("yaw", loc.getYaw());
            config.set("pitch", loc.getPitch());
            save();
        }
    }

    public void setSetting(String path, Object value) {
        config.set("settings." + path, value);
        save();
        loadValues(); // Update cached values
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save spawn.yml!");
        }
    }

    // Getters
    public Location getSpawnLocation() { return spawnLocation; }
    public int getTeleportDelaySeconds() { return teleportDelaySeconds; }
    public boolean isCancelOnMove() { return cancelOnMove; }
    public boolean isCancelOnDamage() { return cancelOnDamage; }
    public boolean isSpawnOnJoin() { return spawnOnJoin; }
    public boolean isSpawnOnDeath() { return spawnOnDeath; }
    public boolean isSpawnFirstJoinOnly() { return spawnFirstJoinOnly; }
    public boolean isSafeTeleport() { return safeTeleport; }
    public boolean isAnimationEnabled() { return animationEnabled; }
    public Particle getParticles() { return particles; }
    public Particle getSecondaryParticles() { return secondaryParticles; }
    public Sound getSound() { return sound; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public boolean isBossbarCountdown() { return bossbarCountdown; }
    public boolean isTitleCountdown() { return titleCountdown; }
}
