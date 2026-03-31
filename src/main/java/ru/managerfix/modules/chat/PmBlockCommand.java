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

import java.util.List;
import java.util.Map;

/**
 * /pmblock &lt;player&gt; — toggle PM block for a player (blocked player cannot send PMs).
 */
public final class PmBlockCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_PMBLOCK = "managerfix.chat.pmblock";
    private static final String METADATA_PM_BLOCKED = "pm_blocked";

    private final ManagerFix plugin;
    private final ProfileManager profileManager;

    public PmBlockCommand(ManagerFix plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkCommandPermission(sender, "pmblock", PERM_PMBLOCK, plugin)) return true;
        if (args.length < 1) {
            MessageUtil.send(plugin, sender, "chat.pmblock.usage");
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        boolean blocked = !Boolean.TRUE.equals(profileManager.getProfile(target).getMetadata(METADATA_PM_BLOCKED).orElse(false));
        profileManager.getProfile(target).setMetadata(METADATA_PM_BLOCKED, blocked);
        MessageUtil.send(plugin, sender, blocked ? "chat.pmblock.blocked" : "chat.pmblock.unblocked", Map.of("player", target.getName()));
        if (target.equals(sender)) {
            MessageUtil.send(plugin, target, blocked ? "chat.pmblock.you-blocked" : "chat.pmblock.you-unblocked");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.command.pmblock") && !sender.hasPermission(PERM_PMBLOCK)) return List.of();
        if (args.length == 1) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        return List.of();
    }

    public static boolean isPmBlocked(Player player, ProfileManager profileManager) {
        return Boolean.TRUE.equals(profileManager.getProfile(player).getMetadata(METADATA_PM_BLOCKED).orElse(false));
    }
}
