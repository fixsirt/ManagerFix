package ru.managerfix.modules.tpa;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * TPA module configuration. All values are null-safe with defaults.
 */
public final class TpaConfig {

    private final FileConfiguration config;

    public TpaConfig(FileConfiguration config) {
        this.config = config;
    }

    public int getCooldownSeconds() {
        return config != null ? config.getInt("cooldown-seconds", 30) : 30;
    }

    public int getRequestTimeoutSeconds() {
        return config != null ? config.getInt("request-timeout-seconds", 60) : 60;
    }

    public int getTeleportDelaySeconds() {
        return config != null ? config.getInt("teleport-delay-seconds", 5) : 5;
    }

    public boolean isAllowSound() {
        return config == null || config.getBoolean("allow-sound", true);
    }

    /** Resolves sound from config name. Uses valueOf (deprecated in 1.21) with suppression until Registry API is used. */
    @SuppressWarnings("deprecation")
    @Nullable
    public Sound getSoundType() {
        if (config == null) return null;
        String name = config.getString("sound.name", "ENTITY_ENDERMAN_TELEPORT");
        if (name == null || name.isEmpty()) return Sound.ENTITY_ENDERMAN_TELEPORT;
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_ENDERMAN_TELEPORT;
        }
    }

    public float getSoundVolume() {
        return config != null ? (float) config.getDouble("sound.volume", 1.0) : 1.0f;
    }

    public float getSoundPitch() {
        return config != null ? (float) config.getDouble("sound.pitch", 1.0) : 1.0f;
    }

    public String getMessage(String key, String defaultValue) {
        if (config == null) return defaultValue;
        String path = "messages." + key;
        return config.getString(path, defaultValue);
    }
}
