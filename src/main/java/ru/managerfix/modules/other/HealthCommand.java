package ru.managerfix.modules.other;

import org.bukkit.attribute.Attribute;
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

public final class HealthCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public HealthCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player self)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (args.length == 0) {
            if (!sender.hasPermission("managerfix.other.health")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            healTarget(self);
            MessageUtil.send(module.getPlugin(), self, "heal.healed");
            return true;
        }
        if (!sender.hasPermission("managerfix.other.health.others")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        healTarget(target);
        MessageUtil.send(module.getPlugin(), sender, "heal.healed-target", Map.of("player", target.getName()));
        if (!target.getUniqueId().equals(self.getUniqueId())) {
            MessageUtil.send(module.getPlugin(), target, "heal.healed");
        }
        return true;
    }

    private void sendHealth(Player viewer, Player target) {
        var attr = target.getAttribute(Attribute.MAX_HEALTH);
        double max = attr != null ? attr.getValue() : 20.0;
        double cur = target.getHealth();
        viewer.sendMessage(MessageUtil.parse("<#3498db>Здоровье <white>" + target.getName() + "</white>: " + (int) cur + "/" + (int) max));
    }

    private void healTarget(Player target) {
        var attr = target.getAttribute(Attribute.MAX_HEALTH);
        double max = attr != null ? attr.getValue() : 20.0;
        target.setHealth(max);
        target.setFireTicks(0);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("managerfix.other.health.others")) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        return Collections.emptyList();
    }
}
