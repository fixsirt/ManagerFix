# ⚙️ ManagerFix — Руководство по настройке

Подробное руководство по конфигурации всех модулей плагина **ManagerFix 1.0.0**

---

## 📖 Содержание

1. [Главный конфиг](#главный-конфиг-configyml)
2. [Модуль Warps](#модуль-warps)
3. [Модуль Homes](#модуль-homes)
4. [Модуль Spawn](#модуль-spawn)
5. [Модуль Chat](#модуль-chat)
6. [Модуль TPA](#модуль-tpa)
7. [Модуль RTP](#модуль-rtp)
8. [Модуль Ban](#модуль-ban)
9. [Модуль AFK](#модуль-afk)
10. [Модуль Kits](#модуль-kits)
11. [Модуль Items](#модуль-items)
12. [Модуль Worlds](#модуль-worlds)
13. [Модуль Other](#модуль-other)
14. [Модуль Tab](#модуль-tab)
15. [Модуль Announcer](#модуль-announcer)
16. [Модуль Names](#модуль-names)
17. [Языковой файл](#языковой-файл)
18. [База данных](#база-данных)
19. [Кластер Redis](#кластер-redis)

---

## 📄 Главный конфиг (config.yml)

Расположение: `plugins/ManagerFix/config.yml`

### Включение модулей

```yaml
modules:
  warps: true           # Варпы - точки телепортации
  homes: true           # Дома - личные точки игроков
  spawn: true           # Спавн - точка появления
  chat: true            # Чат - локальный/глобальный
  tpa: true             # TPA - запросы телепортации
  rtp: true             # RTP - случайная телепортация
  ban: true             # Ban - система банов
  afk: true             # AFK - режим "отошёл"
  kits: true            # Kits - наборы предметов
  worlds: false         # Worlds - управление мирами (отключено по умолчанию)
  other: true           # Other - админ-утилиты
  tab: true             # Tab - кастомный таб-лист
  announcer: true       # Announcer - объявления
  names: true           # Names - кастомные ники
  items: true           # Items - редактирование предметов
```

### Тип хранилища

```yaml
# Storage: YAML или MYSQL
storage:
  type: YAML            # YAML - файлы, MYSQL - база данных
```

**При переключении на MYSQL** данные автоматически мигрируют!

### Настройки MySQL

```yaml
database:
  host: localhost       # Адрес сервера БД
  port: 3306            # Порт
  database: managerfix  # Название базы данных
  username: root        # Пользователь
  password: password    # Пароль
  pool-size: 10         # Размер пула соединений
```

### Глобальные настройки

```yaml
settings:
  debug: false                    # Режим отладки (логи)
  default-language: ru            # Язык по умолчанию (ru, en)
  profile-autosave-minutes: 10    # Автосохранение профилей (минуты)
  warp-cooldown-seconds: 5        # Кулдаун варпов (секунды)
  gui-animation-ticks: 20         # Частота анимации GUI (тики)
```

### Кластер Redis (для сети серверов)

```yaml
cluster:
  enabled: false                  # Включить синхронизацию
  type: REDIS                     # Тип кластера
  server-id: server1              # Уникальный ID сервера
  redis:
    host: localhost               # Адрес Redis
    port: 6379                    # Порт Redis
    password: ""                  # Пароль Redis
    channel-prefix: managerfix    # Префикс каналов
```

---

## 📍 Модуль Warps

Расположение: `plugins/ManagerFix/modules/warps/config.yml`

### Основные настройки

```yaml
settings:
  max-warps-per-player: 10        # Максимум варпов на игрока
  teleport-delay: 3               # Задержка перед телепортацией (сек)
  cooldown: 5                     # Кулдаун между телепортациями (сек)
  default-permission: ""          # Право по умолчанию для варпов
```

### GUI настройки

```yaml
gui:
  enabled: true                   # Включить GUI
  title: "Варпы сервера"          # Заголовок окна
  size: 54                        # Размер (9-54, кратно 9)
  
  # Кнопки навигации
  navigation:
    previous-page:
      slot: 48                    # Слот кнопки "Назад"
      icon: ARROW                 # Иконка
      name: "&cНазад"             # Название
    next-page:
      slot: 50                    # Слот кнопки "Вперёд"
      icon: ARROW                 # Иконка
      name: "&aВперёд"            # Название
```

### Категории варпов

```yaml
categories:
  spawn:
    icon: GRASS_BLOCK             # Иконка категории
    name: "&aСпавн"               # Название
    description: "&7Точка спавна" # Описание
    permission: ""                # Право доступа (опционально)
    
  pvp:
    icon: DIAMOND_SWORD
    name: "&cPVP Арена"
    description: "&7Арена для сражений"
    
  farm:
    icon: WHEAT
    name: "&eФермы"
    description: "&7Территория ферм"
```

### Иконки для варпов

```yaml
icons:
  default:
    icon: COMPASS                 # Иконка по умолчанию
    name: "&6{warp_name}"         # Название ({warp_name} - название варпа)
    description:                  # Описание
      - "&7Варп: &a{warp_name}"
      - "&7Создан: &e{created_at}"
      - ""
      - "&aЛКМ - Телепортироваться"
```

### Команды и кулдауны

Расположение: `plugins/ManagerFix/modules/warps/commands.yml`

```yaml
commands:
  warp:
    enabled: true
    aliases: [w, tp]              # Алиасы: /w, /tp
    cooldown: 5                   # Кулдаун (секунды)
    bypass-permission: managerfix.warps.bypass.cooldown
```

---

## 🏠 Модуль Homes

Расположение: `plugins/ManagerFix/modules/homes/config.yml`

### Основные настройки

```yaml
settings:
  max-homes: 5                    # Максимум домов по умолчанию
  teleport-delay: 3               # Задержка телепортации (сек)
  cooldown: 5                     # Кулдаун (сек)
  allow-renaming: true            # Разрешить переименование
```

### Лимиты по правам

```yaml
# Лимиты устанавливаются правами:
# managerfix.homes.limit.1    — 1 дом
# managerfix.homes.limit.3    — 3 дома
# managerfix.homes.limit.5    — 5 домов
# managerfix.homes.limit.10   — 10 домов
# managerfix.homes.limit.20   — 20 домов
```

### GUI настройки

```yaml
gui:
  enabled: true
  title: "Мои дома"
  size: 45
  
  # Пустой слот (заполнитель)
  filler:
    icon: GRAY_STAINED_GLASS_PANE
    name: " "
```

---

## 🏛️ Модуль Spawn

Расположение: `plugins/ManagerFix/modules/spawn/config.yml`

### Настройки телепортации

```yaml
settings:
  teleport-delay-seconds: 5       # Задержка перед телепортацией
  cancel-on-move: true            # Отмена при движении
  cancel-on-damage: true          # Отмена при получении урона
  spawn-on-join: false            # Телепорт при входе на сервер
  spawn-on-death: false           # Телепорт после смерти
  spawn-first-join-only: false    # Только при первом входе
  safe-teleport: true             # Проверка безопасности локации
```

### GUI настройки

```yaml
gui:
  enabled: true
  title: "Настройки спавна"
  
  # Опции в GUI
  options:
    teleport-delay:
      slot: 10
      icon: CLOCK
      name: "&6Задержка телепортации"
    cancel-on-move:
      slot: 12
      icon: FEATHER
      name: "&aОтмена при движении"
```

---

## 💬 Модуль Chat

Расположение: `plugins/ManagerFix/modules/chat/config.yml`

### Форматы сообщений

```yaml
# Формат локального чата (видят в радиусе)
format-local: "{badge} {prefix}{player}{suffix}: {message}"

# Формат глобального чата (видят все)
format-global: "{badge} {prefix}{player}{suffix}: {message}"

# Бейджи
badge-local: "｢𝐋｣"                # Локальный чат
badge-global: "｢𝐆｣"               # Глобальный чат
badge-pm: "｢𝐏𝐌｣"                  # Личные сообщения
```

### Локальный чат

```yaml
local-radius: 60                  # Радиус в блоках (0 = только глобальный)

# Звуки локального чата
local-chat-sounds-enabled: true
local-sound-send: ENTITY_EXPERIENCE_ORB_PICKUP    # Звук отправителю
local-sound-receive: BLOCK_NOTE_BLOCK_HAT         # Звук получателю
```

### Спам и глобальный чат

```yaml
spam-cooldown: 2                  # Кулдаун между сообщениями (сек)

global-sent-note-enabled: false   # Показывать подсказку после отправки в глобальный
```

### Личные сообщения

```yaml
format-pm: "{badge} {sender} → {receiver}: {message}"

pm-sounds-enabled: true
pm-sound-send: ENTITY_EXPERIENCE_ORB_PICKUP
pm-sound-receive: BLOCK_NOTE_BLOCK_PLING
```

### Тултипы (hover)

```yaml
# Тултип при наведении на ник
hover-enabled: true
hover-format: |
  <gray>Игрок: <white>{player}</white>
  <gray>Баланс: <gold>{balance}</gold>
  <dark_gray>ЛКМ — личное сообщение

# Тултип при наведении на текст сообщения
message-hover-enabled: true
message-hover-format: "<gray>Отправлено: {time}</gray>\n<dark_gray>ПКМ — копировать"
message-hover-time-format: "HH:mm"    # Формат времени (Java DateTimeFormatter)
```

### Join / Quit / Death сообщения

```yaml
format-join: "<gray>[<green>+</green>] <white>{player}</white>"
format-quit: "<gray>[<red>-</red>] <white>{player}</white>"
format-death: "<gray>{player} умер</gray>"
```

### Команды и кулдауны

Расположение: `plugins/ManagerFix/modules/chat/commands.yml`

```yaml
commands:
  pm:
    enabled: true
    aliases: [tell, msg, message]
    cooldown: 1
    bypass-permission: managerfix.chat.bypass.cooldown
    
  chattoggle:
    enabled: true
    aliases: [ct]
    cooldown: 2
    
  chatspy:
    enabled: true
    cooldown: 3
    
  commandspy:
    enabled: true
    cooldown: 3
    
  clearchat:
    enabled: true
    aliases: [cc]
    cooldown: 10
    bypass-permission: managerfix.chat.admin
```

---

## 🚀 Модуль TPA

Расположение: `plugins/ManagerFix/modules/tpa/config.yml`

### Основные настройки

```yaml
settings:
  request-timeout: 60             # Время жизни запроса (сек)
  teleport-delay: 5               # Задержка телепортации (сек)
  cooldown: 10                    # Кулдаун между запросами (сек)
  cancel-on-move: true            # Отмена при движении
  cancel-on-damage: true          # Отмена при уроне
```

### GUI настройки

```yaml
gui:
  enabled: true
  title: "TPA Запросы"
  
  # Кнопки принятия/отклонения
  accept:
    slot: 11
    icon: LIME_WOOL
    name: "&aПринять"
  deny:
    slot: 15
    icon: RED_WOOL
    name: "&cОтклонить"
```

---

## 🎲 Модуль RTP

Расположение: `plugins/ManagerFix/modules/rtp/config.yml`

### Настройки телепортации

```yaml
settings:
  min-distance: 100               # Минимальное расстояние от центра (блоки)
  max-distance: 5000              # Максимальное расстояние
  cooldown: 30                    # Кулдаун (секунды)
  max-attempts: 5                 # Максимум попыток поиска безопасной точки
  safe-teleport: true             # Проверка безопасности
```

### Опции по правам

```yaml
# Права определяют максимальное расстояние:
# managerfix.rtp.option.1000      — до 1000 блоков
# managerfix.rtp.option.5000      — до 5000 блоков
# managerfix.rtp.option.randomplayer — к случайному игроку
```

---

## 🛑 Модуль Ban

Расположение: `plugins/ManagerFix/modules/ban/config.yml`

### Настройки банов

```yaml
settings:
  default-duration: permanent     # Бан по умолчанию (permanent или время)
  broadcast-bans: true            # Оповещать о банах в чат
  kick-message: "<red>Вы забанены!</red>\n<gray>Причина: {reason}</gray>"
  history-enabled: true           # Включить историю банов
  history-limit: 10               # Максимум записей в истории
```

### Настройки мутов

```yaml
mute:
  default-duration: 3600          # Мут по умолчанию (секунды, 3600 = 1 час)
  kick-message: "<red>Вы замучены!</red>"
```

### GUI настройки

```yaml
gui:
  ban-list:
    title: "Список банов"
    size: 54
    
  mute-list:
    title: "Список мутов"
    size: 54
```

---

## 😴 Модуль AFK

Расположение: `plugins/ManagerFix/modules/afk/config.yml`

### Настройки AFK

```yaml
settings:
  afk-timeout-seconds: 300        # Авто-AFK через 5 минут бездействия
  broadcast-afk: true             # Оповещать о входе/выходе из AFK
  block-commands-while-afk: false # Блокировать команды в AFK
  kick-timeout-seconds: 0         # Кик в AFK (0 = отключено)
  
  # Команды, доступные в AFK (если block-commands-while-afk: true)
  allowed-commands:
    - afk
    - managerfix
```

---

## 🎒 Модуль Kits

Расположение: `plugins/ManagerFix/modules/kits/config.yml`

### Основные настройки

```yaml
settings:
  default-cooldown: 3600          # Кулдаун по умолчанию (секунды, 3600 = 1 час)
```

### GUI настройки

```yaml
gui:
  enabled: true
  title: "Наборы предметов"
  size: 45
  
  # Заполнитель пустых слотов
  filler:
    icon: GRAY_STAINED_GLASS_PANE
    name: " "
```

### Создание китов

Киты создаются через команду `/kit create <название>` или в GUI `/editkits`

**Пример кита в конфиге:**
```yaml
kits:
  starter:
    cooldown: 0                   # Без кулдауна
    permission: ""                # Право доступа (опционально)
    items:
      - type: WOODEN_SWORD
        amount: 1
        name: "&6Стартовый меч"
      - type: BREAD
        amount: 16
        name: "&eЕда"
      - type: LEATHER_CHESTPLATE
        amount: 1
        name: "&7Броня"
```

---

## ⚔️ Модуль Items

Расположение: `plugins/ManagerFix/modules/items/config.yml`

### Сообщения

```yaml
messages:
  no-permission: "<red>У вас нет прав!</red>"
  no-item: "<red>Возьмите предмет в руку!</red>"
  invalid-number: "<red>Неверное число!</red>"
  item-saved: "<green>Предмет сохранён!</green>"
  item-given: "<green>Предмет выдан!</green>"
  invalid-enchantment: "<red>Неверное зачарование!</red>"
  invalid-attribute: "<red>Неверный атрибут!</red>"
```

### Сохранённые предметы

Предметы сохраняются в `plugins/ManagerFix/data/items.yml`

**Пример:**
```yaml
items:
  epic_sword:
    type: DIAMOND_SWORD
    amount: 1
    name: "&6⚔ Легендарный меч"
    lore:
      - "&7Наносит урон всем врагам"
    enchantments:
      sharpness: 5
      unbreaking: 3
      looting: 2
    attributes:
      attack_damage: 10.0
      attack_speed: 1.6
```

---

## 🌍 Модуль Worlds

Расположение: `plugins/ManagerFix/modules/worlds/config.yml`

### Основные настройки

```yaml
settings:
  default-generator: default      # Генератор по умолчанию (default, flat, void)
  allow-teleport: true            # Разрешить телепортацию
  allow-create: true              # Разрешить создание миров
  allow-delete: true              # Разрешить удаление миров
```

### GUI настройки

```yaml
gui:
  enabled: true
  title: "Миры сервера"
  size: 45
```

---

## 🛠️ Модуль Other

Расположение: `plugins/ManagerFix/modules/other/config.yml`

### Настройки

```yaml
settings:
  near-radius: 200                # Радиус команды /near (блоки)
  log-admin-actions: true         # Логировать действия админов
  vanish-hide-from-tab: true      # Скрывать из таблицы игроков
  vanish-hide-join-quit: true     # Скрывать вход/выход в чат
  vanish-persist: true            # Сохранять vanish в профиле
  food-god-persist: true          # Сохранять FoodGod в профиле
```

### Алиасы команд

```yaml
aliases:
  fly: [flight]
  god: [godmode]
  workbench: [craft, crafting]
  enderchest: [ec, echest]
  enchanting: [enchant, table]
```

### Кулдауны

```yaml
cooldowns:
  fly: 5
  god: 5
  vanish: 10
  repair: 3
  heal: 10
  feed: 5
```

### Broadcast (объявления)

```yaml
broadcast:
  title: "&6&lОБЪЯВЛЕНИЕ"         # Заголовок (title)
  subtitle: "&e{message}"         # Подзаголовок
  sound: BLOCK_NOTE_BLOCK_PLING   # Звук уведомления
  chat-message: "<gold><b>ОБЪЯВЛЕНИЕ:</b></gold> {message}"
```

---

## 📊 Модуль Tab

Расположение: `plugins/ManagerFix/modules/tab/config.yml`

### Header и Footer

```yaml
header: |
  <#FFD700>⚡ <b>ManagerFix Server</b> ⚡
  <gray>Онлайн: <white>%server_online%</white> <gray>/</gray> <white>%server_max_players%</white>
  
footer: |
  <gray>TPS: <green>%server_tps_1%</green> <gray>|</gray> Ping: <white>%player_ping%ms</white>
  <dark_gray>www.example.com</dark_gray>
```

### Настройки

```yaml
update-interval-ticks: 20         # Частота обновления (20 тиков = 1 секунда)
```

---

## 📢 Модуль Announcer

Расположение: `plugins/ManagerFix/modules/announcer/config.yml`

### Настройки объявлений

```yaml
settings:
  interval-seconds: 60            # Интервал между сообщениями (секунды)
  broadcast-type: CHAT            # CHAT или ACTION_BAR
  random-order: false             # Случайный порядок сообщений
```

### Список сообщений

```yaml
messages:
  - "<gold>Добро пожаловать на наш сервер!</gold>"
  - "<yellow>Купить донат: <aqua>donate.example.com</aqua></yellow>"
  - "<green>Наш Discord: <aqua>discord.example.com</aqua></green>"
  - "<blue>Голосуй за сервер: <aqua>vote.example.com</aqua></blue>"
  - "<light_purple>Техническая поддержка: <aqua>t.me/fixsirt</aqua></light_purple>"
```

---

## 🎭 Модуль Names

Расположение: `plugins/ManagerFix/modules/names/config.yml`

### Настройки ников

```yaml
settings:
  max-length: 16                  # Максимальная длина ника
  cooldown: 60                    # Кулдаун между сменой ника (секунды)
  allow-colors: true              # Разрешить цвета (& код)
  allow-hex: true                 # Разрешить HEX цвета (#RRGGBB)
  allow-formatting: true          # Разрешить форматирование (&l, &o, &n)
  
  # Формат ника по умолчанию
  default-format: "{prefix}{nick}"
```

### Префиксы

```yaml
prefixes:
  default: ""
  vip: "&6[VIP] "
  premium: "&b[PREMIUM] "
  admin: "&c[ADMIN] "
```

---

## 📄 Языковой файл

Расположение: `plugins/ManagerFix/lang/ru.yml`

### Структура

```yaml
messages:
  no-permission: "<red>У вас нет прав!</red>"
  player-only: "<gray>Эта команда только для игроков.</gray>"
  player-not-found: "<red>Игрок не найден!</red>"
  console-only: "<red>Эта команда только для консоли.</red>"
  cooldown: "<red>Подождите ещё <yellow>{seconds}</yellow> сек.</red>"
  module-disabled: "<red>Этот модуль отключен!</red>"
  
menu:
  main-title: "<dark_gray>ManagerFix"
  module-enabled: "&aВключено"
  module-disabled: "&cОтключено"
  
warps:
  not-found: "<red>Варп <white>{name}</white> не найден.</red>"
  created: "<green>Варп <white>{name}</white> создан!</green>"
  deleted: "<red>Варп <white>{name}</white> удалён!</red>"
  teleported: "<green>Телепортация на варп <white>{name}</white>...</green>"
  
homes:
  set: "<green>Дом <white>{name}</white> установлен!</green>"
  deleted: "<red>Дом <white>{name}</white> удалён!</red>"
  limit-reached: "<red>Достигнут лимит домов!</red>"
  
chat:
  local: "<gray>[Локально]</gray> {player}: {message}"
  global: "<gray>[Глобально]</gray> {player}: {message}"
  pm-sent: "<gray>[ЛС] Вы → {player}: {message}</gray>"
  pm-received: "<gray>[ЛС] {player} → Вы: {message}</gray>"
  
ban:
  banned: "<red>Вы забанены!</red>\n<gray>Причина: {reason}</gray>"
  muted: "<red>Вы замучены!</red>\n<gray>Причина: {reason}</gray>"
```

---

## 💾 База данных

### Таблицы MySQL

При использовании MySQL плагин создаёт следующие таблицы:

```sql
-- Профили игроков
CREATE TABLE mf_profiles (
  uuid VARCHAR(36) PRIMARY KEY,
  username VARCHAR(16),
  data TEXT,
  last_seen BIGINT
);

-- Варпы
CREATE TABLE mf_warps (
  name VARCHAR(64) PRIMARY KEY,
  location TEXT,
  world VARCHAR(64),
  created_by VARCHAR(36),
  created_at BIGINT
);

-- Киты
CREATE TABLE mf_kits (
  name VARCHAR(64) PRIMARY KEY,
  items TEXT,
  cooldown BIGINT,
  permission VARCHAR(128)
);

-- Баны
CREATE TABLE mf_bans (
  id INT AUTO_INCREMENT PRIMARY KEY,
  player_uuid VARCHAR(36),
  player_name VARCHAR(16),
  banned_by VARCHAR(36),
  reason TEXT,
  expires_at BIGINT,
  created_at BIGINT
);

-- Муты
CREATE TABLE mf_mutes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  player_uuid VARCHAR(36),
  player_name VARCHAR(16),
  muted_by VARCHAR(36),
  reason TEXT,
  expires_at BIGINT,
  created_at BIGINT
);

-- TPA запросы
CREATE TABLE mf_tpa_requests (
  id INT AUTO_INCREMENT PRIMARY KEY,
  sender_uuid VARCHAR(36),
  target_uuid VARCHAR(36),
  created_at BIGINT,
  expires_at BIGINT
);

-- Сохранённые предметы
CREATE TABLE mf_saved_items (
  name VARCHAR(64) PRIMARY KEY,
  data TEXT
);
```

---

## 🌐 Кластер Redis

### Настройка кластера

Для синхронизации между несколькими серверами:

**config.yml на каждом сервере:**
```yaml
cluster:
  enabled: true
  type: REDIS
  server-id: server1              # Уникальный для каждого сервера
  redis:
    host: redis.example.com
    port: 6379
    password: your_redis_password
    channel-prefix: managerfix
```

### Синхронизируемые события

- ✅ Чат сообщения
- ✅ Личные сообщения
- ✅ Баны/муты
- ✅ AFK статус
- ✅ Профили игроков

---

<div align="center">

**ManagerFix 1.0.0** | Руководство по настройке

Автор: **tg:fixsirt**

</div>
