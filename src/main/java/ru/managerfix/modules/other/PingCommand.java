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
import java.util.Map;

public final class PingCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public PingCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player self)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (!sender.hasPermission("managerfix.other.ping")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        Player target = self;
        if (args.length > 0) {
            target = ru.managerfix.utils.NickResolver.resolve(args[0]);
            if (target == null || !target.isOnline()) {
                MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
                return true;
            }
        }
        self.sendMessage(MessageUtil.parse("<#3498db>Ping <white>" + target.getName() + "</white>: " + target.getPing() + " ms"));
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
