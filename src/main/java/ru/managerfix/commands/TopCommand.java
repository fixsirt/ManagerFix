package ru.managerfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.AfkTopGui;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.modules.afk.AfkManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * /top [afk] — opens AFK top menu (styled like /managerfix menu).
 */
public final class TopCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_TOP_AFK = "managerfix.afk.use";
    private static final String PERMISSION_TOP_OTHER = "managerfix.other.top";

    private final ManagerFix plugin;

    public TopCommand(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(plugin, sender, "command-player-only");
            return true;
        }

        String sub = args.length > 0 ? args[0].trim().toLowerCase() : "afk";
        if ("afk".equals(sub)) {
            if (!player.hasPermission(PERMISSION_TOP_AFK) && !player.hasPermission(PERMISSION_TOP_OTHER)) {
                MessageUtil.send(plugin, player, "no-permission");
                return true;
            }
            var afkMod = plugin.getModuleManager().getEnabledModule("afk");
            if (afkMod.isEmpty()) {
                MessageUtil.send(plugin, player, "module-disabled");
                return true;
            }
            AfkManager afkManager = afkMod.filter(m -> m instanceof ru.managerfix.modules.afk.AfkModule)
                .map(m -> (ru.managerfix.modules.afk.AfkModule) m)
                .map(ru.managerfix.modules.afk.AfkModule::getAfkManager)
                .orElse(null);
            if (afkManager == null) {
                MessageUtil.send(plugin, player, "module-disabled");
                return true;
            }
            GuiManager guiManager = plugin.getGuiManager();
            new AfkTopGui(plugin, guiManager, afkManager).open(player);
            return true;
        }

        MessageUtil.send(plugin, player, "usage", java.util.Map.of("usage", "/top [afk]"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1 && (sender.hasPermission(PERMISSION_TOP_AFK) || sender.hasPermission(PERMISSION_TOP_OTHER))) {
            return Stream.of("afk").filter(s -> s.startsWith(args.length == 1 ? args[0].toLowerCase() : "")).toList();
        }
        return Collections.emptyList();
    }
}
