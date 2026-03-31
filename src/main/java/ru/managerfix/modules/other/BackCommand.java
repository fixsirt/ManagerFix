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

public final class BackCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public BackCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if ("back".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.back")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (!module.checkAndApplyCooldown(player, "back", "managerfix.bypass.cooldown")) {
                return true;
            }
            Location loc = module.getBackService().getBackLocation(player.getUniqueId());
            if (loc == null) {
                MessageUtil.send(module.getPlugin(), sender, "other.back.not-found");
                return true;
            }
            teleport(player, loc);
            return true;
        }
        if ("dback".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.dback")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (!module.checkAndApplyCooldown(player, "dback", "managerfix.bypass.cooldown")) {
                return true;
            }
            Location loc = module.getBackService().getDeathLocation(player.getUniqueId());
            if (loc == null) {
                MessageUtil.send(module.getPlugin(), sender, "other.back.death-not-found");
                return true;
            }
            teleport(player, loc);
            return true;
        }
        return false;
    }

    private void teleport(Player player, Location dest) {
        if (player == null || dest == null || dest.getWorld() == null) return;
        var tpService = module.getServiceRegistry().getOrNull(ru.managerfix.service.TeleportService.class);
        if (tpService != null) {
            tpService.teleport(player, dest, null);
            return;
        }
        player.teleport(dest);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
