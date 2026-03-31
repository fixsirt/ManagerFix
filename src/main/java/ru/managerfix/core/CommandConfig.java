package ru.managerfix.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Конфигурация команд для модуля (commands.yml).
 * Содержит: алиасы, кулдауны, разрешения для обхода КД.
 */
public final class CommandConfig {

    private final JavaPlugin plugin;
    private final String moduleName;
    private final File configFile;
    private FileConfiguration config;

    // Кэш команд: commandName -> CommandData
    private final Map<String, CommandData> commandsCache = new HashMap<>();

    public CommandConfig(JavaPlugin plugin, String moduleName) {
        this.plugin = plugin;
        this.moduleName = moduleName;
        this.configFile = new File(plugin.getDataFolder(), "modules/" + moduleName + "/commands.yml");
        load();
    }

    /**
     * Загружает конфиг команд.
     */
    public void load() {
        if (!configFile.exists()) {
            plugin.saveResource("modules/" + moduleName + "/commands.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadCommandsCache();
    }

    /**
     * Загружает команды в кэш.
     */
    private void loadCommandsCache() {
        commandsCache.clear();
        ConfigurationSection section = config.getConfigurationSection("commands");
        if (section == null) return;

        for (String commandName : section.getKeys(false)) {
            String path = "commands." + commandName;
            List<String> aliases = config.getStringList(path + ".aliases");
            int cooldown = config.getInt(path + ".cooldown", 0);
            String bypassPermission = config.getString(path + ".bypass-permission", "");
            boolean enabled = config.getBoolean(path + ".enabled", true);

            commandsCache.put(commandName.toLowerCase(), new CommandData(
                    commandName,
                    aliases,
                    cooldown,
                    bypassPermission,
                    enabled
            ));
        }
    }

    /**
     * Сохраняет конфиг.
     */
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save commands.yml for module: " + moduleName, e);
        }
    }

    /**
     * Получает данные команды по имени.
     */
    public Optional<CommandData> getCommand(String commandName) {
        return Optional.ofNullable(commandsCache.get(commandName.toLowerCase()));
    }

    /**
     * Проверяет, включена ли команда.
     */
    public boolean isCommandEnabled(String commandName) {
        return getCommand(commandName).map(CommandData::isEnabled).orElse(true);
    }

    /**
     * Получает кулдаун команды в секундах.
     */
    public int getCooldown(String commandName) {
        return getCommand(commandName).map(CommandData::getCooldown).orElse(0);
    }

    /**
     * Получает разрешение для обхода кулдауна.
     */
    public String getBypassPermission(String commandName) {
        return getCommand(commandName).map(CommandData::getBypassPermission).orElse("");
    }

    /**
     * Проверяет, может ли игрок обойти кулдаун.
     */
    public boolean canBypassCooldown(String commandName, org.bukkit.entity.Player player) {
        if (player == null) return false;
        String perm = getBypassPermission(commandName);
        if (perm == null || perm.isEmpty()) return false;
        return player.hasPermission(perm);
    }

    /**
     * Получает все алиасы команды.
     */
    public List<String> getAliases(String commandName) {
        return getCommand(commandName).map(CommandData::getAliases).orElse(Collections.emptyList());
    }

    /**
     * Получает все команды (имена).
     */
    public Set<String> getAllCommandNames() {
        return Collections.unmodifiableSet(commandsCache.keySet());
    }

    /**
     * Данные команды.
     */
    public static class CommandData {
        private final String name;
        private final List<String> aliases;
        private final int cooldown;
        private final String bypassPermission;
        private final boolean enabled;

        public CommandData(String name, List<String> aliases, int cooldown, String bypassPermission, boolean enabled) {
            this.name = name;
            this.aliases = aliases != null ? aliases : Collections.emptyList();
            this.cooldown = cooldown;
            this.bypassPermission = bypassPermission != null ? bypassPermission : "";
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public int getCooldown() {
            return cooldown;
        }

        public String getBypassPermission() {
            return bypassPermission;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}
