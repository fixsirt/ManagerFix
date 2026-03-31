package ru.managerfix.modules.other;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;

public final class CoordsCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public CoordsCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (!sender.hasPermission("managerfix.other.coords")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        player.sendMessage(MessageUtil.parse("<#3498db>Координаты: <white>" +
                player.getLocation().getBlockX() + " " +
                player.getLocation().getBlockY() + " " +
                player.getLocation().getBlockZ() + "</white>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
