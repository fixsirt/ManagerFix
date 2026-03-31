package ru.managerfix.event;

import ru.managerfix.core.Module;

/**
 * Fired when a module is disabled (after onDisable).
 */
public class ModuleDisableEvent extends ManagerFixEvent {

    private final String moduleName;
    private final Module module;

    public ModuleDisableEvent(String moduleName, Module module) {
        this.moduleName = moduleName;
        this.module = module;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Module getModule() {
        return module;
    }
}
