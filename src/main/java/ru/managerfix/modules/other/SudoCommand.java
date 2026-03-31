package ru.managerfix.modules.other;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SudoCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public SudoCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.other.sudo")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/sudo <name> <command>"));
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        String commandLine = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Bukkit.dispatchCommand(target, commandLine);
        MessageUtil.send(module.getPlugin(), sender, "other.sudo.done", Map.of("player", target.getName()));
        module.logAdminAction("[Other] " + sender.getName() + " sudo " + target.getName() + ": " + commandLine);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        return Collections.emptyList();
    }
}
