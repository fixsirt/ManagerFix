package ru.managerfix.core;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.utils.MessageUtil;

import java.util.function.BiFunction;

/**
 * Базовый класс для обработки команд с кулдаунами.
 * @param <T> Тип модуля
 */
public abstract class CommandHandler<T> {

    protected final JavaPlugin plugin;
    protected final T module;
    protected final CommandConfig commandConfig;
    protected final CooldownManager cooldownManager;

    public CommandHandler(JavaPlugin plugin, T module, String moduleName) {
        this.plugin = plugin;
        this.module = module;
        this.commandConfig = new CommandConfig(plugin, moduleName);
        this.cooldownManager = new CooldownManager();
    }

    /**
     * Проверяет кулдаун для команды.
     * @param player игрок
     * @param commandName имя команды
     * @return true если кулдаун активен (нельзя выполнять), false если можно выполнять
     */
    protected boolean isOnCooldown(Player player, String commandName) {
        int cooldown = commandConfig.getCooldown(commandName);
        if (cooldown <= 0) return false;

        // Проверяем bypass permission
        if (commandConfig.canBypassCooldown(commandName, player)) {
            return false;
        }

        // Проверяем кулдаун
        if (!cooldownManager.isReady(player.getUniqueId(), commandName, cooldown)) {
            int remaining = cooldownManager.getRemainingCooldown(player.getUniqueId(), commandName, cooldown);
            player.sendMessage(MessageUtil.parse("<#FF3366>Подождите ещё <#F0F4F8>" + remaining + "</#F0F4F8> сек. перед использованием этой команды!"));
            return true;
        }

        // Устанавливаем кулдаун
        cooldownManager.setCooldown(player.getUniqueId(), commandName);
        return false;
    }

    /**
     * Выполняет команду с проверкой кулдауна.
     * @param player игрок
     * @param commandName имя команды
     * @param action действие команды, возвращает true если успешно
     * @return true если команда выполнена
     */
    protected boolean executeWithCooldown(Player player, String commandName, BiFunction<Player, String, Boolean> action) {
        if (isOnCooldown(player, commandName)) {
            return true; // Команда "обработана" (показано сообщение о кулдауне)
        }
        return action.apply(player, commandName);
    }

    /**
     * Получает CommandConfig.
     */
    public CommandConfig getCommandConfig() {
        return commandConfig;
    }

    /**
     * Получает CooldownManager.
     */
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
