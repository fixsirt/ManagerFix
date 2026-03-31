package ru.managerfix.modules.other;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;

public final class ChatLockCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public ChatLockCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.other.chatlock")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        boolean now = !module.isChatLocked();
        module.setChatLocked(now);
        MessageUtil.send(module.getPlugin(), sender, now ? "other.chatlock.enabled" : "other.chatlock.disabled");
        module.logAdminAction("[Other] " + sender.getName() + " chatlock=" + now);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
