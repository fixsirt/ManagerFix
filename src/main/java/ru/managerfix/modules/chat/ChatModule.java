package ru.managerfix.modules.chat;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.modules.chat.filter.FilterCommand;
import ru.managerfix.modules.chat.filter.ProfanityFilter;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.service.ExternalApiService;
import ru.managerfix.service.ServiceRegistry;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat module: MiniMessage format, local radius, spam cooldown, toggle global/local.
 */
public final class ChatModule extends AbstractModule {

    private static final String MODULE_NAME = "chat";
    private static final String CONFIG_FILE = "chat/config.yml";

    private FileConfiguration moduleConfig;
    private Listener chatListener;
    private Listener chatJoinQuitDeathListener;
    private Listener commandSpyListener;
    private ProfanityFilter profanityFilter;
    /** Last PM partner per player (UUID -> UUID) for /r reply. */
    private final Map<UUID, UUID> lastPmPartner = new ConcurrentHashMap<>();

    public ChatModule(JavaPlugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
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
        ProfileManager profileManager = plugin instanceof ManagerFix mf ? mf.getProfileManager() : null;
        ExternalApiService externalApiService = serviceRegistry.get(ExternalApiService.class).orElse(null);

        // Инициализация команд
        initCommandConfig(MODULE_NAME);

        // Инициализация фильтра мата
        if (configManager.getModuleConfig("chat/filter.yml") != null) {
            profanityFilter = new ProfanityFilter(plugin, configManager.getModuleConfig("chat/filter.yml"));
        }

        if (profileManager != null) {
            chatListener = new ChatListener(this, profileManager, externalApiService);
            plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
        }
        chatJoinQuitDeathListener = new ChatJoinQuitDeathListener(this);
        plugin.getServer().getPluginManager().registerEvents(chatJoinQuitDeathListener, plugin);
        if (plugin instanceof ManagerFix mf) {
            ChatSpyCommand chatSpyCommand = new ChatSpyCommand(mf);
            mf.getCommandManager().register("chatspy", chatSpyCommand, chatSpyCommand);
            CommandSpyCommand commandSpyCommand = new CommandSpyCommand(mf);
            mf.getCommandManager().register("commandspy", commandSpyCommand, commandSpyCommand);
            PmCommand pmCommand = new PmCommand(mf, this);
            mf.getCommandManager().register("pm", pmCommand, pmCommand);
            mf.getCommandManager().register("tell", pmCommand, pmCommand);
            mf.getCommandManager().register("msg", pmCommand, pmCommand);
            mf.getCommandManager().register("r", new ReplyCommand(mf, this), new ReplyCommand(mf, this));

            // Команда очистки чата
            ChatClearCommand chatClearCmd = new ChatClearCommand();
            mf.getCommandManager().register("clearchat", chatClearCmd, chatClearCmd);
            mf.getCommandManager().register("chatchlear", chatClearCmd, chatClearCmd);
            mf.getCommandManager().register("cc", chatClearCmd, chatClearCmd);

            // Команда broadcast
            BroadcastCommand broadcastCommand = new BroadcastCommand(mf);
            mf.getCommandManager().register("broadcast", broadcastCommand, broadcastCommand);
            mf.getCommandManager().register("bc", broadcastCommand, broadcastCommand);

            // Команда фильтра мата
            FilterCommand filterCommand = new FilterCommand(mf);
            mf.getCommandManager().register("filter", filterCommand, filterCommand);
            
            if (profileManager != null) {
                PmBlockCommand pmBlockCommand = new PmBlockCommand(mf, profileManager);
                mf.getCommandManager().register("pmblock", pmBlockCommand, pmBlockCommand);
                IgnoreCommand ignoreCommand = new IgnoreCommand(mf, profileManager);
                mf.getCommandManager().register("ignore", ignoreCommand, ignoreCommand);
            }
            if (profileManager != null) {
                commandSpyListener = new CommandSpyListener(mf, profileManager);
                plugin.getServer().getPluginManager().registerEvents(commandSpyListener, plugin);
            }
        }
        LoggerUtil.debug("Chat module enabled.");
    }

    @Override
    protected void disable() {
        if (chatListener != null) {
            HandlerList.unregisterAll(chatListener);
            chatListener = null;
        }
        if (chatJoinQuitDeathListener != null) {
            HandlerList.unregisterAll(chatJoinQuitDeathListener);
            chatJoinQuitDeathListener = null;
        }
        if (commandSpyListener != null) {
            HandlerList.unregisterAll(commandSpyListener);
            commandSpyListener = null;
        }
        lastPmPartner.clear();
        moduleConfig = null;
        LoggerUtil.debug("Chat module disabled.");
    }

    /** Sets last PM partner for both sender and target (for /r reply). */
    public void setLastPmPartner(Player sender, Player target) {
        if (sender == null || target == null || sender.getUniqueId().equals(target.getUniqueId())) return;
        lastPmPartner.put(sender.getUniqueId(), target.getUniqueId());
        lastPmPartner.put(target.getUniqueId(), sender.getUniqueId());
    }

    /** Returns the last player who exchanged PM with this player, or null if offline/unknown. */
    public Player getLastPmPartner(Player player) {
        if (player == null) return null;
        UUID partnerUuid = lastPmPartner.get(player.getUniqueId());
        if (partnerUuid == null) return null;
        Player p = plugin.getServer().getPlayer(partnerUuid);
        return (p != null && p.isOnline()) ? p : null;
    }

