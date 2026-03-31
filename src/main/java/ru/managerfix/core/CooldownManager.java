package ru.managerfix.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер кулдаунов для команд.
 */
public final class CooldownManager {

    // playerId -> commandName -> lastUsageTime (ms)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Проверяет, готов ли игрок использовать команду.
     * @param playerId UUID игрока
     * @param commandName имя команды
     * @param cooldownSec кулдаун в секундах
     * @return true если можно использовать, false если на кулдауне
     */
    public boolean isReady(UUID playerId, String commandName, int cooldownSec) {
        if (cooldownSec <= 0) return true;

        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return true;

        Long lastUsage = playerCooldowns.get(commandName.toLowerCase());
        if (lastUsage == null) return true;

        long now = System.currentTimeMillis();
        long cooldownMs = cooldownSec * 1000L;
        return (now - lastUsage) >= cooldownMs;
    }

    /**
     * Устанавливает кулдаун для игрока.
     */
    public void setCooldown(UUID playerId, String commandName) {
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(commandName.toLowerCase(), System.currentTimeMillis());
    }

    /**
     * Получает оставшееся время кулдауна в секундах.
     */
    public int getRemainingCooldown(UUID playerId, String commandName, int cooldownSec) {
        if (cooldownSec <= 0) return 0;

        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;

        Long lastUsage = playerCooldowns.get(commandName.toLowerCase());
        if (lastUsage == null) return 0;

        long now = System.currentTimeMillis();
        long cooldownMs = cooldownSec * 1000L;
        long elapsed = now - lastUsage;

        if (elapsed >= cooldownMs) return 0;

        return (int) ((cooldownMs - elapsed) / 1000L) + 1;
    }

    /**
     * Удаляет кулдаун для игрока.
     */
    public void removeCooldown(UUID playerId, String commandName) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns != null) {
            playerCooldowns.remove(commandName.toLowerCase());
        }
    }

    /**
     * Удаляет все кулдауны для игрока.
     */
    public void removePlayerCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * Очищает все кулдауны.
     */
    public void clearAll() {
        cooldowns.clear();
    }
}
