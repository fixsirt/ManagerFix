package ru.managerfix.modules.tab;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * Tab module configuration: multi-line header/footer, player format, afk format, update interval.
 * Supports MiniMessage, HEX, PlaceholderAPI, LuckPerms placeholders.
 */
public final class TabConfig {

    private final FileConfiguration config;

    public TabConfig(FileConfiguration config) {
        this.config = config != null ? config : null;
    }

    /** Multi-line header (MiniMessage, HEX, PlaceholderAPI). */
    public List<String> getHeader() {
        if (config == null) return List.of();
        List<String> list = config.getStringList("header");
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    /** Multi-line footer (MiniMessage, HEX, PlaceholderAPI). */
    public List<String> getFooter() {
        if (config == null) return List.of();
        List<String> list = config.getStringList("footer");
        return list != null ? List.copyOf(list) : Collections.emptyList();
    }

    /** Player list name format. Placeholders: {name}, {displayName}, %luckperms_prefix%, %player_ping%, PAPI, AFK prefix when AFK. */
    public String getPlayerFormat() {
        return config != null ? config.getString("player-format", "<#E0E0E0>%luckperms_prefix%</#E0E0E0> {displayName}") : "<#E0E0E0>%luckperms_prefix%</#E0E0E0> {displayName}";
    }

    /** Format when player is AFK (replaces player-format). Supports same placeholders + AFK prefix. */
    public String getAfkFormat() {
        return config != null ? config.getString("afk-format", "<#FF4D00>｢𝐀𝐅𝐊｣</#FF4D00> <#E0E0E0>%luckperms_prefix%</#E0E0E0> {displayName}") : "<#FF4D00>｢𝐀𝐅𝐊｣</#FF4D00> <#E0E0E0>%luckperms_prefix%</#E0E0E0> {displayName}";
    }

    /** Update interval in ticks (e.g. 40 = 2 sec). Only changed header/footer/names are resent. */
    public long getUpdateIntervalTicks() {
        return config != null ? config.getLong("update-interval-ticks", 40) : 40;
    }

    /** Whether to hide vanished players from tab (when vanish module is used). */
    public boolean isHideVanished() {
        return config != null && config.getBoolean("hide-vanished", true);
    }

    /** Whether to use cluster placeholder %cluster_total_online% when cluster is enabled. */
    public boolean isClusterPlaceholders() {
        return config != null && config.getBoolean("cluster-placeholders", true);
    }

    /** PlaceholderAPI cache duration in ticks (0 = no cache). */
    public long getPlaceholderCacheTicks() {
        return config != null ? config.getLong("placeholder-cache-ticks", 0) : 0;
    }

    /** Whether to sort tab list by LuckPerms weight, then group, then name. */
    public boolean isSortByLuckPerms() {
        return config != null && config.getBoolean("sort-by-luckperms", true);
    }
}
