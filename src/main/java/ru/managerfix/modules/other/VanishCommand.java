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

public final class VanishCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public VanishCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (!sender.hasPermission("managerfix.other.vanish")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        boolean now = !module.getVanishService().isVanished(player);
        module.getVanishService().setVanished(player, now);
        player.sendMessage(MessageUtil.parse(now ? "<green>Vanish включён.</green>" : "<red>Vanish выключен.</red>"));
        module.logAdminAction("[Other] " + sender.getName() + " toggled vanish " + (now ? "on" : "off"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
