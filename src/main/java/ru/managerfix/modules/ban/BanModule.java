package ru.managerfix.modules.ban;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.event.EventBus;
import ru.managerfix.service.ServiceRegistry;
import ru.managerfix.scheduler.TaskScheduler;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.Optional;

/**
 * Ban module: /ban, /unban, /mute, /unmute, /banlist (GUI). YAML storage, AsyncPlayerPreLoginEvent, EventBus.
 */
public final class BanModule extends AbstractModule {

    private static final String MODULE_NAME = "ban";
    private static final String CONFIG_FILE = "ban/config.yml";

    private FileConfiguration moduleConfig;
    private BanStorage storage;
    private BanManager banManager;
    private MuteManager muteManager;
    private BanHistoryStorage historyStorage;
    private BanLoginListener loginListener;
    private KickBroadcastListener kickListener;
    private boolean broadcastBans;
    private boolean broadcastMutes;
    private String fmtBan;
    private String fmtKick;
    private String fmtMute;

    public BanModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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

        TaskScheduler scheduler = plugin instanceof ManagerFix mf ? mf.getScheduler() : null;
        EventBus eventBus = plugin instanceof ManagerFix mf ? mf.getEventBus() : null;

        // Используем SQL или оптимизированное YAML хранилище с кэшированием
        if (plugin instanceof ManagerFix mf && (mf.isMySqlStorage() || mf.isSqliteStorage())) {
            storage = mf.getSqlBanStorage();
        } else {
            storage = new CachedYamlBanStorage(plugin, scheduler != null ? scheduler : new TaskScheduler(plugin));
        }
        storage.init();

        // История банов
        if (plugin instanceof ManagerFix mf && (mf.isMySqlStorage() || mf.isSqliteStorage())) {
            historyStorage = new SqlBanHistoryStorageWrapper(mf.getSqlBanHistoryStorage());
        } else {
            historyStorage = new YamlBanHistoryStorage(plugin, scheduler != null ? scheduler : new TaskScheduler(plugin));
        }
        historyStorage.init();
        banManager = new BanManager(storage, eventBus, scheduler != null ? scheduler : new TaskScheduler(plugin), historyStorage);
        broadcastBans = moduleConfig.getBoolean("broadcast-bans", true);
        broadcastMutes = moduleConfig.getBoolean("broadcast-mutes", true);
        fmtBan = moduleConfig.getString("format.ban-broadcast",
                "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n&c⛔ БАН\n&7Игрок: &f{targetReal}\n&7Забанил: &f{sourceReal}\n&7На: &e{duration}\n&7Причина: &e{reason}\n&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        fmtKick = moduleConfig.getString("format.kick-broadcast",
                "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n&e⬤ КИК\n&7Игрок: &f{targetReal}\n&7Кикнул: &f{sourceReal}\n&7Причина: &c{reason}\n&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        fmtMute = moduleConfig.getString("format.mute-broadcast",
                "&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n&c🔇 МУТ\n&7Игрок: &f{targetReal}\n&7Замутил: &f{sourceReal}\n&7На: &e{duration}\n&7Причина: &e{reason}\n&6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        if (plugin instanceof ManagerFix mf) {
            BanCommand banCommand = new BanCommand(mf, mf.getGuiManager());
            mf.getCommandManager().register("ban", banCommand, banCommand);
            mf.getCommandManager().register("unban", banCommand, banCommand);
            mf.getCommandManager().register("banlist", banCommand, banCommand);

            // BanIP команда
            BanIpCommand banIpCommand = new BanIpCommand(mf, this);
            mf.getCommandManager().register("banip", banIpCommand, banIpCommand);
            
            // UnbanIP команда
            UnbanIpCommand unbanIpCommand = new UnbanIpCommand(mf);
            mf.getCommandManager().register("unbanip", unbanIpCommand, unbanIpCommand);

            // Mute команды
            MuteCommand muteCommand = new MuteCommand(this);
            mf.getCommandManager().register("mute", muteCommand, muteCommand);
            mf.getCommandManager().register("unmute", muteCommand, muteCommand);
            
            // Инициализация mute менеджера
            ru.managerfix.scheduler.TaskScheduler muteScheduler = mf.getScheduler();
            if (muteScheduler == null) {
                muteScheduler = new ru.managerfix.scheduler.TaskScheduler(plugin);
            }
            MuteStorage muteStorage;
            if (mf.isMySqlStorage() || mf.isSqliteStorage()) {
                muteStorage = mf.getSqlMuteStorage();
            } else {
                muteStorage = new CachedYamlMuteStorage(plugin, muteScheduler);
            }
            muteStorage.init();
            muteManager = new MuteManager(muteStorage, muteScheduler);

            // Логирование для отладки
            LoggerUtil.debug("[BanModule] MuteManager initialized: " + (muteManager != null));
            LoggerUtil.debug("[BanModule] MuteStorage: " + muteStorage.getClass().getSimpleName());
            
            // Kick команда
            KickCommand kickCommand = new KickCommand(this);
            mf.getCommandManager().register("kick", kickCommand, kickCommand);
            
            loginListener = new BanLoginListener(this);
            kickListener = new KickBroadcastListener(this);
            plugin.getServer().getPluginManager().registerEvents(loginListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(kickListener, plugin);
        }
        LoggerUtil.debug("Ban module enabled.");
    }

    @Override
    protected void disable() {
        if (loginListener != null) {
            HandlerList.unregisterAll(loginListener);
            loginListener = null;
        }
        if (kickListener != null) {
            HandlerList.unregisterAll(kickListener);
            kickListener = null;
        }
        if (storage != null) storage.shutdown();
        storage = null;
        banManager = null;
        moduleConfig = null;
        historyStorage = null;
        LoggerUtil.debug("Ban module disabled.");
    }

    public String getDefaultKickMessage() {
        if (moduleConfig != null && moduleConfig.contains("kick-message")) {
            return moduleConfig.getString("kick-message");
        }
        String raw = MessageUtil.getRaw(plugin,
                plugin instanceof ManagerFix m ? m.getConfigManager().getDefaultLanguage() : "ru",
                "ban.kick-message");
        return raw != null ? raw : "You are banned. Reason: {reason}";
    }

    public BanManager getBanManager() {
        return banManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public BanHistoryStorage getBanHistoryStorage() {
        return historyStorage;
    }

    public boolean isBroadcastBans() {
        return broadcastBans;
    }

    public boolean isBroadcastMutes() {
        return broadcastMutes;
    }

    public String getFormatBanBroadcast() {
        return fmtBan;
    }

    public String getFormatKickBroadcast() {
        return fmtKick;
    }

    public String getFormatMuteBroadcast() {
        return fmtMute;
    }

    @Override
    public Optional<String> getAdminStats() {
        if (banManager == null) return Optional.empty();
        return Optional.of("Bans: " + banManager.getCachedBanCount());
    }
}
