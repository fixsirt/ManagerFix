# ManagerFix — документация по модулям

> Версия плагинов: **2.0.0** · Ядро: **FixCore** · Для **Paper/Purpur 1.21+**
> Автор: tg:fixsirt

ManagerFix — это набор независимых модульных плагинов. Есть **ядро** (`FixCore`) и **16 самостоятельных модулей** (`MF-*`). Каждый модуль — отдельный `.jar`, работает **в паре только с ядром** (ядро + один модуль достаточно, остальные модули не обязательны).

---

## Содержание

1. [Установка и запуск](#установка-и-запуск)
2. [Ядро FixCore](#ядро-fixcore)
   - [Команды ядра](#команды-ядра)
   - [Перезагрузка модулей `/fixcore reload`](#перезагрузка-модулей-fixcore-reload)
   - [Плейсхолдеры](#плейсхолдеры)
   - [Конфигурация ядра](#конфигурация-ядра)
3. [Модули](#модули)
   - [MF-Afk](#mf-afk)
   - [MF-Announcer](#mf-announcer)
   - [MF-Ban](#mf-ban)
   - [MF-Chat](#mf-chat)
   - [MF-Homes](#mf-homes)
   - [MF-Items](#mf-items)
   - [MF-Kits](#mf-kits)
   - [MF-LiveBoard](#mf-liveboard)
   - [MF-Names](#mf-names)
   - [MF-Other](#mf-other)
   - [MF-Rtp](#mf-rtp)
   - [MF-Spawn](#mf-spawn)
   - [MF-Tab](#mf-tab)
   - [MF-Tpa](#mf-tpa)
   - [MF-Warps](#mf-warps)
   - [MF-Worlds](#mf-worlds)
4. [Полезные советы администратору](#полезные-советы-администратору)

---

## Установка и запуск

1. Положите в папку `plugins/` сервера:
   - **обязательно** — `FixCore-2.0.0.jar`;
   - **любые нужные модули** — `MF-*.jar` (можно один, можно все).
2. Перезапустите сервер (модули нельзя «подключить» командой — нужен рестарт или `reload`).
3. Проверьте, что в консоли появилось `Enabling MF-<Имя> v2.0.0`.

Важно:

- **Любой модуль работает отдельно** — достаточно ядра и его самого. Не обязательно ставить всю линейку.
- Если модуль **не установлен**, его команды недоступны (команды не зарегистрированы вообще).
- Модуль можно также **отключить** в конфиге ядра `plugins/FixCore/config.yml → modules` (см. [Конфигурация ядра](#конфигурация-ядра)).
- Базовые права почти у всех модулей по умолчанию выданы только **операторам** (`default: op`). Чтобы обычные игроки могли пользоваться командами (например, `/home`, `/spawn`, `/rtp`, `/afk`, `/tpa`), выдайте им их права через LuckPerms.
- Необязательные зависимости модулей: **Vault** (экономика/префиксы), **LuckPerms** (права и приоритеты), **PlaceholderAPI** (плейсхолдеры). Без них модули всё равно работают, но часть функций отключается.
- **ClansFix / ClansFixRaids** (кланы и рейды) подключаются к этому же ядру `FixCore` — устанавливайте их вместе с ним.

---

## Ядро FixCore

Ядро предоставляет общие сервисы для всех модулей: единый конфиг, базу данных (SQLite/MySQL/YAML), языковые файлы, GUI-менеджер, систему профилей, плейсхолдеры.

### Команды ядра

| Команда | Описание | Права |
|---|---|---|
| `/managerfix` | Информация о плагине | `managerfix.command.managerfix` |
| `/fixcore` | То же, что `/managerfix` (алиас) | `managerfix.command.managerfix` |
| `/managerfix menu` | Главное меню (включение/админ-меню модулей) | `managerfix.menu` + `managerfix.admin` для полного меню |
| `/managerfix reload [all\|<модуль>]` | Перезагрузка конфигов (см. ниже) | `managerfix.reload` |

Главное меню (`/managerfix menu`): открывает панель управления модулями, оттуда можно попасть в админ-GUI модулей (ники, киты, варпы, дома, миры и т.д.).

### Перезагрузка модулей `/fixcore reload`

Перезагружает конфиги ядра и модулей **без перезапуска сервера**:

```
/fixcore reload                 # перезагрузить всё (все установленные модули)
/fixcore reload all             # то же самое
/fixcore reload afk             # перезагрузить один конкретный модуль
/fixcore reload liveboard       # например, LiveBoard
```

Подсказка: `/fixcore reload <модуль>` сработает **только если модуль установлен** на сервере — если нет, будет сообщение «Модуль … не установлен или не найден». Таб показывает только установленные модули. Можно писать короткое имя (`afk`) или полное (`mf-afk`).

### Плейсхолдеры

**Встроенные (работают всегда, даже без PlaceholderAPI).** Доступны в конфигах, табе, LiveBoard, анонсах и т.д.:

| Плейсхолдер | Значение |
|---|---|
| `%player%`, `%player_name%`, `%name%` | Ник игрока |
| `%player_displayname%`, `%displayname%` | Отображаемое имя (ник с цветами) |
| `%player_ping%`, `%ping%` | Пинг (мс) |
| `%player_world%`, `%world%` | Название мира |
| `%uuid%` | UUID игрока |
| `%online%`, `%server_online%`, `%online_total%` | Кол-во игроков онлайн |
| `%max_online%` | Макс. онлайн |
| `%server_tps%`, `%server_tps_1%` … `%server_tps_3%` | TPS сервера |
| `%server_time%` | Время на сервере (ЧЧ:мм) |

**Расширение `%managerfix_*%` (через PlaceholderAPI).** Нужен установленный PlaceholderAPI:

| Плейсхолдер | Значение |
|---|---|
| `%managerfix_afk%` | Игрок в AFK? (`true`/`false`) |
| `%managerfix_name%` | Кастомный ник (без префикса) |
| `%managerfix_displayname%` | Ник с префиксом |
| `%managerfix_pt_name_1%` … `%managerfix_pt_name_50%` | Топ-50 по времени онлайн (ник) |
| `%managerfix_pt_time_1%` … `%managerfix_pt_time_50%` | Топ-50 по времени онлайн (время) |
| `%managerfix_rep_rating%` / `%managerfix_rep_positive%` / `%managerfix_rep_negative%` | Репутация игрока |
| `%managerfix_rep_name_1%` … `%managerfix_rep_name_50%` | Топ репутации (ник) |
| `%managerfix_rep_rating_1%` … `%managerfix_rep_rating_50%` | Топ репутации (рейтинг) |

Плюс все стандартные PAPI-плейсхолдеры других плагинов: `%luckperms_prefix%`, `%vault_prefix%` и др.

### Конфигурация ядра

Файл: `plugins/FixCore/config.yml`

```yaml
# Включение/выключение модулей (даже если jar установлен — отключённый модуль не запустится)
modules:
  warps: true
  homes: true
  spawn: true
  chat: true
  tpa: true
  rtp: true
  ban: true
  afk: true
  kits: true
  worlds: true
  other: true
  tab: true
  announcer: true
  names: true
  items: true
  liveboard: true

# Хранилище данных: YAML, MYSQL или SQLITE (общее для всех модулей)
storage:
  type: SQLITE
  # database: { host, port, database, username, password, pool-size } — если MYSQL

settings:
  default-language: ru     # язык сообщений (папка plugins/FixCore/lang/)
  debug: false
```

Файлы данных модулей: `plugins/FixCore/data/` (`playtime`, дома, варпы, бан-лист, репутация и т.д.), конфиги — `plugins/FixCore/modules/<модуль>/`.

---

## Модули

# MF-Afk

**Время в AFK не учитывается в игровом времени (playtime).**

Возможности:

- Автоматический AFK после бездействия (таймаут настраивается).
- Опциональный кик за бездействие.
- Оповещение сервера при входе/выходе из AFK.
- Запрет команд в AFK.
- Учёт игрового времени (playtime), автосохранение в БД, топ онлайн.

Команды:

| Команда | Описание | Алиасы | Права |
|---|---|---|---|
| `/afk` | Переключить статус AFK | `away` | `managerfix.command.afk` |
| `/top` | Топ-листы (в т.ч. топ по времени) | — | `managerfix.command.top` |
| `/playtime [ник]` | Время игры | — | (см. модуль) |

Ключевые права: `managerfix.afk.use`, `managerfix.afk.bypass` (обход кика за AFK).

Конфиг: `plugins/FixCore/modules/afk/config.yml` — `afk-timeout-seconds`, `kick-timeout-seconds`, `broadcast-afk`, `block-commands-while-afk`, `playtime` (автосброс, размер топа).

[↑ к содержанию](#содержание)

---

# MF-Announcer

Анонсы и служебные сообщения для игроков.

Возможности:

- Ротация анонсов по таймеру (MiniMessage + HEX + PAPI, кликабельные/ховер-сообщения).
- Трансляция в чат или в action bar.
- Приветствие новому игроку при входе.
- Замена ванильных сообщений: вход, выход, смерть (в т.ч. «убит игроком»).
- Отключение ванильных сообщений о достижениях и командах.
- Кастомное сообщение «Неизвестная команда».

Команды: отдельных команд нет. Управление — через конфиг и права `managerfix.announcer.reload`.

Конфиг: `plugins/FixCore/modules/announcer/config.yml` — `interval-seconds`, `messages`, `broadcast-type` (`CHAT`/`ACTION_BAR`), `welcome`, `custom-join`, `custom-quit`, `custom-death`, `disable-advancement-messages`, `custom-unknown-command`, `send-command-feedback`.

[↑ к содержанию](#содержание)

---

# MF-Ban

Модерация: баны (по нику и IP), муты, кики, GUI-список банов.

Возможности:

- Бан/разбан по нику (срок и причина), бан по IP, мут/размут, кик.
- GUI со списком банов (`/banlist`).
- Трансляции бан/кик/мут/разбан в чат (форматы настраиваются, MiniMessage).
- Красивые сообщения «Вы забанены» с причиной, админом, сроком и IP.
- Приоритет групп LuckPerms: игрок с меньшим приоритетом не может наказывать с большим.

Команды:

| Команда | Описание | Алиасы | Права |
|---|---|---|---|
| `/ban <ник> [срок] [причина]` | Забанить | — | `managerfix.ban.use` |
| `/unban <ник>` | Разбанить | `pardon` | `managerfix.ban.unban` |
| `/banlist` | Список банов (GUI) | `bans` | `managerfix.ban.list` |
| `/banip <ник> <срок> <причина>` | Бан по IP | `ipban`, `ban-ip` | `managerfix.ban.ip` |
| `/unbanip <ip\|ник>` | Разбан по IP | `ipunban`, `unban-ip`, `pardon-ip` | `managerfix.ban.ip.unban` |
| `/mute <ник> [срок] [причина]` | Замутить | — | `managerfix.ban.mute` |
| `/unmute <ник>` | Размутить | — | `managerfix.ban.mute` |
| `/kick <ник> [причина]` | Кикнуть | — | `managerfix.ban.kick` |

Конфиг: `plugins/FixCore/modules/ban/config.yml` — сообщения и форматы, `broadcast-*`, `group-priority`.

[↑ к содержанию](#содержание)

---

# MF-Chat

Кастомизация чата: локальный/глобальный чат, ЛС, спай, фильтр мата.

Возможности:

- **Локальный чат по радиусу** (`local-radius`), глобальная трансляция через префикс `!` (`!привет`).
- Бейджи локального/глобального чата и ЛС.
- Форматы чата и ЛС — полностью настраиваемые (MiniMessage, HEX, Vault-префиксы, PAPI).
- ЛС: `/msg`, `/pm`, `/tell` + быстрый ответ `/r`; блокировка/жалобы (`/pmblock`, `/ignore`).
- Звуки при отправке/получении ЛС и сообщений в чат.
- Клик по ЛС — подстановка `/pm <ник>`, hover с балансом (Vault) и временем.
- **Спай-режимы**: `/chatspy` (видеть чат вне радиуса) и `/commandspy` (команды игроков).
- Очистка чата с анимацией, объявления `/broadcast` (с титулом и звуком).
- Спам-кулдаун, цвета игрокам, фильтр мата (`/filter`, отдельный файл `filter.yml`).

Команды:

| Команда | Описание | Алиасы | Права |
|---|---|---|---|
| `/chatspy` | Спай чата (видеть всё) | `cs` | `managerfix.chat.spy` |
| `/commandspy` | Спай команд | `cmds` | `managerfix.chat.commandspy` |
| `/msg <ник> <сообщение>` | ЛС | `pm`, `tell` | `managerfix.command.pm` |
| `/r <сообщение>` | Ответ на последнее ЛС | — | `managerfix.command.reply` |
| `/pmblock <ник>` | Заблокировать ЛС от игрока | — | `managerfix.chat.pmblock` |
| `/ignore [add\|remove\|list] [ник]` | Управление игнором | — | `managerfix.command.ignore` |
| `/clearchat` | Очистить чат | `chatchlear`, `cc` | `managerfix.command.clearchat` |
| `/broadcast <сообщение>` | Объявление | `bc` | `managerfix.command.broadcast` |
| `/filter [reload\|toggle\|status]` | Управление фильтром мата | — | `managerfix.chat.filter` |

Конфиг: `plugins/FixCore/modules/chat/config.yml` (+ `filter.yml`).

[↑ к содержанию](#содержание)

---

# MF-Homes

Дома игроков с GUI.

Возможности:

- Установка, телепорт, удаление домов; **GUI всех домов** (`/homes`) с переименованием (Shift+ПКМ).
- Админ-режим: просмотр/удаление чужих домов, установка дома другому игроку.
- Лимиты домов: по правам и по группам (`group-limits` в конфиге).
- Телепорт с задержкой, отмена при уроне, звуки, обратный отсчёт в титуле/action bar.

Команды:

| Команда | Описание | Права |
|---|---|---|
| `/sethome [название]` | Установить дом | `managerfix.homes.set` |
| `/home [название]` | Телепорт домой | `managerfix.homes.teleport` |
| `/delhome <название>` | Удалить дом | `managerfix.homes.delete` |
| `/homes` | GUI домов | `managerfix.homes.use` |
| `/adminhomes <ник>` | GUI домов игрока (админ) | `managerfix.homes.admin` |
| `/adminsethome <ник> <название>` | Установить дом игроку | `managerfix.homes.admin.set` |

Лимиты по правам: `managerfix.homes.limit.1/3/5/10/20`. Обход кулдауна: `managerfix.homes.bypass.cooldown`.

Конфиг: `plugins/FixCore/modules/homes/config.yml` — `max-homes`, `group-limits`, `teleport-delay`, `cooldown`, `cancel-on-damage`, звуки.

[↑ к содержанию](#содержание)

---

# MF-Items

Работа с предметом в руке.

Возможности:

- Переименование, изменение описания и количества предмета.
- Добавление зачарований и атрибутов.
- Сохранение предмета под именем и выдача сохранённых предметов.
- Перезагрузка конфига.

Команда `/i` (субкоманды):

| Команда | Описание |
|---|---|
| `/i name <название>` | Изменить название предмета в руке |
| `/i lore <описание>` | Изменить описание (`\n` — новая строка) |
| `/i amount <число>` | Изменить количество |
| `/i enchant add <чара> [уровень]` | Добавить зачарование |
| `/i attribute add <атрибут> <значение>` | Добавить атрибут |
| `/i save <имя>` | Сохранить предмет |
| `/i give <ник> <имя> [количество]` | Выдать сохранённый предмет |
| `/i reload` | Перезагрузить конфиг |

Права: `managerfix.items.use`, `managerfix.items.name`, `managerfix.items.lore`, `managerfix.items.amount`, `managerfix.items.enchant`, `managerfix.items.attribute`, `managerfix.items.save`, `managerfix.items.give`.

Конфиг: `plugins/FixCore/modules/items/config.yml`.

[↑ к содержанию](#содержание)

---

# MF-Kits

Наборы предметов (киты) с GUI.

Возможности:

- Выдача кита по названию или из GUI.
- **Редактор китов** (админ): создание/правка наборов прямо с инвентарём.
- Кулдауны китов (по умолчанию 1 день), доступ к киту по праву `managerfix.kits.kit.<название>`.

Команды:

| Команда | Описание | Права |
|---|---|---|
| `/kit [название]` | Получить кит | `managerfix.kits.use` + `managerfix.kits.kit` или `managerfix.kits.kit.<name>` |
| `/kits` | GUI китов | `managerfix.kits.use` |
| `/editkits` | Админ-редактор китов | `managerfix.kits.create` |

Конфиг: `plugins/FixCore/modules/kits/config.yml` — `default-cooldown`.

[↑ к содержанию](#содержание)

---

# MF-LiveBoard

Живой скорборд (изменяемый) для сервера.

Возможности:

- Настраиваемый заголовок, линии и ширина скорборда.
- Плейсхолдеры в линиях (встроенные + PAPI).
- Период обновления (в тиках).
- Игрок включает/выключает скорборд себе.

Команда:

| Команда | Описание | Алиасы | Права |
|---|---|---|---|
| `/liveboard` | Вкл/выкл скорборд | `lb`, `scoreboard`, `sb` | `liveboard.use` (по умолчанию у всех) |

Конфиг: `plugins/FixCore/modules/liveboard/config.yml` — `title`, `lines`, `update-interval`, `width`, `hide-numbers`.

[↑ к содержанию](#содержание)

---

# MF-Names

Ники (кастомные имена) и тэг над головой.

Возможности:

- Смена ника себе (`/nick`) и другим игрокам (`/nickadmin`).
- **Ник-тэг над головой** (TextDisplay): префикс + ник, HP и пинг под ником с цветами по порогам.
- Отображение ника в табе (используется вместе с [MF-Tab](#mf-tab)) и в чате (используется вместе с [MF-Chat](#mf-chat) — подставляется чистый ник без префикса).
- Админ-GUI (`/names`) для управления никами.
- Скрытие своего ника над головой (`/hidenick`).
- Кулдаун смены, ограничение длины, HEX-цвета, запрет плохих форматов.

Команды:

| Команда | Описание | Алиасы | Права |
|---|---|---|---|
| `/nick [ник/clear]` | Сменить ник | `nickname` | `managerfix.names.nick` |
| `/nickadmin <ник> [ник/clear]` | Сменить ник игроку | `anick`, `adminnick` | `managerfix.names.admin` |
| `/names` | Админ-GUI ников | — | `managerfix.names.admin` |
| `/hidenick` | Скрыть/показать ник над головой | — | `managerfix.command.hidenick` |

Права-обходы: `managerfix.names.bypass.cooldown`, `managerfix.names.bypass.length`, `managerfix.names.bypass.format`.

Конфиг: `plugins/FixCore/modules/names/config.yml` — `nickname-cooldown-seconds`, `max-length`, `allow-hex`, `display-format`, `nametag-offset-y`, `show-hp-under-nick`, `hp-format`, `ping-thresholds`, `text-display`.

[↑ к содержанию](#содержание)

---

# MF-Other

«Утилиты» для админов и игроков + репутация.

Возможности:

- God-режим, полёт, смена гейммодов (`/gmc`, `/gms`, `/gmsp`).
- Починка, виртуальные станки (верстак, наковальня, камнерез и т.д.), эндер-сундук.
- Телепорты: `/tp`, `/back`, `/dback` (на место смерти), `/near`, `/tpahere`.
- Ваниш (в т.ч. скрытие из таба и из сообщений о входе/выходе).
- Инвентари: `/invsee`, `/ecsee` (в т.ч. модификация), `/clear`, `/give`.
- Модерация: `/freeze`, `/lockchat`, `/sudo`, `/killmob`, `/spawnmob`.
- Погода и время: `/weather`, `/day`, `/night`, `/sun`, `/rain`, `/thunder`.
- Инфо: `/pinfo`, `/ping`, `/seen`, `/health`, `/food`, `/coords`, `/speed`.
- **Репутация**: `/rate`/`/rep` (оценить игрока), `/adminrep set|add|take` (админ).

Команды (основные):

| Команда | Описание | Алиасы | Права |
|---|---|---|---|
| `/god [ник]` | God-режим | — | `managerfix.other.god` |
| `/fly [ник]` | Полёт | — | `managerfix.other.fly` |
| `/gmc` `/gms` `/gmsp` `[ник]` | Смена гейммода | `creative` и т.д. | `managerfix.other.gamemode.*` |
| `/repair [all] [ник]` | Починить предмет(ы) | — | `managerfix.other.repair` |
| `/workbench` `/anvil` `/stonecutter` `/grindstone` `/cartography` `/loom` `/enchanting` | Виртуальные станки | — | `managerfix.other.*` |
| `/ec [ник]` | Эндер-сундук | `enderchest` | `managerfix.other.ec` |
| `/tp to\|here\|location\|top` | Телепорт | — | `managerfix.other.tp` |
| `/near` | Игроки рядом | `nearby` | `managerfix.other.near` |
| `/v` | Ваниш | `vanish` | `managerfix.other.vanish` |
| `/back` `/dback` | Вернуться / на место смерти | `return` | `managerfix.other.back` |
| `/invsee <ник>` `/ecsee <ник>` | Просмотр инвентарей | — | `managerfix.other.invsee` |
| `/give <ник> <предмет> <кол-во>` | Выдать предмет | — | `managerfix.other.give` |
| `/freeze <ник>` | Заморозить игрока | `freezeplayer` | `managerfix.other.freeze` |
| `/lockchat` | Блокировка чата | — | `managerfix.other.chatlock` |
| `/sudo <ник> <команда>` | Выполнить команду за игрока | — | `managerfix.other.sudo` |
| `/killmob <тип> <радиус>` | Убить мобов | — | `managerfix.other.killmob` |
| `/spawnmob <тип> <кол-во>` | Заспавнить мобов | — | `managerfix.other.spawnmob` |
| `/day` `/night` `/weather <clear\|rain\|thunder>` | Время/погода | `sun`, `rain`, `thunder` | `managerfix.other.time` / `managerfix.other.weather` |
| `/health [ник]` `/food [ник\|god]` | Здоровье / еда | `heal`, `feed` | `managerfix.other.health` / `.food` |
| `/clear [ник]` | Очистить инвентарь | `clearinventory`, `ci` | `managerfix.other.clear` |
| `/pinfo <ник>` | Инфо об игроке (включая IP — отдельным правом) | — | `managerfix.other.info` |
| `/ping [ник]` | Пинг | — | `managerfix.other.ping` |
| `/seen <ник>` | Последний вход | `lastseen` | `managerfix.other.seen` |
| `/coords` | Координаты | `coordinates`, `xyz` | `managerfix.other.coords` |
| `/speed <скорость>` | Скорость | `setspeed` | `managerfix.other.speed` |
| `/rate <ник>` | Оценить игрока (репутация) | `rep` | `managerfix.other.rate` |
| `/adminrep <set\|add\|take> <ник> <кол-во>` | Управление репутацией | — | `managerfix.other.adminrep` |

Конфиг: `plugins/FixCore/modules/other/config.yml` (+ `reputation.yml`).

[↑ к содержанию](#содержание)

---

# MF-Rtp

Случайная телепортация.

Возможности:

- Телепорт в случайное безопасное место (поиск безопасной точки).
- Радиусы: обычный и дальний (настраиваются), телепорт к случайному игроку рядом.
- Кулдаун, плата за телепорт к игроку (Vault, 1000 по умолчанию).
- Обход кулдауна правом.

Команда:

| Команда | Описание | Права |
|---|---|---|
| `/rtp` | Случайная телепортация | `managerfix.rtp.use` |

Права-опции: `managerfix.rtp.option.1000` (до 1000 блоков), `managerfix.rtp.option.5000` (до 5000), `managerfix.rtp.option.randomplayer` (к случайному игроку), `managerfix.rtp.bypass.cooldown`.

Конфиг: `plugins/FixCore/modules/rtp/config.yml` — `cooldown`, `near-rtp`/`far-rtp` диапазоны, `player-radius`, `costs`.

[↑ к содержанию](#содержание)

---

# MF-Spawn

Спавн сервера.

Возможности:

- Телепорт на спавн с задержкой/обратным отсчётом и звуками.
- Установка спавна, редактор настроек спавна (GUI).
- Опции: телепорт при входе, после смерти, только на первом входе.
- Безопасная телепортация (поиск точки).

Команды:

| Команда | Описание | Права |
|---|---|---|
| `/spawn` | Телепорт на спавн | `managerfix.spawn.use` |
| `/setspawn` | Установить спавн (на месте) | `managerfix.spawn.set` |
| `/editspawn` | Настройки спавна (GUI) | `managerfix.spawn.edit` |

Конфиг: `plugins/FixCore/modules/spawn/config.yml` — координаты спавна + `settings` (`spawn-on-join`, `spawn-on-death`, `spawn-first-join-only`, `safe-teleport`, задержки/звуки).

[↑ к содержанию](#содержание)

---

# MF-Tab

Кастомный таб (header/footer и формат игроков).

Возможности:

- Многострочные header/footer (MiniMessage, HEX, PAPI).
- Формат имени игрока: префикс LuckPerms + ник.
- Отдельный формат для игроков **в AFK** (совместно с [MF-Afk](#mf-afk)).
- Скрытие ванишнутых ([MF-Other](#mf-other)).
- Сортировка таба по весу группы LuckPerms.
- Оптимизация: отправляются только изменения, настраиваемый интервал обновления и кэш плейсхолдеров.
- Плейсхолдер кластера `%cluster_total_online%` (когда включено в конфиге).

Команды: нет. Настройка — полностью в конфиге.

Конфиг: `plugins/FixCore/modules/tab/config.yml` — `header`, `footer`, `player-format`, `afk-format`, `update-interval-ticks`, `sort-by-luckperms`, `hide-vanished`.

[↑ к содержанию](#содержание)

---

# MF-Tpa

Система запросов на телепорт.

Возможности:

- Запрос к игроку и «приглашение» к себе.
- Принятие/отклонение запроса, ответ кликом по сообщению (GUI).
- Отключение приёма запросов (`/tpatoggle`) и **чёрный список** (`/tpablacklist`).
- Кулдаун, таймаут запроса, задержка телепорта, отмена при движении/уроне, звуки.

Команды:

| Команда | Описание | Права |
|---|---|---|
| `/tpa <ник>` | Запросить телепорт к игроку | `managerfix.command.tpa` |
| `/tpahere <ник>` | Пригласить игрока к себе | `managerfix.command.tpahere` |
| `/tpaccept` | Принять запрос | `managerfix.command.tpaccept` |
| `/tpdeny` `/tpadeny` | Отклонить запрос | `managerfix.command.tpdeny` |
| `/tpatoggle` | Вкл/выкл приём запросов | `managerfix.command.tpatoggle` |
| `/tpablacklist [add\|remove\|list] [ник]` | Чёрный список | `managerfix.command.tpablacklist` |
| `/tpareply` | Ответить на запрос (клик) | `managerfix.command.tpareply` |

Права: `managerfix.tpa.use`, `managerfix.tpa.bypass.cooldown`.

Конфиг: `plugins/FixCore/modules/tpa/config.yml` — `cooldown-seconds`, `request-timeout-seconds`, `teleport-delay-seconds`, `cancel-on-damage`, звуки и сообщения.

[↑ к содержанию](#содержание)

---

# MF-Warps

Варпы (точки телепорта) с GUI.

Возможности:

- Телепорт по названию, создание/удаление, **редактирование свойств варпа** (`/editwarp`).
- GUI всех варпов; Shift+ПКМ — обновить локацию варпа.
- Лимиты по группам (`group-limits`) и доступ к варпу по праву `managerfix.warps.warp.<название>`.
- Задержка телепорта, кулдаун, звуки.

Команды:

| Команда | Описание | Права |
|---|---|---|
| `/warp <название>` | Телепорт на варп | `managerfix.command.warp` + `managerfix.warps.warp.*`/`.warp.<name>` |
| `/warps [create\|delete] [название]` | Список/GUI варпов | `managerfix.command.warps` |
| `/setwarp <название>` | Создать варп здесь | `managerfix.warps.create` |
| `/delwarp <название>` | Удалить варп | `managerfix.warps.delete` |
| `/editwarp <название>` | Редактировать варп | `managerfix.warps.edit` |

Алиасы: `warplist`, `createwarp`, `removewarp`.

Конфиг: `plugins/FixCore/modules/warps/config.yml` (+ данные в `plugins/FixCore/data/warps.yml`).

[↑ к содержанию](#содержание)

---

# MF-Worlds

Управление мирами.

Возможности:

- Список миров и **телепорт между мирами** (команда и GUI).
- Создание/удаление/клонирование миров (включая загрузку шаблонов из папки `templates`).
- Генераторы: обычный, плоский, большие биомы, амплифайд, void.
- Авто-выгрузка пустых миров, максимальное число миров.

Команды:

| Команда | Описание | Права |
|---|---|---|
| `/worlds` | Список миров / GUI | `managerfix.command.world` |
| `/world <мир>` | Телепорт в мир | `managerfix.worlds.teleport` |

Права: `managerfix.worlds.create`, `managerfix.worlds.delete`, `managerfix.module.worlds.admin`.

Конфиг: `plugins/FixCore/modules/worlds/config.yml` — `default-generator`, `allow-teleport/create/delete/clone`, `max-worlds`, `auto-unload-minutes`, `templates-folder`.

[↑ к содержанию](#содержание)

---

## Полезные советы администратору

1. **Где что лежит.**
   - Плагины: `plugins/FixCore-2.0.0.jar`, `plugins/MF-<Модуль>-2.0.0.jar`.
   - Конфиги модулей: `plugins/FixCore/modules/<модуль>/`.
   - Данные (дома, варпы, бан-лист, репутация, playtime): `plugins/FixCore/data/`.
   - Языковые файлы: `plugins/FixCore/lang/ru.yml` (меняйте сообщения там).
2. **Не удаляйте и не «чините» файлы вручную во время работы сервера** — всегда используйте команду `/fixcore reload`.
3. **Права** — базовая модель такова: все права по умолчанию для операторов, игрокам выдавайте точечно (например, `managerfix.homes.use` + лимиты, `managerfix.spawn.use`, `managerfix.rtp.use`, `managerfix.afk.use`, `managerfix.command.tpa`, `liveboard.use` уже у всех и т.д.).
4. **Хранилище.** По умолчанию SQLite — ровно один файл `plugins/FixCore/data/`. Для «обслуживаемого» сервера можно включить MySQL в `config.yml` (шить все модули будут в одну БД).
5. **Если модуль не включился** — проверьте, что он включён в `config.yml → modules`, и посмотрите логи: `Enabling MF-<Имя>` / ошибки.
6. **Кланы** — `ClansFix` и `ClansFixRaids` подключаются к ядру `FixCore` и требуют ProtocolLib. Они работают вместе с модулями ManagerFix.

[↑ к содержанию](#содержание)