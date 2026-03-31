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
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /chattoggle — toggle global/local chat (when local-radius > 0).
 */
public final class ChatCommand implements CommandExecutor, TabCompleter {

    private static final String METADATA_LOCAL = "chat_local";

    private final ManagerFix plugin;
    private final ChatModule chatModule;

    public ChatCommand(ManagerFix plugin, ChatModule chatModule) {
        this.plugin = plugin;
        this.chatModule = chatModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        Player player = (Player) sender;
        if (!CommandManager.checkCommandPermission(sender, "chattoggle", "managerfix.chat.use", plugin)) return true;
        if (chatModule.getLocalRadius() <= 0) {
            MessageUtil.send(plugin, player, "chat.local-disabled");
            return true;
        }
        ProfileManager pm = plugin.getProfileManager();
        PlayerProfile profile = pm.getProfile(player);
        boolean local = !Boolean.TRUE.equals(profile.getMetadata(METADATA_LOCAL).orElse(false));
        profile.setMetadata(METADATA_LOCAL, local);
        MessageUtil.send(plugin, player, local ? "chat.mode-local" : "chat.mode-global");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
