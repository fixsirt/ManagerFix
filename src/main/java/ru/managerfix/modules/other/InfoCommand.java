package ru.managerfix.modules.other;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.modules.ban.BanModule;
import ru.managerfix.modules.ban.BanRecord;
import ru.managerfix.modules.names.NamesModule;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class InfoCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public InfoCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.other.info")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (args.length < 1) {
            MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/pinfo <name>"));
            return true;
        }
        Player online = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (online == null || !online.isOnline()) {
            OfflinePlayer off = module.getPlugin().getServer().getOfflinePlayer(args[0]);
            if (off == null || (!off.hasPlayedBefore() && !off.isOnline())) {
                MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
                return true;
            }
            sendOfflineInfo(sender, off);
            startFullLinkedScanAndHistory(sender, off.getUniqueId());
            return true;
        }
        sendOnlineInfo(sender, online);
        startFullLinkedScanAndHistory(sender, online.getUniqueId());
        return true;
    }

    private void sendOnlineInfo(CommandSender sender, Player p) {
        String realName = p.getName();
        String display = ru.managerfix.utils.NickResolver.plainDisplayName(p);
        String customNick = display != null && !display.equals(realName) ? display : "-";
        sender.sendMessage(MessageUtil.parse("<#3498db>UUID: <white>" + p.getUniqueId() + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Реальный ник: <white>" + realName + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Кастом ник: <white>" + customNick + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Мир: <white>" + p.getWorld().getName() + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Координаты: <white>" + p.getLocation().getBlockX() + " "
                + p.getLocation().getBlockY() + " " + p.getLocation().getBlockZ() + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Режим игры: <white>" + p.getGameMode().name().toLowerCase() + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Флай: <white>" + (p.getAllowFlight() ? "вкл" : "выкл") + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Ping: <white>" + p.getPing() + " ms</white>"));
        long ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long hours = TimeUnit.SECONDS.toHours(ticks / 20);
        sender.sendMessage(MessageUtil.parse("<#3498db>Время онлайн: <white>" + hours + " ч.</white>"));
        String ipToShow = null;
        if (sender.hasPermission("managerfix.other.info.ip")) {
            InetSocketAddress addr = p.getAddress();
            ipToShow = addr != null && addr.getAddress() != null ? addr.getAddress().getHostAddress() : "unknown";
            sender.sendMessage(MessageUtil.parse("<#3498db>IP: <white>" + ipToShow + "</white>"));
        }
        String baseIp = resolveBaseIp(p.getUniqueId(), p.getAddress());
        sender.sendMessage(MessageUtil.parse("<#3498db>Аккаунты с таким же IP: <white>" + formatLinkedNames(baseIp, p.getUniqueId()) + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Статус: <white>онлайн</white>"));
        String group = resolveLuckPermsPrimaryGroup(p.getUniqueId());
        sender.sendMessage(MessageUtil.parse("<#3498db>Группа LP: <white>" + (group != null ? group : "-") + "</white>"));
    }

    private void sendOfflineInfo(CommandSender sender, OfflinePlayer off) {
        UUID uuid = off.getUniqueId();
        sender.sendMessage(MessageUtil.parse("<#3498db>UUID: <white>" + uuid + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Реальный ник: <white>" + (off.getName() != null ? off.getName() : "-") + "</white>"));
        
        String nick = "-";
        String lastLoc = "-";
        String lastGm = "-";
        String lastFly = "-";
        String ip = "-";
        
        // Загружаем данные из БД напрямую
        if (module.getPlugin() instanceof ManagerFix mf && mf.isMySqlStorage()) {
            try {
                java.sql.Connection conn = mf.getDatabaseManager().getConnection();
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT last_ip, metadata FROM profiles WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            ip = rs.getString("last_ip");
                            if (ip == null) ip = "-";
                            
                            String metaStr = rs.getString("metadata");
                            if (metaStr != null && !metaStr.isEmpty()) {
                                // Парсим metadata: key\tvalue\n
                                for (String line : metaStr.split("\n")) {
                                    int tab = line.indexOf('\t');
                                    if (tab > 0) {
                                        String key = line.substring(0, tab);
                                        String value = line.substring(tab + 1);
                                        switch (key) {
                                            case "last_location" -> lastLoc = value.replace(",", " ");
                                            case "last_gamemode" -> lastGm = value.toLowerCase(Locale.ROOT);
                                            case "last_fly" -> lastFly = value.equals("true") ? "вкл" : "выкл";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Fallback для YAML
            ProfileManager pm = getProfileManager();
            if (pm != null) {
                PlayerProfile profile = pm.getProfile(uuid);
                Object vNick = profile.getMetadata(NamesModule.getNickMetadataKey()).orElse(null);
                if (vNick instanceof String s && !s.isEmpty()) nick = s;
                Object vLoc = profile.getMetadata("last_location").orElse(null);
                if (vLoc instanceof String s && !s.isEmpty()) lastLoc = s.replace(",", " ");
                Object vGm = profile.getMetadata("last_gamemode").orElse(null);
                if (vGm instanceof String s && !s.isEmpty()) lastGm = s.toLowerCase(Locale.ROOT);
                Object vFly = profile.getMetadata("last_fly").orElse(null);
                if (vFly instanceof Boolean b) lastFly = b ? "вкл" : "выкл";
                Object vIp = profile.getMetadata("last_ip").orElse(null);
                if (vIp instanceof String s && !s.isEmpty()) ip = s;
            }
        }
        
        sender.sendMessage(MessageUtil.parse("<#3498db>Кастом ник: <white>" + nick + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Мир: <white>" + (lastLoc.contains(",") ? lastLoc.substring(0, lastLoc.indexOf(',')) : lastLoc) + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Координаты: <white>" + (lastLoc.contains(",") ? lastLoc.substring(lastLoc.indexOf(',') + 1).replace(",", " ") : lastLoc) + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Режим игры: <white>" + lastGm + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Флай: <white>" + lastFly + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Ping: <white>-</white>"));
        long last = resolveLastSeenMillis(off);
        sender.sendMessage(MessageUtil.parse("<#3498db>Последний онлайн: <white>" + formatDate(last) + "</white>"));
        if (sender.hasPermission("managerfix.other.info.ip")) {
            sender.sendMessage(MessageUtil.parse("<#3498db>IP: <white>" + ip + "</white>"));
        }
        sender.sendMessage(MessageUtil.parse("<#3498db>Аккаунты с таким же IP: <white>" + formatLinkedNames(ip, uuid) + "</white>"));
        sender.sendMessage(MessageUtil.parse("<#3498db>Статус: <white>офлайн</white>"));
        String group = resolveLuckPermsPrimaryGroup(uuid);
        sender.sendMessage(MessageUtil.parse("<#3498db>Группа LP: <white>" + (group != null ? group : "-") + "</white>"));
    }

    private void sendBanHistoryAsync(CommandSender sender, UUID target, Set<UUID> linked) {
        if (!(module.getPlugin() instanceof ManagerFix mf)) return;
        var opt = mf.getModuleManager().getEnabledModule("ban");
        if (opt.isEmpty()) return;
        BanModule banModule = opt.filter(m -> m instanceof BanModule)
            .map(m -> (BanModule) m)
            .orElse(null);
        if (banModule == null) return;
        var history = banModule.getBanHistoryStorage();
        if (history != null) {
            Set<UUID> scope = new HashSet<>(linked);
            scope.add(target);
            history.getHistoryAsync(scope, entries -> {
                List<ru.managerfix.modules.ban.BanHistoryEntry> sorted = new ArrayList<>(entries);
                sorted.sort(Comparator.comparingLong(ru.managerfix.modules.ban.BanHistoryEntry::getCreatedAt));
                if (sorted.isEmpty()) {
                    sender.sendMessage(MessageUtil.parse("<#3498db>История банов: <white>-</white>"));
                    return;
                }
                sender.sendMessage(MessageUtil.parse("<#3498db>История банов:"));
                for (ru.managerfix.modules.ban.BanHistoryEntry e : sorted) {
                    String date = formatDate(e.getCreatedAt());
                    String who = e.getTargetName() != null ? e.getTargetName() : e.getTargetUuid().toString();
                    String reason = e.getReason() != null ? e.getReason() : "";
                    sender.sendMessage(MessageUtil.parse("<white>- " + date + " <#95a5a6>(" + who + ")</#95a5a6>: " + reason));
                }
            });
        } else {
            banModule.getBanManager().getBansAsync(list -> {
                List<BanRecord> filtered = list.stream()
                        .filter(r -> r.getTargetUuid().equals(target) || linked.contains(r.getTargetUuid()))
                        .sorted(Comparator.comparingLong(BanRecord::getCreatedAt))
                        .collect(Collectors.toList());
                if (filtered.isEmpty()) {
                    sender.sendMessage(MessageUtil.parse("<#3498db>История банов: <white>-</white>"));
                    return;
                }
                sender.sendMessage(MessageUtil.parse("<#3498db>История банов:"));
                for (BanRecord r : filtered) {
                    String date = formatDate(r.getCreatedAt());
                    String who = r.getTargetName() != null ? r.getTargetName() : r.getTargetUuid().toString();
                    sender.sendMessage(MessageUtil.parse("<white>- " + date + " <#95a5a6>(" + who + ")</#95a5a6>: " + (r.getReason() != null ? r.getReason() : "")));
                }
            });
        }
    }

    private String formatLinkedNames(String ip, UUID self) {
        if (ip == null || ip.isEmpty()) return "-";
        Set<UUID> uuids = resolveLinkedUuidsByIp(self);
        if (uuids.isEmpty()) return "-";
        List<String> names = new ArrayList<>();
        for (UUID u : uuids) {
            if (u.equals(self)) continue;
            Player online = ru.managerfix.utils.NickResolver.getPlayerByUuid(u);
            if (online != null) {
                names.add(online.getName());
            } else {
                String name = Bukkit.getOfflinePlayer(u).getName();
                names.add(name != null ? name : u.toString());
            }
        }
        return names.isEmpty() ? "-" : String.join(", ", names);
    }

    private Set<UUID> resolveLinkedUuidsByIp(UUID base) {
        ProfileManager pm = getProfileManager();
        if (pm == null) return Collections.emptySet();
        String baseIp = null;
        PlayerProfile baseProfile = pm.getProfile(base);
        Object vIp = baseProfile.getMetadata("last_ip").orElse(null);
        if (vIp instanceof String s && !s.isEmpty()) baseIp = s;
        if (baseIp == null || baseIp.isEmpty()) return Collections.emptySet();
        Set<UUID> result = new HashSet<>();
        for (UUID uuid : pm.getCachedUuids()) {
            PlayerProfile profile = pm.getProfile(uuid);
            Object ip = profile.getMetadata("last_ip").orElse(null);
            if (ip instanceof String s && s.equals(baseIp)) {
                result.add(uuid);
            }
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            InetSocketAddress addr = online.getAddress();
            String ipStr = addr != null && addr.getAddress() != null ? addr.getAddress().getHostAddress() : null;
            if (ipStr != null && ipStr.equals(baseIp)) {
                result.add(online.getUniqueId());
            }
        }
        return result;
    }

    private String resolveBaseIp(UUID uuid, InetSocketAddress currentAddr) {
        ProfileManager pm = getProfileManager();
        String ip = null;
        if (currentAddr != null && currentAddr.getAddress() != null) {
            ip = currentAddr.getAddress().getHostAddress();
        }
        if ((ip == null || ip.isEmpty()) && pm != null) {
            PlayerProfile profile = pm.getProfile(uuid);
            Object vIp = profile.getMetadata("last_ip").orElse(null);
            if (vIp instanceof String s && !s.isEmpty()) ip = s;
        }
        return ip;
    }

    private void startFullLinkedScanAndHistory(CommandSender sender, UUID subject) {
        ProfileManager pm = getProfileManager();
        if (pm == null) {
            sendBanHistoryAsync(sender, subject, resolveLinkedUuidsByIp(subject));
            return;
        }
        Player online = ru.managerfix.utils.NickResolver.getPlayerByUuid(subject);
        String baseIp = resolveBaseIp(subject, online != null ? online.getAddress() : null);
        Set<UUID> initial = resolveLinkedUuidsByIp(subject);
        sendBanHistoryAsync(sender, subject, initial);
        if (baseIp == null || baseIp.isEmpty()) return;
        pm.findUuidsByMetadataEquals("last_ip", baseIp, all -> {
            Set<UUID> merged = new HashSet<>(initial);
            merged.addAll(all);
            if (merged.size() > initial.size()) {
                List<String> names = new ArrayList<>();
                for (UUID u : merged) {
                    if (u.equals(subject)) continue;
                    Player p = ru.managerfix.utils.NickResolver.getPlayerByUuid(u);
                    if (p != null) names.add(p.getName());
                    else {
                        String n = Bukkit.getOfflinePlayer(u).getName();
                        names.add(n != null ? n : u.toString());
                    }
                }
                String line = names.isEmpty() ? "-" : String.join(", ", names);
                sender.sendMessage(MessageUtil.parse("<#3498db>Аккаунты с таким же IP (скан): <white>" + line + "</white>"));
                sendBanHistoryAsync(sender, subject, merged);
            }
        });
    }

    private String resolveLuckPermsPrimaryGroup(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("net.luckperms.api.LuckPerms");
            var reg = Bukkit.getServicesManager().getRegistration(apiClass);
            if (reg == null) return null;
            Object luckPerms = reg.getProvider();
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, uuid);
            if (user == null) return null;
            return (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
        } catch (Throwable ignored) {
            return null;
        }
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

    private String formatDate(long millis) {
        if (millis <= 0) return "-";
        try {
            return new SimpleDateFormat("dd.MM.yyyy").format(new Date(millis));
        } catch (Throwable ignored) {
            return String.valueOf(millis);
        }
    }

    private ProfileManager getProfileManager() {
        if (module.getPlugin() instanceof ManagerFix mf) {
            return mf.getProfileManager();
        }
        return null;
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
