package ru.managerfix.modules.other;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public final class BroadcastCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public BroadcastCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.other.broadcast")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (sender instanceof Player p) {
            if (!module.checkAndApplyCooldown(p, "broadcast", "managerfix.bypass.cooldown")) {
                return true;
            }
        }
        if (args.length == 0) {
            MessageUtil.send(module.getPlugin(), sender, "usage", java.util.Map.of("usage", "/broadcast <message>"));
            return true;
        }
        String message = String.join(" ", args);
        String title = module.getOtherConfig().getBroadcastTitle();
        String subtitle = module.getOtherConfig().getBroadcastSubtitle().replace("{message}", message);
        Bukkit.getServer().sendMessage(MessageUtil.parse(message));
        Title t = Title.title(
                MessageUtil.parse(title),
                MessageUtil.parse(subtitle),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1200), Duration.ofMillis(200))
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(t);
            p.playSound(p.getLocation(), module.getOtherConfig().getBroadcastSound(),
                    module.getOtherConfig().getBroadcastSoundVolume(),
                    module.getOtherConfig().getBroadcastSoundPitch());
        }
        module.logAdminAction("[Other] " + sender.getName() + " broadcasted: " + message);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
