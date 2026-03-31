package ru.managerfix.utils;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Utility to resolve players by custom nick or real name.
 * Used by commands like /pm, /tpa, etc. so players can refer to each other by nick.
 */
public final class NickResolver {

    private NickResolver() {}

    /**
     * Finds a player by custom nick or real name (case-insensitive).
     * Priority:
     * <ol>
     *   <li>Exact displayName match (custom nick)</li>
     *   <li>Exact real name match</li>
     *   <li>Partial displayName match (starts with)</li>
     *   <li>Partial real name match (starts with)</li>
     * </ol>
     */
    public static @Nullable Player resolve(@NotNull String input) {
        if (input.isEmpty()) return null;
        String lower = input.toLowerCase(Locale.ROOT);

        // 1. Exact display name match
        for (Player p : Bukkit.getOnlinePlayers()) {
            String display = plainDisplayName(p);
            if (display.equalsIgnoreCase(input)) return p;
        }

        // 2. Exact real name match
        Player exact = Bukkit.getPlayerExact(input);
        if (exact != null && exact.isOnline()) return exact;

        // 3. Partial display name match
        for (Player p : Bukkit.getOnlinePlayers()) {
            String display = plainDisplayName(p).toLowerCase(Locale.ROOT);
            if (display.startsWith(lower)) return p;
        }

        // 4. Partial real name match (Bukkit default)
        Player partial = Bukkit.getPlayer(input);
        if (partial != null && partial.isOnline()) return partial;

        return null;
    }

    /**
     * Gets online player by UUID. For players with custom nick, getPlayer(uuid) may return null,
     * so we always search getOnlinePlayers() first by UUID — that list is authoritative for who is online.
     */
    public static @Nullable Player getPlayerByUuid(@NotNull UUID uuid) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(uuid)) return online;
        }
        Player p = Bukkit.getPlayer(uuid);
        return (p != null && p.isOnline()) ? p : null;
    }

    /**
     * Returns a list of nick suggestions for tab-completion.
     * Includes both custom nicks (plain text) and real names.
     * Filters by prefix, excludes the sender.
     */
    public static @NotNull List<String> tabComplete(@NotNull String prefix, @Nullable UUID excludeUuid) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> results = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (excludeUuid != null && p.getUniqueId().equals(excludeUuid)) continue;

            String display = plainDisplayName(p);
            String realName = p.getName();

            // Prefer custom nick if it differs from real name
            if (!display.equals(realName)) {
                if (display.toLowerCase(Locale.ROOT).startsWith(lower)) {
                    results.add(display);
                    continue; // don't add real name too — nick is the identity
                }
            }

            // Fall back to real name
            if (realName.toLowerCase(Locale.ROOT).startsWith(lower)) {
                results.add(realName);
            }
        }

        results.sort(String.CASE_INSENSITIVE_ORDER);
        return results;
    }

    /**
     * Returns the plain text display name (nick) for a player.
     * If the display name is empty or null, returns the real name.
     */
    public static @NotNull String plainDisplayName(@NotNull Player player) {
        String name = PlainTextComponentSerializer.plainText().serialize(player.displayName());
        return (name != null && !name.isEmpty()) ? name : player.getName();
    }
}
