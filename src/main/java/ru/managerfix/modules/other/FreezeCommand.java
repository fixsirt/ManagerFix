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

public final class FreezeCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public FreezeCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.other.freeze")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/freeze <name>"));
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        boolean now = !module.isFrozen(target.getUniqueId());
        module.setFrozen(target.getUniqueId(), now);
        MessageUtil.send(module.getPlugin(), sender, now ? "other.freeze.enabled" : "other.freeze.disabled",
                Map.of("player", target.getName()));
        if (!target.equals(sender)) {
            target.sendMessage(MessageUtil.parse(now ? "<red>Вы заморожены.</red>" : "<green>Вас разморозили.</green>"));
        }
        module.logAdminAction("[Other] " + sender.getName() + " freeze=" + now + " for " + target.getName());
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
