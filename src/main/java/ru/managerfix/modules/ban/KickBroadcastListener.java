package ru.managerfix.modules.ban;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import ru.managerfix.utils.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KickBroadcastListener implements Listener {

    private final BanModule module;
    private final Map<UUID, SourceInfo> recentKicks = new ConcurrentHashMap<>();

    public KickBroadcastListener(BanModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKickCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg == null) return;
        String lower = msg.trim().toLowerCase();
        if (!lower.startsWith("/kick") && !lower.startsWith("/minecraft:kick")) return;
        String[] parts = msg.trim().split("\\s+");
        if (parts.length < 2) return;
        Player target = ru.managerfix.utils.NickResolver.resolve(parts[1]);
        if (target == null) return;
        String sourceReal = event.getPlayer().getName();
        String sourceNick = ru.managerfix.utils.NickResolver.plainDisplayName(event.getPlayer());
        recentKicks.put(target.getUniqueId(), new SourceInfo(sourceReal, sourceNick, System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        if (!module.isBroadcastBans()) return;
        String format = module.getFormatKickBroadcast();
        if (format == null || format.isEmpty()) return;
        Player target = event.getPlayer();
        String targetReal = target.getName();
        String targetNick = ru.managerfix.utils.NickResolver.plainDisplayName(target);
        boolean hasTargetNick = targetNick != null && !targetNick.equalsIgnoreCase(targetReal);
        String reason = "";
        if (event.reason() != null) {
            try {
                reason = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.reason());
            } catch (Throwable t) {
                reason = cleanReason(event.reason().toString());
            }
        }

        SourceInfo info = recentKicks.get(target.getUniqueId());
        if (info == null || (System.currentTimeMillis() - info.timeMs) >= 5000L) {
            return;
        }
        String sourceReal = info.sourceReal != null ? info.sourceReal : "Console";
        String sourceNick = info.sourceNick != null ? info.sourceNick : sourceReal;
        boolean hasSourceNick = sourceNick != null && !sourceNick.equalsIgnoreCase(sourceReal);

        String msg = format;
        msg = msg.replace("{targetReal}", targetReal != null ? targetReal : "?");
        msg = msg.replace("{sourceReal}", sourceReal != null ? sourceReal : "Console");
        msg = msg.replace("{reason}", reason != null ? cleanReason(reason) : "");
        msg = msg.replace("{duration}", "");
        msg = msg.replaceAll("\\{targetNick\\?->(.*?)\\}", hasTargetNick ? "$1" : "");
        msg = msg.replaceAll("\\{sourceNick\\?->(.*?)\\}", hasSourceNick ? "$1" : "");
        msg = msg.replace("{targetNick}", hasTargetNick ? targetNick : "");
        msg = msg.replace("{sourceNick}", hasSourceNick ? sourceNick : "");

        var comp = MessageUtil.parse(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(comp);
        }
        Bukkit.getConsoleSender().sendMessage(comp);
    }

    private static String cleanReason(String s) {
        String t = s;
        if (t.startsWith("Component[")) {
            int i = t.indexOf("content=");
            if (i >= 0) {
                int j = t.indexOf(']', i);
                if (j > i) {
                    t = t.substring(i + 8, j);
                }
            }
        }
        // Если есть "Причина: " - берём только текст после него
        int idx = t.lastIndexOf("Причина: ");
        if (idx >= 0) {
            t = t.substring(idx + 9);
        }
        // Если есть "Вас кикнул..." - удаляем эту строку
        if (t.startsWith("Вас кикнул")) {
            int newline = t.indexOf('\n');
            if (newline >= 0) {
                t = t.substring(newline + 1).trim();
                // Теперь снова пробуем найти "Причина: "
                idx = t.lastIndexOf("Причина: ");
                if (idx >= 0) {
                    t = t.substring(idx + 9);
                }
            }
        }
        return t.replaceAll("^\\s+|\\s+$", "");
    }

    private static final class SourceInfo {
        final String sourceReal;
        final String sourceNick;
        final long timeMs;
        SourceInfo(String real, String nick, long t) {
            this.sourceReal = real;
            this.sourceNick = nick;
            this.timeMs = t;
        }
    }
}
