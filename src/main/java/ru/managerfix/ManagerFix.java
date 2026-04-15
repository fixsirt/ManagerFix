package ru.managerfix;

import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.commands.ManagerFixCommand;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.DebugManager;
import ru.managerfix.core.FileManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.core.MigrationManager;
import ru.managerfix.core.ModuleManager;
import ru.managerfix.database.DatabaseManager;
import ru.managerfix.event.EventBus;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.scheduler.TaskScheduler;
import ru.managerfix.service.ServiceRegistry;
import ru.managerfix.storage.ProfileStorage;
import ru.managerfix.storage.WarpStorageAdapter;
import ru.managerfix.storage.YamlProfileStorage;
import ru.managerfix.storage.YamlWarpStorageAdapter;
import ru.managerfix.storage.SqlProfileStorage;
import ru.managerfix.storage.SqlWarpStorageAdapter;
import ru.managerfix.storage.SqlKitStorage;
import ru.managerfix.storage.SqlBanStorage;
import ru.managerfix.storage.SqlMuteStorage;
import ru.managerfix.storage.SqlItemsStorage;
import ru.managerfix.storage.SqlTpaStorage;
import ru.managerfix.storage.SqlBanHistoryStorage;
import ru.managerfix.storage.SqlBanIpStorage;
import ru.managerfix.modules.warps.WarpStorage;
import ru.managerfix.modules.warps.WarpsModule;
import ru.managerfix.modules.homes.HomesModule;
import ru.managerfix.modules.spawn.SpawnModule;
import ru.managerfix.modules.chat.ChatModule;
import ru.managerfix.modules.tpa.TpaModule;
import ru.managerfix.modules.rtp.RtpModule;
import ru.managerfix.modules.ban.BanModule;
import ru.managerfix.modules.afk.AfkModule;
import ru.managerfix.modules.kits.KitsModule;
import ru.managerfix.modules.worlds.WorldsModule;
import ru.managerfix.modules.other.OtherModule;
import ru.managerfix.modules.tab.TabModule;
import ru.managerfix.modules.announcer.AnnouncerModule;
import ru.managerfix.modules.items.ItemsModule;

/**
 * ManagerFix - modular Minecraft plugin for Paper 1.21.4.
 * Supports YAML and MySQL/MariaDB storage, EventBus, ServiceRegistry, module dependencies.
 */
public final class ManagerFix extends JavaPlugin {

    private FileManager fileManager;
    private ConfigManager configManager;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private GuiManager guiManager;
    private UIThemeManager uiThemeManager;
    private GuiTemplate guiTemplate;
    private TaskScheduler scheduler;
    private ProfileManager profileManager;
    private DebugManager debugManager;
    private DatabaseManager databaseManager;
    private ProfileStorage profileStorage;
    private WarpStorageAdapter warpStorageAdapter;
    private WarpStorage warpStorage;
    private EventBus eventBus;
    private ServiceRegistry serviceRegistry;
    private String storageType;
    private MigrationManager migrationManager;
    private ru.managerfix.api.ManagerFixAPI api;
    
    // SQL storage instances
    private ru.managerfix.storage.SqlKitStorage sqlKitStorage;
    private ru.managerfix.storage.SqlBanStorage sqlBanStorage;
    private ru.managerfix.storage.SqlBanIpStorage sqlBanIpStorage;
    private ru.managerfix.storage.SqlMuteStorage sqlMuteStorage;
    private ru.managerfix.storage.SqlItemsStorage sqlItemsStorage;
    private ru.managerfix.storage.SqlTpaStorage sqlTpaStorage;
    private ru.managerfix.storage.SqlBanHistoryStorage sqlBanHistoryStorage;

