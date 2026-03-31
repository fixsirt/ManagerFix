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

public final class SpeedCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public SpeedCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (!sender.hasPermission("managerfix.other.speed")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            MessageUtil.send(module.getPlugin(), sender, "usage", java.util.Map.of("usage", "/speed <value>"));
            return true;
        }
        try {
            float value = Float.parseFloat(args[0]);
            value = Math.max(0f, Math.min(10f, value));
            float speed = value / 10f;
            player.setWalkSpeed(speed);
            player.setFlySpeed(speed);
            player.sendMessage(MessageUtil.parse("<#3498db>Скорость: <white>" + value + "</white>"));
            return true;
        } catch (NumberFormatException e) {
            MessageUtil.send(module.getPlugin(), sender, "other.speed.invalid");
            return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
