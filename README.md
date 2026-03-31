# 🎮 ManagerFix — Универсальный плагин для Paper 1.21.x

<div align="center">

![Версия](https://img.shields.io/badge/версия-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Paper](https://img.shields.io/badge/Paper-1.21.x-green)
![Лицензия](https://img.shields.io/badge/лицензия-MIT-gray)

**Модульный плагин для Minecraft серверов с мощным API и красивыми GUI**

[Особенности](#-особенности) • [Установка](#-установка) • [Модули](#-модули) • [API](#-api-для-разработчиков) • [Поддержка](#-поддержка)

</div>

---

## 📖 Содержание

<details>
<summary>Нажмите, чтобы развернуть полное содержание</summary>

1. [Описание](#-описание)
2. [Особенности](#-особенности)
3. [Требования](#-требования)
4. [Установка](#-установка)
5. [Структура файлов](#-структура-файлов)
6. [Модули](#-модули)
   - [Warps](#warps-варпы)
   - [Homes](#homes-дома)
   - [Spawn](#spawn-спавн)
   - [Chat](#chat-чат)
   - [TPA](#tpa-телепортация)
   - [RTP](#rtp-случайная-телепортация)
   - [Ban](#ban-баны)
   - [AFK](#afk-афк)
   - [Kits](#kits-наборы)
   - [Items](#items-предметы)
   - [Worlds](#worlds-миры)
   - [Other](#other-админ-утилиты)
   - [Tab](#tab-таб-лист)
   - [Announcer](#announcer-объявления)
   - [Names](#names-ники)
7. [Конфигурация](#-конфигурация)
8. [Права доступа](#-права-доступа)
9. [PlaceholderAPI](#-placeholderapi)
10. [API для разработчиков](#-api-для-разработчиков)
11. [Сборка из исходников](#-сборка-из-исходников)
12. [Поддержка](#-поддержка)

</details>

---

## 📝 Описание

**ManagerFix** — это современный модульный плагин для серверов Minecraft (Paper 1.21.x), объединяющий все необходимые функции в одном решении:

- 🏠 **Система домов** с лимитами и GUI
- 📍 **Варпы** с категориями и иконками
- 💬 **Продвинутый чат** с локальным/глобальным режимом
- 🛡️ **Система банов** с GUI и историей
- 🎒 **Киты** с настраиваемыми кулдаунами
- 🗺️ **Управление мирами** и многое другое

Плагин поддерживает **YAML** и **MySQL/MariaDB** хранилища, имеет встроенное **API** для интеграции с другими плагинами и полностью настраиваемые **GUI интерфейсы**.

---

## ✨ Особенности

| 🎯 | Особенность | Описание |
|----|-------------|----------|
| 🧩 | **Модульность** | Включайте только нужные функции в `config.yml` |
| 🎨 | **Красивые GUI** | Стильные меню с анимацией и кастомизацией |
| 💾 | **2 типа хранилищ** | YAML (файлы) или MySQL/MariaDB (база данных) |
| 🔄 | **Миграция данных** | Автоматический перенос между YAML и MySQL |
| 🔌 | **API** | Готовое API для интеграции с другими плагинами |
| 🌐 | **Кластер Redis** | Синхронизация между несколькими серверами |
| 📦 | **PlaceholderAPI** | Поддержка плейсхолдеров в чате и GUI |
| 🛠️ | **Vault** | Интеграция с экономикой и префиксами |
| ⚡ | **Асинхронность** | Не блокирует основной поток сервера |
| 🎭 | **Темы GUI** | Настраиваемый дизайн интерфейсов |

---

## 📋 Требования

| Компонент | Версия | Обязательно |
|-----------|--------|-------------|
| **Сервер** | Paper 1.21.x | ✅ Да |
| **Java** | 21+ | ✅ Да |
| **PlaceholderAPI** | 2.11+ | ❌ Опционально |
| **Vault** | 1.7+ | ❌ Опционально |
| **LuckPerms** | 5.x | ❌ Опционально |
| **ProtocolLib** | 5.x | ❌ Опционально |

---

## 📥 Установка

### Быстрая установка

1. **Скачайте** плагин:
   ```bash
   # Или скопируйте из target/ после сборки
   ManagerFix-1.0.0.jar
   ```

2. **Поместите** файл в папку сервера:
   ```
   сервер/
   └── plugins/
       └── ManagerFix-1.0.0.jar
   ```

3. **Запустите сервер** — конфигурация создастся автоматически

4. **Настройте** файлы в `plugins/ManagerFix/`:
   - `config.yml` — главный конфиг
   - `modules/*.yml` — настройки модулей
   - `lang/ru.yml` — языковые сообщения

### Обновление

1. Сохраните папки `config.yml`, `modules/`, `lang/`
2. Замените `.jar` файл на новую версию
3. Перезапустите сервер
4. При необходимости обновите конфиги

---

## 📁 Структура файлов

```
plugins/ManagerFix/
│
├── 📄 config.yml              # Главный конфиг (модули, БД, кластер)
├── 📂 lang/
│   └── ru.yml                 # Русские сообщения (MiniMessage)
│
├── 📂 modules/                # Конфигурация модулей
│   ├── afk/
│   │   ├── config.yml         # Настройки AFK
│   │   └── commands.yml       # Команды и кулдауны
│   ├── announcer/
│   ├── ban/
│   ├── chat/
│   ├── homes/
│   ├── items/
│   ├── kits/
│   ├── names/
│   ├── other/
│   ├── rtp/
│   ├── spawn/
│   ├── tab/
│   ├── tpa/
│   ├── warps/
│   └── worlds/
│
├── 📂 data/                   # Данные (при YAML хранилище)
│   ├── players/               # Профили игроков
│   ├── warps.yml              # Варпы
│   ├── bans.yml               # Баны
│   ├── mutes.yml              # Муты
│   ├── kits.yml               # Наборы
│   └── items.yml              # Предметы
│
└── 📂 storage/
    └── last_storage_type.txt  # Тип хранилища (для миграции)
```

---

## ⚙️ Модули

### Warps (Варпы)

> Система варпов с GUI, кулдаунами и правами доступа

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/warps` | Открыть GUI варпов | `managerfix.command.warps` |
| `/warp <имя>` | Телепорт на варп | `managerfix.command.warp` |
| `/setwarp <имя>` | Создать варп | `managerfix.command.setwarp` |
| `/delwarp <имя>` | Удалить варп | `managerfix.command.delwarp` |
| `/editwarp <имя>` | Редактировать варп | `managerfix.command.editwarp` |

**Конфигурация** (`modules/warps/config.yml`):
```yaml
settings:
  max-warps-per-player: 10      # Максимум варпов на игрока
  teleport-delay: 3             # Задержка телепортации (сек)
  cooldown: 5                   # Кулдаун между телепортациями
  
gui:
  enabled: true                 # Включить GUI
  title: "Варпы сервера"
  
categories:                     # Категории в GUI
  spawn:
    icon: GRASS_BLOCK
    name: "Спавн"
  pvp:
    icon: DIAMOND_SWORD
    name: "PVP Арена"
```

---

### Homes (Дома)

> Личные точки телепортации игроков с лимитами

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/sethome [название]` | Установить дом | `managerfix.command.sethome` |
| `/home [название]` | Телепорт домой | `managerfix.command.home` |
| `/delhome <название>` | Удалить дом | `managerfix.command.delhome` |
| `/homes` | GUI домов | `managerfix.command.homes` |
| `/adminhomes <игрок>` | Дома другого игрока | `managerfix.command.adminhomes` |

**Лимиты по правам:**
```
managerfix.homes.limit.1    — 1 дом
managerfix.homes.limit.3    — 3 дома
managerfix.homes.limit.5    — 5 домов
managerfix.homes.limit.10   — 10 домов
managerfix.homes.limit.20   — 20 домов
```

---

### Spawn (Спавн)

> Точка спавна с настройками телепортации

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/spawn` | Телепорт на спавн | `managerfix.command.spawn` |
| `/setspawn` | Установить спавн | `managerfix.command.setspawn` |
| `/editspawn` | Настройки спавна | `managerfix.command.editspawn` |

**Конфигурация:**
```yaml
settings:
  teleport-delay-seconds: 5     # Задержка телепортации
  cancel-on-move: true          # Отмена при движении
  cancel-on-damage: true        # Отмена при уроне
  spawn-on-join: false          # Телепорт при входе
  spawn-on-death: false         # Телепорт после смерти
  safe-teleport: true           # Проверка безопасности
```

---

### Chat (Чат)

> Продвинутая система чата с локальным и глобальным режимом

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/chattoggle` | Переключить режим чата | `managerfix.command.chattoggle` |
| `/pm <игрок> <сообщение>` | Личное сообщение | `managerfix.command.pm` |
| `/r <сообщение>` | Ответить на ЛС | `managerfix.command.reply` |
| `/chatspy` | Просмотр локального чата | `managerfix.command.chatspy` |
| `/commandspy` | Просмотр команд | `managerfix.command.commandspy` |
| `/pmblock <игрок>` | Заблокировать ЛС | `managerfix.command.pmblock` |
| `/ignore add/remove <игрок>` | Игнор-лист | `managerfix.command.ignore` |
| `/clearchat` | Очистить чат | `managerfix.command.clearchat` |

**Особенности:**
- 🎯 **Локальный чат** — сообщения видны в радиусе 60 блоков
- 🌍 **Глобальный чат** — префикс `!` для отправки всем
- 🔊 **Звуки** — при отправке/получении ЛС и локальных сообщений
- 💬 **Тултипы** — при наведении на ник (баланс, ЛКМ — ЛС)
- 🛡️ **Анти-спам** — кулдаун между сообщениями

**Конфигурация:**
```yaml
format-local: "{badge} {prefix}{player}{suffix}: {message}"
format-global: "{badge} {prefix}{player}{suffix}: {message}"
badge-local: "｢𝐋｣"
badge-global: "｢𝐆｣"
local-radius: 60
spam-cooldown: 2
```

---

### TPA (Телепортация)

> Система запросов телепортации к игрокам

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/tpa <игрок>` | Запрос телепортации | `managerfix.command.tpa` |
| `/tpahere <игрок>` | Запросить игрока к себе | `managerfix.command.tpahere` |
| `/tpaccept` | Принять запрос | `managerfix.command.tpaccept` |
| `/tpdeny` | Отклонить запрос | `managerfix.command.tpdeny` |
| `/tpatoggle` | Вкл/выкл получение запросов | `managerfix.command.tpatoggle` |
| `/tpablacklist` | Чёрный список TPA | `managerfix.command.tpablacklist` |

**Конфигурация:**
```yaml
request-timeout: 60           # Время жизни запроса (сек)
teleport-delay: 5             # Задержка телепортации
cooldown: 10                  # Кулдаун между запросами
cancel-on-move: true          # Отмена при движении
```

---

### RTP (Случайная телепортация)

> Телепортация в случайную безопасную точку

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/rtp` | Случайная телепортация | `managerfix.command.rtp` |

**Опции по правам:**
```
managerfix.rtp.option.1000      — RTP до 1000 блоков
managerfix.rtp.option.5000      — RTP до 5000 блоков
managerfix.rtp.option.randomplayer — RTP к случайному игроку
```

**Конфигурация:**
```yaml
min-distance: 100             # Минимальное расстояние
max-distance: 5000            # Максимальное расстояние
cooldown: 30                  # Кулдаун (секунды)
```

---

### Ban (Баны)

> Система блокировок с GUI и историей

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/ban <игрок> [время] [причина]` | Забанить | `managerfix.command.ban` |
| `/unban <игрок>` | Разбанить | `managerfix.command.unban` |
| `/banip <игрок> <время> <причина>` | Бан по IP | `managerfix.command.banip` |
| `/unbanip <IP|игрок>` | Разбан по IP | `managerfix.command.unbanip` |
| `/banlist` | GUI списка банов | `managerfix.command.banlist` |
| `/mute <игрок> [время] [причина]` | Мут | `managerfix.command.mute` |
| `/unmute <игрок>` | Размут | `managerfix.command.unmute` |
| `/kick <игрок> [причина]` | Кикнуть | `managerfix.command.kick` |

**Конфигурация:**
```yaml
default-duration: permanent     # Бан по умолчанию
broadcast-bans: true            # Оповещать о банах
kick-message: "<red>Вы забанены!</red>"
```

---

### AFK (АФК)

> Режим «отошёл» с авто-определением

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/afk` | Вкл/выкл AFK режим | `managerfix.command.afk` |
| `/top afk` | Топ по времени AFK | `managerfix.command.top` |

**Конфигурация:**
```yaml
afk-timeout-seconds: 300        # Авто-AFK через 5 минут
broadcast-afk: true             # Оповещать о входе/выходе
block-commands-while-afk: false # Блокировать команды
kick-timeout-seconds: 0         # Кик в AFK (0 = откл)
```

**PlaceholderAPI:** `%managerfix_afk%` → `true` / `false`

---

### Kits (Наборы)

> Выдача наборов предметов с кулдауном

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/kit [название]` | Получить кит | `managerfix.command.kit` |
| `/kits` | GUI китов | `managerfix.command.kits` |
| `/kit create <название>` | Создать кит | `managerfix.kits.create` |
| `/editkits` | Админ GUI китов | `managerfix.command.editkits` |

**Права на киты:**
```
managerfix.kits.kit.<название>  — Доступ к конкретному киту
```

**Конфигурация:**
```yaml
default-cooldown: 3600          # Кулдаун по умолчанию (1 час)
gui:
  enabled: true
  title: "Наборы предметов"
```

---

### Items (Предметы)

> Редактирование предметов в руке

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/i name <название>` | Изменить название | `managerfix.items.name` |
| `/i lore <описание>` | Изменить описание | `managerfix.items.lore` |
| `/i amount <число>` | Изменить количество | `managerfix.items.amount` |
| `/i enchant <зачарование> <уровень>` | Зачаровать | `managerfix.items.enchant` |
| `/i save <имя>` | Сохранить предмет | `managerfix.items.save` |
| `/i give <ник> <предмет> [кол-во]` | Выдать предмет | `managerfix.items.give` |
| `/i reload` | Перезагрузить конфиг | `managerfix.items.reload` |

**Примеры:**
```
/i name &6Легендарный меч
/i lore &7Наносит урон всем врагам
/i amount 64
/i enchant sharpness 5
/i save epic_sword
/i give Notch diamond_sword 1
```

---

### Worlds (Миры)

> Управление мирами сервера

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/world` | GUI миров | `managerfix.command.world` |
| `/world <мир>` | Телепорт в мир | `managerfix.command.world` |
| `/world tp <мир>` | Телепорт в мир | `managerfix.command.world` |
| `/world create <мир> [генератор]` | Создать мир | `managerfix.worlds.create` |
| `/world delete <мир>` | Удалить мир | `managerfix.worlds.delete` |

**Генераторы:** `default`, `flat`, `void`

---

### Other (Админ-утилиты)

> Пакет административных команд (Essentials-стиль)

**Основные команды:**

| Команда | Описание | Право |
|---------|----------|-------|
| `/fly [игрок]` | Режим полёта | `managerfix.other.fly` |
| `/god [игрок]` | Бессмертие | `managerfix.other.god` |
| `/heal [игрок]` | Лечение | `managerfix.other.heal` |
| `/feed [игрок]` | Насыщение | `managerfix.other.food` |
| `/repair [all] [игрок]` | Ремонт предметов | `managerfix.other.repair` |
| `/vanish` | Режим невидимости | `managerfix.other.vanish` |
| `/workbench` | Верстак | `managerfix.other.workbench` |
| `/anvil` | Наковальня | `managerfix.other.anvil` |
| `/enderchest [игрок]` | Эндер-сундук | `managerfix.other.ec` |
| `/invsee <игрок>` | Инвентарь игрока | `managerfix.other.invsee` |
| `/gmc /gms /gmsp [игрок]` | Режим игры | `managerfix.other.gamemode.*` |

**Телепортация:**
| Команда | Описание |
|---------|----------|
| `/tp to <игрок>` | Телепорт к игроку |
| `/tp here <игрок>` | Телепорт игрока к себе |
| `/tp location <x> <y> <z>` | Телепорт по координатам |
| `/tp top` | Телепорт на верхнюю точку |
| `/back` | Возврат на предыдущую точку |
| `/dback` | Возврат на место смерти |
| `/pull <игрок>` | Притянуть игрока |
| `/push <игрок>` | Телепортироваться к игроку |

**Утилиты:**
| Команда | Описание |
|---------|----------|
| `/killmob <тип> <радиус>` | Убить мобов |
| `/spawnmob <тип> <кол-во>` | Заспавнить мобов |
| `/near` | Игроки рядом |
| `/weather <clear|rain|thunder>` | Погода |
| `/day /night` | Время суток |
| `/health [игрок]` | Показать здоровье |
| `/food [игрок]` | Показать голод |
| `/food god` | FoodGod (голод не тратится) |
| `/clear [игрок]` | Очистить инвентарь |
| `/give <игрок> <предмет> <кол-во>` | Выдать предмет |
| `/pinfo <игрок>` | Информация об игроке |
| `/freeze <игрок>` | Заморозить игрока |
| `/lockchat` | Закрыть/открыть чат |
| `/broadcast <сообщение>` | Объявление |
| `/sudo <игрок> <команда>` | Выполнить от имени |
| `/ping [игрок]` | Показать пинг |
| `/coords` | Показать координаты |
| `/seen <игрок>` | Когда был в сети |
| `/speed <значение>` | Скорость полёта/ходьбы |

---

### Tab (Таб-лист)

> Кастомный header/footer в таблице игроков

**Конфигурация:**
```yaml
header: |
  <#FFD700>⚡ ManagerFix Server ⚡
  <gray>Онлайн: <white>%server_online%</white>
footer: |
  <gray>tps: %server_tps_1%</gray>
update-interval-ticks: 20       # Частота обновления (1 сек)
```

---

### Announcer (Объявления)

> Периодическая рассылка сообщений

**Конфигурация:**
```yaml
interval-seconds: 60            # Интервал (секунды)
broadcast-type: CHAT            # CHAT или ACTION_BAR
messages:
  - "<gold>Добро пожаловать на наш сервер!</gold>"
  - "<yellow>Купить донат: <aqua>donate.example.com</aqua></yellow>"
  - "<green>Техническая поддержка: <aqua>discord.example.com</aqua></green>"
```

---

### Names (Ники)

> Кастомные никнеймы с поддержкой цветов

**Команды:**
| Команда | Описание | Право |
|---------|----------|-------|
| `/nick <ник>` | Сменить ник | `managerfix.names.nick` |
| `/nickadmin <игрок> <ник|reset>` | Сменить ник другому | `managerfix.names.admin` |
| `/names` | Админ GUI ников | `managerfix.names.admin` |
| `/hidenick` | Скрыть/показать ник | `managerfix.names.hidenick` |

**Обходы:**
```
managerfix.names.bypass.cooldown  — Обход кулдауна
managerfix.names.bypass.length    — Обход лимита длины
managerfix.names.bypass.format    — Любые цвета/HEX в нике
```

---

## 🔧 Конфигурация

### Главный конфиг (`config.yml`)

```yaml
# Включение модулей
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
  worlds: false
  other: true
  tab: true
  announcer: true
  names: true
  items: true

# Тип хранилища: YAML или MYSQL
storage:
  type: YAML

# Настройки MySQL (если storage.type: MYSQL)
database:
  host: localhost
  port: 3306
  database: managerfix
  username: root
  password: password
  pool-size: 10

# Глобальные настройки
settings:
  debug: false                  # Режим отладки
  default-language: ru          # Язык по умолчанию
  profile-autosave-minutes: 10  # Автосохранение профилей
  warp-cooldown-seconds: 5      # Кулдаун варпов
  gui-animation-ticks: 20       # Анимация GUI
```

### Миграция данных

При переключении между `YAML` и `MYSQL` плагин **автоматически** переносит данные:

- ✅ Варпы, киты, баны, муты
- ✅ Профили игроков (дома, кулдауны)
- ✅ Сохранённые предметы

**Команда миграции:**
```
/migrate yaml2sql    # Перенос из YAML в MySQL
/migrate sql2yaml    # Перенос из MySQL в YAML
```

---

## 🎫 Права доступа

### Универсальные права

| Право | Описание |
|-------|----------|
| `managerfix.admin` | Полный доступ к админ-панели |
| `managerfix.reload` | Перезагрузка плагина |
| `managerfix.menu` | Открытие главного меню |
| `managerfix.module.toggle` | Вкл/выкл модулей через GUI |
| `managerfix.bypass.cooldown` | Обход всех кулдаунов |
| `managerfix.bypass.limit` | Обход всех лимитов |

### Права модулей

| Модуль | Права |
|--------|-------|
| **Warps** | `managerfix.warps.*`, `managerfix.warps.warp.<имя>` |
| **Homes** | `managerfix.homes.*`, `managerfix.homes.limit.N` |
| **Chat** | `managerfix.chat.*`, `managerfix.chat.color`, `managerfix.chat.spy` |
| **Ban** | `managerfix.ban.*`, `managerfix.ban.mute`, `managerfix.ban.kick` |
| **TPA** | `managerfix.tpa.*`, `managerfix.tpa.bypass.cooldown` |
| **AFK** | `managerfix.afk.*`, `managerfix.afk.bypass` |
| **Kits** | `managerfix.kits.*`, `managerfix.kits.kit.<название>` |
| **Other** | `managerfix.other.*` (fly, god, gamemode, vanish, и т.д.) |

---

## 🔌 PlaceholderAPI

Плагин регистрирует экспансию **`%managerfix%`**:

| Плейсхолдер | Описание |
|-------------|----------|
| `%managerfix_afk%` | `true` / `false` — в AFK режиме |

**Пример использования в чате:**
```yaml
format-local: "{badge} %managerfix_afk% {prefix}{player}{suffix}: {message}"
```

---

## 🧩 API для разработчиков

### Подключение к API

```java
import ru.managerfix.api.ManagerFixAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MyPlugin extends JavaPlugin {

    private ManagerFixAPI api;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<ManagerFixAPI> rsp =
            Bukkit.getServicesManager().getRegistration(ManagerFixAPI.class);

        if (rsp == null) {
            getLogger().severe("ManagerFix не найден!");
            return;
        }

        this.api = rsp.getProvider();
        getLogger().info("ManagerFix API подключён! Версия: " + api.getVersion());
    }
}
```

### Chat API

```java
import ru.managerfix.api.chat.ChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

ChatManager chat = api.getChatManager();

// Отправить глобальное сообщение
chat.sendGlobalMessage(MiniMessage.miniMessage().deserialize(
    "<gold>Важное объявление!</gold>"
));

// Отправить личное сообщение
chat.sendPrivateMessage(sender, target, MiniMessage.miniMessage().deserialize(
    "<gray>[ЛС] <white>Привет!</white></gray>"
));

// Проверить мут
if (chat.isMuted(player)) {
    player.sendMessage(Component.text("Вы замучены!"));
}

// Замутить на 1 час
chat.mute(player, "Нарушение правил", "Admin", 3600000);

// Размутить
chat.unmute(player);
```

### События

```java
import ru.managerfix.event.chat.PlayerChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(PlayerChatEvent event) {
        // Отменить событие
        if (event.getPlayer().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            return;
        }

        // Изменить сообщение
        event.setMessage(MiniMessage.miniMessage().deserialize(
            "<red>" + event.getMessage()
        ));
    }
}
```

**Доступные события:**
- `PlayerChatEvent` — сообщение в чат
- `PlayerMuteEvent` — мут игрока
- `PlayerUnmuteEvent` — размут игрока
- `PrivateMessageEvent` — личное сообщение
- `AfkEnterEvent` / `AfkLeaveEvent` — AFK режим
- `HomeCreateEvent` / `HomeTeleportEvent` — дома
- `WarpCreateEvent` / `WarpDeleteEvent` — варпы
- `PlayerBanEvent` / `PlayerUnbanEvent` — баны
- `ProfileLoadEvent` / `ProfileSaveEvent` — профили

---

## 🛠️ Сборка из исходников

### Требования

- **JDK 21** или выше
- **Maven 3.6+**

### Инструкция

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/yourusername/ManagerFix.git
   cd ManagerFix
   ```

2. **Соберите плагин:**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Готовый файл** появится в:
   ```
   target/ManagerFix-1.0.0.jar
   ```

### Команды Maven

```bash
mvn clean              # Очистка
mvn compile            # Компиляция
mvn test               # Тесты
mvn package            # Сборка JAR
mvn install            # Установка в локальный репозиторий
```

---

## 📞 Поддержка

| Канал | Ссылка |
|-------|--------|
| **Telegram** | [@fixsirt](https://t.me/fixsirt) |
| **GitHub** | [Issues](https://github.com/yourusername/ManagerFix/issues) |
| **Discord** | [Сервер поддержки](https://discord.gg/yourserver) |
| **Wiki** | [Документация](https://github.com/yourusername/ManagerFix/wiki) |

---

## 📄 Лицензия

Этот проект распространяется под лицензией **MIT**. Подробнее см. в файле [LICENSE](LICENSE).

---

<div align="center">

**ManagerFix** © 2024 | Автор: **tg:fixsirt**

⭐ Если вам нравится плагин, поставьте звезду на GitHub!

</div>
