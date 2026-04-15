package ru.managerfix.modules.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.utils.MessageUtil;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public final class BroadcastCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;

    public BroadcastCommand(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkCommandPermission(sender, "broadcast", "managerfix.chat.admin", plugin)) {
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(plugin, sender, "chat.broadcast-usage", java.util.Map.of("usage", "/broadcast <message>"));
            return true;
        }

        String message = String.join(" ", args);

        String soundName = "UI_TOAST_CHALLENGE_COMPLETE";
        float volume = 1.0f;
        float pitch = 1.0f;
        String titleStr = "<gradient:#7000FF:#00C8FF>Объявление</gradient>";
        String chatFormat = "<gradient:#7000FF:#00C8FF>⬤</gradient> <#F0F4F8>Объявление от</#F0F4F8> <#00C8FF>{player}</#00C8FF>:";

        if (plugin.getConfig() != null) {
            soundName = plugin.getConfig().getString("modules.chat.broadcast.sound", soundName);
            volume = (float) plugin.getConfig().getDouble("modules.chat.broadcast.sound-volume", volume);
            pitch = (float) plugin.getConfig().getDouble("modules.chat.broadcast.sound-pitch", pitch);
            titleStr = plugin.getConfig().getString("modules.chat.broadcast.title", titleStr);
            chatFormat = plugin.getConfig().getString("modules.chat.broadcast.chat-format", chatFormat);
        }

        String headerText = chatFormat.replace("{player}", sender.getName());

        Component separator = Component.text("   ")
                .hoverEvent(HoverEvent.showText(Component.text("<#F0F4F8>────────────────────</#F0F4F8>")))
                .clickEvent(ClickEvent.copyToClipboard(message));

        Component fullMessage = Component.join(JoinConfiguration.builder()
                        .separator(separator)
                        .build(),
                MessageUtil.parse(headerText),
                MessageUtil.parse(message)
        );

        Sound sound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
        try {
            sound = Sound.valueOf(soundName);
        } catch (Exception ignored) {}

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(fullMessage);
            p.playSound(p.getLocation(), sound, volume, pitch);
            p.showTitle(Title.title(
                    MessageUtil.parse(titleStr),
                    Component.empty(),
                    Times.times(Duration.ofMillis(200), Duration.ofMillis(2000), Duration.ofMillis(300))
            ));
        }

        plugin.getLogger().info("[Broadcast] " + sender.getName() + ": " + message);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
