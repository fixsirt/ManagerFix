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
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.BanListGui;
import ru.managerfix.utils.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /ban <player> [time] [reason], /unban <player>, /banlist (GUI).
 * Время: -1 = навсегда, или формат: 10m, 30m, 1h, 2h, 6h, 12h, 1d, 3d, 7d, 30d
 */
public final class BanCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_BAN = "managerfix.ban.use";
    private static final String PERM_UNBAN = "managerfix.ban.unban";
    private static final String PERM_LIST = "managerfix.ban.list";

    private final ManagerFix plugin;
    private final GuiManager guiManager;

    public BanCommand(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();
        if ("banlist".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "banlist", PERM_LIST, plugin)) return true;
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            var moduleOpt = plugin.getModuleManager().getEnabledModule("ban");
            if (moduleOpt.isEmpty()) { MessageUtil.send(plugin, sender, "module-disabled"); return true; }
            new BanListGui(plugin, guiManager, (ru.managerfix.modules.ban.BanModule) moduleOpt.get()).open((Player) sender, 0);
            return true;
        }

        if ("ban".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "ban", PERM_BAN, plugin)) return true;
            if (args.length < 1) {
                MessageUtil.send(plugin, sender, "ban.usage");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || !target.hasPlayedBefore()) {
                MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args[0]));
                return true;
            }
            
            // Парсинг времени и причины
            long expiresAt = 0; // 0 = навсегда
            String reason;
            int argIndex = 1;
            
            if (args.length > 1) {
                // Проверяем, является ли второй аргумент временем
                long durationMs = parseDuration(args[1]);
                if (durationMs == -1 || (durationMs > 0 && args[1].matches("-?\\d+[smhd]?"))) {
                    // Это время
                    if (durationMs == -1) {
                        // -1 = навсегда
                        expiresAt = 0;
                    } else {
                        expiresAt = System.currentTimeMillis() + durationMs;
                    }
                    argIndex = 2;
                }
            }
            
            reason = argIndex < args.length ? String.join(" ", Arrays.copyOfRange(args, argIndex, args.length)) : "";
            String source = sender.getName();
            
            BanModule banModule = plugin.getModuleManager().getEnabledModule("ban")
                .filter(m -> m instanceof BanModule)
                .map(m -> (BanModule) m)
                .orElse(null);
            if (banModule == null) { MessageUtil.send(plugin, sender, "module-disabled"); return true; }
            
            final long finalExpiresAt = expiresAt;
            // Бан выполняется асинхронно
            banModule.getBanManager().ban(target.getUniqueId(), target.getName(), reason, source, finalExpiresAt, () -> {
                // Возвращаемся в главный поток только для кика и сообщения
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtil.send(plugin, sender, "ban.banned", Map.of("player", target.getName()));
                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().kick(MessageUtil.get(plugin, sender, "ban.kick-message", Map.of("reason", reason)));
                    }
                });
                
                // Broadcast выполняем асинхронно чтобы не блокировать главный поток
                if (banModule.isBroadcastBans()) {
                    String duration = finalExpiresAt == 0 ? "Навсегда" : formatDuration(finalExpiresAt - System.currentTimeMillis());
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
                        broadcast(banModule.getFormatBanBroadcast(), sender, target.getUniqueId(), target.getName(), reason, duration)
                    );
                }
            });
            return true;
        }

        if ("unban".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "unban", PERM_UNBAN, plugin)) return true;
            if (args.length < 1) {
                MessageUtil.send(plugin, sender, "unban.usage");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            BanModule banModule = plugin.getModuleManager().getEnabledModule("ban")
                .filter(m -> m instanceof BanModule)
                .map(m -> (BanModule) m)
                .orElse(null);
            if (banModule == null) { MessageUtil.send(plugin, sender, "module-disabled"); return true; }
            
            // Проверяем, забанен ли игрок
            var banOpt = banModule.getBanManager().getBan(target.getUniqueId());
            if (banOpt.isEmpty() || banOpt.get().isExpired()) {
                MessageUtil.send(plugin, sender, "unban.not-banned", Map.of("player", target.getName()));
                return true;
            }
            
            banModule.getBanManager().unban(target.getUniqueId(), target.getName(), sender.getName(), () ->
                    MessageUtil.send(plugin, sender, "ban.unbanned", Map.of("player", target.getName())));
            return true;
        }

        return false;
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
                default -> num * 1000; // default seconds
            };
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();
        if (args.length == 1) {
            String a = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(a)).sorted().collect(Collectors.toList());
        }
        if ("ban".equals(cmdName) && args.length == 2) {
            String p = args[1].toLowerCase();
            java.util.List<String> base = java.util.List.of("-1", "10m","30m","1h","2h","6h","12h","1d","3d","7d","30d");
            return base.stream().filter(s -> s.startsWith(p)).collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
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
        
        // Асинхронная рассылка чтобы не блокировать главный поток
        net.kyori.adventure.text.Component component = ru.managerfix.utils.MessageUtil.parse(msg);
        Bukkit.getOnlinePlayers().forEach(player -> 
            player.sendMessage(component)
        );
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
}
