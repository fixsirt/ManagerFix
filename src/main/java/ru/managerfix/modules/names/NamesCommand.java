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
import ru.managerfix.gui.NamesGui;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;

/**
 * /names — open admin GUI to change player nicks (managerfix.names.admin).
 */
public final class NamesCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;
    private final NamesModule namesModule;

    public NamesCommand(ManagerFix plugin, NamesModule namesModule) {
        this.plugin = plugin;
        this.namesModule = namesModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        if (!CommandManager.checkCommandPermission(sender, "names", "managerfix.names.admin", plugin)) return true;
        new NamesGui(plugin, plugin.getGuiManager(), namesModule).open((Player) sender, 0);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
