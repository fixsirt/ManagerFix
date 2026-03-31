package ru.managerfix.modules.tab;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Sorts players by LuckPerms weight (desc), then by primary group name, then by player name.
 * Uses LuckPerms API when present; otherwise returns original order.
 */
public final class LuckPermsSortService {

    private final Object luckPermsApi;
    private final boolean available;

    public LuckPermsSortService() {
        this.luckPermsApi = resolveLuckPermsApi();
        this.available = luckPermsApi != null;
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Returns players sorted by: LuckPerms weight (higher first), then group name, then name.
     */
    public List<Player> getSortedPlayers(List<Player> players) {
        if (!available || players == null || players.size() <= 1) {
            return players != null ? new ArrayList<>(players) : List.of();
        }
        List<Player> copy = new ArrayList<>(players);
        copy.sort(this::compare);
        return copy;
    }

    private int compare(Player a, Player b) {
        int weightA = getWeight(a.getUniqueId());
        int weightB = getWeight(b.getUniqueId());
        if (weightA != weightB) return Integer.compare(weightB, weightA); // higher weight first
        String groupA = getPrimaryGroup(a.getUniqueId());
        String groupB = getPrimaryGroup(b.getUniqueId());
        int groupCmp = (groupA != null && groupB != null) ? groupA.compareToIgnoreCase(groupB) : 0;
        if (groupCmp != 0) return groupCmp;
        return a.getName().compareToIgnoreCase(b.getName());
    }

    private Object resolveLuckPermsApi() {
        try {
            Class<?> apiClass = Class.forName("net.luckperms.api.LuckPerms");
            return Bukkit.getServicesManager().getRegistration(apiClass) != null
                    ? Bukkit.getServicesManager().getRegistration(apiClass).getProvider()
                    : null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private int getWeight(UUID playerId) {
        if (luckPermsApi == null) return 0;
        try {
            Object userManager = luckPermsApi.getClass().getMethod("getUserManager").invoke(luckPermsApi);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, playerId);
            if (user == null) return 0;
            String groupName = (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
            if (groupName == null || groupName.isEmpty()) return 0;
            Object groupManager = luckPermsApi.getClass().getMethod("getGroupManager").invoke(luckPermsApi);
            Object group = groupManager.getClass().getMethod("getGroup", String.class).invoke(groupManager, groupName);
            if (group == null) return 0;
            // getWeight() returns OptionalInt in LuckPerms 5.x
            Object opt = group.getClass().getMethod("getWeight").invoke(group);
            if (opt instanceof OptionalInt o) return o.orElse(0);
            if (opt != null && opt.getClass().getName().startsWith("java.util.OptionalInt")) {
                return (Integer) opt.getClass().getMethod("orElse", int.class).invoke(opt, 0);
            }
            return 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private String getPrimaryGroup(UUID playerId) {
        if (luckPermsApi == null) return null;
        try {
            Object userManager = luckPermsApi.getClass().getMethod("getUserManager").invoke(luckPermsApi);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, playerId);
            if (user == null) return null;
            return (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
