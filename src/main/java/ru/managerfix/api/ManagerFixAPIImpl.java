package ru.managerfix.api;

import ru.managerfix.ManagerFix;
import ru.managerfix.api.chat.ChatManager;
import ru.managerfix.api.chat.ChatManagerImpl;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.modules.ban.BanManager;
import ru.managerfix.modules.warps.WarpStorage;

import java.util.Optional;

/**
 * Реализация ManagerFixAPI.
 */
public class ManagerFixAPIImpl implements ManagerFixAPI {

    private final ManagerFix plugin;
    private final ChatManager chatManager;

    public ManagerFixAPIImpl(ManagerFix plugin) {
        this.plugin = plugin;
        this.chatManager = new ChatManagerImpl(plugin);
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public int getApiVersion() {
        return API_VERSION;
    }

    @Override
    public boolean isCompatible(int minVersion) {
        return API_VERSION >= minVersion;
    }

    @Override
    public ChatManager getChatManager() {
        return chatManager;
    }

    @Override
    public ProfileManager getProfileManager() {
        return plugin.getProfileManager();
    }

    @Override
    public BanManager getBanManager() {
        return plugin.getModuleManager()
            .getEnabledModule("ban")
            .map(m -> (ru.managerfix.modules.ban.BanModule) m)
            .map(ru.managerfix.modules.ban.BanModule::getBanManager)
            .orElse(null);
    }

    @Override
    public WarpStorage getWarpStorage() {
        return plugin.getServiceRegistry()
            .get(WarpStorage.class)
            .orElse(null);
    }

    @Override
    public boolean isModuleEnabled(String moduleName) {
        return plugin.getModuleManager().getEnabledModule(moduleName).isPresent();
    }

    @Override
    public void setModuleEnabled(String moduleName, boolean enabled) {
        plugin.getConfigManager().setModuleEnabled(moduleName, enabled);
    }

    @Override
    public void registerListener(Object listener) {
        if (listener instanceof org.bukkit.event.Listener) {
            plugin.getServer().getPluginManager().registerEvents(
                (org.bukkit.event.Listener) listener, plugin);
        }
        // Также регистрируем во внутреннем EventBus
        plugin.getEventBus().registerListener(listener);
    }

    @Override
    public void unregisterListener(Object listener) {
        if (listener instanceof org.bukkit.event.Listener) {
            org.bukkit.event.HandlerList.unregisterAll((org.bukkit.event.Listener) listener);
        }
        // Также отписываем от внутреннего EventBus
        plugin.getEventBus().unregisterListener(listener);
    }
}
