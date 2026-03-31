package ru.managerfix.modules.rtp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;

import java.util.Collections;
import java.util.List;

public final class RtpCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE = "managerfix.rtp.use";

    private final ManagerFix plugin;
    private final RtpGui rtpGui;

    public RtpCommand(ManagerFix plugin, RtpGui rtpGui) {
        this.plugin = plugin;
        this.rtpGui = rtpGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        Player player = (Player) sender;
        if (!CommandManager.checkCommandPermission(sender, "rtp", PERM_USE, plugin)) return true;
        rtpGui.open(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
