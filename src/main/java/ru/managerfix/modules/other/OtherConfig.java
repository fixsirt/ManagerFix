package ru.managerfix.modules.other;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public final class OtherConfig {

    private final FileConfiguration config;

    public OtherConfig(FileConfiguration config) {
        this.config = config;
    }

    public int getNearRadius() {
        return config != null ? config.getInt("near-radius", 100) : 100;
    }

    public boolean isLogAdminActions() {
        return config != null && config.getBoolean("log-admin-actions", true);
    }

    public boolean isVanishHideFromTab() {
        return config != null && config.getBoolean("vanish-hide-from-tab", true);
    }

    public boolean isVanishPersist() {
        return config != null && config.getBoolean("vanish-persist", true);
    }

    public boolean isVanishHideJoinQuit() {
        return config != null && config.getBoolean("vanish-hide-join-quit", true);
    }

    public boolean isFoodGodPersist() {
        return config != null && config.getBoolean("food-god-persist", true);
    }

    public Map<String, String> getAliasToCommandMap() {
        Map<String, String> map = new HashMap<>();
        if (config == null) return map;
        ConfigurationSection sec = config.getConfigurationSection("aliases");
        if (sec == null) return map;
        for (String cmd : sec.getKeys(false)) {
            List<String> aliases = sec.getStringList(cmd);
            if (aliases == null) continue;
            for (String alias : aliases) {
                if (alias != null && !alias.isBlank()) {
                    map.put(alias.toLowerCase(), cmd.toLowerCase());
                }
            }
        }
        return map;
    }

    public int getCooldownSeconds(String key) {
        if (config == null || key == null) return 0;
        return config.getInt("cooldowns." + key.toLowerCase(), 0);
    }

    public String getBroadcastTitle() {
        return config != null ? config.getString("broadcast.title", "<gradient:#00eaff:#0066ff>Объявление</gradient>") : "<gradient:#00eaff:#0066ff>Объявление</gradient>";
    }

    public String getBroadcastSubtitle() {
        return config != null ? config.getString("broadcast.subtitle", "<gray>{message}") : "<gray>{message}";
    }

    public Sound getBroadcastSound() {
        if (config == null) return Sound.UI_TOAST_CHALLENGE_COMPLETE;
        String name = config.getString("broadcast.sound", "UI_TOAST_CHALLENGE_COMPLETE");
        Sound sound = resolveSound(name);
        return sound != null ? sound : Sound.UI_TOAST_CHALLENGE_COMPLETE;
    }

    public float getBroadcastSoundVolume() {
        return config != null ? (float) config.getDouble("broadcast.sound-volume", 1.0) : 1.0f;
    }

    public float getBroadcastSoundPitch() {
        return config != null ? (float) config.getDouble("broadcast.sound-pitch", 1.0) : 1.0f;
    }

    private Sound resolveSound(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        NamespacedKey key = trimmed.contains(":")
                ? NamespacedKey.fromString(trimmed)
                : NamespacedKey.minecraft(trimmed.toLowerCase());
        return key != null ? Registry.SOUNDS.get(key) : null;
    }
}
