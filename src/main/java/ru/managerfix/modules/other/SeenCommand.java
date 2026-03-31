package ru.managerfix.modules.other;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SeenCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public SeenCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.other.seen")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/seen <name>"));
            return true;
        }
        OfflinePlayer off = module.getPlugin().getServer().getOfflinePlayer(args[0]);
        if (off == null || (!off.hasPlayedBefore() && !off.isOnline())) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        if (off.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "other.seen.online", Map.of("player", off.getName() != null ? off.getName() : args[0]));
            return true;
        }
        long last = resolveLastSeenMillis(off);
        if (module.getPlugin() instanceof ManagerFix mf) {
            ProfileManager pm = mf.getProfileManager();
            UUID uuid = off.getUniqueId();
            var profile = pm.getCachedProfile(uuid).orElse(null);
            if (profile != null) {
                long activity = profile.getLastActivity();
                if (activity > 0) {
                    last = activity;
                }
            }
        }
        String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(last));
        sender.sendMessage(MessageUtil.parse("<#3498db>" + (off.getName() != null ? off.getName() : args[0]) +
                " был в онлайне: <white>" + formatted + "</white>"));
        return true;
    }

    private long resolveLastSeenMillis(OfflinePlayer off) {
        try {
            var method = off.getClass().getMethod("getLastLogin");
            Object v = method.invoke(off);
            if (v instanceof Long l) return l;
        } catch (Exception ignored) {
        }
        try {
            var method = off.getClass().getMethod("getLastPlayed");
            Object v = method.invoke(off);
            if (v instanceof Long l) return l;
        } catch (Exception ignored) {
        }
        return 0L;
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
