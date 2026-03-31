package ru.managerfix.core;

import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.event.EventBus;
import ru.managerfix.event.ModuleDisableEvent;
import ru.managerfix.event.ModuleEnableEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads, enables and disables modules based on config.yml.
 * Checks dependencies before enable; fires events; supports hot reload with onReload().
 */
public final class ModuleManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final EventBus eventBus;
    private final Map<String, Module> loadedModules = new LinkedHashMap<>();
    private final Map<String, Module> enabledModules = new LinkedHashMap<>();
    private final boolean debug;

    public ModuleManager(JavaPlugin plugin, ConfigManager configManager, EventBus eventBus) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.eventBus = eventBus;
        this.debug = plugin instanceof ru.managerfix.ManagerFix mf && mf.getDebugManager() != null && mf.getDebugManager().isDebug();
    }

    /**
     * Registers a module and calls onLoad().
     */
    public void registerModule(Module module) {
        String name = module.getName().toLowerCase();
        if (loadedModules.containsKey(name)) {
            LoggerUtil.warning("Module already registered: " + name);
            return;
        }
        loadedModules.put(name, module);
        LoggerUtil.debug("[ModuleManager] Loaded module: " + name);
        try {
            module.onLoad();
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Failed to load module: " + name, e);
        }
    }

    /**
     * Enables only modules that are enabled in config and have dependencies satisfied.
     */
    public void loadAndEnableModules() {
        for (Module module : loadedModules.values()) {
            String name = module.getName().toLowerCase();
            boolean enabledInConfig = configManager.isModuleEnabled(name);
            if (enabledInConfig) {
                LoggerUtil.debug("[ModuleManager] Enabling module: " + name);
                enableModule(name);
            } else {
                if (module.isEnabled()) {
                    disableModule(name);
                }
            }
        }
    }

    /**
     * Returns true if all dependencies of the module are currently enabled.
     */
    private boolean areDependenciesSatisfied(Module module) {
        for (String dep : module.getDependencies()) {
            String depLower = dep.toLowerCase();
            if (!enabledModules.containsKey(depLower)) {
                if (debug) {
                    LoggerUtil.info("[DEBUG] Dependency not satisfied: " + module.getName() + " requires " + dep);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Enables a single module by name. Checks dependencies first; fires ModuleEnableEvent after.
     */
    public void enableModule(String name) {
        name = name.toLowerCase();
        Module module = loadedModules.get(name);
        if (module == null) {
            plugin.getLogger().warning("[ModuleManager] Unknown module: " + name);
            return;
        }
        if (module.isEnabled()) {
            return;
        }
        if (!areDependenciesSatisfied(module)) {
            plugin.getLogger().warning("[ModuleManager] Module " + name + " not enabled: missing dependencies " + module.getDependencies());
            return;
        }
        long start = System.nanoTime();
        try {
            module.onEnable();
            enabledModules.put(name, module);
            if (eventBus != null) {
                eventBus.callEvent(new ModuleEnableEvent(name, module));
            }
            LoggerUtil.debug("[ModuleManager] Module enabled: " + name);
            long elapsed = System.nanoTime() - start;
            if (debug) {
                LoggerUtil.debug("[DEBUG] Module " + name + " enabled in " + elapsed / 1_000_000 + " ms");
            }
            if (plugin instanceof ru.managerfix.ManagerFix mf && mf.getDebugManager() != null) {
                mf.getDebugManager().logModuleLoad(name, elapsed);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[ModuleManager] Failed to enable module: " + name);
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Failed to enable module: " + name, e);
        }
    }

    /**
     * Disables a single module by name. Fires ModuleDisableEvent after.
     */
    public void disableModule(String name) {
        name = name.toLowerCase();
        Module module = enabledModules.get(name);
        if (module == null) {
            return;
        }
        // Сначала удаляем из enabledModules, затем вызываем onDisable
        enabledModules.remove(name);
        try {
            module.onDisable();
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Failed to disable module: " + name, e);
        } finally {
            if (eventBus != null) {
                eventBus.callEvent(new ModuleDisableEvent(name, module));
            }
        }
        LoggerUtil.debug("Module disabled: " + name);
    }

    /**
     * Disables all enabled modules (e.g. on plugin disable or full reload).
     */
    public void disableAll() {
        for (String name : enabledModules.keySet().toArray(new String[0])) {
            disableModule(name);
        }
    }

    /**
     * Full reload: disable all, reload config, then enable only modules that are enabled in config.
     */
    public void reload() {
        disableAll();
        configManager.reload();
        loadAndEnableModules();
    }

    /**
     * Hot reload a single module: disable, fire ModuleDisableEvent, onReload(), then enable if config says so, fire ModuleEnableEvent.
     */
    public void reloadModule(String name) {
        name = name.toLowerCase();
        Module module = loadedModules.get(name);
        if (module == null) {
            LoggerUtil.warning("Unknown module: " + name);
            return;
        }
        if (enabledModules.containsKey(name)) {
            // Сначала удаляем из enabledModules, затем вызываем onDisable
            enabledModules.remove(name);
            try {
                module.onDisable();
            } finally {
                if (eventBus != null) {
                    eventBus.callEvent(new ModuleDisableEvent(name, module));
                }
            }
        }
        try {
            module.onReload();
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Module onReload error: " + name, e);
        }
        if (configManager.isModuleEnabled(name)) {
            enableModule(name);
        }
    }

    public Optional<Module> getModule(String name) {
        return Optional.ofNullable(loadedModules.get(name.toLowerCase()));
    }

    public Optional<Module> getEnabledModule(String name) {
        return Optional.ofNullable(enabledModules.get(name.toLowerCase()));
    }

    public Collection<Module> getLoadedModules() {
        return Collections.unmodifiableCollection(loadedModules.values());
    }

    public Collection<Module> getEnabledModules() {
        return Collections.unmodifiableCollection(enabledModules.values());
    }

    /**
     * Returns stats string for admin panel (e.g. "Warps: 12"). "-" if not provided.
     */
    public String getModuleStats(String name) {
        Module module = loadedModules.get(name.toLowerCase());
        if (module == null) return "-";
        return module.getAdminStats().orElse("-");
    }

    /**
     * Returns reason why module is disabled (e.g. "Missing: vault"). Empty if enabled or unknown.
     */
    public Optional<String> getDependencyFailureReason(String name) {
        name = name.toLowerCase();
        Module module = loadedModules.get(name);
        if (module == null) return Optional.of("Unknown module");
        if (module.isEnabled()) return Optional.empty();
        if (!configManager.isModuleEnabled(name)) return Optional.of("Disabled in config");
        for (String dep : module.getDependencies()) {
            String depLower = dep.toLowerCase();
            if (!enabledModules.containsKey(depLower)) {
                return Optional.of("Missing: " + dep);
            }
        }
        return Optional.of("Unknown");
    }
}