    /** Template for message text in all formats. Placeholder: {text} = actual message content. */
    public String getMessageFormat() {
        return moduleConfig != null ? moduleConfig.getString("message-format", "{text}") : "{text}";
    }

    public String getFormatLocal() {
        return moduleConfig != null ? moduleConfig.getString("format-local", "<{player}> {message}") : "<{player}> {message}";
    }

    public String getFormatGlobal() {
        return moduleConfig != null ? moduleConfig.getString("format-global", "<{player}> {message}") : "<{player}> {message}";
    }

    public int getLocalRadius() {
        return moduleConfig != null ? moduleConfig.getInt("local-radius", 0) : 0;
    }

    /** If false, local chat (radius) send/receive sounds are not played. Default false when key missing so "off" is safe. */
    public boolean isLocalChatSoundsEnabled() {
        return moduleConfig != null && moduleConfig.getBoolean("local-chat-sounds-enabled", false);
    }

    /** Sound when player sends a local message. Empty or "none" = no sound. */
    public String getLocalSoundSend() {
        return moduleConfig != null ? moduleConfig.getString("local-sound-send", "") : "";
    }

    /** Sound when player receives a local message (in radius). Empty or "none" = no sound. */
    public String getLocalSoundReceive() {
        return moduleConfig != null ? moduleConfig.getString("local-sound-receive", "") : "";
    }

    public int getSpamCooldownSeconds() {
        return moduleConfig != null ? moduleConfig.getInt("spam-cooldown", 2) : 2;
    }

    public String getBadgeLocal() {
        return moduleConfig != null ? moduleConfig.getString("badge-local", "𝐋") : "𝐋";
    }

    public String getBadgeGlobal() {
        return moduleConfig != null ? moduleConfig.getString("badge-global", "𝐆") : "𝐆";
    }

    public boolean isGlobalSentNoteEnabled() {
        return moduleConfig != null && moduleConfig.getBoolean("global-sent-note-enabled", false);
    }

    public String getBadgePm() {
        return moduleConfig != null ? moduleConfig.getString("badge-pm", "｢𝐏𝐌｣") : "｢𝐏𝐌｣";
    }

    public String getFormatPm() {
        return moduleConfig != null ? moduleConfig.getString("format-pm",
                "<gradient:#ffd700:#ffaa00>{badge}</gradient> <gray>{sender}</gray> → <white>{receiver}</white>: {message}")
                : "<gradient:#ffd700:#ffaa00>{badge}</gradient> <gray>{sender}</gray> → <white>{receiver}</white>: {message}";
    }

    /** If false, PM send/receive sounds are not played. */
    public boolean isPmSoundsEnabled() {
        return moduleConfig != null && moduleConfig.getBoolean("pm-sounds-enabled", true);
    }

    /** Sound when player sends a PM. Empty or "none" = no sound. Value: org.bukkit.Sound name. */
    public String getPmSoundSend() {
        return moduleConfig != null ? moduleConfig.getString("pm-sound-send", "") : "";
    }

    /** Sound when player receives a PM. Empty or "none" = no sound. Value: org.bukkit.Sound name. */
    public String getPmSoundReceive() {
        return moduleConfig != null ? moduleConfig.getString("pm-sound-receive", "") : "";
    }

    /** Format for join message. Empty = use vanilla. Placeholder: {player} */
    public String getFormatJoin() {
        return moduleConfig != null ? moduleConfig.getString("format-join", "") : "";
    }

    /** Format for quit message. Empty = use vanilla. Placeholder: {player} */
    public String getFormatQuit() {
        return moduleConfig != null ? moduleConfig.getString("format-quit", "") : "";
    }

    /** Format for death message. Empty = use vanilla. Placeholders: {player}, {message} (vanilla death text) */
    public String getFormatDeath() {
        return moduleConfig != null ? moduleConfig.getString("format-death", "") : "";
    }

    /** Hover over player name in chat: show balance + click to PM. Placeholders: {player}, {balance} */
    public boolean isHoverEnabled() {
        return moduleConfig != null && moduleConfig.getBoolean("hover-enabled", true);
    }

    public String getHoverFormat() {
        return moduleConfig != null ? moduleConfig.getString("hover-format",
                "<#F0F4F8>Баланс: <#00C8FF>{balance}</#00C8FF>\n<#F0F4F8>Нажмите ЛКМ — личное сообщение <#F0F4F8>{player}</#F0F4F8></#F0F4F8>") : "";
    }

    /** Hover over message text: show send time. Right-click to copy. Placeholder: {time} */
    public boolean isMessageHoverEnabled() {
        return moduleConfig != null && moduleConfig.getBoolean("message-hover-enabled", true);
    }

    public String getMessageHoverFormat() {
        return moduleConfig != null ? moduleConfig.getString("message-hover-format",
                "<#F0F4F8>Отправлено: <#F0F4F8>{time}</#F0F4F8>\nПКМ — скопировать сообщение</#F0F4F8>") : "";
    }

    /** Time format for message hover (e.g. HH:mm, dd.MM.yyyy HH:mm) */
    public String getMessageHoverTimeFormat() {
        return moduleConfig != null ? moduleConfig.getString("message-hover-time-format", "HH:mm") : "HH:mm";
    }

    public ProfanityFilter getProfanityFilter() {
        return profanityFilter;
    }

    public boolean isFilterEnabled() {
        return moduleConfig != null && moduleConfig.getBoolean("filter-enabled", true);
    }
}
