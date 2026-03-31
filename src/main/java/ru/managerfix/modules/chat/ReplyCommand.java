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

import java.util.Arrays;
import java.util.List;

/**
 * /r &lt;message&gt; — quick reply to the last player who PM'd you (or you PM'd).
 */
public final class ReplyCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE = "managerfix.chat.use";

    private final ManagerFix plugin;
    private final ChatModule chatModule;

    public ReplyCommand(ManagerFix plugin, ChatModule chatModule) {
        this.plugin = plugin;
        this.chatModule = chatModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        if (!CommandManager.checkCommandPermission(sender, "r", PERM_USE, plugin)) return true;
        Player player = (Player) sender;
        if (args.length < 1) {
            MessageUtil.send(plugin, player, "chat.reply.usage");
            return true;
        }
        Player target = chatModule.getLastPmPartner(player);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(plugin, player, "chat.reply-no-target");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageUtil.send(plugin, player, "chat.pm.self");
            return true;
        }
        ProfileManager pm = plugin.getProfileManager();
        if (PmBlockCommand.isPmBlocked(player, pm)) {
            MessageUtil.send(plugin, player, "chat.pm.blocked");
            return true;
        }
        if (IgnoreCommand.isIgnored(target.getUniqueId(), player.getUniqueId(), pm)) {
            MessageUtil.send(plugin, player, "chat.ignore.cannot-message");
            return true;
        }
        String message = String.join(" ", Arrays.asList(args));
        PmCommand.sendPm(plugin, chatModule, pm, player, target, message);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
