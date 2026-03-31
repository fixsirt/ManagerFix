package ru.managerfix.modules.other;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishService {

    private static final String META_KEY = "managerfix_vanish";
    private static final String PROFILE_KEY = "other.vanish";

    private final JavaPlugin plugin;
    private final ProfileManager profileManager;
    private final OtherConfig config;
    private final Map<UUID, Boolean> vanished = new ConcurrentHashMap<>();

    public VanishService(JavaPlugin plugin, ProfileManager profileManager, OtherConfig config) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.config = config;
    }

    public boolean isVanished(Player player) {
        if (player == null) return false;
        return Boolean.TRUE.equals(vanished.get(player.getUniqueId()));
    }

    public void setVanished(Player player, boolean state) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        vanished.put(uuid, state);
        player.setMetadata(META_KEY, new FixedMetadataValue(plugin, state));
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (state) {
                other.hidePlayer(plugin, player);
            } else {
                other.showPlayer(plugin, player);
            }
        }
        if (config.isVanishHideFromTab()) {
            setListedSafely(player, !state);
        } else {
            setListedSafely(player, true);
        }
        saveState(player, state);
    }

    public void applyForOnline(Player player) {
        if (player == null) return;
        boolean persisted = loadState(player);
        if (persisted) {
            setVanished(player, true);
        } else {
            vanished.remove(player.getUniqueId());
            setListedSafely(player, true);
        }
    }

    private void setListedSafely(Player player, boolean listed) {
        try {
            var method = player.getClass().getMethod("setListed", boolean.class);
            method.invoke(player, listed);
        } catch (Exception ignored) {
        }
    }

    public void clear(UUID uuid) {
        if (uuid == null) return;
        vanished.remove(uuid);
    }

    private void saveState(Player player, boolean state) {
        if (profileManager == null || player == null) return;
        PlayerProfile profile = profileManager.getProfile(player);
        profile.setMetadata(PROFILE_KEY, state);
        profileManager.saveProfileAsync(player.getUniqueId());
    }

    private boolean loadState(Player player) {
        if (profileManager == null || player == null) return false;
        if (!config.isVanishPersist()) return false;
        PlayerProfile profile = profileManager.getProfile(player);
        Object v = profile.getMetadata(PROFILE_KEY).orElse(false);
        return v instanceof Boolean b && b;
    }
}
