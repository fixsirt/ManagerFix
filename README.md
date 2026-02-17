# ManagerFix — подробная документация

Подробная документация плагина **ManagerFix** для серверов Minecraft (Paper 1.21.x, Java 21).

**Автор:** tg:fixsirt

---

## Содержание

1. [Описание](#описание)
2. [Требования](#требования)
3. [Установка и обновление](#установка-и-обновление)
4. [Сборка из исходников](#сборка-из-исходников)
5. [Структура файлов](#структура-файлов)
6. [Главный конфиг](#главный-конфиг)
7. [Модули](#модули)
8. [Команды модулей с кулдаунами](#команды-модулей-с-кулдаунами)
9. [Other (админ-утилиты)](#other-админ-утилиты)
10. [Все команды](#команды-и-права) — см. [COMMANDS.md](COMMANDS.md)
11. [Языковые файлы](#языковые-файлы)
12. [Хранилище данных](#хранилище-данных)
13. [Миграция данных](#миграция-данных)
14. [Кластер (Redis)](#кластер-redis)
15. [PlaceholderAPI](#placeholderapi)
16. [API для разработчиков](#api-для-разработчиков)

---

## Описание

**ManagerFix** — модульный плагин для Paper 1.21.x (Java 21), объединяющий типовые функции сервера: дома, варпы, киты, TPA, RTP, AFK, чат, баны, спавн, миры, таб-лист, объявления и расширенный административный пакет **Other**.

Особенности:

- **Модульность** — каждый функционал включается/выключается в конфиге или через GUI.
- **Единая архитектура** — модули, EventBus, сервисы, асинхронное хранилище (YAML/MySQL).
- **GUI** — меню варпов, домов, китов, банов, миров, TPA-подтверждение, главное меню.
- **Сообщения и настройки** — все тексты из языковых файлов (MiniMessage), настройки из `modules/*.yml`.
- **Опционально**: PlaceholderAPI, Vault (префиксы в чате), кластер Redis.

**Автор:** tg:fixsirt

---

## Требования

- **Сервер:** Paper 1.21.x (включая 1.21.1)
- **Java:** 21
- **Опционально:** PlaceholderAPI (плейсхолдеры в чате, табе, объявлениях), Vault + плагин совместимости (префиксы в чате)

---

## Установка и обновление

1. Скачайте `ManagerFix-1.0.0.jar` (или актуальную версию).
2. Поместите JAR в папку `plugins/` сервера.
3. Перезапустите сервер (первая загрузка всегда через полный рестарт).
4. Настройте `plugins/ManagerFix/config.yml` и при необходимости файлы в `plugins/ManagerFix/modules/`.
5. Языковой файл: `plugins/ManagerFix/lang/ru.yml`.

Обновление:

- Сохраните ваши `config.yml`, `modules/*.yml`, `lang/`.
- Замените JAR, перезапустите сервер.
- Сравните новые дефолтные модули с вашим конфигом, при необходимости перенесите новые ключи.

---

## Сборка из исходников

Требуется JDK 21 и Maven.

1. Установите JDK 21 и убедитесь, что `java -version` показывает 21.
2. Выполните сборку:

```bash
mvn -q -DskipTests package
```

Готовый JAR лежит в `target/ManagerFix-1.0.0.jar`.

---

## Структура файлов

```
plugins/ManagerFix/
├── config.yml              # Главный конфиг (модули, хранилище, кластер, общие настройки)
├── lang/
│   └── ru.yml              # Русские сообщения (MiniMessage)
├── modules/                # Конфиги модулей (каждый в отдельной папке)
│   ├── afk/
│   │   ├── config.yml      # Настройки AFK
│   │   └── commands.yml    # Команды, алиасы, кулдауны
│   ├── announcer/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── ban/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── chat/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── homes/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── items/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── kits/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── names/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── other/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── rtp/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── spawn/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── tab/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── tpa/
│   │   ├── config.yml
│   │   └── commands.yml
│   ├── warps/
│   │   ├── config.yml
│   │   └── commands.yml
│   └── worlds/
│       ├── config.yml
│       └── commands.yml
└── data/                   # Данные (создаётся при первом использовании)
    ├── players/            # Профили игроков (при YAML-хранилище)
    ├── warps.yml           # Варпы (при YAML-хранилище)
    ├── bans.yml            # Список банов
    ├── mutes.yml           # Список мутов
    ├── kits.yml            # Наборы предметов
    └── items.yml           # Сохранённые предметы
```

Профили игроков (дома, кулдауны, метаданные) и при необходимости варпы хранятся в БД, если в `config.yml` указано `storage.type: MYSQL`.

---

## Главный конфиг

Файл: `config.yml`

| Параметр | Описание |
|----------|----------|
| **modules** | Включение/выключение модулей (`true`/`false`). Имена: warps, homes, spawn, chat, tpa, rtp, ban, afk, kits, worlds, other, tab, announcer, names. |
| **storage.type** | `YAML` или `MYSQL` — тип хранилища для профилей и (при поддержке) варпов. |
| **database** | Параметры MySQL: host, port, database, username, password, pool-size. Используется при `storage.type: MYSQL`. |
| **cluster** | Кластер для нескольких серверов: enabled, type (REDIS), server-id, redis (host, port, password, channel-prefix). |
| **settings** | debug, default-language (например `ru`), profile-autosave-minutes, warp-cooldown-seconds, gui-animation-ticks. |

---

## Модули

### AFK

**Назначение:** Режим «отошёл», авто-AFK по отсутствию движения, опциональный кик и блокировка команд.

**Конфиг:** `modules/afk.yml`

| Параметр | Описание |
|----------|----------|
| afk-timeout-seconds | Секунд без движения до автоматического перевода в AFK. |
| broadcast-afk | Сообщать ли в чат о входе/выходе из AFK. |
| block-commands-while-afk | Блокировать команды в AFK (кроме /afk и с обходом по праву). |
| kick-timeout-seconds | Кикать в AFK через столько секунд (0 — отключено). |

**Команды:** `/afk`  
**Права:** `managerfix.afk.use`, `managerfix.afk.bypass`  
**PlaceholderAPI:** `%managerfix_afk%`

---

### Announcer (объявления)

**Назначение:** Периодическая рассылка сообщений в чат или в action bar.

**Конфиг:** `modules/announcer.yml`

| Параметр | Описание |
|----------|----------|
| interval-seconds | Интервал между сообщениями (в секундах). |
| messages | Список строк (MiniMessage). Поддерживается PlaceholderAPI. |
| broadcast-type | `CHAT` или `ACTION_BAR`. |

---

### Ban (баны)

**Назначение:** Баны и временные баны, кик при входе, GUI со списком банов.

**Конфиг:** `modules/ban.yml`

| Параметр | Описание |
|----------|----------|
| default-duration | По умолчанию постоянный бан. |
| broadcast-bans | Оповещать ли о банах в чат. |
| kick-message | Текст кика (MiniMessage). Плейсхолдер: `{reason}`. |

**Команды:** `/ban`, `/unban`, `/tempban`, `/banlist`  
**Права:** `managerfix.ban.use`, `managerfix.ban.list`, `managerfix.ban.unban`

---

### Команды модулей с кулдаунами

Каждый модуль имеет файл `modules/<название>/commands.yml`, который настраивает:

- **Алиасы команд** — короткие названия
- **Кулдауны** — задержка между использованиями (в секундах)
- **Bypass permission** — разрешение для обхода кулдауна (по умолчанию OP)

#### Формат commands.yml

```yaml
commands:
  <command_name>:
    enabled: true/false          # Включена ли команда
    aliases: [alias1, alias2]    # Алиасы команды
    cooldown: 5                  # Кулдаун в секундах
    bypass-permission: managerfix.module.bypass.cooldown
```

#### Пример для Chat модуля

```yaml
commands:
  pm:
    enabled: true
    aliases: [tell, msg, message]
    cooldown: 1
    bypass-permission: managerfix.chat.bypass.cooldown
  clearchat:
    enabled: true
    aliases: [chatchlear, cc]
    cooldown: 10
    bypass-permission: managerfix.chat.admin
```

#### Сообщение о кулдауне

Если игрок использует команду слишком часто, он получит сообщение:

```
Подождите ещё X сек. перед использованием этой команды!
```

#### Обход кулдауна

Игроки с соответствующим `bypass-permission` не получают кулдаун.

---

### Миграция данных

При переключении между `YAML` и `MYSQL` в `config.yml` плагин **автоматически** переносит данные:

#### Что мигрирует:

- ✅ Варпы (`data/warps.yml` ↔ таблица `warps`)
- ✅ Киты (`data/kits.yml` ↔ таблица `kits`)
- ✅ Баны (`data/bans.yml` ↔ таблица `bans`)
- ✅ Муты (`data/mutes.yml` ↔ таблица `mutes`)
- ✅ Сохранённые предметы (`data/items.yml` ↔ таблица `saved_items`)
- ✅ Профили игроков (мигрируются при входе/выходе)

#### Как работает:

1. При старте плагин проверяет `data/last_storage_type.txt`
2. Если тип хранилища изменился → запускается миграция
3. Данные копируются, но **не удаляются** из старого хранилища
4. После миграции создаётся флаг `data/migration_done.flag`

#### Логи миграции:

```
[INFO] Storage type changed: YAML → MYSQL
[INFO] Starting data migration...
[INFO] Migrated warps to MySQL.
[INFO] Migrated kits to MySQL.
[INFO] Migrated bans to MySQL.
[INFO] Migrated mutes to MySQL.
[INFO] Migrated items to MySQL.
[INFO] Migration completed successfully!
```

#### Безопасность:

- Данные **не удаляются** при миграции
- Используется `INSERT ... ON DUPLICATE KEY UPDATE`
- Миграция запускается **один раз** при смене типа хранилища
- Если данных нет в исходном хранилище → миграция пропускается

---

### Chat (чат)

**Назначение:** Формат сообщений, локальный/глобальный чат, анти-спам, hover, локальные звуки.

**Конфиг:** `modules/chat.yml`

| Параметр | Описание |
|----------|----------|
| format-local | Формат локального чата (MiniMessage). |
| format-global | Формат глобального чата (MiniMessage). |
| badge-local | Префикс локального чата. |
| badge-global | Префикс глобального чата. |
| message-format | Формат сообщения `{text}`. |
| hover-enabled | Включить hover по нику. |
| hover-format | Формат hover-блока. |
| message-hover-enabled | Hover по тексту + copy-to-clipboard. |
| message-hover-format | Формат hover по тексту. |
| message-hover-time-format | Формат времени. |
| local-radius | Радиус локального чата (блоки). 0 — только глобальный. |
| spam-cooldown | Кулдаун между сообщениями (секунды). |
| local-chat-sounds-enabled | Включить звуки локального чата. |
| local-sound-send | Звук отправки локального сообщения. |
| local-sound-receive | Звук получения локального сообщения. |
| pm-sounds-enabled | Включить звуки ЛС. |
| pm-sound-send | Звук отправки ЛС. |
| pm-sound-receive | Звук получения ЛС. |
| format-join | Формат входа. |
| format-quit | Формат выхода. |
| format-death | Формат смерти. |

**Команды:** `/chattoggle`, `/pm`, `/tell`, `/msg`, `/r`, `/pmblock`, `/ignore`, `/chatspy`, `/commandspy`, `/clearchat`, `/chatchlear`
**Права:** `managerfix.chat.use`, `managerfix.chat.bypass.cooldown`, `managerfix.chat.spy`, `managerfix.command.clearchat`

---

### Homes (дома)

**Назначение:** Точки «дом» с лимитом, задержкой и кулдауном телепортации.

**Конфиг:** `modules/homes.yml`

| Параметр | Описание |
|----------|----------|
| max-homes | Максимум домов на игрока (можно переопределять правами managerfix.homes.limit.N). |
| teleport-delay | Задержка перед телепортом (секунды). Отмена при движении. |
| cooldown | Кулдаун между телепортами (секунды). |

**Команды:** `/sethome`, `/home`, `/delhome`, `/homes`  
**Права:** `managerfix.homes.use`, `managerfix.homes.set`, `managerfix.homes.delete`, `managerfix.homes.teleport`, `managerfix.homes.rename`, `managerfix.homes.bypass.cooldown`, `managerfix.homes.limit.N`

---

### Kits (киты)

**Назначение:** Выдача наборов предметов по команде или из GUI с кулдауном.

**Конфиг:** `modules/kits.yml`

| Параметр | Описание |
|----------|----------|
| default-cooldown | Кулдаун по умолчанию (секунды). |

**Команды:** `/kit [имя]`, `/kits`, `/kit create <имя>`  
**Права:** `managerfix.kits.use`, `managerfix.kits.create`, `managerfix.kits.kit.<имя>`

---

### Items (предметы)

**Назначение:** Управление предметами в руке: название, описание, количество, зачарования, атрибуты, сохранение и выдача.

**Конфиг:** `modules/items/config.yml`

| Параметр | Описание |
|----------|----------|
| messages.no-permission | Сообщение об отсутствии прав. |
| messages.no-item | Сообщение, если предмет не в руке. |
| messages.invalid-number | Сообщение о неверном числе. |
| messages.item-saved | Сообщение о сохранении предмета. |
| messages.item-given | Сообщение о выдаче предмета. |

**Команды:** `/i name`, `/i lore`, `/i amount`, `/i enchant`, `/i attribute`, `/i save`, `/i give`, `/i reload`

#### Примеры использования:

```
/i name <название>              # Изменить название предмета
/i lore <описание>              # Изменить описание
/i amount <число>               # Изменить количество
/i enchant <зачарование> <уровень>  # Зачаровать
/i save <имя>                   # Сохранить предмет
/i give <ник> <предмет> [кол-во]  # Выдать предмет игроку
/i reload                       # Перезагрузить конфиг
```

**Права:** `managerfix.items.name`, `managerfix.items.lore`, `managerfix.items.amount`, `managerfix.items.enchant`, `managerfix.items.attribute`, `managerfix.items.save`, `managerfix.items.give`, `managerfix.items.reload`

---


### RTP (случайная телепортация)

**Назначение:** Телепорт в случайную безопасную точку в мире.

**Конфиг:** `modules/rtp.yml`

| Параметр | Описание |
|----------|----------|
| min-distance | Минимальное расстояние от центра (блоки). |
| max-distance | Максимальное расстояние. |
| cooldown | Кулдаун в секундах. |

**Команды:** `/rtp`  
**Права:** `managerfix.rtp.use`, `managerfix.rtp.bypass.cooldown`

---

### Spawn (спавн)

**Назначение:** Телепортация на спавн, GUI настроек, автоматический телепорт при входе/смерти.

**Конфиг:** `modules/spawn.yml`

| Параметр | Описание |
|----------|----------|
| settings.teleport-delay-seconds | Задержка перед телепортом. |
| settings.cancel-on-move | Отмена телепортации при движении. |
| settings.cancel-on-damage | Отмена телепортации при уроне. |
| settings.spawn-on-join | Телепорт на спавн при входе. |
| settings.spawn-on-death | Телепорт на спавн после смерти. |
| settings.spawn-first-join-only | Только при первом входе. |
| settings.safe-teleport | Проверка безопасности локации. |
| animation.enabled | Включение визуальных эффектов. |

**Команды:** `/spawn`, `/spawn edit`, `/setspawn`  
**Права:** `managerfix.spawn.use`, `managerfix.spawn.set`, `managerfix.spawn.edit`

---

### Tab (таб-лист)

**Назначение:** Кастомный header/footer с PlaceholderAPI.

**Конфиг:** `modules/tab.yml`

| Параметр | Описание |
|----------|----------|
| header | Текст сверху (MiniMessage). |
| footer | Текст снизу. |
| update-interval-ticks | Частота обновления. |

**Права:** `managerfix.tab.use`

---

### TPA (запросы телепортации)

**Назначение:** Запросы телепортации к игроку, принятие/отклонение, GUI.

**Конфиг:** `modules/tpa.yml`

| Параметр | Описание |
|----------|----------|
| request-timeout | Время жизни запроса (секунды). |
| teleport-delay | Задержка перед телепортом. |
| cooldown | Кулдаун между запросами. |
| cancel-on-move | Отменять ли телепорт при движении. |

**Команды:** `/tpa`, `/tpaccept`, `/tpdeny`  
**Права:** `managerfix.tpa.use`, `managerfix.tpa.bypass.cooldown`

---

### Warps (варпы)

**Назначение:** Варпы с GUI, кулдауном и правами на отдельные точки.

**Конфиг:** `modules/warps.yml`

| Параметр | Описание |
|----------|----------|
| default-permission | Право по умолчанию на варпы. |
| max-warps-per-player | Лимит варпов на игрока. |
| teleport-delay | Задержка телепорта. |
| cooldown | Кулдаун после использования. |
| icons | Иконки в GUI. |
| categories | Группировка варпов в GUI. |

**Команды:** `/warps`, `/warp`, `/setwarp`, `/delwarp`  
**Права:** `managerfix.warps.use`, `managerfix.warps.create`, `managerfix.warps.delete`, `managerfix.warps.edit`, `managerfix.warps.bypass.cooldown`, `managerfix.warps.warp.<имя>`

---

### Worlds (миры)

**Назначение:** Управление мирами: создание, удаление, телепорт, GUI.

**Конфиг:** `modules/worlds.yml`

| Параметр | Описание |
|----------|----------|
| default-generator | Генератор: default, flat, void. |
| allow-teleport | Разрешить телепорт. |
| allow-create | Разрешить создание. |
| allow-delete | Разрешить удаление. |

**Команды:** `/world` (GUI), `/world <мир>`, `/world tp`, `/world create`, `/world delete`  
**Права:** `managerfix.worlds.teleport`, `managerfix.worlds.create`, `managerfix.worlds.delete`

---

### Names (ники)

**Назначение:** Кастомные ники с админским GUI.

**Права:** `managerfix.names.use`, `managerfix.names.admin`

---

## Other (админ-утилиты)

**Назначение:** Пакет админских команд уровня Essentials. Содержит god/fly/gamemode, repair, invsee (с бронёй и второй рукой), vanish, back, weather/time, pinfo/seen, утилиты блоков, мобов и телепортацию (/tp top).

**Конфиг:** `modules/other.yml`

| Параметр | Описание |
|----------|----------|
| near-radius | Радиус команды /near. |
| log-admin-actions | Логировать админ-действия. |
| vanish-hide-from-tab | Скрывать из таба. |
| vanish-hide-join-quit | Скрывать join/quit. |
| vanish-persist | Сохранять vanish в профиле. |
| food-god-persist | Сохранять FoodGod. |
| aliases | Алиасы команд (map). |
| cooldowns | Кулдауны по ключам команд. |
| broadcast.* | Заголовок/сабтайтл/звук для /broadcast. |

**TeleportService:** если сервис уже зарегистрирован, используется он; иначе включается дефолтный с интеграцией TPA.

---

## Команды и права

**⚠️ Полный список всех команд** см. в отдельном файле [COMMANDS.md](COMMANDS.md)

Ниже приведены только основные команды модулей.

### Основные (ManagerFix)

| Команда | Описание | Права |
|---------|----------|-------|
| `/managerfix` | Главное меню или перезагрузка | `managerfix.menu`, `managerfix.reload` |
| `/managerfix menu` | Открыть меню модулей | `managerfix.menu` |
| `/managerfix reload` | Перезагрузить конфиг и модули | `managerfix.reload` |

### Ключевые команды модулей

| Модуль | Команды |
|--------|---------|
| **Warps** | `/warps`, `/warp <имя>`, `/setwarp`, `/delwarp`, `/editwarp` |
| **Homes** | `/sethome`, `/home`, `/delhome`, `/homes`, `/adminhomes` |
| **TPA** | `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpatoggle` |
| **Chat** | `/chattoggle`, `/pm`, `/r`, `/chatspy`, `/commandspy`, `/clearchat` |
| **Ban** | `/ban`, `/tempban`, `/unban`, `/banlist`, `/mute`, `/tempmute`, `/unmute`, `/kick` |
| **Kits** | `/kit`, `/kits` |
| **Items** | `/i name`, `/i lore`, `/i give`, `/i save` |
| **RTP** | `/rtp` |
| **Spawn** | `/spawn`, `/setspawn`, `/editspawn` |
| **Worlds** | `/world`, `/world tp`, `/world create`, `/world delete` |
| **Names** | `/nick`, `/nickadmin`, `/names` |
| **Other** | `/fly`, `/god`, `/heal`, `/feed`, `/repair`, `/vanish`, `/weather`, `/ping` |

**Универсальные права:**
- `managerfix.bypass.cooldown` — обход кулдаунов
- `managerfix.bypass.limit` — обход лимитов

### Kits

| Команда | Описание | Права |
|---------|----------|-------|
| /kit [имя] | Получить кит | managerfix.kits.use и managerfix.kits.kit.<имя> |
| /kits | Открыть GUI китов | managerfix.kits.use |
| /kit create <имя> | Создать кит | managerfix.kits.create |

### Worlds

| Команда | Описание | Права |
|---------|----------|-------|
| /world | GUI миров | managerfix.worlds.teleport |
| /world <мир> | Телепорт в мир (короткая форма) | managerfix.worlds.teleport |
| /world tp <мир> | Телепорт в мир | managerfix.worlds.teleport |
| /world create <мир> [generator] | Создать мир | managerfix.worlds.create |
| /world delete <мир> | Удалить мир | managerfix.worlds.delete |

### Names

| Команда | Описание | Права |
|---------|----------|-------|
| /nick <ник> | Установить ник | managerfix.names.nick |
| /nickadmin <игрок> <ник\|reset> | Установить/сбросить ник | managerfix.names.admin |
| /names | GUI администрирования ников | managerfix.names.admin |

Обходы: `managerfix.names.bypass.cooldown`, `managerfix.names.bypass.length`, `managerfix.names.bypass.format`

### Other (админ-утилиты)

| Команда | Описание | Права |
|---------|----------|-------|
| /god [игрок] | Неуязвимость | managerfix.other.god, managerfix.other.god.others |
| /fly [игрок] | Полёт | managerfix.other.fly, managerfix.other.fly.others |
| /gmc /gms /gmsp [игрок] | Смена режима | managerfix.other.gamemode.* + managerfix.other.gamemode.others |
| /repair [all] [игрок] | Ремонт предметов | managerfix.other.repair, managerfix.other.repair.all, managerfix.other.repair.others |
| /ec [игрок] | Эндер‑сундук | managerfix.other.ec, managerfix.other.ec.others |
| /invsee <игрок> | Просмотр инвентаря | managerfix.other.invsee, managerfix.other.invsee.modify |
| /workbench | Верстак | managerfix.other.workbench |
| /anvil | Наковальня | managerfix.other.anvil |
| /stonecutter | Камнерез | managerfix.other.stonecutter |
| /grindstone | Точило | managerfix.other.grindstone |
| /cartography | Картографический стол | managerfix.other.cartography |
| /loom | Ткацкий станок | managerfix.other.loom |
| /enchanting | Стол зачарований | managerfix.other.enchanting |
| /killmob <тип> <радиус> | Убить мобов в радиусе | managerfix.other.killmob |
| /spawnmob <тип> <кол-во> | Заспавнить мобов | managerfix.other.spawnmob |
| /tp to|here|location|top | Телепорт админов (без ожидания) | managerfix.other.tp, managerfix.other.tp.location |
| /pull <игрок> | Притянуть игрока | managerfix.other.pull |
| /push <игрок> | Телепорт к игроку | managerfix.other.push |
| /near | Игроки рядом | managerfix.other.near |
| /v | Vanish | managerfix.other.vanish |
| /back | Назад на прошлую точку | managerfix.other.back |
| /dback | Назад на место смерти | managerfix.other.dback |
| /weather <clear|rain|thunder> | Погода | managerfix.other.weather |
| /sun /rain /thunder | Быстрая погода | managerfix.other.weather |
| /day /night | Время | managerfix.other.time |
| /health [игрок] | Показать здоровье | managerfix.other.health, managerfix.other.health.others |
| /food [игрок] | Насыщение | managerfix.other.food, managerfix.other.food.others |
| /food god | FoodGod (не тратится голод) | managerfix.other.food.god |
| /clear [игрок] | Очистить инвентарь | managerfix.other.clear |
| /give <игрок> <предмет> <кол-во> | Выдать предмет | managerfix.other.give |
| /pinfo <игрок> | Информация об игроке | managerfix.other.info, managerfix.other.info.ip |
| /freeze <игрок> | Заморозить игрока | managerfix.other.freeze |
| /lockchat | Закрыть/открыть чат | managerfix.other.chatlock |
| /broadcast <сообщение> | Объявление в чат и title | managerfix.other.broadcast |
| /clearchat, /chatchlear | Очистить чат | managerfix.command.clearchat |
| /sudo <игрок> <команда> | Выполнить команду от игрока | managerfix.other.sudo |
| /ping [игрок] | Пинг игрока | managerfix.other.ping |
| /coords | Координаты | managerfix.other.coords |
| /seen <игрок> | Был в сети | managerfix.other.seen |
| /speed <значение> | Скорость ходьбы/полёта | managerfix.other.speed |

**Универсальные права:**  
- `managerfix.bypass.cooldown` — обход кулдаунов.  
- `managerfix.bypass.limit` — обход лимитов.

**Примечание о совместимости:** в `plugin.yml` присутствуют legacy‑узлы `managerfix.command.*`. Они оставлены для совместимости, но актуальная логика использует профильные разрешения `managerfix.<module>.*` и `managerfix.other.*`.

---

## Языковые файлы

Файлы лежат в `plugins/ManagerFix/lang/`, например `ru.yml`.  
Формат сообщений: **MiniMessage** (цвета, градиенты, теги).

Структура (сокращённо):

```yaml
messages:
  no-permission: "<red>У вас нет прав!"
  player-only: "<gray>Эта команда только для игроков."
menu:
  main-title: "<dark_gray>ManagerFix"
warps:
  not-found: "<red>Варп <white>{name}</white> не найден."
```

Плейсхолдеры в сообщениях задаются в формате `{имя}`, например `{player}`, `{name}`, `{reason}`. Язык по умолчанию задаётся в `config.yml` → `settings.default-language`.

---

## Хранилище данных

- **YAML** (`storage.type: YAML`) — профили и данные в файлах в папке плагина (в т.ч. `data/`).  
- **MYSQL** (`storage.type: MYSQL`) — профили (и при необходимости варпы) в MySQL/MariaDB.

Профили содержат: метаданные (AFK, vanish, chatspy, FoodGod), кулдауны, дома. Автосохранение задаётся в `settings.profile-autosave-minutes`.

---

## Кластер (Redis)

При `cluster.enabled: true` плагин подключается к Redis и синхронизирует события между серверами. Настройки в `config.yml` → `cluster`.

---

## PlaceholderAPI

При установленном PlaceholderAPI плагин регистрирует экспансию **managerfix**.

| Плейсхолдер | Описание |
|-------------|----------|
| %managerfix_afk% | `true` или `false` — в режиме ли AFK игрок. |

---

## API для разработчиков

Плагин регистрирует сервис **ManagerFixAPI** в `ServicesManager` Bukkit. Другие плагины могут получить API так:

```java
RegisteredServiceProvider<ManagerFixAPI> rsp = Bukkit.getServicesManager().getRegistration(ManagerFixAPI.class);
if (rsp != null) {
    ManagerFixAPI api = rsp.getProvider();
}
```

Внутри плагина используются: **EventBus**, **ModuleManager**, **ProfileManager**, **ServiceRegistry**, **TaskScheduler**, **GuiManager**.

---

*Документация актуальна для ManagerFix 1.0.0 (Paper 1.21.x, Java 21).*
