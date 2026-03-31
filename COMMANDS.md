# 📜 ManagerFix — Полный список команд

Подробное руководство по всем командам плагина **ManagerFix 1.0.0**

---

## 📖 Содержание

1. [Основные команды](#основные-команды)
2. [Warps](#warps-варпы)
3. [Homes](#homes-дома)
4. [Spawn](#spawn-спавн)
5. [Chat](#chat-чат)
6. [TPA](#tpa-телепортация)
7. [RTP](#rtp-случайная-телепортация)
8. [Ban](#ban-баны)
9. [AFK](#afk-афк)
10. [Kits](#kits-наборы)
11. [Items](#items-предметы)
12. [Worlds](#worlds-миры)
13. [Other](#other-админ-утилиты)
14. [Names](#names-ники)
15. [Tab](#tab-таб-лист)
16. [Announcer](#announcer-объявления)
17. [Утилиты](#утилиты)

---

## 🎯 Основные команды

| Команда | Описание | Право | По умолчанию |
|---------|----------|-------|--------------|
| `/managerfix` | Главное меню плагина | `managerfix.command.managerfix` | OP |
| `/managerfix reload` | Перезагрузить конфиг и модули | `managerfix.reload` | OP |
| `/managerfix menu` | Открыть меню модулей | `managerfix.menu` | OP |
| `/migrate <yaml2sql\|sql2yaml>` | Миграция данных между хранилищами | `managerfix.command.migrate` | OP |

---

## 📍 Warps (Варпы)

Система точек телепортации с GUI интерфейсом

| Команда | Описание | Право | Аргументы |
|---------|----------|-------|-----------|
| `/warps` | Открыть GUI варпов | `managerfix.command.warps` | — |
| `/warp <имя>` | Телепорт на варп | `managerfix.command.warp` | `<имя>` — название варпа |
| `/setwarp <имя>` | Создать варп в текущей локации | `managerfix.command.setwarp` | `<имя>` — название |
| `/delwarp <имя>` | Удалить варп | `managerfix.command.delwarp` | `<имя>` — название |
| `/editwarp <имя>` | Редактировать свойства варпа | `managerfix.command.editwarp` | `<имя>` — название |

**Примеры:**
```
/warps                          # Открыть GUI
/warp spawn                     # Телепорт на варп "spawn"
/setwarp arena                  # Создать варп "arena"
/delwarp oldwarp                # Удалить варп "oldwarp"
/editwarp spawn                 # Редактировать варп "spawn"
```

**Права на отдельные варпы:**
```
managerfix.warps.warp.<имя>     # Доступ к конкретному варпу
managerfix.warps.warp.*         # Доступ ко всем варпам
```

---

## 🏠 Homes (Дома)

Личные точки телепортации игроков

| Команда | Описание | Право | Аргументы |
|---------|----------|-------|-----------|
| `/sethome [название]` | Установить дом | `managerfix.command.sethome` | `[название]` — опционально, по умолчанию "home" |
| `/home [название]` | Телепорт домой | `managerfix.command.home` | `[название]` — если домов несколько |
| `/delhome <название>` | Удалить дом | `managerfix.command.delhome` | `<название>` — обязательный аргумент |
| `/homes` | Открыть GUI домов | `managerfix.command.homes` | — |
| `/adminhomes <игрок>` | Просмотреть дома другого игрока | `managerfix.command.adminhomes` | `<игрок>` — ник игрока |
| `/adminsethome <игрок> <название>` | Установить дом для другого игрока | `managerfix.command.adminsethome` | `<игрок>`, `<название>` |

**Примеры:**
```
/sethome                          # Дом по умолчанию "home"
/sethome база                     # Дом с названием "база"
/home                             # Телепорт к дому по умолчанию
/home база                        # Телепорт к дому "база"
/delhome стараябаза               # Удалить дом "стараябаза"
/homes                            # Открыть GUI
/adminhomes Notch                 # Просмотреть дома Notch
/adminsethome Notch спавн         # Установить дом "спавн" для Notch
```

**Лимиты домов (права):**
```
managerfix.homes.limit.1          # Максимум 1 дом
managerfix.homes.limit.3          # Максимум 3 дома
managerfix.homes.limit.5          # Максимум 5 домов
managerfix.homes.limit.10         # Максимум 10 домов
managerfix.homes.limit.20         # Максимум 20 домов
```

---

## 🏛️ Spawn (Спавн)

Точка спавна сервера

| Команда | Описание | Право |
|---------|----------|-------|
| `/spawn` | Телепорт на спавн | `managerfix.command.spawn` |
| `/setspawn` | Установить спавн в текущей локации | `managerfix.command.setspawn` |
| `/editspawn` | Открыть GUI настроек спавна | `managerfix.command.editspawn` |

---

## 💬 Chat (Чат)

Продвинутая система чата

### Основные команды

| Команда | Описание | Право |
|---------|----------|-------|
| `/chattoggle` | Переключить режим чата (локальный/глобальный) | `managerfix.command.chattoggle` |
| `/chatspy` | Вкл/выкл просмотр локального чата (вне радиуса) | `managerfix.command.chatspy` |
| `/commandspy` | Вкл/выкл просмотр команд игроков | `managerfix.command.commandspy` |
| `/clearchat` | Очистить чат для всех | `managerfix.command.clearchat` |
| `/cc` | Короткая форма /clearchat | `managerfix.command.cc` |

### Личные сообщения

| Команда | Описание | Право |
|---------|----------|-------|
| `/pm <игрок> <сообщение>` | Отправить личное сообщение | `managerfix.command.pm` |
| `/tell <игрок> <сообщение>` | Аналог /pm | `managerfix.command.pm` |
| `/msg <игрок> <сообщение>` | Аналог /pm | `managerfix.command.pm` |
| `/r <сообщение>` | Быстрый ответ последнему собеседнику | `managerfix.command.reply` |
| `/pmblock <игрок>` | Заблокировать/разблокировать ЛС от игрока | `managerfix.command.pmblock` |

### Игнор-лист

| Команда | Описание | Право |
|---------|----------|-------|
| `/ignore add <игрок>` | Добавить игрока в игнор | `managerfix.command.ignore` |
| `/ignore remove <игрок>` | Удалить игрока из игнора | `managerfix.command.ignore` |
| `/ignore list` | Показать список игнорируемых | `managerfix.command.ignore` |

**Примеры:**
```
!Привет всем!                     # Глобальное сообщение (с префиксом !)
Привет локально                   # Локальное сообщение (без !)
/pm Steve Привет, как дела?       # ЛС игроку Steve
/r Всё отлично, спасибо!          # Ответ на последнее ЛС
/pmblock Steve                    # Заблокировать ЛС от Steve
/ignore add Notch                 # Добавить Notch в игнор
/chatspy                          # Включить просмотр локального чата
/commandspy                       # Включить просмотр команд
/clearchat                        # Очистить чат
```

**Особенности:**
- **Локальный чат** — сообщения видны в радиусе 60 блоков (настраивается)
- **Глобальный чат** — префикс `!` перед сообщением
- **ChatSpy** — просмотр локальных сообщений вне радиуса
- **CommandSpy** — просмотр команд других игроков
- **Звуки** — при отправке/получении ЛС и локальных сообщений
- **Тултипы** — при наведении на ник (баланс, ЛКМ — ЛС)

---

## 🚀 TPA (Телепортация)

Система запросов телепортации

| Команда | Описание | Право |
|---------|----------|-------|
| `/tpa <игрок>` | Отправить запрос на телепортацию к игроку | `managerfix.command.tpa` |
| `/tpahere <игрок>` | Запросить игрока телепортироваться к вам | `managerfix.command.tpahere` |
| `/tpaccept` | Принять запрос на телепортацию | `managerfix.command.tpaccept` |
| `/tpdeny` | Отклонить запрос на телепортацию | `managerfix.command.tpdeny` |
| `/tpadeny` | Аналог /tpdeny | `managerfix.command.tpdeny` |
| `/tpatoggle` | Вкл/выкл получение TPA запросов | `managerfix.command.tpatoggle` |
| `/tpablacklist` | Управление чёрным списком TPA | `managerfix.command.tpablacklist` |
| `/tpablacklist add <игрок>` | Добавить в чёрный список | `managerfix.command.tpablacklist` |
| `/tpablacklist remove <игрок>` | Удалить из чёрного списка | `managerfix.command.tpablacklist` |
| `/tpablacklist list` | Показать чёрный список | `managerfix.command.tpablacklist` |

**Примеры:**
```
/tpa Steve                        # Запрос на телепорт к Steve
/tpahere Notch                    # Запросить Notch к себе
/tpaccept                         # Принять запрос
/tpdeny                           # Отклонить запрос
/tpatoggle                        # Вкл/выкл получение запросов
/tpablacklist add Griefer           # Добавить Griefer в чёрный список
```

---

## 🎲 RTP (Случайная телепортация)

Телепортация в случайную безопасную точку

| Команда | Описание | Право |
|---------|----------|-------|
| `/rtp` | Случайная телепортация | `managerfix.command.rtp` |

**Опции по правам:**
```
managerfix.rtp.option.1000          # RTP до 1000 блоков
managerfix.rtp.option.5000          # RTP до 5000 блоков
managerfix.rtp.option.randomplayer  # RTP к случайному игроку (100 блоков)
```

---

## 🛑 Ban (Баны)

Система блокировок игроков

### Баны

| Команда | Описание | Право |
|---------|----------|-------|
| `/ban <игрок> [время] [причина]` | Забанить игрока | `managerfix.command.ban` |
| `/unban <игрок>` | Разбанить игрока | `managerfix.command.unban` |
| `/banip <игрок> <время> <причина>` | Забанить по IP | `managerfix.command.banip` |
| `/unbanip <IP|игрок>` | Разбанить по IP | `managerfix.command.unbanip` |
| `/banlist` | Открыть GUI списка банов | `managerfix.command.banlist` |

### Муты

| Команда | Описание | Право |
|---------|----------|-------|
| `/mute <игрок> [время] [причина]` | Замутить игрока | `managerfix.command.mute` |
| `/unmute <игрок>` | Размутить игрока | `managerfix.command.unmute` |

### Кик

| Команда | Описание | Право |
|---------|----------|-------|
| `/kick <игрок> [причина]` | Кикнуть игрока | `managerfix.command.kick` |

**Примеры:**
```
/ban Griefer 7d Гриферство          # Бан на 7 дней
/ban Hacker Перманентный бан         # Перманентный бан
/unban Griefer                      # Разбанить Griefer
/banip 192.168.1.1 30d Рассылка     # Бан по IP на 30 дней
/unbanip 192.168.1.1                # Разбан по IP
/mute Spammer 1h Спам               # Мут на 1 час
/unmute Spammer                     # Размутить
/kick Troller Надоели шутки         # Кикнуть с причиной
/banlist                            # Открыть GUI банов
```

**Форматы времени:**
```
1m — 1 минута
1h — 1 час
1d — 1 день
7d — 7 дней
30d — 30 дней
perm — перманентно
```

---

## 😴 AFK (АФК)

Режим «отошёл»

| Команда | Описание | Право |
|---------|----------|-------|
| `/afk` | Вкл/выкл AFK режим вручную | `managerfix.command.afk` |
| `/top afk` | Топ игроков по времени в AFK | `managerfix.command.top` |

**PlaceholderAPI:**
```
%managerfix_afk%  →  true / false
```

---

## 🎒 Kits (Наборы)

Выдача наборов предметов

| Команда | Описание | Право |
|---------|----------|-------|
| `/kit [название]` | Получить кит (или список если без аргумента) | `managerfix.command.kit` |
| `/kits` | Открыть GUI китов | `managerfix.command.kits` |
| `/kit create <название>` | Создать новый кит | `managerfix.kits.create` |
| `/editkits` | Админ GUI для редактирования китов | `managerfix.command.editkits` |

**Примеры:**
```
/kit                              # Показать доступные киты
/kit starter                      # Получить стартовый набор
/kits                             # Открыть GUI китов
/kit create vip                   # Создать VIP кит
/editkits                         # Админ редактирование
```

**Права на киты:**
```
managerfix.kits.kit.starter       # Доступ к киту "starter"
managerfix.kits.kit.vip           # Доступ к киту "vip"
managerfix.kits.kit.*             # Доступ ко всем китам
```

---

## ⚔️ Items (Предметы)

Редактирование предметов в руке

| Команда | Описание | Право | Аргументы |
|---------|----------|-------|-----------|
| `/i name <название>` | Изменить название предмета | `managerfix.items.name` | `<название>` — текст с поддержкой цветов |
| `/i lore <описание>` | Изменить описание предмета | `managerfix.items.lore` | `<описание>` — текст |
| `/i amount <число>` | Изменить количество | `managerfix.items.amount` | `<число>` — от 1 до 64 |
| `/i enchant <зачарование> <уровень>` | Зачаровать предмет | `managerfix.items.enchant` | `<зачарование>`, `<уровень>` |
| `/i attribute <тип> <значение>` | Изменить атрибуты | `managerfix.items.attribute` | `<тип>`, `<значение>` |
| `/i save <имя>` | Сохранить предмет в конфиг | `managerfix.items.save` | `<имя>` — название для сохранения |
| `/i give <ник> <предмет> [кол-во]` | Выдать предмет игроку | `managerfix.items.give` | `<ник>`, `<предмет>`, `[кол-во]` |
| `/i reload` | Перезагрузить конфиг предметов | `managerfix.items.reload` | — |

**Примеры:**
```
/i name &6⚔ Легендарный меч &7(Урон: +10)
/i lore &7Наносит урон всем врагам
/i lore &cС проклятием потери
/i amount 64
/i enchant sharpness 5
/i enchant unbreaking 3
/i save epic_sword
/i give Notch diamond_sword 1
/i reload
```

**Популярные зачарования:**
```
sharpness       — Острота
smite           — Небесная кара
bane_of_arthropods — Бич членистоногих
knockback       — Отбрасывание
fire_aspect     — Заговор огня
looting         — Добыча
sweeping_edge   — Разящий клинок
unbreaking      — Прочность
knockback_resistance — Сопротивление отбрасыванию
```

---

## 🌍 Worlds (Миры)

Управление мирами сервера

| Команда | Описание | Право |
|---------|----------|-------|
| `/world` | Открыть GUI миров | `managerfix.command.world` |
| `/world <мир>` | Телепорт в мир | `managerfix.command.world` |
| `/world tp <мир>` | Телепорт в мир | `managerfix.command.world` |
| `/world create <мир> [генератор]` | Создать новый мир | `managerfix.worlds.create` |
| `/world delete <мир>` | Удалить мир | `managerfix.worlds.delete` |

**Генераторы:**
```
default         — Обычный мир
flat            — Плоский мир
void            — Пустой мир
```

**Примеры:**
```
/world                            # Открыть GUI
/world survival                   # Телепорт в мир "survival"
/world tp creative                # Телепорт в мир "creative"
/world create myworld             # Создать мир "myworld"
/world create flatworld flat      # Создать плоский мир
/world delete oldworld            # Удалить мир "oldworld"
```

---

## 🛠️ Other (Админ-утилиты)

### Режимы игры

| Команда | Описание | Право |
|---------|----------|-------|
| `/fly [игрок]` | Вкл/выкл режим полёта | `managerfix.other.fly` |
| `/god [игрок]` | Вкл/выкл бессмертие | `managerfix.other.god` |
| `/gmc [игрок]` | Режим творческий | `managerfix.other.gamemode.creative` |
| `/gms [игрок]` | Режим выживание | `managerfix.other.gamemode.survival` |
| `/gmsp [игрок]` | Режим наблюдатель | `managerfix.other.gamemode.spectator` |

### Лечение и еда

| Команда | Описание | Право |
|---------|----------|-------|
| `/heal [игрок]` | Полное лечение | `managerfix.other.heal` |
| `/feed [игрок]` | Насыщение | `managerfix.other.food` |
| `/food [игрок]` | Показать уровень голода | `managerfix.other.food` |
| `/food god` | FoodGod (голод не тратится) | `managerfix.other.food.god` |
| `/health [игрок]` | Показать здоровье | `managerfix.other.health` |

### Ремонт и очистка

| Команда | Описание | Право |
|---------|----------|-------|
| `/repair [all] [игрок]` | Ремонт предмета в руке или всех | `managerfix.other.repair` |
| `/clear [игрок]` | Очистить инвентарь | `managerfix.other.clear` |
| `/give <игрок> <предмет> [кол-во]` | Выдать предмет | `managerfix.other.give` |

### Контейнеры и блоки

| Команда | Описание | Право |
|---------|----------|-------|
| `/workbench` | Открыть верстак | `managerfix.other.workbench` |
| `/anvil` | Открыть наковальню | `managerfix.other.anvil` |
| `/enderchest [игрок]` | Открыть эндер-сундук | `managerfix.other.ec` |
| `/ec [игрок]` | Короткая форма /enderchest | `managerfix.other.ec` |
| `/invsee <игрок>` | Просмотр инвентаря игрока | `managerfix.other.invsee` |
| `/ecsee <игрок>` | Просмотр эндер-сундука игрока | `managerfix.other.ecsee` |
| `/stonecutter` | Открыть камнерез | `managerfix.other.stonecutter` |
| `/grindstone` | Открыть точило | `managerfix.other.grindstone` |
| `/cartography` | Открыть картографический стол | `managerfix.other.cartography` |
| `/loom` | Открыть ткацкий станок | `managerfix.other.loom` |
| `/enchanting` | Открыть стол зачарований | `managerfix.other.enchanting` |

### Телепортация

| Команда | Описание | Право |
|---------|----------|-------|
| `/tp to <игрок>` | Телепорт к игроку | `managerfix.other.tp` |
| `/tp here <игрок>` | Телепорт игрока к себе | `managerfix.other.tp` |
| `/tp location <x> <y> <z>` | Телепорт по координатам | `managerfix.other.tp.location` |
| `/tp top` | Телепорт на верхнюю точку | `managerfix.other.tp` |
| `/back` | Возврат на предыдущую точку | `managerfix.other.back` |
| `/dback` | Возврат на место смерти | `managerfix.other.dback` |
| `/pull <игрок>` | Притянуть игрока к себе | `managerfix.other.pull` |
| `/push <игрок>` | Телепортироваться к игроку | `managerfix.other.push` |

### Мобы

| Команда | Описание | Право |
|---------|----------|-------|
| `/killmob <тип> <радиус>` | Убить мобов указанного типа в радиусе | `managerfix.other.killmob` |
| `/spawnmob <тип> <кол-во>` | Заспавнить мобов | `managerfix.other.spawnmob` |

### Погода и время

| Команда | Описание | Право |
|---------|----------|-------|
| `/weather <clear|rain|thunder>` | Изменить погоду | `managerfix.other.weather` |
| `/sun` | Установить ясную погоду | `managerfix.other.weather` |
| `/rain` | Установить дождь | `managerfix.other.weather` |
| `/thunder` | Установить грозу | `managerfix.other.weather` |
| `/day` | Установить день | `managerfix.other.time` |
| `/night` | Установить ночь | `managerfix.other.time` |

### Администрирование

| Команда | Описание | Право |
|---------|----------|-------|
| `/vanish` | Вкл/выкл невидимость | `managerfix.other.vanish` |
| `/v` | Короткая форма /vanish | `managerfix.other.vanish` |
| `/near` | Показать игроков рядом | `managerfix.other.near` |
| `/pinfo <игрок>` | Подробная информация об игроке | `managerfix.other.info` |
| `/seen <игрок>` | Когда игрок был в сети последний раз | `managerfix.other.seen` |
| `/freeze <игрок>` | Заморозить игрока | `managerfix.other.freeze` |
| `/lockchat` | Закрыть/открыть чат | `managerfix.other.chatlock` |
| `/broadcast <сообщение>` | Объявление всем игрокам | `managerfix.other.broadcast` |
| `/sudo <игрок> <команда>` | Выполнить команду от имени игрока | `managerfix.other.sudo` |
| `/ping [игрок]` | Показать пинг | `managerfix.other.ping` |
| `/coords` | Показать текущие координаты | `managerfix.other.coords` |
| `/speed <значение>` | Установить скорость (0-10) | `managerfix.other.speed` |

**Примеры:**
```
/fly                              # Вкл/выкл полёт себе
/fly Steve                        # Вкл/выкл полёт Steve
/god                              # Бессмертие себе
/god Notch                        # Бессмертие Notch
/gmc                              # Творческий режим себе
/gms Steve                        # Выживание Steve
/heal                             # Лечение себе
/heal Steve                       # Лечение Steve
/feed                             # Насыщение себе
/repair                           # Ремонт предмета в руке
/repair all                       # Ремонт всех предметов
/clear Steve                      # Очистить инвентарь Steve
/give Notch diamond 64            # Выдать 64 алмаза Notch
/tp to Steve                      # Телепорт к Steve
/tp here Notch                    # Телепорт Notch к себе
/tp location 100 64 200           # Телепорт по координатам
/tp top                           # Телепорт наверх
/back                             # Возврат назад
/dback                            # Возврат на место смерти
/pull Steve                       # Притянуть Steve
/push Notch                       # Телепортироваться к Notch
/killmob zombie 50                # Убить зомби в радиусе 50 блоков
/spawnmob skeleton 5              # Заспавнить 5 скелетов
/weather clear                    # Ясная погода
/rain                             # Дождь
/day                              # День
/night                            # Ночь
/vanish                           # Невидимость
/near                             # Игроки рядом
/pinfo Steve                      # Информация о Steve
/seen Notch                       # Когда Notch был онлайн
/freeze Griefer                   # Заморозить Griefer
/lockchat                         # Закрыть чат
/broadcast &cВнимание! Рестарт через 5 минут!
/sudo Steve say Привет!           # Steve скажет "Привет!"
/ping                             # Показать пинг
/coords                           # Показать координаты
/speed 5                          # Установить скорость 5
```

---

## 🎭 Names (Ники)

Кастомные никнеймы

| Команда | Описание | Право |
|---------|----------|-------|
| `/nick <ник>` | Сменить свой ник | `managerfix.names.nick` |
| `/nickadmin <игрок> <ник|reset>` | Сменить ник другому игроку или сбросить | `managerfix.names.admin` |
| `/names` | Открыть админ GUI ников | `managerfix.names.admin` |
| `/hidenick` | Скрыть/показать свой ник над головой | `managerfix.names.hidenick` |

**Примеры:**
```
/nick &6VIP &7Игрок                # Сменить ник с цветом
/nickadmin Steve &cАдмин           # Сменить ник Steve
/nickadmin Notch reset             # Сбросить ник Notch
/names                             # Админ GUI
/hidenick                          # Скрыть ник
```

**Обходы:**
```
managerfix.names.bypass.cooldown    # Обход кулдауна
managerfix.names.bypass.length      # Обход лимита длины
managerfix.names.bypass.format      # Любые цвета/HEX
```

---

## 📊 Tab (Таб-лист)

Кастомный header/footer

| Команда | Описание | Право |
|---------|----------|-------|
| — | Нет команд, настраивается в конфиге | `managerfix.tab.use` |

**Конфигурация** (`modules/tab/config.yml`):
```yaml
header: |
  <#FFD700>⚡ ManagerFix Server ⚡
  <gray>Онлайн: <white>%server_online%</white>
footer: |
  <gray>TPS: %server_tps_1% | Ping: %player_ping%</gray>
update-interval-ticks: 20
```

---

## 📢 Announcer (Объявления)

Периодические сообщения

| Команда | Описание | Право |
|---------|----------|-------|
| — | Нет команд, настраивается в конфиге | — |

**Конфигурация** (`modules/announcer/config.yml`):
```yaml
interval-seconds: 60
broadcast-type: CHAT
messages:
  - "<gold>Добро пожаловать на наш сервер!</gold>"
  - "<yellow>Купить донат: <aqua>donate.example.com</aqua></yellow>"
```

---

## 🔧 Утилиты

### Команды консоли

Все команды могут быть выполнены из консоли, если не требуется аргумент игрока.

### Алиасы

Некоторые команды имеют короткие алиасы:
```
/cc          → /clearchat
/ec          → /enderchest
/v           → /vanish
/msg         → /pm
/tell        → /pm
```

---

## 📝 Примечания

1. **Кулдауны** — многие команды имеют задержку между использованиями (настраивается в `modules/*/commands.yml`)
2. **Обход прав** — `managerfix.bypass.cooldown` и `managerfix.bypass.limit` для обхода ограничений
3. **OP** — по умолчанию все права выданы операторам сервера
4. **PlaceholderAPI** — поддерживает плейсхолдеры в сообщениях и форматах

---

<div align="center">

**ManagerFix 1.0.0** | Полный список команд

Автор: **tg:fixsirt**

</div>
