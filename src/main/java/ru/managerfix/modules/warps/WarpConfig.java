package ru.managerfix.modules.warps;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Warp configuration (settings only): modules/warps/config.yml
 * For warp data use WarpsDataStorage (data/warps.yml)
 */
public class WarpConfig {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public WarpConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "modules/warps/config.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("modules/warps/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps config.yml!");
        }
    }

    public int getDefaultMaxWarps() {
        return config != null ? config.getInt("max-warps", 1) : 1;
    }

    public int getMaxWarpsFor(Player player) {
        if (player == null) return getDefaultMaxWarps();
        int fromConfig = getGroupLimitByPrimaryGroup(player);
        return fromConfig > 0 ? fromConfig : getDefaultMaxWarps();
    }

    private int getGroupLimitByPrimaryGroup(Player player) {
        try {
            if (config == null) return 0;
            String group = getLuckPermsPrimaryGroup(player.getUniqueId());
            if (group == null || group.isEmpty()) return config.getInt("group-limits.default", 0);
            ConfigurationSection sec = config.getConfigurationSection("group-limits");
            if (sec != null) {
                if (sec.getKeys(false).contains(group)) {
                    return sec.getInt(group, 0);
                }
                for (String key : sec.getKeys(false)) {
                    if (key.equalsIgnoreCase(group)) {
                        return sec.getInt(key, 0);
                    }
                }
            }
            return config.getInt("group-limits.default", 0);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String getLuckPermsPrimaryGroup(java.util.UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("net.luckperms.api.LuckPerms");
            var reg = Bukkit.getServicesManager().getRegistration(apiClass);
            if (reg == null) return null;
            Object luckPerms = reg.getProvider();
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, uuid);
            if (user == null) return null;
            return (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
