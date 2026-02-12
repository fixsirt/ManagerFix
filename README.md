# ManagerFix

# ManagerFix — документация

Подробная документация плагина **ManagerFix** для серверов Minecraft (Paper 1.21+, Java 21).

---

## Содержание

1. [Описание](#описание)
2. [Требования](#требования)
3. [Установка](#установка)
4. [Структура файлов](#структура-файлов)
5. [Главный конфиг](#главный-конфиг)
6. [Модули](#модули)
7. [Команды и права](#команды-и-права)
8. [Языковые файлы](#языковые-файлы)
9. [Хранилище данных](#хранилище-данных)
10. [Кластер (Redis)](#кластер-redis)
11. [PlaceholderAPI](#placeholderapi)
12. [API для разработчиков](#api-для-разработчиков)

---

## Описание

**ManagerFix** — модульный плагин для Paper 1.21.4 (Java 21), объединяющий типовые функции сервера: дома, варпы, киты, TPA, RTP, AFK, чат, баны, спавны, миры, ванш, таб-лист, объявления и модерацию (GoodMod).

Особенности:

- **Модульность** — каждый функционал включается/выключается в конфиге или через GUI.
- **Единая архитектура** — модули, EventBus, сервисы, асинхронное хранилище (YAML/MySQL).
- **GUI** — меню варпов, домов, китов, банов, миров, TPA-подтверждение, главное меню.
- **Сообщения и настройки** — все тексты из языковых файлов (MiniMessage), настройки из `modules/*.yml`.
- **Опционально**: PlaceholderAPI, Vault (префиксы в чате), кластер Redis.

**Автор:** tg:fixsirt

---

## Требования

- **Сервер:** Paper 1.21+ (рекомендуется 1.21.4)
- **Java:** 21
- **Опционально:** PlaceholderAPI (для плейсхолдеров в чате, табе, объявлениях), Vault + плагин совместимости (префиксы в чате)

---

## Установка

1. Скачайте `ManagerFix-1.0.0.jar` (или актуальную версию).
2. Поместите JAR в папку `plugins/` сервера.
3. Перезапустите сервер или выполните `/reload` (предпочтительна полная перезагрузка при первом запуске).
4. Настройте `plugins/ManagerFix/config.yml` и при необходимости файлы в `plugins/ManagerFix/modules/`.
5. Языковой файл: `plugins/ManagerFix/lang/ru.yml` (русский по умолчанию).

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
│   ├── goodmod.yml
│   ├── homes.yml
│   ├── kits.yml
│   ├── rtp.yml
│   ├── spawns.yml
│   ├── tab.yml
│   ├── tpa.yml
│   ├── vanish.yml
│   ├── warps.yml
│   └── worlds.yml
└── data/                   # Создаётся при первом использовании
    ├── warps.yml           # Варпы (при YAML-хранилище)
    ├── spawns.yml          # Спавны по мирам
    ├── bans.yml            # Список банов
    └── ...
```

Профили игроков (дома, кулдауны, метаданные) и при необходимости варпы хранятся в БД, если в `config.yml` указано `storage.type: MYSQL`.

---

## Главный конфиг

Файл: `config.yml`

| Параметр | Описание |
|----------|----------|
| **modules** | Включение/выключение каждого модуля (`true`/`false`). Имена: warps, homes, spawns, chat, tpa, rtp, ban, afk, kits, worlds, vanish, tab, goodmod, announcer. |
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
**Права:** `managerfix.afk.use`, `managerfix.afk.bypass` (обход кика)  
**PlaceholderAPI:** `%managerfix_afk%` — true/false.

---

### Announcer (объявления)

**Назначение:** Периодическая рассылка сообщений в чат или в action bar.

**Конфиг:** `modules/announcer.yml`

| Параметр | Описание |
|----------|----------|
| interval-seconds | Интервал между сообщениями (в секундах). |
| messages | Список строк (MiniMessage). Поддерживается PlaceholderAPI для каждого игрока. |
| broadcast-type | `CHAT` или `ACTION_BAR`. |

Сообщения по очереди выводятся всем игрокам; для каждого игрока к тексту применяются плейсхолдеры PlaceholderAPI (если установлен).

---

### Ban (баны)

**Назначение:** Баны и временные баны, кик при входе, GUI со списком банов.

**Конфиг:** `modules/ban.yml`

| Параметр | Описание |
|----------|----------|
| default-duration | По умолчанию постоянный бан. |
| broadcast-bans | Оповещать ли о банах в чат. |
| kick-message | Текст кика (MiniMessage). Плейсхолдер: `{reason}`. |

**Команды:** `/ban <игрок> [причина]`, `/unban <игрок>`, `/tempban <игрок> <время> [причина]`, `/banlist` (GUI).  
**Время:** например `1d`, `2h`, `30m`.  
**Права:** `managerfix.ban.use`, `managerfix.ban.list`, `managerfix.ban.unban`

Данные банов хранятся в `data/bans.yml` (при YAML) или в БД при соответствующей настройке.

---

### Chat (чат)

**Назначение:** Формат сообщений, локальный/глобальный чат, анти-спам, опционально префиксы Vault.

**Конфиг:** `modules/chat.yml`

| Параметр | Описание |
|----------|----------|
| format | Формат сообщения (MiniMessage). Плейсхолдеры: `{player}`, `{message}`, при наличии Vault — `{prefix}`, `{suffix}`. |
| local-radius | Радиус локального чата (блоки). 0 — только глобальный. |
| spam-cooldown | Кулдаун между сообщениями (секунды). 0 — отключено. |
| colors-enabled | Разрешать ли цветовые коды (без отдельного права). |

**Команды:** `/chattoggle` — переключение локальный/глобальный чат (если включён local-radius).  
**Права:** `managerfix.chat.use`, `managerfix.chat.bypass.cooldown`, `managerfix.chat.color`

---

### GoodMod (модерация)

**Назначение:** Мут, размут, очистка чата, SocialSpy.

**Конфиг:** `modules/goodmod.yml`

| Параметр | Описание |
|----------|----------|
| default-mute-time | Время мута по умолчанию (например `10m`, `1d`). |
| clearchat-lines | Сколько пустых строк отправить при очистке чата. |

**Команды:** `/mute <игрок> [время]`, `/unmute <игрок>`, `/clearchat`, `/socialspy`  
**Права:** `managerfix.goodmod.use`

Мут хранится в профиле игрока (метаданные), проверяется при отправке сообщения в чат.

---

### Homes (дома)

**Назначение:** Точки «дом» с лимитом, задержкой и кулдауном телепортации.

**Конфиг:** `modules/homes.yml`

| Параметр | Описание |
|----------|----------|
| max-homes | Максимум домов на игрока (можно переопределять правами managerfix.homes.limit.1, .3, .5, .10, .20). |
| teleport-delay | Задержка перед телепортом (секунды). Отмена при движении. |
| cooldown | Кулдаун между телепортами (секунды). |

**Команды:** `/sethome [имя]`, `/home [имя]`, `/delhome <имя>`, `/homes` (GUI).  
**Права:** `managerfix.homes.use`, `managerfix.homes.set`, `managerfix.homes.delete`, `managerfix.homes.teleport`, `managerfix.homes.rename`, `managerfix.homes.bypass.cooldown`, `managerfix.homes.limit.N`

В GUI: ЛКМ — телепорт, ПКМ — удалить, Shift+ПКМ — подсказка по переименованию.

---

### Kits (киты)

**Назначение:** Выдача наборов предметов по команде или из GUI с кулдауном.

**Конфиг:** `modules/kits.yml`

| Параметр | Описание |
|----------|----------|
| default-cooldown | Кулдаун по умолчанию (секунды), например 86400 (сутки). |

Киты задаются через команду (например `/kit create <имя>` при наличии прав) или в конфиге/хранилище.  
**Команды:** `/kit [имя]`, `/kits` (GUI).  
**Права:** `managerfix.kits.use`, `managerfix.kits.create`, `managerfix.kits.kit.<имя>` — доступ к конкретному киту.

---

### RTP (случайная телепортация)

**Назначение:** Телепорт в случайную безопасную точку в мире.

**Конфиг:** `modules/rtp.yml`

| Параметр | Описание |
|----------|----------|
| min-distance | Минимальное расстояние от начала координат (блоки). |
| max-distance | Максимальное расстояние. |
| cooldown | Кулдаун в секундах. |

**Команды:** `/rtp`  
**Права:** `managerfix.rtp.use`, `managerfix.rtp.bypass.cooldown`

---

### Spawns (спавны)

**Назначение:** Спавн по миру, телепорт на спавн с задержкой и отменой при движении.

**Конфиг:** `modules/spawns.yml`

| Параметр | Описание |
|----------|----------|
| teleport-delay | Задержка перед телепортом (секунды). |

**Команды:** `/spawn`, `/setspawn`  
**Права:** `managerfix.spawns.teleport`, `managerfix.spawns.set`

Спавны хранятся в `data/spawns.yml` (по мирам).

---

### Tab (таб-лист)

**Назначение:** Кастомный header и footer в таб-листе с поддержкой PlaceholderAPI.

**Конфиг:** `modules/tab.yml`

| Параметр | Описание |
|----------|----------|
| header | Текст сверху (MiniMessage). |
| footer | Текст снизу. |
| update-interval-ticks | Как часто обновлять (тики, 20 = 1 сек). |

**Права:** `managerfix.tab.use`

---

### TPA (запросы телепортации)

**Назначение:** Запрос телепорта к игроку, принятие/отклонение командами или через GUI.

**Конфиг:** `modules/tpa.yml`

| Параметр | Описание |
|----------|----------|
| request-timeout | Время жизни запроса (секунды). |
| teleport-delay | Задержка перед телепортом после принятия. |
| cooldown | Кулдаун между запросами (секунды). |
| cancel-on-move | Отменять ли телепорт при движении после принятия. |

**Команды:** `/tpa <игрок>`, `/tpaccept`, `/tpdeny`  
**Права:** `managerfix.tpa.use`, `managerfix.tpa.bypass.cooldown`

При получении запроса игроку может открываться GUI «Принять» / «Отклонить».

---

### Vanish (исчезновение)

**Назначение:** Скрытие игрока от других (невидимость, скрытие в табе, сообщений входа/выхода). Состояние сохраняется в профиле и восстанавливается при повторном входе.

**Конфиг:** `modules/vanish.yml`

| Параметр | Описание |
|----------|----------|
| permission | Право на использование ванша. |
| show-in-tab | Показывать ли в таб-листе. |

**Команды:** `/vanish`  
**Права:** `managerfix.vanish.use` (или managerfix.command.vanish)

---

### Warps (варпы)

**Назначение:** Точки телепортации с созданием/удалением, GUI, кулдауном и правами на отдельные варпы.

**Конфиг:** `modules/warps.yml`

| Параметр | Описание |
|----------|----------|
| default-permission | Право по умолчанию на использование варпов. |
| max-warps-per-player | Лимит варпов на игрока (0 — без лимита). |
| teleport-delay | Задержка телепорта (секунды). |
| cooldown | Кулдаун после использования варпа. |
| icons | Иконки в GUI: default (материал), а также icons.<имя_варпа> для переопределения. |
| categories | (Опционально) Группировка варпов в GUI. |

**Команды:** `/warps` (GUI или подкоманды), `/warp <имя>`, `/setwarp <имя>`, `/delwarp <имя>`  
**Права:** `managerfix.warps.use`, `managerfix.warps.create`, `managerfix.warps.delete`, `managerfix.warps.edit`, `managerfix.warps.bypass.cooldown`, `managerfix.warps.warp.<имя>` — доступ к конкретному варпу.

В GUI: ЛКМ — телепорт, ПКМ — удалить, Shift+ПКМ — изменить позицию.

---

### Worlds (миры)

**Назначение:** Управление мирами: создание, удаление, телепорт, GUI со списком миров.

**Конфиг:** `modules/worlds.yml`

| Параметр | Описание |
|----------|----------|
| default-generator | Генератор по умолчанию: default, flat, void. |
| allow-teleport | Разрешить телепорт в миры (GUI и /world tp). |
| allow-create | Разрешить создание миров. |
| allow-delete | Разрешить удаление миров (опасно). |

**Команды:** `/world` — открыть GUI списка миров; `/world tp <имя>` — телепорт в мир; `/world create <имя> [генератор]`; `/world delete <имя>`  
**Права:** `managerfix.worlds.teleport`, `managerfix.worlds.create`, `managerfix.worlds.delete`

В GUI отображаются все загруженные миры; клик — телепорт на спавн мира.

---

## Команды и права

### Основные

| Команда | Описание | Право |
|---------|----------|--------|
| /managerfix [reload\|menu] | Перезагрузка конфига или открытие главного меню | managerfix.reload, managerfix.menu |
| /managerfix reload | Перезагрузка плагина и модулей | managerfix.reload |
| /managerfix menu | Главное меню (модули, варпы, дома и т.д.) | managerfix.menu |

В главном меню Shift+ПКМ по модулю — включение/выключение модуля (требуется managerfix.module.toggle).

### Варпы

| Команда | Право |
|---------|--------|
| /warps | managerfix.warps.use |
| /warps create <имя> | managerfix.warps.create |
| /warps delete <имя> | managerfix.warps.delete |
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

### Остальные команды

| Команда | Право |
|---------|--------|
| /rtp | managerfix.rtp.use |
| /kit [имя], /kits | managerfix.kits.use, managerfix.kits.kit.<имя> |
| /spawn, /setspawn | managerfix.spawns.teleport, managerfix.spawns.set |
| /afk | managerfix.afk.use |
| /ban, /unban, /tempban, /banlist | managerfix.ban.use, managerfix.ban.unban, managerfix.ban.list |
| /chattoggle | managerfix.chat.use |
| /mute, /unmute, /clearchat, /socialspy | managerfix.goodmod.use |
| /world, /world tp/create/delete | managerfix.worlds.teleport/create/delete |
| /fly, /heal, /feed, /god, /workbench, /enderchest | managerfix.command.<команда> |
| /vanish, /invsee, /ecsee, /sudo | managerfix.command.vanish/invsee/ecsee/sudo |

**Универсальные права:**  
- `managerfix.bypass.cooldown` — обход кулдаунов.  
- `managerfix.bypass.limit` — обход лимитов (дома, варпы и т.п.).

---

## Языковые файлы

Файлы лежат в `plugins/ManagerFix/lang/`, например `ru.yml`.  
Формат сообщений: **MiniMessage** (цвета, градиенты, теги). Документация: [Adventure MiniMessage](https://docs.adventure.kyori.net/minimessage/format.html).

Структура (сокращённо):

```yaml
messages:
  no-permission: "<red>У вас нет прав!"
  player-only: "<gray>Эта команда только для игроков."
  # ...
menu:
  main-title: "<dark_gray>ManagerFix"
  warps-title: "<dark_gray>Варпы"
  # ...
warps:
  not-found: "<red>Варп <white>{name}</white> не найден."
  teleported: "<green>Телепортация на варп <white>{name}</white>."
  # ...
```

Плейсхолдеры в сообщениях задаются в формате `{имя}`, например `{player}`, `{name}`, `{reason}`. Язык по умолчанию задаётся в `config.yml` → `settings.default-language`.

---

## Хранилище данных

- **YAML** (`storage.type: YAML`) — профили и данные в файлах в папке плагина (в т.ч. `data/`).  
- **MYSQL** (`storage.type: MYSQL`) — профили (и при реализации — варпы и др.) в MySQL/MariaDB. Параметры в `config.yml` → `database`.

Профили содержат: метаданные (AFK, vanish, mute, chat_local, socialspy), кулдауны, дома. Автосохранение задаётся в `settings.profile-autosave-minutes`.

---

## Кластер (Redis)

При `cluster.enabled: true` плагин подключается к Redis и может синхронизировать события между серверами (например, баны, варпы — в зависимости от реализации). Настройки в `config.yml` → `cluster`: server-id, redis (host, port, password, channel-prefix).

---

## PlaceholderAPI

При установленном PlaceholderAPI плагин регистрирует экспансию **managerfix**.

| Плейсхолдер | Описание |
|-------------|----------|
| %managerfix_afk% | `true` или `false` — в режиме ли AFK игрок. |

В сообщениях чата, таба и объявлений можно использовать любые плейсхолдеры PlaceholderAPI (например `%server_online%` в табе).

---

## API для разработчиков

Плагин регистрирует сервис **ManagerFixAPI** в `ServicesManager` Bukkit. Другие плагины могут получить API так:

```java
RegisteredServiceProvider<ManagerFixAPI> rsp = Bukkit.getServicesManager().getRegistration(ManagerFixAPI.class);
if (rsp != null) {
    ManagerFixAPI api = rsp.getProvider();
    // проверка модулей, получение менеджеров и т.д.
}
```

Внутри плагина используются: **EventBus** (события ManagerFix), **ModuleManager**, **ProfileManager**, **ServiceRegistry**, **TaskScheduler**, **GuiManager**. События (например, AfkEnterEvent, PlayerBanEvent, HomeTeleportEvent, RTPEvent) позволяют интегрироваться с другими плагинами без изменения ядра.

---

*Документация актуальна для ManagerFix 1.0.0 (Paper 1.21.4, Java 21).*
