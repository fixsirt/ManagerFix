package ru.managerfix.core;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Base interface for all ManagerFix modules.
 * Each module can be enabled or disabled via config.yml.
 * Optional: getAdminStats() for admin panel, clearData() for admin actions.
 */
public interface Module {

    /**
     * Called when the module is loaded (after registration, before first enable).
     */
    default void onLoad() {
    }

    /**
     * Called when the module is enabled.
     */
    void onEnable();

    /**
     * Called when the module is disabled.
     */
    void onDisable();

    /**
     * Called when the module is reloaded (after disable, before enable).
     */
    default void onReload() {
    }

    /**
     * Returns the unique module name (used as key in config).
     */
    String getName();

    /**
     * Returns whether the module is currently enabled.
     */
    boolean isEnabled();

    /**
     * Returns list of module names this module depends on. If any dependency is disabled,
     * this module will not be enabled.
     */
    default List<String> getDependencies() {
        return Collections.emptyList();
    }

    /**
     * Optional: short stats line for admin panel (e.g. "Warps: 12", "Bans: 5"). Empty = not provided.
     */
    default Optional<String> getAdminStats() {
        return Optional.empty();
    }

    /**
     * Optional: clear/reset module data (admin action). Default no-op.
     */
    default void clearData() {
    }
}
