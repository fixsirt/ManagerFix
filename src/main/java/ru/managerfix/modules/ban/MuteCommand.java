package ru.managerfix.modules.ban;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.modules.ban.BanModule;
import ru.managerfix.utils.MessageUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MuteCommand implements CommandExecutor, TabCompleter {

    private final BanModule banModule;

    public MuteCommand(BanModule banModule) {
        this.banModule = banModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        MuteManager mm = banModule.getMuteManager();

        if (mm == null) {
            sender.sendMessage(MessageUtil.parse("<#FF3366>Модуль мутов не загружен!"));
            return true;
        }

        if ("mute".equals(cmd)) {
            if (args.length < 1) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Использование: /mute <игрок> [время] [причина]"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Игрок не найден!"));
                return true;
            }
            
            // Парсинг времени и причины
            long expiresAt = 0; // 0 = навсегда
            String reason;
            int argIndex = 1;
            
            if (args.length > 1) {
                long durationMs = parseDuration(args[1]);
                if (durationMs == -1 || (durationMs > 0 && args[1].matches("-?\\d+[smhd]?"))) {
                    if (durationMs == -1) {
                        expiresAt = 0;
                    } else {
                        expiresAt = System.currentTimeMillis() + durationMs;
                    }
                    argIndex = 2;
                }
            }
            
            reason = argIndex < args.length ? String.join(" ", java.util.Arrays.copyOfRange(args, argIndex, args.length)) : "";
            String source = sender.getName();
            
            final long finalExpiresAt = expiresAt;
            mm.mute(target.getUniqueId(), target.getName(), reason, source, finalExpiresAt, () -> {
                sender.sendMessage(MessageUtil.parse("<#00C8FF>Игрок замучен!"));
                if (banModule.isBroadcastMutes()) {
                    String duration = finalExpiresAt == 0 ? "Навсегда" : formatDuration(finalExpiresAt - System.currentTimeMillis());
                    broadcast(banModule.getFormatMuteBroadcast(), sender, target.getUniqueId(), target.getName(), reason, duration);
                }
            });
            return true;
        }
        if ("unmute".equals(cmd)) {
            if (args.length < 1) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Использование: /unmute <игрок>"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Игрок не найден!"));
                return true;
            }
            
            // Проверяем, замьючен ли игрок
            var muteOpt = mm.getMute(target.getUniqueId());
            if (muteOpt.isEmpty() || muteOpt.get().isExpired()) {
                sender.sendMessage(MessageUtil.parse("<#FF3366>Игрок <#F0F4F8>" + target.getName() + "</#F0F4F8> не замьючен."));
                return true;
            }
            
            mm.unmute(target.getUniqueId(), () -> sender.sendMessage(MessageUtil.parse("<#00C8FF>Игрок <#F0F4F8>" + target.getName() + "</#F0F4F8> размучен!")));
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (args.length == 1) {
            String a = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(a)).sorted().collect(Collectors.toList());
        }
        if ("mute".equals(cmd) && args.length == 2) {
            String p = args[1].toLowerCase();
            List<String> base = List.of("-1", "10m","30m","1h","2h","6h","12h","1d","3d","7d","30d");
            return base.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static long parseDuration(String s) {
        if (s == null || s.isEmpty()) return 0;
        // Проверка на -1 (навсегда)
        if ("-1".equals(s)) return -1;
        try {
            char last = Character.toLowerCase(s.charAt(s.length() - 1));
            long num = Long.parseLong(last >= '0' && last <= '9' ? s : s.substring(0, s.length() - 1));
            return switch (last) {
                case 's' -> num * 1000;
                case 'm' -> num * 60 * 1000;
                case 'h' -> num * 3600 * 1000;
                case 'd' -> num * 86400 * 1000;
                default -> num * 1000;
            };
        } catch (Exception e) {
            return 0;
        }
    }

    private static String formatDuration(long ms) {
        long seconds = (ms + 500) / 1000;
        long d = seconds / 86400; seconds %= 86400;
        long h = seconds / 3600; seconds %= 3600;
        long m = seconds / 60;
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (d > 0) parts.add(d + "д");
        if (h > 0) parts.add(h + "ч");
        if (m > 0) parts.add(m + "м");
        if (parts.isEmpty()) parts.add("менее минуты");
        return String.join(" ", parts);
    }

    private void broadcast(String format, CommandSender sender, java.util.UUID targetUuid, String targetReal, String reason, String duration) {
        String sourceReal = sender.getName();
        String targetNick = targetReal;
        Player online = ru.managerfix.utils.NickResolver.getPlayerByUuid(targetUuid);
        if (online != null) {
            String dn = ru.managerfix.utils.NickResolver.plainDisplayName(online);
            if (dn != null && !dn.isBlank()) targetNick = dn;
        }
        String msg = format;
        msg = msg.replace("{targetReal}", targetReal != null ? targetReal : "?");
        msg = msg.replace("{sourceReal}", sourceReal != null ? sourceReal : "Console");
        msg = msg.replace("{reason}", reason != null ? reason : "");
        msg = msg.replace("{duration}", duration != null ? duration : "");
        msg = msg.replace("{targetNick}", targetNick != null ? targetNick : "");
        msg = msg.replace("{sourceNick}", "");
        var component = MessageUtil.parse(msg);
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.sendMessage(component);
        }
        banModule.getPlugin().getServer().getConsoleSender().sendMessage(component);
    }
}
