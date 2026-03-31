package ru.managerfix.modules.homes;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.AdminHomesCommand;
import ru.managerfix.commands.HomesCommand;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ExternalApiService;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;

/**
 * Homes module: player home points. Stored in ProfileStorage.
 * Commands: /sethome [name], /home [name], /delhome [name], /homes (GUI).
 * Limit by managerfix.homes.limit.X, cooldown on teleport.
 */
public final class HomesModule extends AbstractModule {

    private static final String MODULE_NAME = "homes";
    private static final String CONFIG_FILE = "homes/config.yml";

    private FileConfiguration moduleConfig;

    public HomesModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        super(plugin, configManager, serviceRegistry);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public List<String> getDependencies() {
        return List.of();
    }

    @Override
    protected void enable() {
        moduleConfig = configManager.getModuleConfig(CONFIG_FILE);
        
        // Инициализация команд
        initCommandConfig(MODULE_NAME);
        
        if (plugin instanceof ManagerFix mf) {
            HomesCommand homesCommand = new HomesCommand(mf, mf.getGuiManager());
            mf.getCommandManager().register("sethome", homesCommand, homesCommand);
            mf.getCommandManager().register("home", homesCommand, homesCommand);
            mf.getCommandManager().register("delhome", homesCommand, homesCommand);
            mf.getCommandManager().register("homes", homesCommand, homesCommand);
            AdminHomesCommand adminHomesCommand = new AdminHomesCommand(mf, mf.getGuiManager());
            mf.getCommandManager().register("adminhomes", adminHomesCommand, adminHomesCommand);
            mf.getCommandManager().register("adminsethome", adminHomesCommand, adminHomesCommand);
        }
        LoggerUtil.debug("Homes module enabled.");
    }

    @Override
    protected void disable() {
        moduleConfig = null;
        LoggerUtil.debug("Homes module disabled.");
    }

    public int getDefaultMaxHomes() {
        return moduleConfig != null ? moduleConfig.getInt("max-homes", 5) : 5;
    }

    public int getCooldownSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("cooldown", 0) : 0;
    }

    public int getTeleportDelaySeconds() {
        return moduleConfig != null ? moduleConfig.getInt("teleport-delay", 0) : 0;
    }

    public int getMaxHomesFor(Player player) {
        if (player == null) return getDefaultMaxHomes();
        int fromConfig = getGroupLimitByPrimaryGroup(player);
        return fromConfig > 0 ? fromConfig : getDefaultMaxHomes();
    }

    private int getGroupLimitByPrimaryGroup(Player player) {
        try {
            if (moduleConfig == null) return 0;
            // Используем кэшированный ExternalApiService вместо reflection
            ExternalApiService apiService = serviceRegistry.get(ExternalApiService.class).orElse(null);
            String group = apiService != null ? apiService.getPrimaryGroup(player.getUniqueId()) : getLuckPermsPrimaryGroupLegacy(player.getUniqueId());
            if (group == null || group.isEmpty()) return moduleConfig.getInt("group-limits.default", 0);
            // case-insensitive lookup
            if (moduleConfig.isConfigurationSection("group-limits")) {
                var sec = moduleConfig.getConfigurationSection("group-limits");
                if (sec.getKeys(false).contains(group)) {
                    return sec.getInt(group, 0);
                }
                for (String key : sec.getKeys(false)) {
                    if (key.equalsIgnoreCase(group)) {
                        return sec.getInt(key, 0);
                    }
                }
            }
            return moduleConfig.getInt("group-limits.default", 0);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Deprecated
    private String getLuckPermsPrimaryGroupLegacy(java.util.UUID uuid) {
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
}
