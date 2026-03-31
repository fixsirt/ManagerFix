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

public final class GodCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public GodCommand(OtherModule module) {
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
            if (!sender.hasPermission("managerfix.other.god")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            toggleGod(self);
            return true;
        }
        if (!sender.hasPermission("managerfix.other.god.others")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        toggleGod(target);
        module.logAdminAction("[Other] " + sender.getName() + " toggled god for " + target.getName());
        return true;
    }

    private void toggleGod(Player player) {
        boolean enabled = !module.isGod(player.getUniqueId());
        module.setGod(player.getUniqueId(), enabled);
        player.setInvulnerable(enabled);
        player.sendMessage(MessageUtil.parse(enabled ? "<green>God mode включён.</green>" : "<red>God mode выключен.</red>"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("managerfix.other.god.others")) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        return Collections.emptyList();
    }
}
