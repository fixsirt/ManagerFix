# 🔧 ManagerFix — Руководство для разработчиков

Полное руководство по интеграции, расширению и разработке плагинов для **ManagerFix 1.0.0**

---

## 📖 Содержание

1. [Архитектура плагина](#архитектура-плагина)
2. [Подключение к API](#подключение-к-api)
3. [События](#события)
4. [Создание собственных модулей](#создание-собственных-модулей)
5. [Работа с хранилищем](#работа-с-хранилищем)
6. [GUI API](#gui-api)
7. [Chat API](#chat-api)
8. [Profile API](#profile-api)
9. [Service Registry](#service-registry)
10. [EventBus](#eventbus)
11. [Task Scheduler](#task-scheduler)
12. [Примеры интеграции](#примеры-интеграции)

---

## 🏗️ Архитектура плагина

### Основные компоненты

```
ManagerFix
├── Core (Ядро)
│   ├── ConfigManager          # Управление конфигурацией
│   ├── ModuleManager          # Менеджер модулей
│   ├── CommandManager         # Менеджер команд
│   ├── GuiManager             # Менеджер GUI
│   ├── ProfileManager         # Менеджер профилей игроков
│   ├── DatabaseManager        # Менеджер базы данных
│   ├── MigrationManager       # Менеджер миграции данных
│   └── DebugManager           # Менеджер отладки
│
├── Services (Сервисы)
│   ├── ServiceRegistry        # Реестр сервисов
│   ├── TaskScheduler          # Планировщик задач
│   ├── EventBus               # Шина событий
│   └── ExternalApiService     # Кэширование Vault/LuckPerms
│
├── Storage (Хранилище)
│   ├── ProfileStorage         # Хранилище профилей
│   ├── WarpStorageAdapter     # Адаптер варпов
│   ├── SqlKitStorage          # SQL хранилище китов
│   ├── SqlBanStorage          # SQL хранилище банов
│   ├── SqlMuteStorage         # SQL хранилище мутов
│   └── YamlProfileStorage     # YAML хранилище профилей
│
├── Modules (Модули)
│   ├── WarpsModule            # Варпы
│   ├── HomesModule            # Дома
│   ├── SpawnModule            # Спавн
│   ├── ChatModule             # Чат
│   ├── TpaModule              # TPA
│   ├── RtpModule              # RTP
│   ├── BanModule              # Баны
│   ├── AfkModule              # AFK
│   ├── KitsModule             # Киты
│   ├── WorldsModule           # Миры
│   ├── OtherModule            # Админ-утилиты
│   ├── TabModule              # Tab-лист
│   ├── AnnouncerModule        # Объявления
│   ├── NamesModule            # Ники
│   └── ItemsModule            # Предметы
│
└── API (Публичный API)
    ├── ManagerFixAPI          # Основное API
    └── chat/                  # Chat API
        ├── ChatManager
        ├── ChatFilterResult
        ├── MuteInfo
        └── MessageHandler
```

### Жизненный цикл плагина

```java
onEnable():
  1. Инициализация LoggerUtil
  2. Создание FileManager
  3. Загрузка ConfigManager
  4. Инициализация TaskScheduler, EventBus, ServiceRegistry
  5. Подключение к базе данных (если MySQL)
  6. Создание хранилищ (ProfileStorage, WarpStorage, etc.)
  7. Регистрация сервисов в ServiceRegistry
  8. Создание ModuleManager и CommandManager
  9. Регистрация модулей
  10. Загрузка и включение модулей
  11. Регистрация команд
  12. Регистрация API
  13. Регистрация PlaceholderAPI

onDisable():
  1. Остановка ProfileManager autosave
  2. Отключение всех модулей
  3. Закрытие хранилищ
  4. Закрытие соединения с БД
```

---

## 🔌 Подключение к API

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
        
        // Проверка совместимости
        if (!api.isCompatible(1)) {
            getLogger().severe("Несовместимая версия API!");
            return;
        }
    }
    
    public ManagerFixAPI getAPI() {
        return api;
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

### Получение компонентов API

```java
// ChatManager - управление чатом
ChatManager chat = api.getChatManager();

// ProfileManager - профили игроков
ProfileManager profileManager = api.getProfileManager();

// WarpStorage - хранилище варпов
WarpStorage warpStorage = api.getWarpStorage();

// GuiManager - GUI интерфейс
GuiManager guiManager = api.getGuiManager();

// EventBus - шина событий
EventBus eventBus = api.getEventBus();

// ServiceRegistry - реестр сервисов
ServiceRegistry serviceRegistry = api.getServiceRegistry();
```

---

## ⚡ События

### События чата

#### PlayerChatEvent

```java
import ru.managerfix.event.chat.PlayerChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        Component message = event.getMessage();
        
        // Отменить событие
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            player.sendMessage(Component.text("В аду нельзя писать!"));
            return;
        }
        
        // Изменить сообщение
        Component formatted = MiniMessage.miniMessage().deserialize(
            "<red>" + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(message)
        );
        event.setMessage(formatted);
        
        // Изменить получателей
        event.getRecipients().clear();
        event.getRecipients().add(player.getUniqueId());
        event.getRecipients().addAll(Bukkit.getOnlinePlayers().stream()
            .map(Player::getUniqueId)
            .toList());
    }
}
```

#### PlayerMuteEvent

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

#### PlayerUnmuteEvent

```java
@EventHandler
public void onUnmute(PlayerUnmuteEvent event) {
    getLogger().info(event.getPlayerName() + " размучен");
}
```

#### PrivateMessageEvent

```java
import ru.managerfix.event.chat.PrivateMessageEvent;

@EventHandler
public void onPrivateMessage(PrivateMessageEvent event) {
    // Проверка на игнор
    ChatManager chat = getAPI().getChatManager();
    
    if (chat.isIgnoring(event.getTarget().getUniqueId(), event.getSender().getUniqueId())) {
        event.setCancelled(true);
        event.getSender().sendMessage(Component.text("Игрок игнорирует вас"));
        return;
    }
    
    // Модерация ЛС
    String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
        .plainText().serialize(event.getMessage());
    
    if (containsBadWord(message)) {
        event.setCancelled(true);
        event.getSender().sendMessage(Component.text("Запрещённые слова в ЛС!"));
    }
}
```

### События домов

#### HomeCreateEvent

```java
import ru.managerfix.event.HomeCreateEvent;

@EventHandler
public void onHomeCreate(HomeCreateEvent event) {
    Player player = event.getPlayer();
    String homeName = event.getHomeName();
    
    // Логирование
    getLogger().info(player.getName() + " создал дом: " + homeName);
    
    // Проверка лимита
    if (getHomeCount(player) >= getMaxHomes(player)) {
        event.setCancelled(true);
        player.sendMessage(Component.text("Достигнут лимит домов!"));
    }
}
```

#### HomeTeleportEvent

```java
import ru.managerfix.event.HomeTeleportEvent;

@EventHandler
public void onHomeTeleport(HomeTeleportEvent event) {
    Player player = event.getPlayer();
    
    // Эффекты при телепортации
    player.getWorld().playEffect(player.getLocation(), Effect.ENDEREYE_LAUNCH, 10);
    
    // Сообщение
    player.sendMessage(Component.text("Телепортация домой..."));
}
```

### События варпов

#### WarpCreateEvent

```java
import ru.managerfix.event.WarpCreateEvent;

@EventHandler
public void onWarpCreate(WarpCreateEvent event) {
    String warpName = event.getWarpName();
    
    // Проверка названия
    if (!isValidWarpName(warpName)) {
        event.setCancelled(true);
        // Отправитель может быть null, если варп создан из консоли
        Bukkit.getConsoleSender().sendMessage(Component.text("Неверное название варпа!"));
    }
}
```

#### WarpDeleteEvent

```java
import ru.managerfix.event.WarpDeleteEvent;

@EventHandler
public void onWarpDelete(WarpDeleteEvent event) {
    String warpName = event.getWarpName();
    
    // Логирование удаления
    getLogger().info("Варп удалён: " + warpName);
}
```

### События AFK

#### AfkEnterEvent

```java
import ru.managerfix.event.AfkEnterEvent;

@EventHandler
public void onAfkEnter(AfkEnterEvent event) {
    Player player = event.getPlayer();
    
    // Эффект при входе в AFK
    player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 10);
    
    // Сообщение
    if (event.isBroadcast()) {
        Bukkit.broadcastMessage(Component.text(player.getName() + " теперь AFK"));
    }
}
```

#### AfkLeaveEvent

```java
import ru.managerfix.event.AfkLeaveEvent;

@EventHandler
public void onAfkLeave(AfkLeaveEvent event) {
    Player player = event.getPlayer();
    
    // Сообщение
    if (event.isBroadcast()) {
        Bukkit.broadcastMessage(Component.text(player.getName() + " больше не AFK"));
    }
}
```

### События банов

#### PlayerBanEvent

```java
import ru.managerfix.event.PlayerBanEvent;

@EventHandler
public void onPlayerBan(PlayerBanEvent event) {
    // Логирование
    getLogger().info(String.format(
        "Игрок %s забанен администратором %s. Причина: %s, Срок: %s",
        event.getPlayerName(),
        event.getBannedBy(),
        event.getReason(),
        event.getDuration()
    ));
    
    // Проверка на обход
    if (event.getPlayer().hasPermission("managerfix.bypass.ban")) {
        event.setCancelled(true);
        return;
    }
}
```

#### PlayerUnbanEvent

```java
import ru.managerfix.event.PlayerUnbanEvent;

@EventHandler
public void onPlayerUnban(PlayerUnbanEvent event) {
    getLogger().info("Игрок " + event.getPlayerName() + " разбанен");
}
```

### События профилей

#### ProfileLoadEvent

```java
import ru.managerfix.event.ProfileLoadEvent;

@EventHandler
public void onProfileLoad(ProfileLoadEvent event) {
    UUID playerUuid = event.getProfile().getUuid();
    
    // Загрузка дополнительных данных
    loadCustomData(playerUuid);
}
```

#### ProfileSaveEvent

```java
import ru.managerfix.event.ProfileSaveEvent;

@EventHandler
public void onProfileSave(ProfileSaveEvent event) {
    // Сохранение дополнительных данных
    saveCustomData(event.getProfile());
}
```

### События модулей

#### ModuleEnableEvent

```java
import ru.managerfix.event.ModuleEnableEvent;

@EventHandler
public void onModuleEnable(ModuleEnableEvent event) {
    String moduleName = event.getModuleName();
    getLogger().info("Модуль включён: " + moduleName);
}
```

#### ModuleDisableEvent

```java
import ru.managerfix.event.ModuleDisableEvent;

@EventHandler
public void onModuleDisable(ModuleDisableEvent event) {
    String moduleName = event.getModuleName();
    getLogger().info("Модуль отключён: " + moduleName);
}
```

### События RTP

#### RTPEvent

```java
import ru.managerfix.event.RTPEvent;

@EventHandler
public void onRTP(RTPEvent event) {
    Player player = event.getPlayer();
    
    // Проверка мира
    if (!event.getWorld().getName().equals("world")) {
        event.setCancelled(true);
        player.sendMessage(Component.text("RTP запрещён в этом мире!"));
    }
}
```

---

## 🧩 Создание собственных модулей

### Базовый класс модуля

```java
package com.example.mymodule;

import ru.managerfix.core.AbstractModule;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.service.ServiceRegistry;
import org.bukkit.plugin.Plugin;

public class MyModule extends AbstractModule {

    private final Plugin plugin;
    private final ConfigManager configManager;
    
    public MyModule(Plugin plugin, ConfigManager configManager, ServiceRegistry serviceRegistry) {
        super("mymodule", plugin, configManager, serviceRegistry);
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @Override
    public void onEnable() {
        // Загрузка конфига
        saveDefaultConfig();
        
        // Регистрация команд
        registerCommands();
        
        // Регистрация слушателей
        registerListeners();
        
        // Регистрация задач
        registerTasks();
        
        getLogger().info("Модуль MyModule включён!");
    }
    
    @Override
    public void onDisable() {
        // Очистка ресурсов
        saveData();
        
        getLogger().info("Модуль MyModule отключён!");
    }
    
    @Override
    public void reload() {
        // Перезагрузка конфига
        reloadConfig();
    }
    
    private void registerCommands() {
        // Регистрация команд
        // getCommandManager().register("mycommand", new MyCommand(this));
    }
    
    private void registerListeners() {
        // Регистрация слушателей
        // getPlugin().getServer().getPluginManager().registerEvents(new MyListener(this), getPlugin());
    }
    
    private void registerTasks() {
        // Регистрация задач
        // getScheduler().runTaskTimer(() -> doTask(), 20L, 20L);
    }
    
    private void saveData() {
        // Сохранение данных
    }
}
```

### Регистрация модуля

В главном классе плагина ManagerFix:

```java
private void registerModules() {
    // Существующие модули
    moduleManager.registerModule(new WarpsModule(this, configManager, serviceRegistry));
    moduleManager.registerModule(new HomesModule(this, configManager, serviceRegistry));
    
    // Ваш модуль
    moduleManager.registerModule(new MyModule(this, configManager, serviceRegistry));
}
```

### Конфигурация модуля

`plugins/ManagerFix/modules/mymodule/config.yml`:

```yaml
settings:
  enabled: true
  some-option: "value"
  cooldown: 10
  
messages:
  prefix: "&6[MyModule] "
  no-permission: "&cУ вас нет прав!"
```

### Команды модуля

```java
package com.example.mymodule;

import ru.managerfix.core.CommandHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class MyCommand implements CommandHandler {

    private final MyModule module;
    
    public MyCommand(MyModule module) {
        this.module = module;
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("mymodule.command.use")) {
            player.sendMessage("Нет прав!");
            return true;
        }
        
        // Логика команды
        player.sendMessage("Привет из MyModule!");
        
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // Автодополнение
        return List.of();
    }
}
```

### Слушатель событий модуля

```java
package com.example.mymodule;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class MyListener implements Listener {

    private final MyModule module;
    
    public MyListener(MyModule module) {
        this.module = module;
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Логика при входе игрока
        event.getPlayer().sendMessage("Добро пожаловать на сервер с MyModule!");
    }
}
```

---

## 💾 Работа с хранилищем

### ProfileStorage

```java
import ru.managerfix.storage.ProfileStorage;
import ru.managerfix.profile.PlayerProfile;
import java.util.UUID;

// Получить ProfileStorage из API
ProfileStorage storage = api.getProfileStorage();

// Загрузка профиля
UUID playerUuid = player.getUniqueId();
PlayerProfile profile = storage.loadProfile(playerUuid);

// Сохранение профиля
storage.saveProfile(profile);

// Удаление профиля
storage.deleteProfile(playerUuid);

// Проверка существования
boolean exists = storage.hasProfile(playerUuid);
```

### PlayerProfile

```java
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileData;

// Создание профиля
PlayerProfile profile = new PlayerProfile(playerUuid, playerName);

// Метаданные
profile.setAfk(true);
profile.setVanish(true);
profile.setChatSpy(true);
profile.setFoodGod(true);

// Кулдауны
profile.setCooldown("rtp", System.currentTimeMillis());
long rtpCooldown = profile.getCooldown("rtp");

// Дома
profile.addHome("home", location);
profile.removeHome("home");
Location homeLoc = profile.getHome("home");
Map<String, Location> homes = profile.getHomes();

// Сохранение
profileManager.saveProfile(profile);
```

### WarpStorage

```java
import ru.managerfix.modules.warps.WarpStorage;
import ru.managerfix.modules.warps.Warp;

// Получить WarpStorage
WarpStorage warpStorage = api.getWarpStorage();

// Создание варпа
Warp warp = new Warp("spawn", location, player.getUniqueId());
warpStorage.addWarp(warp);

// Получение варпа
Warp spawn = warpStorage.getWarp("spawn");
Location spawnLoc = spawn.getLocation();

// Удаление варпа
warpStorage.removeWarp("oldwarp");

// Список всех варпов
Collection<Warp> warps = warpStorage.getWarps();

// Сохранение
warpStorage.save();
```

### SQL хранилища

```java
// Получить SQL хранилища из ManagerFix
SqlKitStorage kitStorage = managerFix.getSqlKitStorage();
SqlBanStorage banStorage = managerFix.getSqlBanStorage();
SqlMuteStorage muteStorage = managerFix.getSqlMuteStorage();
SqlItemsStorage itemsStorage = managerFix.getSqlItemsStorage();
SqlTpaStorage tpaStorage = managerFix.getSqlTpaStorage();

// Пример: получение кита
Kit kit = kitStorage.getKit("vip");

// Пример: проверка бана
boolean isBanned = banStorage.isBanned(playerUuid);
```

---

## 🎨 GUI API

### Создание GUI

```java
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.GuiBuilder;
import ru.managerfix.gui.Button;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;

GuiManager guiManager = api.getGuiManager();

// Создание GUI
GuiBuilder builder = guiManager.createBuilder("Меню", 54);

// Добавление кнопки
Button button = new Button(
    Material.DIAMOND_SWORD,
    "&6Меч",
    List.of("&7Легендарный предмет", "", "&aЛКМ - взять")
);

button.onClick(event -> {
    Player player = (Player) event.getWhoClicked();
    player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
    player.closeInventory();
});

builder.setButton(10, button);

// Заполнитель
Button filler = new Button(
    Material.GRAY_STAINED_GLASS_PANE,
    " "
);
builder.fill(filler);

// Открытие GUI
Inventory gui = builder.build();
player.openInventory(gui);
```

### Кастомное GUI

```java
import ru.managerfix.gui.GuiHolder;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CustomGui extends GuiHolder {

    public CustomGui(String title, int size) {
        super(title, size);
        initialize();
    }
    
    private void initialize() {
        // Инициализация GUI
        createItems();
    }
    
    private void createItems() {
        // Создание предметов
    }
    
    @Override
    public void onClick(InventoryClickEvent event) {
        // Обработка кликов
        event.setCancelled(true);
        
        int slot = event.getSlot();
        if (slot == 10) {
            // Действие при клике на слот 10
        }
    }
}
```

---

## 💬 Chat API

### Отправка сообщений

```java
import ru.managerfix.api.chat.ChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

ChatManager chat = api.getChatManager();

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
import ru.managerfix.api.chat.MessageHandler;
import ru.managerfix.api.chat.ChatFilterResult;

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

## 👤 Profile API

### Работа с профилями

```java
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.profile.PlayerProfile;

ProfileManager profileManager = api.getProfileManager();

// Загрузка профиля
PlayerProfile profile = profileManager.getProfile(player);

// Метаданные
boolean isAfk = profile.isAfk();
profile.setAfk(true);

boolean isVanished = profile.isVanished();
profile.setVanished(true);

// Кулдауны
profile.setCooldown("rtp", System.currentTimeMillis());
long cooldown = profile.getCooldown("rtp");
long remaining = profile.getRemainingCooldown("rtp", 30000); // 30 сек кулдаун

// Дома
int maxHomes = profile.getMaxHomes(); // С учётом прав
int homeCount = profile.getHomeCount();
profile.addHome("base", location);
profile.removeHome("base");
Location home = profile.getHome("base");

// Сохранение
profileManager.saveProfile(profile);
```

---

## 🔗 Service Registry

### Получение сервисов

```java
import ru.managerfix.service.ServiceRegistry;

ServiceRegistry serviceRegistry = api.getServiceRegistry();

// Получить зарегистрированный сервис
TaskScheduler scheduler = serviceRegistry.get(TaskScheduler.class);
GuiManager guiManager = serviceRegistry.get(GuiManager.class);
EventBus eventBus = serviceRegistry.get(EventBus.class);
ConfigManager configManager = serviceRegistry.get(ConfigManager.class);
```

### Регистрация собственных сервисов

```java
// Зарегистрировать свой сервис
MyService myService = new MyService();
serviceRegistry.register(MyService.class, myService);

// Получить сервис
MyService service = serviceRegistry.get(MyService.class);
```

---

## 📨 EventBus

### Публикация событий

```java
import ru.managerfix.event.EventBus;

EventBus eventBus = api.getEventBus();

// Опубликовать событие
eventBus.publish(new MyCustomEvent(player, data));
```

### Подписка на события

```java
import ru.managerfix.event.MFEventHandler;
import ru.managerfix.event.EventPriority;

public class MyListener {

    @MFEventHandler(priority = EventPriority.NORMAL)
    public void onMyEvent(MyCustomEvent event) {
        // Обработка события
    }
}

// Регистрация слушателя
eventBus.register(new MyListener());

// Отписка
eventBus.unregister(myListener);
```

### Создание собственных событий

```java
import ru.managerfix.event.ManagerFixEvent;
import org.bukkit.entity.Player;
import lombok.Getter;

@Getter
public class MyCustomEvent extends ManagerFixEvent {
    
    private final Player player;
    private final String data;
    
    public MyCustomEvent(Player player, String data) {
        this.player = player;
        this.data = data;
    }
}
```

---

## ⏰ Task Scheduler

### Планировщик задач

```java
import ru.managerfix.scheduler.TaskScheduler;

TaskScheduler scheduler = api.getScheduler();

// Выполнить задачу немедленно
scheduler.runTask(() -> {
    // Код выполняется в следующем тике
});

// Выполнить с задержкой
scheduler.runTaskLater(() -> {
    // Код выполняется через 20 тиков (1 секунда)
}, 20L);

// Выполнить периодически
scheduler.runTaskTimer(() -> {
    // Код выполняется каждые 20 тиков
}, 0L, 20L);

// Асинхронное выполнение
scheduler.runAsync(() -> {
    // Код выполняется асинхронно
});

// Асинхронно с задержкой
scheduler.runAsyncLater(() -> {
    // Асинхронно через 20 тиков
}, 20L);

// Асинхронно периодически
scheduler.runAsyncTimer(() -> {
    // Асинхронно каждые 20 тиков
}, 0L, 20L);
```

---

## 🔌 Примеры интеграции

### 1. Интеграция с Discord

```java
public class DiscordIntegration implements Listener {

    private final ChatManager chat;
    private final DiscordBot bot;

    public DiscordIntegration(ChatManager chat, DiscordBot bot) {
        this.chat = chat;
        this.bot = bot;
        
        // Регистрация обработчика сообщений
        chat.registerMessageHandler(this::onMessage);
    }

    private ChatFilterResult onMessage(Player player, String message) {
        // Отправить в Discord
        bot.sendMessageToChannel(
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

### 2. Интеграция с экономикой

```java
import ru.managerfix.service.ExternalApiService;
import net.milkbowl.vault.economy.Economy;

public class EconomyIntegration {

    private final ExternalApiService externalApi;
    private Economy economy;

    public EconomyIntegration(ExternalApiService externalApi) {
        this.externalApi = externalApi;
        this.economy = externalApi.getEconomy();
    }

    public boolean hasMoney(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public void deposit(Player player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }

    public double getBalance(Player player) {
        return economy != null ? economy.getBalance(player) : 0.0;
    }
}
```

### 3. Интеграция с LuckPerms

```java
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class LuckPermsIntegration {

    private final ExternalApiService externalApi;

    public LuckPermsIntegration(ExternalApiService externalApi) {
        this.externalApi = externalApi;
    }

    public LuckPerms getLuckPerms() {
        return externalApi.getLuckPerms();
    }

    public String getPrefix(Player player) {
        LuckPerms api = getLuckPerms();
        if (api == null) return "";
        
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        
        return user.getCachedData().getMetaData().getPrefix();
    }

    public String getSuffix(Player player) {
        LuckPerms api = getLuckPerms();
        if (api == null) return "";
        
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        
        return user.getCachedData().getMetaData().getSuffix();
    }

    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }
}
```

### 4. Анти-мат система

```java
public class AntiSwear implements Listener {

    private final List<String> badWords = Arrays.asList("badword1", "badword2");
    private final ChatManager chat;

    public AntiSwear(ChatManager chat) {
        this.chat = chat;
    }

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        String message = PlainTextComponentSerializer.plainText()
            .serialize(event.getMessage()).toLowerCase();

        for (String word : badWords) {
            if (message.contains(word)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("<red>Не ругайтесь!</red>"));
                
                // Логирование
                chat.sendToOps(Component.text(
                    "<red>" + event.getPlayer().getName() + " пытался использовать запрещённое слово</red>"
                ));
                return;
            }
        }
    }
}
```

### 5. Префиксы в чате

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
    return luckPermsIntegration.getPrefix(player);
}
```

### 6. Авто-модерация

```java
public class AutoModeration implements Listener {

    private final ChatManager chat;

    public AutoModeration(ChatManager chat) {
        this.chat = chat;
    }

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
        if (chat.isSpam(event.getPlayer(), message)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("<red>Не спамьте!</red>"));
        }

        // Проверка на рекламу
        if (chat.containsAds(message)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("<red>Реклама запрещена!</red>"));
            
            // Логирование
            chat.sendToOps(Component.text(
                "<yellow>Реклама от " + event.getPlayer().getName() + ": " + message
            ));
        }
    }
}
```

---

<div align="center">

**ManagerFix 1.0.0** | Руководство для разработчиков

Автор: **tg:fixsirt**

</div>
