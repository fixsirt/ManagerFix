package ru.managerfix.modules.other;

import org.bukkit.GameMode;
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
import java.util.Map;

public final class GamemodeCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public GamemodeCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player self)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        GameMode mode = switch (command.getName().toLowerCase()) {
            case "gmc" -> GameMode.CREATIVE;
            case "gms" -> GameMode.SURVIVAL;
            case "gmsp" -> GameMode.SPECTATOR;
            default -> null;
        };
        if (mode == null) return false;
        if (!hasModePermission(sender, mode)) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            self.setGameMode(mode);
            MessageUtil.send(module.getPlugin(), sender, "gamemode.set", Map.of("mode", mode.name().toLowerCase()));
            return true;
        }
        if (!sender.hasPermission("managerfix.other.gamemode.others")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        target.setGameMode(mode);
        MessageUtil.send(module.getPlugin(), sender, "gamemode.set-target", Map.of("player", target.getName(), "mode", mode.name().toLowerCase()));
        module.logAdminAction("[Other] " + sender.getName() + " set gamemode " + mode + " for " + target.getName());
        return true;
    }

    private boolean hasModePermission(CommandSender sender, GameMode mode) {
        return switch (mode) {
            case CREATIVE -> sender.hasPermission("managerfix.other.gamemode.creative");
            case SURVIVAL -> sender.hasPermission("managerfix.other.gamemode.survival");
            case SPECTATOR -> sender.hasPermission("managerfix.other.gamemode.spectator");
            default -> false;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("managerfix.other.gamemode.others")) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        return Collections.emptyList();
    }
}