    @Override
    public void onEnable() {
        try {
            LoggerUtil.init(this);

            fileManager = new FileManager(this);
            fileManager.setupFolders();

            configManager = new ConfigManager(this, fileManager);
            configManager.load();

            scheduler = new TaskScheduler(this);
            debugManager = new DebugManager(this, configManager);
            boolean debug = configManager.isDebug();
            eventBus = new EventBus(debug);
            serviceRegistry = new ServiceRegistry();

            // Регистрируем ExternalApiService для кэширования Vault/LuckPerms
            ru.managerfix.service.ExternalApiService externalApiService = new ru.managerfix.service.ExternalApiService(this);
            serviceRegistry.register(ru.managerfix.service.ExternalApiService.class, externalApiService);

            storageType = configManager.getStorageType();

            if (configManager.isMySqlStorage() || configManager.isSqliteStorage()) {
                databaseManager = new DatabaseManager(this, configManager.getMainConfig(), configManager);
                try {
                    databaseManager.init();
                } catch (RuntimeException e) {
                    if (databaseManager != null) {
                        databaseManager.shutdown();
                    }
                    throw e;
                }
                profileStorage = new SqlProfileStorage(databaseManager, scheduler);
                warpStorageAdapter = new SqlWarpStorageAdapter(databaseManager, scheduler);
                sqlKitStorage = new SqlKitStorage(databaseManager, scheduler);
                sqlBanStorage = new SqlBanStorage(databaseManager, scheduler);
                sqlBanIpStorage = new SqlBanIpStorage(databaseManager, scheduler);
                sqlMuteStorage = new SqlMuteStorage(databaseManager, scheduler);
                sqlItemsStorage = new SqlItemsStorage(databaseManager, scheduler);
                sqlTpaStorage = new SqlTpaStorage(databaseManager, scheduler);
                sqlBanHistoryStorage = new SqlBanHistoryStorage(databaseManager, scheduler);
            } else {
                profileStorage = new YamlProfileStorage(this, scheduler);
                warpStorageAdapter = new YamlWarpStorageAdapter(this, scheduler);
            }
            profileStorage.init();
            warpStorageAdapter.init();
            if (sqlKitStorage != null) sqlKitStorage.init();
            if (sqlBanStorage != null) sqlBanStorage.init();
            if (sqlBanIpStorage != null) sqlBanIpStorage.init();
            if (sqlMuteStorage != null) sqlMuteStorage.init();
            if (sqlItemsStorage != null) sqlItemsStorage.init();
            if (sqlTpaStorage != null) sqlTpaStorage.init();
            if (sqlBanHistoryStorage != null) sqlBanHistoryStorage.init();

            // Создаём MigrationManager всегда (для миграции yaml2sql)
            if (databaseManager != null && databaseManager.isInitialized()) {
                migrationManager = new MigrationManager(this, fileManager, databaseManager, storageType);
                migrationManager.migrateIfNeeded();
            } else {
                // Для YAML тоже создаём, но без БД
                migrationManager = new MigrationManager(this, fileManager, null, storageType);
            }

            profileManager = new ProfileManager(this, profileStorage, configManager.getProfileAutosaveMinutes(), scheduler, eventBus);
            profileManager.startAutosave();

            warpStorage = new WarpStorage(this, warpStorageAdapter, scheduler, eventBus);

            serviceRegistry.register(TaskScheduler.class, scheduler);
            serviceRegistry.register(ProfileManager.class, profileManager);
            serviceRegistry.register(EventBus.class, eventBus);
            serviceRegistry.register(ru.managerfix.modules.warps.WarpStorage.class, warpStorage);
            serviceRegistry.register(ConfigManager.class, configManager);
            serviceRegistry.register(FileManager.class, fileManager);
            if (databaseManager != null) {
                serviceRegistry.register(DatabaseManager.class, databaseManager);
                serviceRegistry.register(ru.managerfix.storage.SqlKitStorage.class, sqlKitStorage);
                serviceRegistry.register(ru.managerfix.storage.SqlBanStorage.class, sqlBanStorage);
                serviceRegistry.register(ru.managerfix.storage.SqlMuteStorage.class, sqlMuteStorage);
                serviceRegistry.register(ru.managerfix.storage.SqlItemsStorage.class, sqlItemsStorage);
                serviceRegistry.register(ru.managerfix.storage.SqlTpaStorage.class, sqlTpaStorage);
                serviceRegistry.register(ru.managerfix.storage.SqlBanHistoryStorage.class, sqlBanHistoryStorage);
            }

            moduleManager = new ModuleManager(this, configManager, eventBus);
            commandManager = new CommandManager(this);
            guiManager = new GuiManager(this);
            uiThemeManager = new UIThemeManager();
            guiTemplate = new GuiTemplate(uiThemeManager);
            serviceRegistry.register(GuiManager.class, guiManager);
            serviceRegistry.register(UIThemeManager.class, uiThemeManager);
            serviceRegistry.register(GuiTemplate.class, guiTemplate);
            serviceRegistry.register(CommandManager.class, commandManager);
            serviceRegistry.register(ModuleManager.class, moduleManager);

            registerModules();
            // Commands from plugin.yml are already available; modules register their executors here.
            moduleManager.loadAndEnableModules();

            registerCommands();
            registerAPI();
            registerPlaceholderAPI();

            LoggerUtil.info("ManagerFix enabled. Storage: " + storageType);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "CRITICAL ERROR DURING PLUGIN ENABLE!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (profileManager != null) {
            profileManager.stopAutosave();
        }
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (warpStorageAdapter != null) {
            warpStorageAdapter.shutdown();
        }
        if (profileStorage != null) {
            profileStorage.shutdown();
        }
        if (sqlKitStorage != null) {
            sqlKitStorage.shutdown();
        }
        if (sqlBanStorage != null) {
            sqlBanStorage.shutdown();
        }
        if (sqlBanIpStorage != null) {
            sqlBanIpStorage.shutdown();
        }
        if (sqlMuteStorage != null) {
            sqlMuteStorage.shutdown();
        }
        if (sqlItemsStorage != null) {
            sqlItemsStorage.shutdown();
        }
        if (sqlTpaStorage != null) {
            sqlTpaStorage.shutdown();
        }
        if (sqlBanHistoryStorage != null) {
            sqlBanHistoryStorage.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        LoggerUtil.info("ManagerFix disabled.");
    }

    private void registerCommands() {
        ManagerFixCommand mainCmd = new ManagerFixCommand(this, guiManager);
        commandManager.register("managerfix", mainCmd, mainCmd);
        
        // Команда миграции
        ru.managerfix.commands.MigrateCommand migrateCmd = new ru.managerfix.commands.MigrateCommand(this);
        commandManager.register("migrate", migrateCmd, migrateCmd);
        
        // Commands nick, nickadmin, names are registered by NamesModule when enabled.
        // If module is disabled, set fallback so Bukkit doesn't show raw usage "/nick <nickname>".
        registerNamesModuleCommandFallbacks();
    }

    private void registerNamesModuleCommandFallbacks() {
        org.bukkit.command.PluginCommand nickCmd = getCommand("nick");
        if (nickCmd != null && nickCmd.getExecutor() == null) {
            nickCmd.setExecutor((sender, command, label, args) -> {
                ru.managerfix.utils.MessageUtil.send(this, sender, "module-disabled");
                return true;
            });
        }
        org.bukkit.command.PluginCommand nickadminCmd = getCommand("nickadmin");
        if (nickadminCmd != null && nickadminCmd.getExecutor() == null) {
            nickadminCmd.setExecutor((sender, command, label, args) -> {
                ru.managerfix.utils.MessageUtil.send(this, sender, "module-disabled");
                return true;
            });
        }
        org.bukkit.command.PluginCommand namesCmd = getCommand("names");
        if (namesCmd != null && namesCmd.getExecutor() == null) {
            namesCmd.setExecutor((sender, command, label, args) -> {
                ru.managerfix.utils.MessageUtil.send(this, sender, "module-disabled");
                return true;
            });
        }
    }

    private void registerAPI() {
        api = new ru.managerfix.api.ManagerFixAPIImpl(this);
        getServer().getServicesManager().register(
                ru.managerfix.api.ManagerFixAPI.class,
                api,
                this,
                org.bukkit.plugin.ServicePriority.Normal
        );
        LoggerUtil.info("ManagerFix API registered (version " + ru.managerfix.api.ManagerFixAPI.VERSION + ")");
    }

    private void registerPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new ru.managerfix.placeholder.ManagerFixPlaceholders(this).register();
                LoggerUtil.info("PlaceholderAPI expansion registered (%managerfix_afk%).");
            } catch (Throwable t) {
                LoggerUtil.log(java.util.logging.Level.WARNING, "PlaceholderAPI expansion failed", t);
            }
        }
    }

    private void registerModules() {
        moduleManager.registerModule(new WarpsModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new HomesModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new SpawnModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new BanModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new ChatModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new TpaModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new RtpModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new AfkModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new KitsModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new WorldsModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new OtherModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new TabModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new AnnouncerModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new ru.managerfix.modules.names.NamesModule(this, configManager, serviceRegistry));
        moduleManager.registerModule(new ru.managerfix.modules.items.ItemsModule(this, configManager, serviceRegistry));
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ru.managerfix.api.ManagerFixAPI getAPI() {
        return api;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public UIThemeManager getUIThemeManager() {
        return uiThemeManager;
    }

    public GuiTemplate getGuiTemplate() {
        return guiTemplate;
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public String getStorageType() {
        return configManager != null ? configManager.getStorageType() : storageType;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public boolean isMySqlStorage() {
        return configManager != null && configManager.isMySqlStorage();
    }

    public boolean isSqliteStorage() {
        return configManager != null && configManager.isSqliteStorage();
    }

    public ru.managerfix.storage.SqlKitStorage getSqlKitStorage() {
        return sqlKitStorage;
    }

    public ru.managerfix.storage.SqlBanStorage getSqlBanStorage() {
        return sqlBanStorage;
    }

    public ru.managerfix.storage.SqlBanIpStorage getSqlBanIpStorage() {
        return sqlBanIpStorage;
    }

    public ru.managerfix.storage.SqlMuteStorage getSqlMuteStorage() {
        return sqlMuteStorage;
    }

    public ru.managerfix.storage.SqlItemsStorage getSqlItemsStorage() {
        return sqlItemsStorage;
    }

    public ru.managerfix.storage.SqlTpaStorage getSqlTpaStorage() {
        return sqlTpaStorage;
    }

    public ru.managerfix.storage.SqlBanHistoryStorage getSqlBanHistoryStorage() {
        return sqlBanHistoryStorage;
    }

    public MigrationManager getMigrationManager() {
        return migrationManager;
    }

    /**
     * Reloads main config and all modules.
     */
    public void reload() {
        configManager.reload();
        if (profileManager != null) {
            profileManager.stopAutosave();
            profileManager.startAutosave();
        }
        moduleManager.reload();
    }
}
