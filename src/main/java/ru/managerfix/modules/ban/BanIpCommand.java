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
import ru.managerfix.storage.SqlBanIpStorage;
import ru.managerfix.utils.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /banip <player> <time> <reason> - бан по IP адресу.
 * Время: -1 = навсегда, или формат: 10m, 30m, 1h, 2h, 6h, 12h, 1d, 3d, 7d, 30d
 */
public final class BanIpCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_BAN_IP = "managerfix.ban.ip";

    private final ManagerFix plugin;
    private final BanModule banModule;

    public BanIpCommand(ManagerFix plugin, BanModule banModule) {
        this.plugin = plugin;
        this.banModule = banModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkCommandPermission(sender, "banip", PERM_BAN_IP, plugin)) return true;

        if (args.length < 3) {
            MessageUtil.send(plugin, sender, "banip.usage");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || !target.hasPlayedBefore()) {
            MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }

        // Парсинг времени
        long durationMs = parseDuration(args[1]);
        if (durationMs == 0) {
            MessageUtil.send(plugin, sender, "banip.invalid-time");
            return true;
        }

        long expiresAt = durationMs == -1 ? 0 : System.currentTimeMillis() + durationMs;

        // Причина (все аргументы после времени)
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Получаем IP игрока
        String ipAddress = null;
        if (target.isOnline()) {
            Player onlinePlayer = target.getPlayer();
            if (onlinePlayer != null) {
                ipAddress = onlinePlayer.getAddress().getAddress().getHostAddress();
            }
        }

        // Если игрок офлайн, пытаемся получить IP из базы данных
        if (ipAddress == null) {
            ipAddress = getPlayerIpFromDatabase(target.getUniqueId());
        }

        if (ipAddress == null || ipAddress.isEmpty()) {
            MessageUtil.send(plugin, sender, "banip.no-ip-found", Map.of("player", target.getName()));
            return true;
        }

        final String finalIpAddress = ipAddress;
        String source = sender.getName();
        UUID targetUuid = target.getUniqueId();

        // Бан всех аккаунтов с этим IP
        banAllAccountsByIp(targetUuid, finalIpAddress, reason, source, expiresAt, target.getName(), () -> {
            MessageUtil.send(plugin, sender, "banip.banned", Map.of(
                "player", target.getName(),
                "ip", finalIpAddress
            ));

            // Кикаем онлайн игроков с этим IP
            kickOnlinePlayersByIp(finalIpAddress, reason);

            // Транслируем сообщение о бане
            if (banModule.isBroadcastBans()) {
                String duration = expiresAt == 0 ? "Навсегда" : formatDuration(expiresAt - System.currentTimeMillis());
                broadcastBanIp(sender, target.getName(), finalIpAddress, reason, duration);
            }
        });

        return true;
    }

    /**
     * Банит все аккаунты с указанным IP.
     */
    private void banAllAccountsByIp(UUID targetUuid, String ipAddress, String reason, String source, long expiresAt, String targetName, Runnable onDone) {
        // Получаем все UUID с этим IP из истории входов
        List<UUID> uuidsByIp = getUuidsByIp(ipAddress);

        // Добавляем IP-бан
        BanIpRecord ipBan = new BanIpRecord(targetUuid, ipAddress, reason, source, System.currentTimeMillis(), expiresAt);

        if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
            SqlBanIpStorage storage = mf.getSqlBanIpStorage();
            if (storage != null) {
                // Выполняем все операции параллельно в асинхронном потоке
                storage.addBanAsync(ipBan, null); // Добавляем IP-бан

                // Сохраняем IP для целевого игрока в bans
                saveIpForTargetPlayerAsync(targetUuid, targetName, ipAddress, reason, source, expiresAt);

                // Баняем все аккаунты по UUID
                banAllUuids(uuidsByIp, reason, source, expiresAt, onDone);
                return;
            }
        }

        // Для YAML хранилища - просто логируем
        banAllUuids(uuidsByIp, reason, source, expiresAt, onDone);
    }
    
    /**
     * Сохраняет IP для целевого игрока в таблице bans (асинхронно).
     */
    private void saveIpForTargetPlayerAsync(UUID targetUuid, String playerName, String ipAddress, String reason, String source, long expiresAt) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                    java.sql.Connection conn = mf.getDatabaseManager().getConnection();
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO bans (uuid, name, reason, source, created_at, expires_at, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE name = VALUES(name), reason = VALUES(reason), source = VALUES(source), " +
                        "created_at = VALUES(created_at), expires_at = VALUES(expires_at), ip_address = VALUES(ip_address)")) {
                        ps.setString(1, targetUuid.toString());
                        ps.setString(2, playerName);
                        ps.setString(3, reason);
                        ps.setString(4, source);
                        ps.setLong(5, System.currentTimeMillis());
                        ps.setLong(6, expiresAt);
                        ps.setString(7, ipAddress);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Банит все указанные UUID.
     */
    private void banAllUuids(List<UUID> uuids, String reason, String source, long expiresAt, Runnable onDone) {
        if (uuids == null || uuids.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }

        // Выполняем бан всех UUID пакетом в асинхронном потоке
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                    java.sql.Connection conn = mf.getDatabaseManager().getConnection();

                    // Сначала получаем все ники одним запросом
                    Map<UUID, String> uuidToName = getPlayerNamesBatch(uuids);

                    // Пакетная вставка всех банов
                    String sql = "INSERT INTO bans (uuid, name, reason, source, created_at, expires_at, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE name = VALUES(name), reason = VALUES(reason), source = VALUES(source), " +
                                 "created_at = VALUES(created_at), expires_at = VALUES(expires_at)";

                    try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                        long now = System.currentTimeMillis();
                        for (UUID uuid : uuids) {
                            String playerName = uuidToName.getOrDefault(uuid, "Unknown");
                            ps.setString(1, uuid.toString());
                            ps.setString(2, playerName);
                            ps.setString(3, reason);
                            ps.setString(4, source);
                            ps.setLong(5, now);
                            ps.setLong(6, expiresAt);
                            ps.setNull(7, java.sql.Types.VARCHAR);
                            ps.addBatch();
                        }
                        ps.executeBatch();

                        // Обновляем кэш BanManager
                        if (banModule != null && banModule.getBanManager() != null) {
                            for (UUID uuid : uuids) {
                                String playerName = uuidToName.getOrDefault(uuid, "Unknown");
                                BanRecord record = new BanRecord(uuid, playerName, reason, source, now, expiresAt);
                                banModule.getBanManager().addToCache(record);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Возвращаемся в главный поток
            if (onDone != null) {
                plugin.getServer().getScheduler().runTask(plugin, onDone);
            }
        });
    }

    /**
     * Пакетно получает ники игроков по UUID из БД.
     */
    private Map<UUID, String> getPlayerNamesBatch(List<UUID> uuids) {
        Map<UUID, String> result = new java.util.HashMap<>();
        if (uuids == null || uuids.isEmpty()) return result;

        try {
            if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                java.sql.Connection conn = mf.getDatabaseManager().getConnection();
                
                // Строим запрос с нужным количеством placeholder'ов
                String placeholders = String.join(",", java.util.Collections.nCopies(uuids.size(), "?"));
                String sql = "SELECT uuid, name FROM profiles WHERE uuid IN (" + placeholders + ")";
                
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 0; i < uuids.size(); i++) {
                        ps.setString(i + 1, uuids.get(i).toString());
                    }
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            try {
                                UUID uuid = java.util.UUID.fromString(rs.getString("uuid"));
                                String name = rs.getString("name");
                                if (name != null && !name.isEmpty()) {
                                    result.put(uuid, name);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Для UUID которых нет в БД, ставим Unknown
        for (UUID uuid : uuids) {
            result.putIfAbsent(uuid, "Unknown");
        }

        return result;
    }

    /**
     * Кикает всех онлайн игроков с указанным IP.
     */
    private void kickOnlinePlayersByIp(String ipAddress, String reason) {
        // Кикаем асинхронно чтобы не блокировать главный поток
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            net.kyori.adventure.text.Component kickMessage = MessageUtil.parse("<#FF4D00>Вы забанены по IP. Причина: <#FFFFFF>" + reason + "</#FFFFFF>");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    String playerIp = player.getAddress().getAddress().getHostAddress();
                    if (ipAddress.equals(playerIp)) {
                        // Кик в главном потоке
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            player.kick(kickMessage);
                        });
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки получения IP
                }
            }
        });
    }

    /**
     * Получает все UUID, которые заходили с указанного IP.
     */
    private List<UUID> getUuidsByIp(String ipAddress) {
        List<UUID> uuids = new ArrayList<>();
        
        // Получаем из базы данных profiles по last_ip
        try {
            if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                java.sql.Connection conn = mf.getDatabaseManager().getConnection();
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid FROM profiles WHERE last_ip = ?")) {
                    ps.setString(1, ipAddress);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            try {
                                uuids.add(UUID.fromString(rs.getString("uuid")));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return uuids;
    }

    /**
     * Получает имя игрока по UUID.
     */
    private String getPlayerNameByUuid(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer != null && offlinePlayer.hasPlayedBefore() 
            ? offlinePlayer.getName() : "Unknown";
    }

    /**
     * Получает последний IP игрока из базы данных.
     */
    private String getPlayerIpFromDatabase(UUID uuid) {
        // Сначала пробуем получить из профиля
        if (plugin instanceof ManagerFix mf) {
            var profile = mf.getProfileManager().getProfile(uuid);
            if (profile != null && profile.getLastIpAddress() != null && !profile.getLastIpAddress().isEmpty()) {
                return profile.getLastIpAddress();
            }
        }
        
        try {
            if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                java.sql.Connection conn = mf.getDatabaseManager().getConnection();
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT last_ip FROM profiles WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("last_ip");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String a = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(a)).sorted().collect(Collectors.toList());
        }
        if (args.length == 2) {
            String p = args[1].toLowerCase();
            List<String> base = List.of("-1", "10m","30m","1h","2h","6h","12h","1d","3d","7d","30d");
            return base.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static long parseDuration(String s) {
        if (s == null || s.isEmpty()) return 0;
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

    private void broadcastBanIp(CommandSender sender, String targetName, String ipAddress, String reason, String duration) {
        String sourceReal = sender.getName();
        
        // Получаем формат из конфига бан-модуля
        String format = plugin instanceof ManagerFix mf 
            ? mf.getConfigManager().getModuleConfig("ban/config.yml").getString("format.ip-ban-broadcast",
                "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n&c⛔ IP БАН\n&7Игрок: &f{targetReal}\n&7IP: &f{ip}\n&7Забанил: &f{sourceReal}\n&7На: &e{duration}\n&7Причина: &e{reason}\n&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            : "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n&c⛔ IP БАН\n&7Игрок: &f{targetReal}\n&7IP: &f{ip}\n&7Забанил: &f{sourceReal}\n&7На: &e{duration}\n&7Причина: &e{reason}\n&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        
        String msg = format;
        msg = msg.replace("{targetReal}", targetName != null ? targetName : "?");
        msg = msg.replace("{sourceReal}", sourceReal != null ? sourceReal : "Console");
        msg = msg.replace("{reason}", reason != null ? reason : "");
        msg = msg.replace("{duration}", duration != null ? duration : "");
        msg = msg.replace("{ip}", ipAddress != null ? ipAddress : "?");

        Bukkit.getServer().sendMessage(MessageUtil.parse(msg));
    }

    private static String formatDuration(long ms) {
        long seconds = (ms + 500) / 1000;
        long d = seconds / 86400; seconds %= 86400;
        long h = seconds / 3600; seconds %= 3600;
        long m = seconds / 60;
        List<String> parts = new ArrayList<>();
        if (d > 0) parts.add(d + "д");
        if (h > 0) parts.add(h + "ч");
        if (m > 0) parts.add(m + "м");
        if (parts.isEmpty()) parts.add("менее минуты");
        return String.join(" ", parts);
    }
}
