# ManagerFix API Documentation

Полная документация по API плагина ManagerFix для разработчиков.

---

## Содержание

1. [Подключение к API](#подключение-к-api)
2. [Chat API](#chat-api)
3. [События](#события)
4. [Примеры использования](#примеры-использования)

---

## Подключение к API

### Способ 1: Через ServicesManager (рекомендуется)

```java
import ru.managerfix.api.ManagerFixAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MyPlugin extends JavaPlugin {
    
    private ManagerFixAPI api;
    
    @Override
    public void onEnable() {
        // Получить API
        RegisteredServiceProvider<ManagerFixAPI> rsp = 
            Bukkit.getServicesManager().getRegistration(ManagerFixAPI.class);
        
        if (rsp == null) {
            getLogger().severe("ManagerFix не найден! Плагин отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        this.api = rsp.getProvider();
        getLogger().info("ManagerFix API подключён! Версия: " + api.getVersion());
    }
}
```

### Способ 2: Через PluginManager

```java
import ru.managerfix.ManagerFix;
import ru.managerfix.api.ManagerFixAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

Plugin plugin = Bukkit.getPluginManager().getPlugin("ManagerFix");
if (plugin instanceof ManagerFix) {
    ManagerFixAPI api = ((ManagerFix) plugin).getAPI();
    // Использовать API
}
```

### Проверка совместимости

```java
if (!api.isCompatible(1)) {
    getLogger().severe("Несовместимая версия API!");
    return;
}
```

---

## Chat API

### Получение ChatManager

```java
ChatManager chat = api.getChatManager();
```

### Отправка сообщений

```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

// Глобальное сообщение
chat.sendGlobalMessage(MiniMessage.miniMessage().deserialize(
    "<gold>Важное объявление!</gold>"
));

// Личное сообщение
chat.sendPrivateMessage(sender, target, MiniMessage.miniMessage().deserialize(
    "<gray>[ЛС] <white>Привет!</white></gray>"
));

// Локальное сообщение (радиус 50 блоков)
chat.sendLocalMessage(player, message, 50);

// Сообщение в канал
chat.sendChannelMessage(player, "admin", message);

// Рассылка только игрокам с правом
chat.broadcast(message, "managerfix.receive.announcements");

// Сообщение только операторам
chat.sendToOps(message);
```

### Mute API

```java
// Проверить, замучен ли игрок
if (chat.isMuted(player)) {
    player.sendMessage(Component.text("Вы не можете писать!"));
}

// Получить информацию о муте
Optional<MuteInfo> muteInfo = chat.getMuteInfo(player);
muteInfo.ifPresent(info -> {
    player.sendMessage(Component.text(
        "Замучен до: " + info.getFormattedExpiresAt()
    ));
});

// Замутить на 1 час
chat.mute(player, "Нарушение правил", "Admin", 3600000);

// Размутить
chat.unmute(player);
```

### Игнор-листы

```java
// Добавить в игнор
chat.addToIgnore(player, ignoredUuid);

// Проверить
if (chat.isIgnoring(player, ignoredUuid)) {
    player.sendMessage(Component.text("Вы игнорируете этого игрока"));
}

// Получить список игнорируемых
List<UUID> ignored = chat.getIgnoredPlayers(player);

// Удалить из игнора
chat.removeFromIgnore(player, ignoredUuid);
```

### Каналы чата

```java
// Создать канал
chat.createChannel("vip", "managerfix.channel.vip");

// Вступить в канал
chat.joinChannel(player, "vip");

// Установить активный канал
chat.setActiveChannel(player, "vip");

// Получить активный канал
String channel = chat.getActiveChannel(player);

// Покинуть канал
chat.leaveChannel(player, "vip");
```

### Утилиты

```java
// Очистить чат для всех
chat.clearChatForAll();

// Очистить чат для игрока
chat.clearChatFor(player);

// Заблокировать чат
chat.setChatLocked(true);

// Проверить, заблокирован ли чат
if (chat.isChatLocked()) {
    // Чат заблокирован
}
```

### Фильтры

```java
// Проверить сообщение
ChatFilterResult result = chat.filterMessage(player, message);
if (result == ChatFilterResult.BLOCKED) {
    player.sendMessage(Component.text("Сообщение заблокировано!"));
}

// Проверить на спам
if (chat.isSpam(player, message)) {
    player.sendMessage(Component.text("Не спамьте!"));
}

// Проверить на капс
if (chat.isCapsSpam(message)) {
    player.sendMessage(Component.text("Не пишите капсом!"));
}

// Проверить на рекламу
if (chat.containsAds(message)) {
    player.sendMessage(Component.text("Реклама запрещена!"));
}
```

### Обработчики сообщений

```java
// Зарегистрировать обработчик
chat.registerMessageHandler((player, message) -> {
    // Проверка на маты
    if (message.toLowerCase().contains("badword")) {
        player.sendMessage(Component.text("Не ругайтесь!"));
        return ChatFilterResult.BLOCKED;
    }
    
    // Проверка на рекламу
    if (chat.containsAds(message)) {
        getLogger().warning(player.getName() + " пытался отправить рекламу: " + message);
        return ChatFilterResult.BLOCKED;
    }
    
    return ChatFilterResult.ALLOWED;
});
```

---

## События

### PlayerChatEvent

```java
import ru.managerfix.event.chat.PlayerChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    
    @EventHandler
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.getMessage();
        
        // Отменить событие
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            player.sendMessage(Component.text("В аду нельзя писать!"));
        }
        
        // Изменить сообщение
        event.setMessage(MiniMessage.miniMessage().deserialize(
            "<red>" + PlainTextComponentSerializer.plainText().serialize(message)
        ));
        
        // Изменить получателей
        event.getRecipients().clear();
        event.getRecipients().add(player.getUniqueId());
    }
}
```

### PlayerMuteEvent

```java
import ru.managerfix.event.chat.PlayerMuteEvent;

@EventHandler
public void onMute(PlayerMuteEvent event) {
    // Отменить мут
    if (event.getPlayer().hasPermission("managerfix.bypass.mute")) {
        event.setCancelled(true);
        return;
    }
    
    // Изменить причину
    event.setReason("Нарушение правил чата: " + event.getReason());
    
    // Логирование
    getLogger().info(event.getPlayerName() + " замучен игроком " + event.getSource());
}
```

### PlayerUnmuteEvent

```java
@EventHandler
public void onUnmute(PlayerUnmuteEvent event) {
    getLogger().info(event.getPlayerName() + " размучен");
}
```

### PrivateMessageEvent

```java
import ru.managerfix.event.chat.PrivateMessageEvent;

@EventHandler
public void onPrivateMessage(PrivateMessageEvent event) {
    // Проверка на игнор
    if (chat.isIgnoring(event.getTarget().getUniqueId(), event.getSender().getUniqueId())) {
        event.setCancelled(true);
        event.getSender().sendMessage(Component.text("Игрок игнорирует вас"));
    }
}
```

---

## Примеры использования

### 1. Интеграция с Discord

```java
public class DiscordIntegration implements Listener {
    
    private final ChatManager chat;
    
    public DiscordIntegration(ChatManager chat) {
        this.chat = chat;
        chat.registerMessageHandler(this::onMessage);
    }
    
    private ChatFilterResult onMessage(Player player, String message) {
        // Отправить в Discord
        DiscordBot.sendMessage(
            "**" + player.getName() + "**: " + message
        );
        return ChatFilterResult.ALLOWED;
    }
    
    @EventHandler
    public void onDiscordMessage(DiscordMessageEvent event) {
        // Отправить из Discord в чат
        chat.sendGlobalMessage(MiniMessage.miniMessage().deserialize(
            "<dark_gray>[Discord] <gray>" + event.getUsername() + ": " + event.getMessage()
        ));
    }
}
```

### 2. Анти-мат система

```java
public class AntiSwear implements Listener {
    
    private final List<String> badWords = Arrays.asList("badword1", "badword2");
    
    @EventHandler
    public void onChat(PlayerChatEvent event) {
        String message = PlainTextComponentSerializer.plainText()
            .serialize(event.getMessage()).toLowerCase();
        
        for (String word : badWords) {
            if (message.contains(word)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("<red>Не ругайтесь!</red>"));
                return;
            }
        }
    }
}
```

### 3. Префиксы в чате

```java
@EventHandler
public void onChat(PlayerChatEvent event) {
    Player player = event.getPlayer();
    
    // Получить префикс из LuckPerms
    String prefix = getPrefix(player);
    
    // Обновить сообщение
    Component original = event.getMessage();
    Component formatted = MiniMessage.miniMessage().deserialize(
        prefix + " " + PlainTextComponentSerializer.plainText().serialize(original)
    );
    event.setMessage(formatted);
}

private String getPrefix(Player player) {
    // Интеграция с LuckPerms
    return LuckPerms.getApi().getUserManager().getUser(player.getUniqueId())
        .getCachedData().getMetaData().getPrefix();
}
```

### 4. Авто-модерация

```java
public class AutoModeration implements Listener {
    
    @EventHandler
    public void onChat(PlayerChatEvent event) {
        String message = PlainTextComponentSerializer.plainText()
            .serialize(event.getMessage());
        
        // Проверка на капс
        if (message.length() > 10) {
            int caps = 0;
            for (char c : message.toCharArray()) {
                if (Character.isUpperCase(c)) caps++;
            }
            
            if ((caps / (double) message.length()) > 0.7) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("<red>Не пишите капсом!</red>"));
            }
        }
        
        // Проверка на спам
        if (isSpam(event.getPlayer(), message)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("<red>Не спамьте!</red>"));
        }
    }
    
    private boolean isSpam(Player player, String message) {
        // Реализация проверки на спам
        return false;
    }
}
```

---

## Version History

| Версия | Изменения |
|--------|-----------|
| 1.0.0 | Initial release |

---

## Поддержка

- Discord: [ссылка]
- GitHub: [ссылка]
- Telegram: @fixsirt

---

*Документация актуальна для ManagerFix API 1.0.0*
