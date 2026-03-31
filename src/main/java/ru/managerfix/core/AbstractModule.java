package ru.managerfix.core;

import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.service.ServiceRegistry;

import java.util.Objects;

/**
 * Abstract base for modules. Holds plugin reference, config, service registry and enabled state.
 * Lifecycle: onLoad → onEnable → (onReload) → onDisable.
 */
public abstract class AbstractModule implements Module {

    protected final JavaPlugin plugin;
    protected final ConfigManager configManager;
    protected final ServiceRegistry serviceRegistry;
    private boolean enabled;

    // Command configuration and cooldown management
    protected CommandConfig commandConfig;
    protected CooldownManager cooldownManager;

    protected AbstractModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.serviceRegistry = Objects.requireNonNull(serviceRegistry, "serviceRegistry cannot be null");
        this.enabled = false;
    }
    
    /**
     * Инициализирует конфиг команд и менеджер кулдаунов.
     * Вызывается в enable() после super.enable().
     */
    protected void initCommandConfig(String moduleName) {
        this.commandConfig = new CommandConfig(plugin, moduleName);
        this.cooldownManager = new CooldownManager();
    }

    @Override
    public final void onLoad() {
        load();
    }

    @Override
    public final void onEnable() {
        enabled = true;
        enable();
    }

    @Override
    public final void onDisable() {
        enabled = false;
        disable();
    }

    @Override
    public final void onReload() {
        reload();
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Module-specific load logic. Called once after registration.
     */
    protected void load() {
    }

    /**
     * Module-specific enable logic. Override in subclasses.
     */
    protected abstract void enable();

    /**
     * Module-specific disable logic. Override in subclasses.
     */
    protected abstract void disable();

    /**
     * Module-specific reload logic. Called on hot reload (after disable, before enable).
     */
    protected void reload() {
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    public CommandConfig getCommandConfig() {
        return commandConfig;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}
