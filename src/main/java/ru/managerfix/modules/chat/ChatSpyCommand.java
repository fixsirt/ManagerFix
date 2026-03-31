package ru.managerfix.modules.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;

/**
 * /chatspy — toggle seeing local chat messages (when not in radius).
 * Permission: managerfix.chat.spy
 */
public final class ChatSpyCommand implements CommandExecutor, TabCompleter {

    private static final String METADATA_CHATSPY = "chatspy";

    private final ManagerFix plugin;

    public ChatSpyCommand(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        if (!CommandManager.checkCommandPermission(sender, "chatspy", "managerfix.chat.spy", plugin)) return true;
        Player player = (Player) sender;
        ProfileManager pm = plugin.getProfileManager();
        boolean enabled = !Boolean.TRUE.equals(pm.getProfile(player).getMetadata(METADATA_CHATSPY).orElse(false));
        pm.getProfile(player).setMetadata(METADATA_CHATSPY, enabled);
        MessageUtil.send(plugin, player, enabled ? "chat.chatspy-on" : "chat.chatspy-off");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    public static boolean isChatSpyEnabled(Player player, ProfileManager profileManager) {
        return Boolean.TRUE.equals(profileManager.getProfile(player).getMetadata(METADATA_CHATSPY).orElse(false));
    }
}
