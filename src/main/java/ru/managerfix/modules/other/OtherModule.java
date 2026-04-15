package ru.managerfix.modules.other;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.service.ServiceRegistry;
import ru.managerfix.utils.MessageUtil;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OtherModule extends AbstractModule {

    private static final String MODULE_NAME = "other";
    private static final String CONFIG_FILE = "other/config.yml";

    private FileConfiguration moduleConfig;
    private OtherConfig config;
    private BackService backService;
    private VanishService vanishService;
    private OtherListener listener;
    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    private final Set<UUID> foodGod = ConcurrentHashMap.newKeySet();
    private final Set<UUID> god = ConcurrentHashMap.newKeySet();
    private volatile boolean chatLocked;

    public OtherModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        super(plugin, configManager, serviceRegistry);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    protected void enable() {
        moduleConfig = configManager.getModuleConfig(CONFIG_FILE);
        config = new OtherConfig(moduleConfig);
        ProfileManager profileManager = plugin instanceof ManagerFix mf ? mf.getProfileManager() : null;
        backService = new BackService();
        vanishService = new VanishService(plugin, profileManager, config);
        if (plugin instanceof ManagerFix mf) {
            if (serviceRegistry.getOrNull(ru.managerfix.service.TeleportService.class) == null) {
                serviceRegistry.register(ru.managerfix.service.TeleportService.class, new DefaultTeleportService(mf));
            }
        }
        listener = new OtherListener(this, backService, vanishService, profileManager);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
            vanishService.applyForOnline(p);
            if (config.isFoodGodPersist() && profileManager != null) {
                Object val = profileManager.getProfile(p).getMetadata("other.foodgod").orElse(false);
                if (val instanceof Boolean b && b) {
                    setFoodGod(p.getUniqueId(), true);
                    p.setFoodLevel(20);
                    p.setSaturation(20f);
                }
            }
        }
        if (plugin instanceof ManagerFix mf) {
            registerCommands(mf.getCommandManager());
        }
        LoggerUtil.debug("Other module enabled.");
    }

    @Override
    protected void disable() {
        frozen.clear();
        foodGod.clear();
        god.clear();
        chatLocked = false;
        listener = null;
        backService = null;
        vanishService = null;
        moduleConfig = null;
        config = null;
        LoggerUtil.debug("Other module disabled.");
    }

    private void registerCommands(CommandManager cm) {
        try { cm.register("god", new GodCommand(this), new GodCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip god: " + t); }
        try { cm.register("fly", new FlyCommand(this), new FlyCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip fly: " + t); }
        try { cm.register("gmc", new GamemodeCommand(this), new GamemodeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip gmc: " + t); }
        try { cm.register("gms", new GamemodeCommand(this), new GamemodeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip gms: " + t); }
        try { cm.register("gmsp", new GamemodeCommand(this), new GamemodeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip gmsp: " + t); }
        try { cm.register("repair", new RepairCommand(this), new RepairCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip repair: " + t); }
        try { cm.register("workbench", new FunctionalBlocksCommand(this), new FunctionalBlocksCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip workbench: " + t); }
        try { cm.register("anvil", new FunctionalBlocksCommand(this), new FunctionalBlocksCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip anvil: " + t); }
        try { cm.register("stonecutter", new FunctionalBlocksCommand(this), new FunctionalBlocksCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip stonecutter: " + t); }
        try { cm.register("grindstone", new FunctionalBlocksCommand(this), new FunctionalBlocksCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip grindstone: " + t); }
        try { cm.register("cartography", new FunctionalBlocksCommand(this), new FunctionalBlocksCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip cartography: " + t); }
        try { cm.register("loom", new FunctionalBlocksCommand(this), new FunctionalBlocksCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip loom: " + t); }
        try { cm.register("enchanting", new FunctionalBlocksCommand(this), new FunctionalBlocksCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip enchanting: " + t); }
        
        // Команды администрирования через AdminCommands
        try { cm.register("ec", new InventoryCommand(this), new InventoryCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip ec: " + t); }
        try { cm.register("invsee", new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin), new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin)); }
        catch (Throwable t) { LoggerUtil.warning("Skip invsee: " + t); }
        try { cm.register("ecsee", new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin), new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin)); }
        catch (Throwable t) { LoggerUtil.warning("Skip ecsee: " + t); }
        try { cm.register("vanish", new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin), new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin)); }
        catch (Throwable t) { LoggerUtil.warning("Skip vanish: " + t); }
        try { cm.register("sudo", new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin), new ru.managerfix.commands.AdminCommands((ru.managerfix.ManagerFix) plugin)); }
        catch (Throwable t) { LoggerUtil.warning("Skip sudo: " + t); }
        
        try { cm.register("killmob", new MobCommand(this), new MobCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip killmob: " + t); }
        try { cm.register("spawnmob", new MobCommand(this), new MobCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip spawnmob: " + t); }
        try { cm.register("tp", new TeleportCommand(this), new TeleportCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip tp: " + t); }
        try { cm.register("pull", new TeleportCommand(this), new TeleportCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip pull: " + t); }
        try { cm.register("push", new TeleportCommand(this), new TeleportCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip push: " + t); }
        try { cm.register("kick", new KickCommand(this), new KickCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip kick: " + t); }
        try { cm.register("near", new NearCommand(this), new NearCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip near: " + t); }
        try { cm.register("v", new VanishCommand(this), new VanishCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip v: " + t); }
        try { cm.register("vanish", new VanishCommand(this), new VanishCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip vanish: " + t); }
        try { cm.register("back", new BackCommand(this), new BackCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip back: " + t); }
        try { cm.register("dback", new BackCommand(this), new BackCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip dback: " + t); }
        try { cm.register("weather", new WeatherTimeCommand(this), new WeatherTimeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip weather: " + t); }
        try { cm.register("day", new WeatherTimeCommand(this), new WeatherTimeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip day: " + t); }
        try { cm.register("night", new WeatherTimeCommand(this), new WeatherTimeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip night: " + t); }
        try { cm.register("sun", new WeatherTimeCommand(this), new WeatherTimeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip sun: " + t); }
        try { cm.register("rain", new WeatherTimeCommand(this), new WeatherTimeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip rain: " + t); }
        try { cm.register("thunder", new WeatherTimeCommand(this), new WeatherTimeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip thunder: " + t); }
        try { cm.register("health", new HealthCommand(this), new HealthCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip health: " + t); }
        try { cm.register("food", new FoodCommand(this), new FoodCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip food: " + t); }
        try { cm.register("clear", new ClearCommand(this), new ClearCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip clear: " + t); }
        try { cm.register("give", new GiveCommand(this), new GiveCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip give: " + t); }
        try { cm.register("pinfo", new InfoCommand(this), new InfoCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip pinfo: " + t); }
        try { cm.register("freeze", new FreezeCommand(this), new FreezeCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip freeze: " + t); }
        try { cm.register("lockchat", new ChatLockCommand(this), new ChatLockCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip lockchat: " + t); }
        try { cm.register("sudo", new SudoCommand(this), new SudoCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip sudo: " + t); }
        try { cm.register("ping", new PingCommand(this), new PingCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip ping: " + t); }
        try { cm.register("coords", new CoordsCommand(this), new CoordsCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip coords: " + t); }
        try { cm.register("seen", new SeenCommand(this), new SeenCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip seen: " + t); }
        try { cm.register("speed", new SpeedCommand(this), new SpeedCommand(this)); }
        catch (Throwable t) { LoggerUtil.warning("Skip speed: " + t); }
    }

    public OtherConfig getOtherConfig() {
        return config;
    }

    public BackService getBackService() {
        return backService;
    }

    public VanishService getVanishService() {
        return vanishService;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public boolean isChatLocked() {
        return chatLocked;
    }

    public void setChatLocked(boolean locked) {
        this.chatLocked = locked;
    }

    public boolean isFrozen(UUID uuid) {
        return uuid != null && frozen.contains(uuid);
    }

    public void setFrozen(UUID uuid, boolean state) {
        if (uuid == null) return;
        if (state) frozen.add(uuid); else frozen.remove(uuid);
    }

    public boolean isFoodGod(UUID uuid) {
        return uuid != null && foodGod.contains(uuid);
    }

    public void setFoodGod(UUID uuid, boolean state) {
        if (uuid == null) return;
        if (state) foodGod.add(uuid); else foodGod.remove(uuid);
    }

    public boolean isGod(UUID uuid) {
        return uuid != null && god.contains(uuid);
    }

    public void setGod(UUID uuid, boolean state) {
        if (uuid == null) return;
        if (state) god.add(uuid); else god.remove(uuid);
    }

    public void logAdminAction(String message) {
        if (config != null && config.isLogAdminActions()) {
            LoggerUtil.debug(message);
        }
    }

    public boolean checkAndApplyCooldown(org.bukkit.entity.Player player, String key, String bypassPerm) {
        if (player == null || key == null || key.isBlank()) return true;
        int seconds = config != null ? config.getCooldownSeconds(key) : 0;
        if (seconds <= 0) return true;
        if (bypassPerm != null && player.hasPermission(bypassPerm)) return true;
        if (plugin instanceof ManagerFix mf) {
            ru.managerfix.profile.PlayerProfile profile = mf.getProfileManager().getProfile(player);
            String cooldownKey = "other_" + key.toLowerCase();
            if (profile.hasCooldown(cooldownKey)) {
                long remaining = profile.getCooldownRemaining(cooldownKey);
                player.sendMessage(MessageUtil.parse("<red>Кулдаун: " + ((remaining + 999) / 1000) + " сек.</red>"));
                return false;
            }
            profile.setCooldown(cooldownKey, seconds * 1000L);
            mf.getProfileManager().saveProfileAsync(player.getUniqueId());
        }
        return true;
    }
}
