package ru.managerfix.modules.ban;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import ru.managerfix.ManagerFix;
import ru.managerfix.storage.SqlBanIpStorage;
import ru.managerfix.utils.MessageUtil;

import java.util.Optional;

/**
 * Checks ban on login (AsyncPlayerPreLoginEvent). Kick with reason.
 */
public final class BanLoginListener implements Listener {

    private final BanModule module;

    public BanLoginListener(BanModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        
        // Проверяем обычный бан
        Optional<BanRecord> opt = module.getBanManager().getBan(event.getUniqueId());
        if (opt.isPresent()) {
            BanRecord r = opt.get();
            if (!r.isExpired()) {
                String format = module.getDefaultKickMessage();
                String reason = r.getReason() != null ? r.getReason() : "";
                String msg = format.replace("{reason}", reason);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, MessageUtil.parse(msg));
                return;
            }
        }
        
        // Проверяем IP-бан
        String ipAddress = event.getAddress().getHostAddress();
        if (module.getPlugin() instanceof ManagerFix mf && mf.isMySqlStorage()) {
            SqlBanIpStorage ipStorage = mf.getSqlBanIpStorage();
            if (ipStorage != null) {
                Optional<BanIpRecord> ipBan = ipStorage.getBan(ipAddress);
                if (ipBan.isPresent() && !ipBan.get().isExpired()) {
                    BanIpRecord r = ipBan.get();
                    String reason = r.getReason() != null ? r.getReason() : "";
                    String source = r.getSource() != null ? r.getSource() : "Console";
                    String duration = r.isPermanent() ? "Навсегда" : formatDuration(r.getExpiresAt() - System.currentTimeMillis());
                    
                    // Формируем красивое сообщение
                    String raw = "<color:#F0F4F8>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</color>\n" +
                                 "<color:#FF3366>⛔ IP БАН</color>\n" +
                                 "<color:#F0F4F8>IP:</color> <color:#F0F4F8>{ip}</color>\n" +
                                 "<color:#F0F4F8>Забанил:</color> <color:#F0F4F8>{source}</color>\n" +
                                 "<color:#F0F4F8>На:</color> <color:#00C8FF>{duration}</color>\n" +
                                 "<color:#F0F4F8>Причина:</color> <color:#00C8FF>{reason}</color>\n" +
                                 "<color:#F0F4F8>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</color>";
                    
                    String msg = raw.replace("{ip}", ipAddress)
                                    .replace("{source}", source)
                                    .replace("{duration}", duration)
                                    .replace("{reason}", reason);
                    net.kyori.adventure.text.Component component = MessageUtil.parse(msg);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, component);
                }
            }
        }
    }
    
    private static String formatDuration(long ms) {
        if (ms <= 0) return "Навсегда";
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
