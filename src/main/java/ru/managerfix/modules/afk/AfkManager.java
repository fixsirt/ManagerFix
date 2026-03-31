package ru.managerfix.modules.afk;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.managerfix.ManagerFix;
import ru.managerfix.event.AfkEnterEvent;
import ru.managerfix.event.AfkLeaveEvent;
import ru.managerfix.event.EventBus;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * AFK state: stored in profile metadata "afk" (boolean).
 * Total AFK time: "afk_enter_time" (Long ms when entered), "afk_total_seconds" (Long cumulative).
 */
public final class AfkManager {

    private static final String METADATA_AFK = "afk";
    private static final String METADATA_AFK_ENTER_TIME = "afk_enter_time";
    private static final String METADATA_AFK_TOTAL_SECONDS = "afk_total_seconds";

    private final ProfileManager profileManager;
    private final EventBus eventBus;

    public AfkManager(ProfileManager profileManager, EventBus eventBus) {
        this.profileManager = profileManager;
        this.eventBus = eventBus;
    }

    public boolean isAfk(Player player) {
        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null) return false;
        Object afkObj = profile.getMetadata(METADATA_AFK).orElse(null);
        if (afkObj instanceof Boolean b) return b;
        // Поддержка строковых значений "true"/"false"
        if (afkObj instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }

    public void setAfk(Player player, boolean afk) {
        if (!player.isOnline()) return;
        PlayerProfile profile = profileManager.getProfile(player);
        if (profile == null) {
            ru.managerfix.core.LoggerUtil.warning("Profile not found for player " + player.getName() + " in AfkManager.setAfk()");
            return;
        }
        Object afkObj = profile.getMetadata(METADATA_AFK).orElse(false);
        Boolean was;
        if (afkObj instanceof Boolean b) {
            was = b;
        } else if (afkObj instanceof String s) {
            was = Boolean.parseBoolean(s);
        } else {
            was = false;
        }
        
        if (was == afk) return;
        if (!afk) {
            // Leaving AFK: add this session to total
            Object enterObj = profile.getMetadata(METADATA_AFK_ENTER_TIME).orElse(null);
            if (enterObj instanceof Long enterTime && enterTime > 0) {
                long durationSec = (System.currentTimeMillis() - enterTime) / 1000;
                // Поддержка разных типов для afk_total_seconds
                Object totalObj = profile.getMetadata(METADATA_AFK_TOTAL_SECONDS).orElse(null);
                long total;
                if (totalObj instanceof Number n) {
                    total = n.longValue();
                } else if (totalObj instanceof String s) {
                    try {
                        total = Long.parseLong(s.trim());
                    } catch (NumberFormatException e) {
                        total = 0L;
                    }
                } else {
                    total = 0L;
                }
                profile.setMetadata(METADATA_AFK_TOTAL_SECONDS, total + durationSec);
                profile.setMetadata(METADATA_AFK_ENTER_TIME, null);
            }
        } else {
            profile.setMetadata(METADATA_AFK_ENTER_TIME, System.currentTimeMillis());
        }
        profile.setMetadata(METADATA_AFK, afk);
        if (eventBus != null) {
            if (afk) {
                eventBus.callEvent(new AfkEnterEvent(player));
            } else {
                eventBus.callEvent(new AfkLeaveEvent(player));
            }
        }
    }

    public void toggleAfk(Player player) {
        setAfk(player, !isAfk(player));
    }

    /**
     * Returns top players by total AFK time (from cached profiles). Sorted descending.
     * Each entry: (uuid, totalSeconds). Only includes players with profile in cache.
     */
    public List<AfkTopEntry> getTopAfkPlayers(int limit) {
        Set<UUID> uuids = profileManager.getCachedUuids();
        List<AfkTopEntry> list = new ArrayList<>();
        for (UUID uuid : uuids) {
            PlayerProfile profile = profileManager.getCachedProfile(uuid).orElse(null);
            if (profile == null) continue;
            Object totalObj = profile.getMetadata(METADATA_AFK_TOTAL_SECONDS).orElse(null);
            long totalSec;
            if (totalObj instanceof Number n) {
                totalSec = n.longValue();
            } else if (totalObj instanceof String s) {
                try {
                    totalSec = Long.parseLong(s.trim());
                } catch (NumberFormatException e) {
                    totalSec = 0L;
                }
            } else {
                totalSec = 0L;
            }
            if (totalSec <= 0) continue;
            list.add(new AfkTopEntry(uuid, totalSec));
        }
        list.sort(Comparator.comparingLong(AfkTopEntry::totalSeconds).reversed());
        return list.stream().limit(Math.max(1, limit)).toList();
    }

    public record AfkTopEntry(UUID uuid, long totalSeconds) {
        public String getPlayerName() {
            OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
            return off.getName() != null ? off.getName() : uuid.toString();
        }
    }
}
