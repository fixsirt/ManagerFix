package ru.managerfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.MainMenuGui;
import ru.managerfix.gui.ManagerFixMainMenu;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Main plugin command: /managerfix [reload|menu]
 */
public final class ManagerFixCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_RELOAD = "managerfix.reload";
    private static final String PERMISSION_MENU = "managerfix.menu";
    private static final String PERMISSION_ADMIN = "managerfix.admin";

    private final ManagerFix plugin;
    private final GuiManager guiManager;

    public ManagerFixCommand(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        long start = System.nanoTime();
        boolean result = handleCommand(sender, command, label, args);
        if (plugin.getDebugManager() != null && plugin.getDebugManager().isDebug()) {
            plugin.getDebugManager().logCommandExecution("managerfix " + String.join(" ", args), System.nanoTime() - start);
        }
        return result;
    }

    private boolean handleCommand(@NotNull CommandSender sender, @NotNull Command command,
                                  @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageUtil.get(plugin, sender, "plugin-info",
                    Map.of("version", plugin.getPluginMeta().getVersion())));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!CommandManager.checkCommandPermission(sender, "managerfix", PERMISSION_RELOAD, plugin)) {
                    return true;
                }
                plugin.reload();
                MessageUtil.send(plugin, sender, "reload");
                return true;
            }
            case "menu" -> {
                if (!CommandManager.checkCommandPermission(sender, "managerfix", PERMISSION_MENU, plugin)) {
                    return true;
                }
                if (!CommandManager.checkPlayer(sender, plugin)) {
                    return true;
                }
                Player player = (Player) sender;
                if (player.hasPermission(PERMISSION_ADMIN)) {
                    new ManagerFixMainMenu(plugin, guiManager,
                            plugin.getUIThemeManager(), plugin.getGuiTemplate()).open(player);
                } else {
                    new MainMenuGui(plugin, guiManager).open(player);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        if (sender.hasPermission("managerfix.command.managerfix") || sender.hasPermission(PERMISSION_RELOAD)) {
            list.add("reload");
        }
        if (sender.hasPermission("managerfix.command.managerfix") || sender.hasPermission(PERMISSION_MENU)) {
            list.add("menu");
        }
        String arg = args[0].toLowerCase();
        if (arg.isEmpty()) {
            return list;
        }
        list.removeIf(s -> !s.startsWith(arg));
        Collections.sort(list);
        return list;
    }
}
