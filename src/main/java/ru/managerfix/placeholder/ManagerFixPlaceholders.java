package ru.managerfix.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.managerfix.ManagerFix;
import ru.managerfix.modules.afk.AfkManager;

/**
 * PlaceholderAPI expansion: %managerfix_afk% and others.
 */
public final class ManagerFixPlaceholders extends PlaceholderExpansion {

    private final ManagerFix plugin;

    public ManagerFixPlaceholders(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "managerfix";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        switch (params.toLowerCase()) {
            case "afk":
                return isAfk(player) ? "true" : "false";
            case "name":
                return player.getName();
            case "displayname":
                return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(player.displayName());
            default:
                return null;
        }
    }

    private boolean isAfk(Player player) {
        var afkModule = plugin.getModuleManager().getEnabledModule("afk");
        if (afkModule.isEmpty()) return false;
        AfkManager afkManager = ((ru.managerfix.modules.afk.AfkModule) afkModule.get()).getAfkManager();
        return afkManager != null && afkManager.isAfk(player);
    }
}
