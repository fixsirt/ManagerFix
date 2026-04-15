# ManagerFix — Документация плагина

**Версия:** 1.0.0
**Платформа:** Paper/Purpur 1.21+

---

## Содержание

1. [Установка](#установка)
2. [Модуль Chat (Чат)](#модуль-chat-чат)
3. [Модуль Ban (Бан/Мут)](#модуль-ban-банмут)
4. [Модуль Kits (Наборы)](#модуль-kits-наборы)
5. [Модуль Homes (Дома)](#модуль-homes-дома)
6. [Модуль Warps (Варпы)](#модуль-warps-варпы)
7. [Модуль Spawn (Спавн)](#модуль-spawn-спавн)
8. [Модуль TPA (Телепорт)](#модуль-tpa-телепорт)
9. [Модуль RTP (Случайный телепорт)](#модуль-rtp-случайный-телепорт)
10. [Модуль AFK (AFK система)](#модуль-afk-afk-система)
11. [Модуль Names (Никнеймы)](#модуль-names-никнеймы)
12. [Модуль Tab (Таб)](#модуль-tab-таб)
13. [Модуль Announcer (Объявления)](#модуль-announcer-объявления)
14. [Модуль Items (Предметы)](#модуль-items-предметы)
15. [Модуль Other (Прочее)](#модуль-other-прочее)
16. [Модуль Worlds (Миры)](#модуль-worlds-миры)
17. [Фильтр мата](#фильтр-мата)
18. [Общие разрешения](#общие-разрешения)
19. [API и интеграция](#api-и-интеграция)

---

## Установка

1. Скачайте последнюю версию JAR файла
2. Поместите в папку `plugins` вашего сервера
3. Перезапустите сервер
4. Настройте модули в папке `plugins/ManagerFix/modules/`

---

## Модуль Chat (Чат)

Модуль управления чатом с локальным и глобальным чатом, системой ЛС, игнор-листом и фильтром мата.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/chatspy` | Включить/выключить шпионский режим чата | `managerfix.chat.spy` |
| `/commandspy` | Включить/выключить шпионский режим команд | `managerfix.commandspy` |
| `/pm <игрок> <сообщение>` | Отправить личное сообщение | `managerfix.chat.pm` |
| `/msg <игрок> <сообщение>` | Синоним /pm | `managerfix.chat.pm` |
| `/tell <игрок> <сообщение>` | Синоним /pm | `managerfix.chat.pm` |
| `/r <сообщение>` | Быстрый ответ на последнее ЛС | `managerfix.chat.pm` |
| `/reply <сообщение>` | Синоним /r | `managerfix.chat.pm` |
| `/ignore <игрок>` | Добавить игрока в игнор-лист | `managerfix.chat.ignore` |
| `/unignore <игрок>` | Убрать игрока из игнор-листа | `managerfix.chat.ignore` |
| `/ignore list` | Показать список игнорируемых | `managerfix.chat.ignore` |
| `/pmblock <игрок>` | Заблокировать ЛС от игрока | `managerfix.chat.pm.block` |
| `/pmunblock <игрок>` | Разблокировать ЛС от игрока | `managerfix.chat.pm.block` |
| `/broadcast <сообщение>` | Объявление всем игрокам | `managerfix.broadcast` |
| `/bc <сообщение>` | Синоним /broadcast | `managerfix.broadcast` |
| `/clearchat` | Очистить чат | `managerfix.chat.clear` |
| `/cc` | Синоним /clearchat | `managerfix.chat.clear` |
| `/filter reload` | Перезагрузить фильтр мата | `managerfix.chat.filter` |
| `/filter toggle` | Включить/выключить фильтр | `managerfix.chat.filter` |
| `/filter status` | Показать статус фильтра | `managerfix.chat.filter` |

### Конфигурация (`modules/chat/config.yml`)

```yaml
# Включить фильтр мата
filter-enabled: true

# Формат сообщений
message-format: "{text}"

# Формат локального чата (радиус)
format-local: "<#FF3366>{badge} <#F0F4F8>%luckperms_prefix% <#FF3366>｢<#F0F4F8>{player}</#F0F4F8>｣ <#FF3366>{message}</#FF3366>"

# Формат глобального чата
format-global: "<#00C8FF>{badge} <#F0F4F8>%luckperms_prefix% <#00C8FF>｢<#F0F4F8>{player}</#F0F4F8>｣ <#00C8FF>{message}</#00C8FF>"

# Значки в чате
badge-local: "｢𝐋｣"      # Локальный
badge-global: "｢𝐆｣"     # Глобальный
badge-pm: "｢𝐏𝐌｣"        # Личные сообщения

# Локальный чат (радиус в блоках, 0 = только глобальный)
local-radius: 60

# Кулдаун спама (секунды)
spam-cooldown: 2

# Звуки локального чата
local-chat-sounds-enabled: true
local-sound-send: "ENTITY_EXPERIENCE_ORB_PICKUP"
local-sound-receive: "BLOCK_NOTE_BLOCK_HAT"

# Формат ЛС
format-pm: "<gradient:#7000FF:#00C8FF>{badge}</gradient> <#FF3366>{sender}</#FF3366> → <#FF3366>{receiver}</#FF3366>: {message}"

# Звуки ЛС
pm-sounds-enabled: true
pm-sound-send: "ENTITY_EXPERIENCE_ORB_PICKUP"
pm-sound-receive: "BLOCK_NOTE_BLOCK_PLING"

# Hover на никнейме
hover-enabled: true
hover-format: |
  <#F0F4F8>Баланс: <#00C8FF>{balance}</#00C8FF>
  <#F0F4F8>Нажмите ЛКМ — личное сообщение <#F0F4F8>{player}</#F0F4F8></#F0F4F8>

# Hover на тексте сообщения
message-hover-enabled: true
message-hover-format: "<#F0F4F8>Отправлено: <#F0F4F8>{time}</#F0F4F8>\nЛКМ — скопировать сообщение</#F0F4F8>"

# Формат входа/выхода/смерти
format-join: "<#F0F4F8>[<#00C8FF>+<#F0F4F8>] <#F0F4F8>{player}<#00C8FF> зᴀшел ʜᴀ ᴄᴇᴘʙᴇᴘ<#00C8FF>"
format-quit: "<#F0F4F8>[<#FF3366>-<#F0F4F8>] <#F0F4F8>{player}<#00C8FF> ʙышᴇл ᴄ ᴄᴇᴘʙᴇᴘᴀ<#00C8FF>"
format-death: "<#F0F4F8>[<#FF3366>💀<#F0F4F8>] <#F0F4F8>{player}<#00C8FF> пᴏгиб.<#00C8FF>"

# Broadcast настройки
broadcast:
  enabled: true
  sound: "UI_TOAST_CHALLENGE_COMPLETE"
  sound-volume: 1.0
  sound-pitch: 1.0
  title: "<gradient:#7000FF:#00C8FF>Объявление</gradient>"
  chat-format: "<gradient:#7000FF:#00C8FF>⬤</gradient> <#F0F4F8>Объявление от</#F0F4F8> <#00C8FF>{player}</#00C8FF>:"
```

### Использование чата

- **Локальный чат:** Сообщения видны игрокам в радиусе `local-radius` блоков
- **Глобальный чат:** Добавьте `!` в начало сообщения
- **Цвета:** Автоматическое понижение CAPS (6+ букв капсом → lowercase)
- **Фильтр мата:** Блокирует/цензурирует нецензурные сообщения

### Разрешения модуля Chat

| Разрешение | Описание |
|-----------|---------|
| `managerfix.chat.use` | Использование чата |
| `managerfix.chat.bypass.filter` | Обход фильтра мата |
| `managerfix.chat.bypass.cooldown` | Обход кулдауна спама |
| `managerfix.chat.spy` | Шпионский режим чата |
| `managerfix.commandspy` | Шпионский режим команд |
| `managerfix.chat.pm` | Личные сообщения |
| `managerfix.chat.pm.block` | Блокировка ЛС |
| `managerfix.chat.ignore` | Игнор-лист |
| `managerfix.chat.clear` | Очистка чата |
| `managerfix.broadcast` | Объявления |
| `managerfix.chat.filter` | Управление фильтром |

---

## Модуль Ban (Бан/Мут)

Модуль управления банами, мутами, киками и историей нарушений.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/ban <игрок> [причина] [время]` | Забанить игрока | `managerfix.ban.ban` |
| `/unban <игрок>` | Разбанить игрока | `managerfix.ban.ban` |
| `/banlist` | Список банов (GUI) | `managerfix.ban.list` |
| `/mute <игрок> [причина] [время]` | Замутить игрока | `managerfix.ban.mute` |
| `/unmute <игрок>` | Размутить игрока | `managerfix.ban.mute` |
| `/kick <игрок> [причина]` | Кикнуть игрока | `managerfix.ban.kick` |
| `/banip <IP/игрок> [причина]` | Забанить IP адрес | `managerfix.ban.ban` |
| `/unbanip <IP>` | Разбанить IP | `managerfix.ban.ban` |

### Формат времени

- `1h` — 1 час
- `1d` — 1 день
- `1w` — 1 неделя
- `1m` — 1 месяц
- `permanent` или ` навсегда` — бессрочный бан

### Конфигурация (`modules/ban/config.yml`)

```yaml
# Срок по умолчанию
default-duration: permanent

# Оповещения о банах/мутах
broadcast-bans: true
broadcast-mutes: true

# Сообщение при кике
kick-message: "<color:#FF3366>Вы забанены. Причина: <color:#00C8FF>{reason}</color></color>"

# Форматы оповещений
format:
  ban-broadcast: "..."
  kick-broadcast: "..."
  mute-broadcast: "..."
  ip-ban-broadcast: "..."
  ip-unban-broadcast: "..."
```

### Плейсхолдеры в форматах

| Плейсхолдер | Описание |
|-------------|---------|
| `{targetReal}` | Настоящий ник игрока |
| `{targetNick}` | Отображаемый ник |
| `{sourceReal}` | Ник администратора |
| `{sourceNick}` | Отображаемый ник админа |
| `{reason}` | Причина бана/мута |
| `{duration}` | Срок наказания |
| `{ip}` | IP адрес |

### Разрешения модуля Ban

| Разрешение | Описание |
|-----------|---------|
| `managerfix.ban.ban` | Баны/разбаны |
| `managerfix.ban.mute` | Муты/размуты |
| `managerfix.ban.kick` | Кик игроков |
| `managerfix.ban.bypass.cooldown` | Обход кулдауна команд |

---

## Модуль Kits (Наборы)

Модуль выдачи наборов предметов с системой кулдаунов.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/kit [имя]` | Получить набор | `managerfix.kit.<имя>` |
| `/kits` | Список наборов (GUI) | `managerfix.kit.use` |
| `/editkits` | Управление наборами (админ) | `managerfix.kit.admin` |

### Конфигурация (`modules/kits/config.yml`)

```yaml
# Кулдаун по умолчанию (секунды)
default-cooldown: 86400
```

### Создание набора (админ)

```
/editkits create <имя> — создать набор
/editkits add <имя> — добавить предмет в руке
/editkits remove <имя> <номер> — убрать предмет
/editkits cooldown <имя> <секунды> — установить кулдаун
/editkits perm <имя> <разрешение> — установить разрешение
/editkits delete <имя> — удалить набор
```

### Разрешения модуля Kits

| Разрешение | Описание |
|-----------|---------|
| `managerfix.kit.use` | Просмотр списка наборов |
| `managerfix.kit.<имя>` | Получение конкретного набора |
| `managerfix.kit.admin` | Управление наборами |

---

## Модуль Homes (Дома)

Система домашних точек телепортации для игроков.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/home [имя]` | Телепорт домой | `managerfix.home.use` |
| `/sethome [имя]` | Установить дом | `managerfix.home.set` |
| `/delhome <имя>` | Удалить дом | `managerfix.home.delete` |
| `/homes` | Список домов | `managerfix.home.list` |

### Конфигурация (`modules/homes/config.yml`)

```yaml
# Максимум домов для группы default
max-homes: 5

# Задержка телепортации (секунды)
teleport-delay: 0

# Кулдаун между телепортами (секунды)
cooldown: 0

# Лимиты по группам LuckPerms
group-limits:
  default: 5
  vip: 10
  premium: 15
```

### Разрешения модуля Homes

| Разрешение | Описание |
|-----------|---------|
| `managerfix.home.use` | Использование домов |
| `managerfix.home.set` | Установка домов |
| `managerfix.home.delete` | Удаление домов |
| `managerfix.home.list` | Список домов |
| `managerfix.home.bypass-limit` | Обход лимита домов |

---

## Модуль Warps (Варпы)

Система публичных точек телепортации.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/warp <имя>` | Телепорт к варпу | `managerfix.warp.<имя>` |
| `/warps` | Список варпов | `managerfix.warp.list` |
| `/setwarp <имя>` | Создать варп | `managerfix.warp.set` |
| `/delwarp <имя>` | Удалить варп | `managerfix.warp.delete` |

### Конфигурация (`modules/warps/config.yml`)

```yaml
# Максимум варпов
max-warps: 1

# Лимиты по группам
group-limits:
  default: 1
  vip: 3
  premium: 5
```

### Разрешения модуля Warps

| Разрешение | Описание |
|-----------|---------|
| `managerfix.warp.list` | Список варпов |
| `managerfix.warp.set` | Создание варпов |
| `managerfix.warp.delete` | Удаление варпов |
| `managerfix.warp.<имя>` | Телепорт к конкретному варпу |

---

## Модуль Spawn (Спавн)

Управление точкой спавна сервера.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/spawn` | Телепорт на спавн | `managerfix.spawn.use` |
| `/setspawn` | Установить точку спавна | `managerfix.spawn.set` |

### Конфигурация (`modules/spawn/config.yml`)

```yaml
# Координаты спавна
world: world
x: 0.5
y: 64.0
z: 0.5
yaw: 0.0
pitch: 0.0

# Настройки телепортации
settings:
  teleport-delay-seconds: 5
  cancel-on-move: true          # Отмена при движении
  cancel-on-damage: true        # Отмена при уроне
  spawn-on-join: false          # Спавн при входе
  spawn-on-death: false         # Спавн после смерти
  spawn-first-join-only: false   # Только первый вход
  safe-teleport: true           # Безопасная телепортация

# Анимация телепортации
animation:
  enabled: true
  particles: PORTAL
  secondary-particles: ENCHANT
  sound: ENTITY_ENDERMAN_TELEPORT
  volume: 1.0
  pitch: 1.0
  bossbar-countdown: true       # BossBar обратный отсчёт
  title-countdown: true         # Title обратный отсчёт
```

### Разрешения модуля Spawn

| Разрешение | Описание |
|-----------|---------|
| `managerfix.spawn.use` | Телепорт на спавн |
| `managerfix.spawn.set` | Установка точки спавна |

---

## Модуль TPA (Телепорт)

Система запросов телепортации между игроками.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/tpa <игрок>` | Запрос телепортации к игроку | `managerfix.tpa.use` |
| `/tpahere <игрок>` | Запрос телепортации игрока к себе | `managerfix.tpa.use` |
| `/tpaccept` | Принять запрос | `managerfix.tpa.use` |
| `/tpadeny` | Отклонить запрос | `managerfix.tpa.use` |
| `/tpatoggle` | Включить/выключить запросы | `managerfix.tpa.toggle` |
| `/tpalist` | Чёрный список | `managerfix.tpa.blacklist` |
| `/tpblacklist <игрок>` | Добавить в ЧС | `managerfix.tpa.blacklist` |
| `/tpunblacklist <игрок>` | Убрать из ЧС | `managerfix.tpa.blacklist` |

### Конфигурация (`modules/tpa/config.yml`)

```yaml
# Кулдаун запросов (секунды)
cooldown-seconds: 30

# Время жизни запроса (секунды)
request-timeout-seconds: 60

# Задержка телепортации (секунды)
teleport-delay-seconds: 5

# Звуки
allow-sound: true
sound:
  name: ENTITY_ENDERMAN_TELEPORT
  volume: 1.0
  pitch: 1.0

# Тип анимации телепортации
animation:
  type: swirl
  # Доступные типы: ring, swirl, pulse, pillar, cyclone,
  # explosion, snow, ring_in, double_helix, orbit

# Все сообщения настраиваются
messages:
  request-sent: "<#00C8FF>Запрос отправлен игроку {target}</#00C8FF>"
  request-received: "..."
  accepted: "..."
  denied: "..."
  # ... и другие
```

### Разрешения модуля TPA

| Разрешение | Описание |
|-----------|---------|
| `managerfix.tpa.use` | Использование TPA |
| `managerfix.tpa.toggle` | Включение/выключение TPA |
| `managerfix.tpa.blacklist` | Чёрный список |

---

## Модуль RTP (Случайный телепорт)

Телепортация в случайную точку мира.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/rtp` | Случайная телепортация | `managerfix.rtp.use` |

### Конфигурация (`modules/rtp/config.yml`)

```yaml
# Кулдаун (секунды)
cooldown: 300

# Сообщения
messages:
  searching: "<#00C8FF>Поиск безопасного места...</#00C8FF>"
  failed: "<#FF3366>Не удалось найти безопасное место.</#FF3366>"
  success: "<#00C8FF>Вы были телепортированы.</#00C8FF>"
  cooldown: "<#FF3366>Подождите {seconds} сек.</#FF3366>"
  no-nearby: "<#FF3366>Рядом нет игроков.</#FF3366>"
  insufficient-funds: "<#FF3366>Недостаточно средств: требуется {amount}.</#FF3366>"

# Цена за телепорт рядом с игроками
costs:
  near-player: 1000

# Диапазоны телепортации
near-rtp:
  min: 600
  max: 1000
far-rtp:
  min: 4000
  max: 5000

# Радиус проверки игроков
player-radius:
  min: 30
  max: 80
```

### Разрешения модуля RTP

| Разрешение | Описание |
|-----------|---------|
| `managerfix.rtp.use` | Использование RTP |
| `managerfix.rtp.bypass.cooldown` | Обход кулдауна |

---

## Модуль AFK (AFK система)

Автоматическое определение статуса AFK.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/afk` | Вручную установить AFK | `managerfix.afk.use` |

### Конфигурация (`modules/afk/config.yml`)

```yaml
# Время до автоматического AFK (секунды)
afk-timeout-seconds: 300

# Оповещение в чат
broadcast-afk: true

# Блокировка команд в AFK
block-commands-while-afk: false

# Кик AFK игроков (0 = выключено)
kick-timeout-seconds: 0
```

### Разрешения модуля AFK

| Разрешение | Описание |
|-----------|---------|
| `managerfix.afk.use` | Ручной AFK |
| `managerfix.afk.bypass` | Обход AFK таймаута |

---

## Модуль Names (Никнеймы)

Управление отображаемыми именами игроков.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/nick [ник]` | Сменить ник | `managerfix.nick.use` |
| `/nick reset` | Сбросить ник | `managerfix.nick.use` |
| `/nickadmin <игрок> <ник/off>` | Сменить ник игроку (админ) | `managerfix.nick.admin` |
| `/hidenick` | Скрыть свой ник | `managerfix.nick.hide` |
| `/names <игрок>` | История ников игрока | `managerfix.names.history` |

### Конфигурация (`modules/names/config.yml`)

```yaml
# Кулдаун смены ника (секунды)
nickname-cooldown-seconds: 10

# Разрешить HEX цвета
allow-hex: true

# Максимальная длина ника
max-length: 16

# Формат отображения
display-format: "{prefix} {displayName}"

# Оповещение о смене ника админом
admin-change-broadcast: false

# Смещение текста над головой
nametag-offset-y: 0.3
```

### Разрешения модуля Names

| Разрешение | Описание |
|-----------|---------|
| `managerfix.nick.use` | Смена своего ника |
| `managerfix.nick.admin` | Смена ника других |
| `managerfix.nick.hide` | Скрытие ника |
| `managerfix.names.history` | История ников |

---

## Модуль Tab (Таб)

Настройка заголовка, футера и формата игроков в таб-листе.

### Конфигурация (`modules/tab/config.yml`)

```yaml
# Заголовок (многострочный)
header:
  - "<gradient:#7000FF:#00C8FF>✦ ᴠᴀɴɪʟᴀ sᴜɴs ✦</gradient>"
  - "<#F0F4F8>онᴧᴀйн: <#FF3366>%server_online%</#FF3366> | ᴛᴘs: <#00C8FF>%server_tps_1%</#00C8FF>"
  - "<#F0F4F8>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Футер
footer:
  - "<#F0F4F8>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  - "<#F0F4F8>Ваш пинг: <#00C8FF>%player_ping%</#00C8FF> мs"
  - "<#F0F4F8>Мир: <#FF3366>%player_world%</#FF3366>"
  - "<gradient:#7000FF:#00C8FF>https://t.me/vanillasunsteam</gradient>"

# Формат игроков
player-format: "%luckperms_prefix% {displayName}"

# Формат для AFK игроков
afk-format: "<#FF3366>｢𝐀𝐅𝐊｣</#FF3366> <#F0F4F8>%luckperms_prefix%</#F0F4F8> {displayName}"

# Интервал обновления (тики, 20 = 1 сек)
update-interval-ticks: 40

# Дополнительные настройки
hide-vanished: true           # Скрыть изчезнувших из таба
cluster-placeholders: true    # %cluster_total_online%
placeholder-cache-ticks: 0   # Кэш PlaceholderAPI
sort-by-luckperms: true      # Сортировка по LuckPerms
```

### Поддерживаемые плейсхолдеры

| Плейсхолдер | Описание |
|-------------|---------|
| `%server_online%` | Онлайн сервера |
| `%server_tps_1%` | TPS сервера |
| `%player_ping%` | Пинг игрока |
| `%player_world%` | Мир игрока |
| `%server_time%` | Время сервера |
| `%luckperms_prefix%` | Префикс LuckPerms |
| `%luckperms_suffix%` | Суффикс LuckPerms |
| `{name}` | Имя игрока |
| `{displayName}` | Отображаемое имя |

---

## Модуль Announcer (Объявления)

Ротация автоматических сообщений.

### Конфигурация (`modules/announcer/config.yml`)

```yaml
# Интервал между сообщениями (секунды)
interval-seconds: 300

# Тип рассылки: CHAT или ACTION_BAR
broadcast-type: CHAT

# Сообщения (ротация по порядку)
messages:
  - "<#FF3366>➥ Текст сообщения 1</#FF3366>"
  - "<#FF3366>➥ Текст сообщения 2</#FF3366>"
  - "<#FF3366>➥ Текст сообщения 3</#FF3366>"
```

### Поддерживаемые форматы

- MiniMessage (HEX цвета)
- Click events: `<click:open_url:"URL">текст</click>`
- Hover events: `<hover:show_text:'текст'>текст</hover>`
- PlaceholderAPI плейсхолдеры

---

## Модуль Items (Предметы)

Инструмент для работы с предметами в инвентаре.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/item save <имя>` | Сохранить предмет в руке | `managerfix.items.save` |
| `/item give <имя>` | Выдать сохранённый предмет | `managerfix.items.give` |
| `/item name <текст>` | Изменить название предмета | `managerfix.items.modify` |
| `/item lore <текст>` | Изменить описание предмета | `managerfix.items.modify` |
| `/item amount <число>` | Изменить количество | `managerfix.items.modify` |
| `/item enchant <зачарование> [уровень]` | Добавить зачарование | `managerfix.items.enchant` |
| `/item attribute <атрибут> <значение>` | Добавить атрибут | `managerfix.items.attribute` |
| `/item reload` | Перезагрузить конфиг | `managerfix.items.admin` |

### Конфигурация (`modules/items/config.yml`)

```yaml
messages:
  no-permission: "&cУ вас нет разрешения."
  no-item: "&cВ руке нет предмета."
  invalid-number: "&cНеверное число."
  invalid-material: "&cНеверный материал."
  invalid-enchantment: "&cНеверное зачарование."
  invalid-attribute: "&cНеверный атрибут."
  item-saved: "&aПредмет сохранён как {name}."
  item-given: "&aПредмет выдан."
  config-reloaded: "&aКонфиг перезагружен."
```

### Разрешения модуля Items

| Разрешение | Описание |
|-----------|---------|
| `managerfix.items.save` | Сохранение предметов |
| `managerfix.items.give` | Выдача предметов |
| `managerfix.items.modify` | Модификация предметов |
| `managerfix.items.enchant` | Зачарования |
| `managerfix.items.attribute` | Атрибуты |
| `managerfix.items.admin` | Администрирование |

---

## Модуль Other (Прочее)

Набор административных и вспомогательных команд.

### Команды управления

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/god [игрок]` | Бессмертие | `managerfix.other.god` |
| `/fly [игрок]` | Полёт | `managerfix.other.fly` |
| `/gmc [игрок]` | Режим креатива | `managerfix.other.gamemode` |
| `/gms [игрок]` | Режим выживания | `managerfix.other.gamemode` |
| `/gmsp [игрок]` | Режим наблюдателя | `managerfix.other.gamemode` |
| `/speed [0-10] [игрок]` | Скорость | `managerfix.other.speed` |
| `/heal [игрок]` | Восстановление здоровья | `managerfix.other.heal` |
| `/food [игрок]` | Насыщение | `managerfix.other.food` |
| `/repair [hand/all] [игрок]` | Починка предметов | `managerfix.other.repair` |
| `/clear [игрок] [ предмет]` | Очистка инвентаря | `managerfix.other.clear` |

### Команды телепортации

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/tp <игрок> [игрок2]` | Телепорт | `managerfix.other.tp` |
| `/tphere <игрок>` | Притянуть к себе | `managerfix.other.tp` |
| `/pull <игрок>` | Телепорт к игроку | `managerfix.other.tp` |
| `/push <игрок>` | Оттолкнуть игрока | `managerfix.other.tp` |
| `/back` | Вернуться на место | `managerfix.other.back` |
| `/dback` | Вернуться после смерти | `managerfix.other.back` |
| `/coords` | Показать координаты | `managerfix.other.coords` |
| `/near [радиус]` | Игроки рядом | `managerfix.other.near` |
| `/freeze <игрок>` | Заморозить игрока | `managerfix.other.freeze` |

### Команды инвентаря

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/invsee <игрок>` | Просмотр инвентаря | `managerfix.other.invsee` |
| `/ec [игрок]` | Открыть эндер-сундук | `managerfix.other.ec` |
| `/ecsee <игрок>` | Чужой эндер-сундук | `managerfix.other.ecsee` |
| `/workbench [игрок]` | Верстак | `managerfix.other.workbench` |
| `/anvil [игрок]` | Наковальня | `managerfix.other.workbench` |
| `/enchanting [игрок]` | Стол зачарований | `managerfix.other.workbench` |

### Команды информации

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/pinfo <игрок>` | Информация об игроке | `managerfix.other.pinfo` |
| `/ping [игрок]` | Пинг игрока | `managerfix.other.ping` |
| `/seen <игрок>` | Когда был онлайн | `managerfix.other.seen` |

### Команды мира

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/weather <sun/rain/thunder>` | Погода | `managerfix.other.weather` |
| `/day` | День | `managerfix.other.weather` |
| `/night` | Ночь | `managerfix.other.weather` |
| `/sun` | Ясно | `managerfix.other.weather` |
| `/rain` | Дождь | `managerfix.other.weather` |
| `/thunder` | Гроза | `managerfix.other.weather` |

### Команды мобов

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/spawnmob <тип> [количество] [игрок]` | Призвать моба | `managerfix.other.spawnmob` |
| `/killmob [radius] [игрок]` | Убить мобов | `managerfix.other.killmob` |

### Команды администрирования

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/vanish` | Режим невидимости | `managerfix.other.vanish` |
| `/v` | Синоним /vanish | `managerfix.other.vanish` |
| `/sudo <игрок> <команда>` | Выполнить команду от имени | `managerfix.other.sudo` |
| `/kick <игрок> [причина]` | Кикнуть | `managerfix.other.kick` |
| `/give <игрок> <предмет> [кол-во]` | Выдать предмет | `managerfix.other.give` |
| `/lockchat` | Заблокировать чат | `managerfix.other.lockchat` |

### Конфигурация (`modules/other/config.yml`)

```yaml
# Радиус команды /near
near-radius: 100

# Логирование админских действий
log-admin-actions: true

# Настройки невидимости
vanish-hide-from-tab: true
vanish-hide-join-quit: true
vanish-persist: true

# Сохранение еды/бессмертия
food-god-persist: true

# Алиасы команд
aliases:
  gmc: [creative, gm1]
  gms: [survival, gm0]
  gmsp: [spectator, gm3]
  v: [vanish]
  health: [heal]
  food: [feed]
  ec: [enderchest]

# Кулдауны команд (секунды)
cooldowns:
  back: 0
  dback: 0
  near: 0
  tp: 0
  pull: 0
  push: 0
  repair: 0
  killmob: 0
  spawnmob: 0
  broadcast: 0

# Настройки объявлений
broadcast:
  title: "<gradient:#7000FF:#00C8FF>Объявление</gradient>"
  subtitle: "<#F0F4F8>{message}"
  sound: "UI_TOAST_CHALLENGE_COMPLETE"
  sound-volume: 1.0
  sound-pitch: 1.0
```

### Разрешения модуля Other

| Разрешение | Описание |
|-----------|---------|
| `managerfix.other.god` | Бессмертие |
| `managerfix.other.fly` | Полёт |
| `managerfix.other.gamemode` | Смена режима игры |
| `managerfix.other.speed` | Скорость |
| `managerfix.other.heal` | Лечение |
| `managerfix.other.food` | Насыщение |
| `managerfix.other.repair` | Починка |
| `managerfix.other.clear` | Очистка инвентаря |
| `managerfix.other.tp` | Телепортация |
| `managerfix.other.back` | Возврат |
| `managerfix.other.coords` | Координаты |
| `managerfix.other.near` | Игроки рядом |
| `managerfix.other.freeze` | Заморозка |
| `managerfix.other.invsee` | Просмотр инвентаря |
| `managerfix.other.ec` | Эндер-сундук |
| `managerfix.other.ecsee` | Чужой эндер-сундук |
| `managerfix.other.workbench` | Верстак и др. |
| `managerfix.other.pinfo` | Информация об игроке |
| `managerfix.other.ping` | Пинг |
| `managerfix.other.seen` | История онлайна |
| `managerfix.other.weather` | Погода/время |
| `managerfix.other.spawnmob` | Призыв мобов |
| `managerfix.other.killmob` | Убийство мобов |
| `managerfix.other.vanish` | Невидимость |
| `managerfix.other.sudo` | Выполнение команд |
| `managerfix.other.kick` | Кик |
| `managerfix.other.give` | Выдача предметов |
| `managerfix.other.lockchat` | Блокировка чата |

---

## Модуль Worlds (Миры)

Управление мирами сервера.

### Команды

| Команда | Описание | Разрешение |
|---------|---------|-----------|
| `/worlds` | Список миров (GUI) | `managerfix.worlds.list` |
| `/world tp <мир>` | Телепорт в мир | `managerfix.worlds.tp` |
| `/world create <имя>` | Создать мир | `managerfix.worlds.create` |
| `/world delete <имя>` | Удалить мир | `managerfix.worlds.delete` |

### Конфигурация (`modules/worlds/config.yml`)

```yaml
# Генератор по умолчанию
default-generator: default

# Разрешения
allow-teleport: true
allow-create: true
allow-delete: true
```

### Разрешения модуля Worlds

| Разрешение | Описание |
|-----------|---------|
| `managerfix.worlds.list` | Список миров |
| `managerfix.worlds.tp` | Телепортация |
| `managerfix.worlds.create` | Создание мира |
| `managerfix.worlds.delete` | Удаление мира |

---

## Фильтр мата

Система фильтрации нецензурной лексики в чате.

### Конфигурация (`modules/chat/filter.yml`)

Фильтр настраивается через YAML файл с секциями:

1. **normalization** — правила нормализации текста
   - `digit-to-letter` — замена цифр на буквы (0→о, 3→е, etc.)
   - `latin-to-cyrillic` — замена латиницы на кириллицу (a→а, c→с, etc.)
   - `separators` — разделители для удаления

2. **exact-words** — точные слова для блокировки

3. **root-patterns** — regex паттерны корней мата

4. **masked-patterns** — паттерны для масокрованного мата

5. **whitelist** — исключения (безопасные слова)
   - `words` — точные слова
   - `phrases` — фразы
   - `patterns` — regex паттерны

6. **settings** — настройки
   - `strictness` — уровень строгости (SOFT/NORMAL/STRICT)

### Настройки фильтра

```yaml
# Включить фильтр
enabled: true

# Действие при обнаружении мата
action: BLOCK
# BLOCK — заблокировать
# CENSOR — заменить на ***
# WARN — предупреждение

# Символ цензурирования
censor-symbol: "***"

# Уровень строгости
strictness: "NORMAL"
# SOFT — только точные слова
# NORMAL — + корневые паттерны
# STRICT — + маски + опечатки
```

### Разрешения фильтра мата

| Разрешение | Описание |
|-----------|---------|
| `managerfix.chat.bypass.filter` | Обход фильтра мата |

---

## Общие разрешения

### Базовые разрешения

| Разрешение | Описание |
|-----------|---------|
| `managerfix.*` | Все разрешения |
| `managerfix.admin` | Полный доступ администратора |

### Групповые разрешения (рекомендуемые)

```yaml
# Группа VIP
managerfix.chat.use
managerfix.home.use
managerfix.home.set
managerfix.home.set: 10  # 10 домов
managerfix.warp.use
managerfix.tpa.use
managerfix.kit.vip

# Группа Premium
managerfix.chat.use
managerfix.home.use
managerfix.home.set
managerfix.home.set: 15
managerfix.warp.use
managerfix.tpa.use
managerfix.kit.premium

# Группа Moderator
managerfix.ban.ban
managerfix.ban.mute
managerfix.ban.kick
managerfix.chat.clear
managerfix.other.freeze
managerfix.other.invsee

# Группа Admin
managerfix.*
```

---

## API и интеграция

### Внешние зависимости

| Плагин | Функционал |
|--------|-----------|
| LuckPerms | Группы, префиксы, разрешения |
| PlaceholderAPI | Плейсхолдеры в сообщениях |
| Vault (опционально) | Баланс игроков |

### PlaceholderAPI плейсхолдеры

```
%managerfix_player_afk%       — статус AFK
%managerfix_player_vanish%    — статус невидимости
%managerfix_player_home_count% — количество домов
%managerfix_player_kit_last_<name>% — время последнего кита
```

### Хуки для других плагинов

Плагин предоставляет события для интеграции:
- `AsyncChatEvent` — обработка сообщений чата
- `PlayerAFKEvent` — вход/выход из AFK
- `PlayerTeleportEvent` — телепортации

---

## Структура файлов

```
plugins/ManagerFix/
├── config.yml              # Основной конфиг
├── modules/
│   ├── afk/config.yml
│   ├── announcer/config.yml
│   ├── ban/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── chat/
│   │   ├── config.yml
│   │   ├── commands.yml
│   │   └── filter.yml
│   ├── homes/config.yml
│   ├── items/config.yml
│   ├── kits/config.yml
│   ├── names/config.yml
│   ├── other/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── rtp/config.yml
│   ├── spawn/config.yml
│   ├── tab/config.yml
│   ├── tpa/config.yml
│   ├── warps/config.yml
│   └── worlds/config.yml
├── data/
│   ├── homes.yml
│   ├── warps.yml
│   ├── kits.yml
│   └── ...
└── logs/
    └── profanity.log
```

---

## Поддержка цветовых кодов

Плагин использует MiniMessage для форматирования:

### HEX цвета
```
<#FF3366>текст</#FF3366>
```

### Градиенты
```
<gradient:#7000FF:#00C8FF>текст</gradient>
```

### Форматирование
```
<b>жирный</b>
<i>курсив</i>
<u>подчёркнутый</u>
<st>зачёркнутый</st>
<obfuscated>скрытый</obfuscated>
```

### Кликабельные элементы
```
<click:run_command:'/команда'>текст</click>
<click:suggest_command:'/команда'>текст</click>
<click:open_url:'https://ссылка'>текст</click>
```

### Hover события
```
<hover:show_text:'текст'>элемент</hover>
```

---

## Устранение неполадок

### Фильтр мата не работает
1. Проверьте `filter-enabled: true` в `config.yml`
2. Проверьте синтаксис YAML в `filter.yml`
3. Убедитесь, что игрок не имеет `managerfix.chat.bypass.filter`

### Команды не работают
1. Проверьте разрешения игрока
2. Убедитесь, что модуль включён
3. Проверьте консоль на ошибки

### TPA не работает
1. Оба игрока должны иметь разрешение `managerfix.tpa.use`
2. Цель не должна быть в чёрном списке
3. TPA должна быть включена у цели (`/tpatoggle`)

---

## Часто задаваемые вопросы

**Q: Как добавить новый кит?**
A: Используйте `/editkits create <имя>`, затем `/editkits add <имя>` держа в руке нужный предмет.

**Q: Как настроить лимиты домов по группам?**
A: В `modules/homes/config.yml` добавьте `group-limits` с названиями групп LuckPerms.

**Q: Можно ли использовать свой формат сообщений?**
A: Да, все форматы настраиваются через MiniMessage в соответствующих `config.yml`.

**Q: Как экспортировать данные?**
A: Данные хранятся в YAML файлах в папке `plugins/ManagerFix/data/`.

---

*Документация создана для ManagerFix v1.0.0*
