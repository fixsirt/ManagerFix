package ru.managerfix.modules.names;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * /nickadmin &lt;player&gt; &lt;name|reset&gt; — change or reset another player's nick.
 * Permission: managerfix.names.admin. Ignores cooldown; bypass.length / bypass.format for limits.
 */
public final class NickAdminCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;
    private final NamesModule namesModule;
    private final NamesService namesService;

    public NickAdminCommand(ManagerFix plugin, NamesModule namesModule, NamesService namesService) {
        this.plugin = plugin;
        this.namesModule = namesModule;
        this.namesService = namesService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        if (!CommandManager.checkCommandPermission(sender, "nickadmin", "managerfix.names.admin", plugin)) return true;
        Player admin = (Player) sender;

        if (args.length < 2) {
            MessageUtil.send(plugin, admin, "names.admin-usage");
            return true;
        }

        String targetName = args[0];
        String nameOrReset = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();

        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetUuid = targetPlayer.getUniqueId();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            if (!off.hasPlayedBefore() && !off.isOnline()) {
                MessageUtil.send(plugin, admin, "names.target-offline", Map.of("player", targetName));
                return true;
            }
            targetUuid = off.getUniqueId();
        }

        boolean isReset = "reset".equalsIgnoreCase(nameOrReset) || nameOrReset.isEmpty();
        if (!isReset) {
            int maxLen = namesModule.getMaxLength();
            if (!admin.hasPermission("managerfix.names.bypass.length")) {
                int plainLen = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(namesModule.parseNickToComponent(nameOrReset)).length();
                if (plainLen > maxLen) {
                    MessageUtil.send(plugin, admin, "names.too-long", Map.of("max", String.valueOf(maxLen)));
                    return true;
                }
            }
        }

        if (targetPlayer != null && targetPlayer.isOnline()) {
            namesService.setNickByAdmin(admin, targetPlayer.getUniqueId(), isReset ? "reset" : nameOrReset);
        } else {
            plugin.getProfileManager().getProfile(targetUuid);
            namesService.setNickByAdmin(admin, targetUuid, isReset ? "reset" : nameOrReset);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.names.admin")) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(prefix))
                    .sorted()
                    .toList();
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            return Stream.of("reset").filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
