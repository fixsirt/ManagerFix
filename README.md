# ManagerFix — подробная документация

Подробная документация плагина **ManagerFix** для серверов Minecraft (Paper 1.21.4, Java 21).

---

## Содержание

1. [Описание](#описание)
2. [Требования](#требования)
3. [Установка и обновление](#установка-и-обновление)
4. [Сборка из исходников](#сборка-из-исходников)
5. [Структура файлов](#структура-файлов)
6. [Главный конфиг](#главный-конфиг)
7. [Модули](#модули)
8. [Other (админ-утилиты)](#other-админ-утилиты)
9. [Команды и права](#команды-и-права)
10. [Языковые файлы](#языковые-файлы)
11. [Хранилище данных](#хранилище-данных)
12. [Кластер (Redis)](#кластер-redis)
13. [PlaceholderAPI](#placeholderapi)
14. [API для разработчиков](#api-для-разработчиков)

---

## Описание

**ManagerFix** — модульный плагин для Paper 1.21.4 (Java 21), объединяющий типовые функции сервера: дома, варпы, киты, TPA, RTP, AFK, чат, баны, спавн, миры, таб-лист, объявления и расширенный административный пакет **Other**.

Особенности:

- **Модульность** — каждый функционал включается/выключается в конфиге или через GUI.
- **Единая архитектура** — модули, EventBus, сервисы, асинхронное хранилище (YAML/MySQL).
- **GUI** — меню варпов, домов, китов, банов, миров, TPA-подтверждение, главное меню.
- **Сообщения и настройки** — все тексты из языковых файлов (MiniMessage), настройки из `modules/*.yml`.
- **Опционально**: PlaceholderAPI, Vault (префиксы в чате), кластер Redis.

**Автор:** tg:fixsirt

---

## Требования

- **Сервер:** Paper 1.21.4
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
├── modules/                # Конфиги модулей
│   ├── afk.yml
│   ├── announcer.yml
│   ├── ban.yml
│   ├── chat.yml
│   ├── homes.yml
│   ├── kits.yml
│   ├── other.yml
│   ├── rtp.yml
│   ├── spawn.yml
│   ├── tab.yml
│   ├── tpa.yml
│   ├── warps.yml
│   └── worlds.yml
└── data/                   # Создаётся при первом использовании
    ├── warps.yml           # Варпы (при YAML-хранилище)
    ├── bans.yml            # Список банов
    └── ...
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

**Команды:** `/chattoggle`, `/pm`, `/tell`, `/msg`, `/r`, `/pmblock`, `/ignore`, `/chatspy`, `/commandspy`  
**Права:** `managerfix.chat.use`, `managerfix.chat.bypass.cooldown`, `managerfix.chat.spy`

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

**Команды:** `/world` (GUI), `/world tp`, `/world create`, `/world delete`  
**Права:** `managerfix.worlds.teleport`, `managerfix.worlds.create`, `managerfix.worlds.delete`

---

### Names (ники)

**Назначение:** Кастомные ники с админским GUI.

**Права:** `managerfix.names.use`, `managerfix.names.admin`

---

## Other (админ-утилиты)

**Назначение:** Пакет админских команд уровня Essentials. Содержит god/fly/gamemode, repair, invsee, vanish, back, weather/time, info/seen, staffmode, утилиты блоков, мобов и телепортацию.

**Конфиг:** `modules/other.yml`

| Параметр | Описание |
|----------|----------|
| near-radius | Радиус команды /near. |
| log-admin-actions | Логировать админ-действия. |
| vanish-hide-from-tab | Скрывать из таба. |
| vanish-hide-join-quit | Скрывать join/quit. |
| vanish-persist | Сохранять vanish в профиле. |
| food-god-persist | Сохранять FoodGod. |
| staffmode-enable-fly | Включать fly в staffmode. |
| staffmode-enable-god | Включать god в staffmode. |
| aliases | Алиасы команд (map). |
| cooldowns | Кулдауны по ключам команд. |
| broadcast.* | Заголовок/сабтайтл/звук для /broadcast. |

**TeleportService:** если сервис уже зарегистрирован, используется он; иначе включается дефолтный с интеграцией TPA.

---

## Команды и права

### Основные

| Команда | Описание | Право |
|---------|----------|--------|
| /managerfix [reload\|menu] | Перезагрузка конфига или открытие главного меню | managerfix.reload, managerfix.menu |
| /managerfix reload | Перезагрузка плагина и модулей | managerfix.reload |
| /managerfix menu | Главное меню | managerfix.menu |

### Варпы

| Команда | Право |
|---------|--------|
| /warps | managerfix.warps.use |
| /warp <имя> | managerfix.warps.use или managerfix.warps.warp.<имя> |
| /setwarp <имя> | managerfix.warps.create |
| /delwarp <имя> | managerfix.warps.delete |

### Дома

| Команда | Право |
|---------|--------|
| /sethome [имя] | managerfix.homes.set |
| /home [имя] | managerfix.homes.teleport |
| /delhome <имя> | managerfix.homes.delete |
| /homes | managerfix.homes.use |

### TPA

| Команда | Право |
|---------|--------|
| /tpa <игрок> | managerfix.tpa.use |
| /tpaccept | managerfix.tpa.use |
| /tpdeny | managerfix.tpa.use |

### Other (краткий список)

| Команда | Право |
|---------|--------|
| /god, /god <name> | managerfix.other.god, managerfix.other.god.others |
| /fly, /fly <name> | managerfix.other.fly, managerfix.other.fly.others |
| /gmc /gms /gmsp (+ <name>) | managerfix.other.gamemode.* + managerfix.other.gamemode.others |
| /repair [all] [player] | managerfix.other.repair, managerfix.other.repair.all, managerfix.other.repair.others |
| /ec [player] | managerfix.other.ec, managerfix.other.ec.others |
| /invsee <name> | managerfix.other.invsee, managerfix.other.invsee.modify |
| /workbench /anvil /stonecutter /grindstone /cartography /loom /enchanting | managerfix.other.<cmd> |
| /killmob <type> <radius> | managerfix.other.killmob |
| /spawnmob <type> <amount> | managerfix.other.spawnmob |
| /tp to|here|location | managerfix.other.tp, managerfix.other.tp.location |
| /near | managerfix.other.near |
| /v | managerfix.other.vanish |
| /back | managerfix.other.back |
| /dback | managerfix.other.dback |
| /weather /sun /rain /thunder | managerfix.other.weather |
| /day /night | managerfix.other.time |
| /health [name] | managerfix.other.health, managerfix.other.health.others |
| /food [name] | managerfix.other.food, managerfix.other.food.others |
| /food god | managerfix.other.food.god |
| /clear [name] | managerfix.other.clear |
| /give <name> <item> <amount> | managerfix.other.give |
| /info <name> | managerfix.other.info, managerfix.other.info.ip |
| /freeze <name> | managerfix.other.freeze |
| /lockchat | managerfix.other.chatlock |
| /broadcast <message> | managerfix.other.broadcast |
| /sudo <name> <command> | managerfix.other.sudo |
| /ping [name] | managerfix.other.ping |
| /coords | managerfix.other.coords |
| /seen <name> | managerfix.other.seen |
| /top | managerfix.other.top |
| /world <name> | managerfix.other.world |
| /pull <name> | managerfix.other.pull |
| /push <name> | managerfix.other.push |
| /speed <value> | managerfix.other.speed |
| /staffmode | managerfix.other.staffmode |

**Универсальные права:**  
- `managerfix.bypass.cooldown` — обход кулдаунов.  
- `managerfix.bypass.limit` — обход лимитов.

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

*Документация актуальна для ManagerFix 1.0.0 (Paper 1.21.4, Java 21).*
