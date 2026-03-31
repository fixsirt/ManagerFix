package ru.managerfix.modules.names;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;

/**
 * /nick &lt;nickname&gt; or /nick reset — set or reset display nick.
 * Permission: managerfix.names.nick. Always returns true after handling (never triggers Bukkit usage).
 */
public final class NickCommand implements CommandExecutor, TabCompleter {

    private static final String COOLDOWN_KEY = "nick";

    private final ManagerFix plugin;
    private final NamesModule namesModule;
    private final NamesService namesService;

    public NickCommand(ManagerFix plugin, NamesModule namesModule, NamesService namesService) {
        this.plugin = plugin;
        this.namesModule = namesModule;
        this.namesService = namesService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        LoggerUtil.debug("[Names] NickCommand executed.");
        LoggerUtil.debug("[Names] /nick onCommand: args.length=" + (args != null ? args.length : "null"));

        if (!(sender instanceof Player player)) {
            MessageUtil.send(plugin, sender, "player-only");
            return true;
        }

        if (!CommandManager.checkCommandPermission(sender, "nick", "managerfix.names.nick", plugin)) {
            return true;
        }

        if (args == null || args.length < 1) {
            MessageUtil.send(plugin, player, "names.usage");
            return true;
        }

        String input = args[0];
        if (input == null) {
            input = "";
        }

        if ("reset".equalsIgnoreCase(input.trim())) {
            namesService.setNickForSelfReset(player);
            return true;
        }

        String rawNick = String.join(" ", args).trim();
        if (rawNick.isEmpty()) {
            MessageUtil.send(plugin, player, "names.usage");
            return true;
        }

        boolean bypassLength = player.hasPermission("managerfix.names.bypass.length");
        int maxLen = namesModule.getMaxLength();
        net.kyori.adventure.text.Component parsed = namesModule.parseNickToComponent(rawNick, player.hasPermission("managerfix.names.bypass.format"));
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(parsed);
        if (!bypassLength && plain.length() > maxLen) {
            MessageUtil.send(plugin, player, "names.too-long", java.util.Map.of("max", String.valueOf(maxLen)));
            return true;
        }

        ProfileManager pm = plugin.getProfileManager();
        if (pm == null) {
            MessageUtil.send(plugin, player, "module-disabled");
            return true;
        }
        int cooldownSec = namesModule.getNicknameCooldownSeconds();
        if (cooldownSec > 0 && !player.hasPermission("managerfix.names.bypass.cooldown")) {
            PlayerProfile profile = pm.getProfile(player);
            if (profile.hasCooldown(COOLDOWN_KEY)) {
                long remaining = profile.getCooldownRemaining(COOLDOWN_KEY);
                MessageUtil.send(plugin, player, "names.cooldown",
                        java.util.Map.of("seconds", String.valueOf((remaining + 999) / 1000)));
                return true;
            }
            profile.setCooldown(COOLDOWN_KEY, cooldownSec * 1000L);
        }

        namesService.setNickForSelf(player, rawNick);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.names.nick") && !sender.hasPermission("managerfix.command.nick")) {
            return Collections.emptyList();
        }
        if (args != null && args.length == 1) {
            String prefix = args[0] == null ? "" : args[0].toLowerCase();
            if ("reset".startsWith(prefix)) {
                return List.of("reset");
            }
        }
        return Collections.emptyList();
    }
}
