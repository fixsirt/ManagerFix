package ru.managerfix.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.FileManager;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends messages from lang files using MiniMessage (HEX, tags).
 * Supports PlaceholderAPI when installed.
 * In configs also supports Minecraft color codes (§0-§f, §l, §m, §n, §o, §r).
 */
public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)}");

    /** Converts Minecraft § and & color/format codes to MiniMessage tags. Public so callers can convert placeholder values (e.g. balance from Vault) before insert. */
    public static String convertLegacyColors(String text) {
        if (text == null) return text;
        if (!text.contains("§") && !text.contains("&")) return text;
        String t = text
                .replace("§0", "<black>").replace("&0", "<black>")
                .replace("§1", "<dark_blue>").replace("&1", "<dark_blue>")
                .replace("§2", "<dark_green>").replace("&2", "<dark_green>")
                .replace("§3", "<dark_aqua>").replace("&3", "<dark_aqua>")
                .replace("§4", "<dark_red>").replace("&4", "<dark_red>")
                .replace("§5", "<dark_purple>").replace("&5", "<dark_purple>")
                .replace("§6", "<gold>").replace("&6", "<gold>")
                .replace("§7", "<gray>").replace("&7", "<gray>")
                .replace("§8", "<dark_gray>").replace("&8", "<dark_gray>")
                .replace("§9", "<blue>").replace("&9", "<blue>")
                .replace("§a", "<green>").replace("&a", "<green>")
                .replace("§b", "<aqua>").replace("&b", "<aqua>")
                .replace("§c", "<red>").replace("&c", "<red>")
                .replace("§d", "<light_purple>").replace("&d", "<light_purple>")
                .replace("§e", "<yellow>").replace("&e", "<yellow>")
                .replace("§f", "<white>").replace("&f", "<white>")
                .replace("§l", "<bold>").replace("&l", "<bold>")
                .replace("§m", "<strikethrough>").replace("&m", "<strikethrough>")
                .replace("§n", "<underline>").replace("&n", "<underline>")
                .replace("§o", "<italic>").replace("&o", "<italic>")
                .replace("§r", "<reset>").replace("&r", "<reset>");
        return t;
    }

    private MessageUtil() {
    }

    /**
     * Gets raw message string from lang file (path under "messages" or direct).
     */
    public static String getRaw(JavaPlugin plugin, String locale, String path) {
        FileManager fm = plugin instanceof ru.managerfix.ManagerFix
                ? ((ru.managerfix.ManagerFix) plugin).getFileManager()
                : null;
        ConfigManager cm = plugin instanceof ru.managerfix.ManagerFix
                ? ((ru.managerfix.ManagerFix) plugin).getConfigManager()
                : null;
        if (fm == null || cm == null) {
            return "<red>Lang not available";
        }
        String loc = locale != null ? locale : cm.getDefaultLanguage();
        FileConfiguration lang = fm.getLangFile(loc);
        String fullPath = path.startsWith("messages.") ? path : "messages." + path;
        String value = lang.getString(fullPath);
        if (value == null) {
            value = lang.getString(path);
        }
        return value != null ? value : "<gray>" + path;
    }

    /**
     * Applies PlaceholderAPI to text if plugin is present and sender is Player.
     */
    public static String setPlaceholders(CommandSender sender, String text) {
        if (text == null) return "";
        if (!(sender instanceof Player player)) return text;
        try {
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
            }
        } catch (NoClassDefFoundError ignored) {
        }
        return text;
    }

    /**
     * Replaces {key} placeholders in text.
     */
    public static String replace(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) return text;
        String out = text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    /**
     * Parses string to Component. Supports MiniMessage (hex, tags) and Minecraft § color codes in configs.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        text = convertLegacyColors(text);
        try {
            return MINI_MESSAGE.deserialize(text);
        } catch (Exception e) {
            return LEGACY.deserialize(text);
        }
    }

    /**
     * Gets message from lang, applies PAPI and placeholders, parses MiniMessage, returns Component.
     */
    public static Component get(JavaPlugin plugin, CommandSender sender, String path,
                               Map<String, String> placeholders) {
        String locale = plugin instanceof ru.managerfix.ManagerFix
                ? ((ru.managerfix.ManagerFix) plugin).getConfigManager().getDefaultLanguage()
                : "ru";
        String raw = getRaw(plugin, locale, path);
        raw = setPlaceholders(sender, raw);
        raw = placeholders != null ? replace(raw, placeholders) : raw;
        return parse(raw);
    }

    public static Component get(JavaPlugin plugin, CommandSender sender, String path) {
        return get(plugin, sender, path, null);
    }

    /**
     * Sends message from lang to sender (MiniMessage + PAPI).
     */
    public static void send(JavaPlugin plugin, CommandSender sender, String path) {
        sender.sendMessage(get(plugin, sender, path));
    }

    public static void send(JavaPlugin plugin, CommandSender sender, String path,
                            Map<String, String> placeholders) {
        sender.sendMessage(get(plugin, sender, path, placeholders));
    }

    /**
     * Broadcasts message from lang to all players (default locale, {key} placeholders only).
     */
    public static void broadcast(JavaPlugin plugin, String path, Map<String, String> placeholders) {
        String raw = getRaw(plugin,
                plugin instanceof ru.managerfix.ManagerFix m ? m.getConfigManager().getDefaultLanguage() : "ru",
                path);
        raw = placeholders != null ? replace(raw, placeholders) : raw;
        org.bukkit.Bukkit.getServer().sendMessage(parse(raw));
    }

    public static void broadcast(JavaPlugin plugin, String path) {
        broadcast(plugin, path, null);
    }
}
