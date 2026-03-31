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
 * /unbanip <ip|player> - разбан по IP адресу.
 */
public final class UnbanIpCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_UNBAN_IP = "managerfix.ban.ip.unban";

    private final ManagerFix plugin;

    public UnbanIpCommand(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkCommandPermission(sender, "unbanip", PERM_UNBAN_IP, plugin)) return true;

        if (args.length < 1) {
            MessageUtil.send(plugin, sender, "unbanip.usage");
            return true;
        }

        String input = args[0];
        
        // Проверяем, является ли аргумент IP-адресом или ником
        if (input.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            // Это IP - разбаниваем сразу
            unbanByIp(sender, input);
        } else {
            // Это ник - нужно найти IP игрока асинхронно
            unbanByPlayerName(sender, input);
        }

        return true;
    }
    
    private void unbanByIp(@NotNull CommandSender sender, String ipAddress) {
        if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
            SqlBanIpStorage ipStorage = mf.getSqlBanIpStorage();
            if (ipStorage != null) {
                // Проверяем наличие бана асинхронно
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    var banOpt = ipStorage.getBan(ipAddress);
                    if (banOpt.isEmpty() || banOpt.get().isExpired()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> 
                            MessageUtil.send(plugin, sender, "unbanip.not-banned", Map.of("ip", ipAddress))
                        );
                        return;
                    }
                    
                    // Разбаниваем IP
                    ipStorage.removeBanAsync(ipAddress, () -> {
                        // Также удаляем обычный бан если он есть
                        removeBanFromBansTable(ipAddress);
                        
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            MessageUtil.send(plugin, sender, "unbanip.unbanned", Map.of("ip", ipAddress));
                            broadcastUnbanIp(sender, ipAddress);
                        });
                    });
                });
                return;
            }
        }
        MessageUtil.send(plugin, sender, "unbanip.unbanned", Map.of("ip", ipAddress));
    }
    
    /**
     * Удаляет бан из таблицы bans по IP.
     */
    private void removeBanFromBansTable(String ipAddress) {
        if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
            mf.getServer().getScheduler().runTaskAsynchronously(mf, () -> {
                try (java.sql.Connection conn = mf.getDatabaseManager().getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM bans WHERE ip_address = ?")) {
                    ps.setString(1, ipAddress);
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    private void unbanByPlayerName(@NotNull CommandSender sender, String playerName) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (target == null || !target.hasPlayedBefore()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> 
                    MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", playerName))
                );
                return;
            }
            
            String ipAddress = getPlayerIp(target);
            if (ipAddress == null) {
                // Проверяем бан по UUID
                if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                    SqlBanIpStorage ipStorage = mf.getSqlBanIpStorage();
                    if (ipStorage != null) {
                        var banOpt = ipStorage.getBanByUuid(target.getUniqueId());
                        if (banOpt.isPresent() && !banOpt.get().isExpired()) {
                            ipAddress = banOpt.get().getIpAddress();
                        }
                    }
                }
            }
            
            if (ipAddress == null) {
                plugin.getServer().getScheduler().runTask(plugin, () -> 
                    MessageUtil.send(plugin, sender, "unbanip.no-ip-found", Map.of("player", playerName))
                );
                return;
            }
            
            final String finalIpAddress = ipAddress;
            
            // Проверяем и разбаниваем
            if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                SqlBanIpStorage ipStorage = mf.getSqlBanIpStorage();
                if (ipStorage != null) {
                    var banOpt = ipStorage.getBan(finalIpAddress);
                    if (banOpt.isEmpty() || banOpt.get().isExpired()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> 
                            MessageUtil.send(plugin, sender, "unbanip.not-banned", Map.of("ip", finalIpAddress))
                        );
                        return;
                    }
                    
                    ipStorage.removeBanAsync(finalIpAddress, () -> {
                        // Также удаляем обычный бан если он есть
                        removeBanFromBansTable(finalIpAddress);
                        
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            MessageUtil.send(plugin, sender, "unbanip.unbanned", Map.of("ip", finalIpAddress));
                            broadcastUnbanIp(sender, finalIpAddress);
                        });
                    });
                    return;
                }
            }
            
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                MessageUtil.send(plugin, sender, "unbanip.unbanned", Map.of("ip", finalIpAddress))
            );
        });
    }
    
    /**
     * Получает IP бан по UUID игрока из таблицы ip_bans.
     */
    private Optional<String> getAllBansByUuid(UUID uuid) {
        try {
            if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                SqlBanIpStorage ipStorage = mf.getSqlBanIpStorage();
                if (ipStorage != null) {
                    var banOpt = ipStorage.getBanByUuid(uuid);
                    if (banOpt.isPresent() && !banOpt.get().isExpired()) {
                        return Optional.of(banOpt.get().getIpAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Получает последний IP игрока.
     */
    private String getPlayerIp(OfflinePlayer target) {
        // Сначала пробуем получить из профиля если игрок онлайн
        if (target.isOnline() && target.getPlayer() != null) {
            return target.getPlayer().getAddress().getAddress().getHostAddress();
        }

        // Получаем из БД
        try {
            if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                java.sql.Connection conn = mf.getDatabaseManager().getConnection();
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT last_ip, name FROM profiles WHERE uuid = ?")) {
                    ps.setString(1, target.getUniqueId().toString());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String ip = rs.getString("last_ip");
                            if (ip != null && !ip.isEmpty()) {
                                return ip;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void broadcastUnbanIp(CommandSender sender, String ipAddress) {
        String sourceReal = sender.getName();
        String msg = plugin instanceof ManagerFix mf 
            ? mf.getConfigManager().getModuleConfig("ban/config.yml").getString("format.ip-unban-broadcast",
                "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n&e⬤ IP РАЗБАН\n&7IP: &f{ip}\n&7Разбанил: &f{sourceReal}\n&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            : "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n&e⬤ IP РАЗБАН\n&7IP: &f{ip}\n&7Разбанил: &f{sourceReal}\n&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
        
        msg = msg.replace("{ip}", ipAddress != null ? ipAddress : "?");
        msg = msg.replace("{sourceReal}", sourceReal != null ? sourceReal : "Console");
        
        Bukkit.getServer().sendMessage(MessageUtil.parse(msg));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String a = args[0].toLowerCase();
            // Предлагаем ники онлайн игроков
            List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(a))
                .sorted()
                .collect(Collectors.toList());
            
            // Также можно предложить IP из активных банов
            if (plugin instanceof ManagerFix mf && mf.isMySqlStorage()) {
                SqlBanIpStorage ipStorage = mf.getSqlBanIpStorage();
                if (ipStorage != null) {
                    try {
                        java.util.List<BanIpRecord> bans = new java.util.ArrayList<>();
                        ipStorage.getBansAsync(bans::addAll);
                        for (BanIpRecord ban : bans) {
                            String ip = ban.getIpAddress();
                            if (ip.toLowerCase().startsWith(a)) {
                                suggestions.add(ip);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            return suggestions;
        }
        return Collections.emptyList();
    }
}
