package ru.managerfix.modules.other;

import org.bukkit.Location;
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
import java.util.stream.Collectors;

public final class NearCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public NearCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (!sender.hasPermission("managerfix.other.near")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (!module.checkAndApplyCooldown(player, "near", "managerfix.bypass.cooldown")) {
            return true;
        }
        int radius = module.getOtherConfig().getNearRadius();
        Location loc = player.getLocation();
        String list = player.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> p.getLocation().distanceSquared(loc) <= (double) radius * radius)
                .map(p -> p.getName() + " (" + Math.round(p.getLocation().distance(loc)) + "м)")
                .collect(Collectors.joining(", "));
        if (list.isEmpty()) {
            player.sendMessage(MessageUtil.parse("<gray>Рядом нет игроков.</gray>"));
        } else {
            player.sendMessage(MessageUtil.parse("<#3498db>Рядом: <white>" + list + "</white>"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
