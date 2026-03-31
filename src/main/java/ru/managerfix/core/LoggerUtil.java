package ru.managerfix.core;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Centralized logging utility for ManagerFix.
 */
public final class LoggerUtil {

    private static JavaPlugin plugin;

    private LoggerUtil() {
    }

    public static void init(JavaPlugin plugin) {
        LoggerUtil.plugin = plugin;
    }

    public static void info(String message) {
        if (plugin != null && plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info(message);
        }
    }

    public static void warning(String message) {
        if (plugin != null) {
            plugin.getLogger().warning(message);
        }
    }

    public static void severe(String message) {
        if (plugin != null) {
            plugin.getLogger().severe(message);
        }
    }

    public static void debug(String message) {
        if (plugin != null && plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    public static void log(Level level, String message, Throwable throwable) {
        if (plugin != null) {
            plugin.getLogger().log(level, message, throwable);
        }
    }
}
