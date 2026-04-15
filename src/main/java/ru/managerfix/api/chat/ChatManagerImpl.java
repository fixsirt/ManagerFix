package ru.managerfix.api.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.modules.ban.MuteManager;
import ru.managerfix.modules.ban.MuteRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Реализация ChatManager.
 */
public class ChatManagerImpl implements ChatManager {
    
    private final ManagerFix plugin;
    private final Map<String, ChatFormat> formats = new ConcurrentHashMap<>();
    private final List<MessageHandler> handlers = new CopyOnWriteArrayList<>();
    private final Map<UUID, Set<UUID>> ignoreList = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerChannels = new ConcurrentHashMap<>();
    private final Set<String> channels = ConcurrentHashMap.newKeySet();
    private boolean chatLocked = false;
    
    public ChatManagerImpl(ManagerFix plugin) {
        this.plugin = plugin;
        
        // Регистрируем стандартные форматы
        registerChatFormat("global", ChatFormat.GLOBAL);
        registerChatFormat("local", ChatFormat.LOCAL);
        registerChatFormat("private", ChatFormat.PRIVATE);
        registerChatFormat("channel", ChatFormat.CHANNEL);
    }
    
    @Override
    public void sendGlobalMessage(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        LoggerUtil.debug("[Global] " + PlainTextComponentSerializer.plainText().serialize(message));
    }
    
