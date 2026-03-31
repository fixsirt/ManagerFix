package ru.managerfix.modules.worlds;

import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;

/**
 * Worlds module: /world, /world tp <name>, /world create <name> [generator], /world delete <name>, GUI.
 */
public final class WorldsModule extends AbstractModule {

    private static final String MODULE_NAME = "worlds";

    public WorldsModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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
        if (plugin instanceof ManagerFix mf) {
            WorldCommand worldCommand = new WorldCommand(mf, mf.getGuiManager());
            mf.getCommandManager().register("world", worldCommand, worldCommand);
        }
        LoggerUtil.debug("Worlds module enabled.");
    }

    @Override
    protected void disable() {
        LoggerUtil.debug("Worlds module disabled.");
    }
}
