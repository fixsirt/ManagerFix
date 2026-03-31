package ru.managerfix.modules.kits;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.gui.GuiBuilder;
import ru.managerfix.gui.Button;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.*;

/**
 * Команда /editkits - Администрирование китов через GUI
 */
public final class EditKitsCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;
    private final KitsModule kitsModule;

    public EditKitsCommand(ManagerFix plugin, KitsModule kitsModule) {
        this.plugin = plugin;
        this.kitsModule = kitsModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("managerfix.command.editkits")) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.parse("<red>Эта команда доступна только игрокам!"));
            return true;
        }

        KitManager kitManager = kitsModule.getKitManager();
        if (kitManager == null) {
            player.sendMessage(MessageUtil.parse("<red>KitManager не инициализирован!"));
            return true;
        }

        // Открываем GUI со списком китов
        openKitsListGui(player);
        return true;
    }

    /**
     * Открывает GUI со списком всех китов.
     */
    private void openKitsListGui(Player player) {
        KitManager kitManager = kitsModule.getKitManager();
        List<String> kitNames = kitManager.getKitNames();
        
        // Отладка
        LoggerUtil.debug("[EditKits] Opening GUI with " + kitNames.size() + " kit(s): " + kitNames);

        int size = Math.max(54, ((kitNames.size() + 44) / 45) * 9);
        GuiBuilder builder = GuiBuilder.of(size)
                .title("<gradient:#ff9800:#ffc107>Редактирование китов (" + kitNames.size() + ")</gradient>")
                .holderId("editkits_list");

        // Заполняем китами (слоты 0-44)
        int slot = 0;
        for (String kitName : kitNames) {
            final int currentSlot = slot++;
            kitManager.getKit(kitName).ifPresent(kit -> {
                Material icon = getKitIcon(kitName);
                List<String> loreStrings = new ArrayList<>();
                loreStrings.add("<gray>Нажмите для редактирования</gray>");
                loreStrings.add("");
                loreStrings.add("<gray>КД: <white>" + formatCooldown(kit.getCooldownSeconds()) + "</white></gray>");
                loreStrings.add("<gray>Предметов: <white>" + kit.getItems().size() + "</white></gray>");
                loreStrings.add("<gray>Приоритет: <white>" + kit.getPriority() + "</white></gray>");
                loreStrings.add("<gray>Разрешение: <white>" + kit.getPermission() + "</white></gray>");
                if (kit.isOneTime()) {
                    loreStrings.add("<green>✓ Только один раз</green>");
                }

                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                for (String s : loreStrings) {
                    lore.add(MessageUtil.parse(s));
                }

                builder.button(currentSlot, Button.builder(new ItemBuilder(icon)
                                .name(MessageUtil.parse("<gold>Кит: " + kit.getName() + "</gold>"))
                                .lore(lore)
                                .build())
                        .onClick(e -> openKitEditGui(player, kit))
                        .build());
            });
        }

        // Кнопка "Создать кит" (в конце)
        builder.button(size - 9, Button.builder(new ItemBuilder(Material.LIME_DYE)
                        .name(MessageUtil.parse("<green>Создать новый кит</green>"))
                        .lore(MessageUtil.parse("<gray>Напишите название кита в чат</gray>"))
                        .build())
                .onClick(e -> {
                    player.closeInventory();
                    player.sendMessage(MessageUtil.parse("<green>Введите название нового кита в чат:"));
                    new KitCreateListener(plugin, kitsModule, player);
                })
                .build());

        // Кнопка "Обновить"
        builder.button(size - 5, Button.builder(new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.parse("<yellow>Обновить список</yellow>"))
                        .build())
                .onClick(e -> openKitsListGui(player))
                .build());

        builder.open(player);
    }

    /**
     * Открывает GUI выбора иконки кита.
     */
    public void openIconSelectionGui(Player player, KitData kit) {
        GuiBuilder builder = GuiBuilder.of(54)
                .title("<gradient:#ff9800:#ffc107>Выберите иконку для: " + kit.getName() + "</gradient>")
                .holderId("kit_icon_" + kit.getName());

        // Заполняем рамку стеклом (слоты 0-8, 18, 27, 36, 45-53)
        int[] frameSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 27, 36, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : frameSlots) {
            builder.button(slot, Button.builder(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                            .name(MessageUtil.parse(" "))
                            .build())
                    .onClick(e -> {
                        e.setCancelled(true);
                        ((Player) e.getWhoClicked()).updateInventory();
                    })
                    .build());
        }

        // Пустой слот для иконки (22)
        builder.button(22, Button.builder(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .name(MessageUtil.parse("<yellow>Положите предмет сюда</yellow>"))
                        .lore(MessageUtil.parse("<gray>Кликните с предметом в руке</gray>"),
                              MessageUtil.parse("<gray>или перетащите сюда</gray>"))
                        .build())
                .onClick(e -> {
                    e.setCancelled(true);
                    Player p = (Player) e.getWhoClicked();
                    ItemStack cursor = p.getItemOnCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        setKitIcon(p, kit, cursor.getType());
                    } else {
                        p.sendMessage("§cВозьмите предмет в руку!");
                    }
                })
                .build());

        // Кнопка "Назад" (31)
        builder.button(31, Button.builder(new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.parse("<yellow>← Назад</yellow>"))
                        .lore(MessageUtil.parse("<gray>Вернуться к редактированию</gray>"))
                        .build())
                .onClick(e -> {
                    e.setCancelled(true);
                    openKitEditGui((Player) e.getWhoClicked(), kit);
                })
                .build());

        builder.open(player);
    }

    /**
     * Устанавливает иконку кита.
     */
    private void setKitIcon(Player player, KitData kit, Material material) {
        kit.setIconMaterial(material.name());
        kitsModule.getKitManager().saveKit(kit);
        player.sendMessage("§a✓ Иконка кита установлена: §f" + material.name());
        
        // Получаем обновлённый кит из кэша и открываем меню
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            KitData updatedKit = kitsModule.getKitManager().getKit(kit.getName()).orElse(kit);
            openKitEditGui(player, updatedKit);
        }, 2L);
    }

    /**
     * Открывает GUI редактирования конкретного кита.
     */
    public void openKitEditGui(Player player, KitData kit) {
        // Получаем свежий кит из кэша
        KitData freshKit = kitsModule.getKitManager().getKit(kit.getName()).orElse(kit);
        
        GuiBuilder builder = GuiBuilder.of(54)
                .title("<gradient:#ff9800:#ffc107>Кит: " + freshKit.getName() + "</gradient>")
                .holderId("kit_edit_" + freshKit.getName());

        // Верхняя панель (0-8): Кнопки управления
        // Переименовать (0)
        builder.button(0, Button.builder(new ItemBuilder(Material.NAME_TAG)
                        .name(MessageUtil.parse("<blue>✎ Переименовать</blue>"))
                        .lore(MessageUtil.parse("<gray>Нажмите и введите</gray>"),
                              MessageUtil.parse("<gray>новое название в чат</gray>"))
                        .build())
                .onClick(e -> {
                    player.closeInventory();
                    player.sendMessage(MessageUtil.parse("<green>Введите новое название кита:"));
                    new KitRenameListener(plugin, kitsModule, player, kit);
                })
                .build());

        // Назад к списку (4)
        builder.button(4, Button.builder(new ItemBuilder(Material.BOOK)
                        .name(MessageUtil.parse("<yellow>← Назад к списку</yellow>"))
                        .lore(MessageUtil.parse("<gray>Вернуться к списку китов</gray>"))
                        .build())
                .onClick(e -> openKitsListGui(player))
                .build());

        // Удалить кит (8)
        builder.button(8, Button.builder(new ItemBuilder(Material.BARRIER)
                        .name(MessageUtil.parse("<red>⚠ Удалить кит</red>"))
                        .lore(MessageUtil.parse("<gray>Нажмите для удаления</gray>"),
                              MessageUtil.parse("<red>⚠ Действие необратимо!</red>"))
                        .build())
                .onClick(e -> {
                    player.closeInventory();
                    player.sendMessage(MessageUtil.parse("<yellow>Вы уверены? Напишите <gold>да</gold> для удаления кита <white>" + kit.getName() + "</white>:"));
                    new KitDeleteConfirmListener(plugin, kitsModule, player, kit);
                })
                .build());

        // Средняя панель (9-17): Информация и настройки
        // Приоритет - (10)
        builder.button(10, Button.builder(new ItemBuilder(Material.REDSTONE_TORCH)
                        .name(MessageUtil.parse("<red>▼ Приоритет -1</red>"))
                        .lore(MessageUtil.parse("<gray>Текущий: <white>" + kit.getPriority() + "</white></gray>"),
                              MessageUtil.parse("<gray>Клик: -1</gray>"))
                        .build())
                .onClick(e -> {
                    kit.setPriority(kit.getPriority() - 1);
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<green>Приоритет: " + kit.getPriority()));
                    openKitEditGui(player, kit);
                })
                .build());

        // Приоритет + (16)
        builder.button(16, Button.builder(new ItemBuilder(Material.LEVER)
                        .name(MessageUtil.parse("<green>▲ Приоритет +1</green>"))
                        .lore(MessageUtil.parse("<gray>Текущий: <white>" + kit.getPriority() + "</white></gray>"),
                              MessageUtil.parse("<gray>Клик: +1</gray>"))
                        .build())
                .onClick(e -> {
                    kit.setPriority(kit.getPriority() + 1);
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<green>Приоритет: " + kit.getPriority()));
                    openKitEditGui(player, kit);
                })
                .build());

        // КД - (11)
        builder.button(11, Button.builder(new ItemBuilder(Material.CLOCK)
                        .name(MessageUtil.parse("<red>▼ КД -1 час</red>"))
                        .lore(MessageUtil.parse("<gray>Текущий: <white>" + formatCooldown(kit.getCooldownSeconds()) + "</white></gray>"))
                        .build())
                .onClick(e -> {
                    int newCd = Math.max(0, kit.getCooldownSeconds() - 3600);
                    kit.setCooldownSeconds(newCd);
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<green>КД: " + formatCooldown(newCd)));
                    openKitEditGui(player, kit);
                })
                .build());

        // КД + (15)
        builder.button(15, Button.builder(new ItemBuilder(Material.SUNFLOWER)
                        .name(MessageUtil.parse("<green>▲ КД +1 час</green>"))
                        .lore(MessageUtil.parse("<gray>Текущий: <white>" + formatCooldown(kit.getCooldownSeconds()) + "</white></gray>"))
                        .build())
                .onClick(e -> {
                    kit.setCooldownSeconds(kit.getCooldownSeconds() + 3600);
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<green>КД: " + formatCooldown(kit.getCooldownSeconds())));
                    openKitEditGui(player, kit);
                })
                .build());

        // One-time toggle (13)
        builder.button(13, Button.builder(new ItemBuilder(kit.isOneTime() ? Material.LIME_DYE : Material.GRAY_DYE)
                        .name(MessageUtil.parse(kit.isOneTime() ? "<green>✓ Только один раз: ВКЛ</green>" : "<red>✗ Только один раз: ВЫКЛ</red>"))
                        .lore(MessageUtil.parse("<gray>Клик для переключения</gray>"),
                              kit.isOneTime() ? MessageUtil.parse("<green>Игрок получит 1 раз</green>") : MessageUtil.parse("<gray>Можно брать многократно</gray>"))
                        .build())
                .onClick(e -> {
                    kit.setOneTime(!kit.isOneTime());
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<green>Режим: " + (kit.isOneTime() ? "Только 1 раз" : "Многократно")));
                    openKitEditGui(player, kit);
                })
                .build());

        // Разрешение (12)
        builder.button(12, Button.builder(new ItemBuilder(Material.BOOK)
                        .name(MessageUtil.parse("<blue>✎ Разрешение</blue>"))
                        .lore(MessageUtil.parse("<gray>Текущее: <white>" + kit.getPermission() + "</white></gray>"),
                              MessageUtil.parse("<gray>Кликните для изменения</gray>"))
                        .build())
                .onClick(e -> {
                    player.closeInventory();
                    player.sendMessage(MessageUtil.parse("<green>Введите новое разрешение:"));
                    new KitPermissionListener(plugin, kitsModule, player, kit);
                })
                .build());

        // Нижняя панель (45-53): Предметы кита
        // Кнопка "Назад" (45)
        builder.button(45, Button.builder(new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.parse("<yellow>← Назад</yellow>"))
                        .lore(MessageUtil.parse("<gray>К списку китов</gray>"))
                        .build())
                .onClick(e -> openKitsListGui(player))
                .build());

        // Кнопка "Иконка кита" (47)
        Material currentIcon = freshKit.getIcon();
        builder.button(47, Button.builder(new ItemBuilder(currentIcon)
                        .name(MessageUtil.parse("<blue>✎ Иконка кита</blue>"))
                        .lore(MessageUtil.parse("<gray>Текущая: <white>" + currentIcon.name() + "</white></gray>"),
                              MessageUtil.parse("<gray>Кликните для выбора</gray>"))
                        .build())
                .onClick(e -> {
                    openIconSelectionGui(player, freshKit);
                })
                .build());

        // Кнопка "Сохранить предметы" (49)
        builder.button(49, Button.builder(new ItemBuilder(Material.EMERALD)
                        .name(MessageUtil.parse("<green>✓ Сохранить предметы</green>"))
                        .lore(MessageUtil.parse("<gray>Сохраняет предметы</gray>"),
                              MessageUtil.parse("<gray>из вашего инвентаря</gray>"))
                        .build())
                .onClick(e -> {
                    ItemStack[] contents = player.getInventory().getContents();
                    List<ItemStack> items = new ArrayList<>();
                    for (ItemStack item : contents) {
                        if (item != null && !item.getType().isAir()) {
                            items.add(item.clone());
                        }
                    }
                    kit.setItems(items);
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<green>✓ Предметы сохранены! (" + items.size() + " шт.)"));
                    openKitEditGui(player, kit);
                })
                .build());

        // Кнопка "Заполнить из инвентаря" (51)
        builder.button(51, Button.builder(new ItemBuilder(Material.CHEST)
                        .name(MessageUtil.parse("<blue>📦 Заполнить из инвентаря</blue>"))
                        .lore(MessageUtil.parse("<gray>Копирует предметы</gray>"),
                              MessageUtil.parse("<gray>из вашего инвентаря</gray>"))
                        .build())
                .onClick(e -> {
                    ItemStack[] contents = player.getInventory().getContents();
                    List<ItemStack> items = new ArrayList<>();
                    for (ItemStack item : contents) {
                        if (item != null && !item.getType().isAir()) {
                            items.add(item.clone());
                        }
                    }
                    kit.setItems(items);
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<green>✓ Инвентарь скопирован! (" + items.size() + " шт.)"));
                    openKitEditGui(player, kit);
                })
                .build());

        // Кнопка "Очистить предметы" (53)
        builder.button(53, Button.builder(new ItemBuilder(Material.BARRIER)
                        .name(MessageUtil.parse("<red>✗ Очистить предметы</red>"))
                        .lore(MessageUtil.parse("<gray>Удаляет все предметы</gray>"),
                              MessageUtil.parse("<red>⚠ Действие необратимо!</red>"))
                        .build())
                .onClick(e -> {
                    kit.setItems(null);
                    kitsModule.getKitManager().saveKit(kit);
                    player.sendMessage(MessageUtil.parse("<red>✓ Предметы удалены!"));
                    openKitEditGui(player, kit);
                })
                .build());

        // Отображение предметов кита (18-44, 27 слотов)
        List<ItemStack> kitItems = kit.getItems();
        for (int i = 0; i < 27 && i < kitItems.size(); i++) {
            ItemStack item = kitItems.get(i);
            if (item != null && !item.getType().isAir()) {
                final int slot = 18 + i;
                final int itemIndex = i;
                builder.button(slot, Button.builder(item.clone())
                        .onClick(e -> {
                            // Удаляем предмет из кита
                            List<ItemStack> currentItems = kit.getItems();
                            currentItems.remove(itemIndex);
                            kit.setItems(currentItems);
                            kitsModule.getKitManager().saveKit(kit);
                            player.sendMessage(MessageUtil.parse("<yellow>Предмет удалён из кита"));
                            openKitEditGui(player, kit);
                        })
                        .build());
            }
        }

        builder.open(player);
    }

    private Material getKitIcon(String kitName) {
        // Проверяем есть ли сохранённая иконка в ките
        KitManager kitManager = kitsModule.getKitManager();
        Optional<KitData> kitOpt = kitManager.getKit(kitName);
        if (kitOpt.isPresent()) {
            return kitOpt.get().getIcon();
        }

        // Авто-выбор по названию кита если кит не найден
        String lower = kitName.toLowerCase();
        if (lower.contains("warrior") || lower.contains("fight")) return Material.DIAMOND_SWORD;
        if (lower.contains("miner") || lower.contains("pick")) return Material.DIAMOND_PICKAXE;
        if (lower.contains("archer") || lower.contains("bow")) return Material.BOW;
        if (lower.contains("armor") || lower.contains("def")) return Material.DIAMOND_CHESTPLATE;
        if (lower.contains("food") || lower.contains("eat")) return Material.APPLE;
        if (lower.contains("vip") || lower.contains("premium")) return Material.GOLD_INGOT;
        return Material.CHEST;
    }

    private String formatCooldown(int seconds) {
        if (seconds <= 0) return "<green>Нет</green>";
        
        StringBuilder sb = new StringBuilder();
        int days = seconds / 86400;
        int hours = (seconds % 86400) / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("с");

        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