    @Override
    public void sendLocalMessage(Player sender, Component message, int radius) {
        Component formatted = applyChatFormat(sender, message, ChatFormat.LOCAL)
            .replaceText(builder -> builder.matchLiteral("{radius}").replacement(String.valueOf(radius)));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(sender.getWorld()) && 
                player.getLocation().distance(sender.getLocation()) <= radius) {
                player.sendMessage(formatted);
            }
        }
    }
    
    @Override
    public void sendPrivateMessage(Player sender, Player target, Component message) {
        Component formatted = ChatFormat.PRIVATE.apply(sender.getName(), message);
        sender.sendMessage(formatted);
        target.sendMessage(formatted);
    }
    
    @Override
    public void sendChannelMessage(Player sender, String channel, Component message) {
        Component formatted = ChatFormat.CHANNEL.apply(sender.getName(), message)
            .replaceText(builder -> builder.matchLiteral("{channel}").replacement(channel));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (channel.equals(playerChannels.get(player.getUniqueId()))) {
                player.sendMessage(formatted);
            }
        }
    }
    
    @Override
    public void broadcast(Component message, String permission) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (permission == null || player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        }
    }
    
    @Override
    public void sendToOps(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }
    
    @Override
    public Component formatMessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }
    
    @Override
    public Component applyChatFormat(Player sender, Component message, ChatFormat format) {
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(message);
        String formatted = format.getFormat()
            .replace("{player}", sender.getName())
            .replace("{message}", plainMessage);
        return MiniMessage.miniMessage().deserialize(formatted);
    }
    
    @Override
    public ChatFilterResult filterMessage(Player sender, String message) {
        for (MessageHandler handler : handlers) {
            ChatFilterResult result = handler.onMessage(sender, message);
            if (result != ChatFilterResult.ALLOWED) {
                return result;
            }
        }
        
        // Встроенные проверки
        if (isCapsSpam(message)) {
            return ChatFilterResult.BLOCKED;
        }
        
        if (containsAds(message)) {
            return ChatFilterResult.BLOCKED;
        }
        
        return ChatFilterResult.ALLOWED;
    }
    
    @Override
    public boolean isSpam(Player sender, String message) {
        // Простая проверка на спам (можно улучшить)
        return false;
    }
    
    @Override
    public boolean isCapsSpam(String message) {
        if (message.isEmpty()) return false;
        
        int caps = 0;
        for (char c : message.toCharArray()) {
            if (Character.isUpperCase(c)) caps++;
        }
        
        return (caps / (double) message.length()) > 0.7;
    }
    
    @Override
    public boolean containsAds(String message) {
        String lower = message.toLowerCase();
        return lower.contains("http://") || 
               lower.contains("https://") || 
               lower.contains("www.") ||
               lower.contains(".ru") ||
               lower.contains(".com");
    }
    
    @Override
    public void registerMessageHandler(MessageHandler handler) {
        handlers.add(handler);
    }
    
    @Override
    public void unregisterMessageHandler(MessageHandler handler) {
        handlers.remove(handler);
    }
    
    @Override
    public void registerChatFormat(String name, ChatFormat format) {
        formats.put(name, format);
    }
    
    @Override
    public void createChannel(String name, String permission) {
        channels.add(name);
        LoggerUtil.debug("Channel created: " + name);
    }
    
    @Override
    public void joinChannel(Player player, String channel) {
        if (!channels.contains(channel)) {
            player.sendMessage(Component.text("Канал не найден!"));
            return;
        }
        playerChannels.put(player.getUniqueId(), channel);
        player.sendMessage(Component.text("Вы вступили в канал: " + channel));
    }
    
    @Override
    public void leaveChannel(Player player, String channel) {
        if (playerChannels.get(player.getUniqueId()) != null) {
            playerChannels.remove(player.getUniqueId());
            player.sendMessage(Component.text("Вы покинули канал"));
        }
    }
    
    @Override
    public List<String> getPlayerChannels(Player player) {
        return new ArrayList<>(channels);
    }
    
    @Override
    public void setActiveChannel(Player player, String channel) {
        playerChannels.put(player.getUniqueId(), channel);
    }
    
    @Override
    public String getActiveChannel(Player player) {
        return playerChannels.get(player.getUniqueId());
    }
    
    @Override
    public void addToIgnore(Player player, UUID ignored) {
        ignoreList.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
            .add(ignored);
    }
    
    @Override
    public void removeFromIgnore(Player player, UUID ignored) {
        Set<UUID> ignoredSet = ignoreList.get(player.getUniqueId());
        if (ignoredSet != null) {
            ignoredSet.remove(ignored);
        }
    }
    
    @Override
    public boolean isIgnoring(Player player, UUID ignored) {
        Set<UUID> ignoredSet = ignoreList.get(player.getUniqueId());
        return ignoredSet != null && ignoredSet.contains(ignored);
    }
    
    @Override
    public List<UUID> getIgnoredPlayers(Player player) {
        Set<UUID> ignoredSet = ignoreList.get(player.getUniqueId());
        return ignoredSet != null ? new ArrayList<>(ignoredSet) : Collections.emptyList();
    }
    
    @Override
    public boolean isMuted(Player player) {
        // Получаем MuteManager из BanModule
        return getMuteManager().isMuted(player.getUniqueId());
    }
    
    @Override
    public Optional<MuteInfo> getMuteInfo(Player player) {
        MuteManager mm = getMuteManager();
        Optional<MuteRecord> record = mm.getMute(player.getUniqueId());
        
        return record.map(r -> new MuteInfo(
            r.getReason(),
            r.getSource(),
            r.getCreatedAt(),
            r.getExpiresAt()
        ));
    }
    
    @Override
    public void mute(Player player, String reason, String source, long duration) {
        MuteManager mm = getMuteManager();
        mm.mute(player.getUniqueId(), player.getName(), reason, source, 
                duration > 0 ? System.currentTimeMillis() + duration : 0, 
                () -> {});
    }
    
    @Override
    public void unmute(Player player) {
        MuteManager mm = getMuteManager();
        mm.unmute(player.getUniqueId(), () -> {});
    }
    
    @Override
    public void clearChatForAll() {
        String clearText = "\n".repeat(150);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(clearText);
        }
    }
    
    @Override
    public void clearChatFor(Player player) {
        player.sendMessage("\n".repeat(150));
    }
    
    @Override
    public void setChatLocked(boolean locked) {
        this.chatLocked = locked;
        sendGlobalMessage(MiniMessage.miniMessage().deserialize(
            locked ? "<#FF3366>Чат заблокирован!</#FF3366>" : "<#00C8FF>Чат разблокирован!</#00C8FF>"
        ));
    }
    
    @Override
    public boolean isChatLocked() {
        return chatLocked;
    }
    
    private MuteManager getMuteManager() {
        return plugin.getModuleManager()
            .getEnabledModule("ban")
            .map(m -> (ru.managerfix.modules.ban.BanModule) m)
            .map(ru.managerfix.modules.ban.BanModule::getMuteManager)
            .orElseThrow(() -> new IllegalStateException("Ban module not enabled"));
    }
}
