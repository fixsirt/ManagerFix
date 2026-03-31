package ru.managerfix.modules.items;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.managerfix.ManagerFix;
import org.bukkit.command.CommandExecutor;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Команда /i с субкомандами: name, lore, amount, enchant, attribute, save, give, reload.
 */
public final class ItemsCommand implements CommandExecutor, TabCompleter {

    private final ItemsModule module;
    private final ManagerFix plugin;

    public ItemsCommand(ManagerFix plugin, ItemsModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игроки могут использовать эту команду.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "name":
                return handleName(player, Arrays.copyOfRange(args, 1, args.length));
            case "lore":
                return handleLore(player, Arrays.copyOfRange(args, 1, args.length));
            case "amount":
                return handleAmount(player, Arrays.copyOfRange(args, 1, args.length));
            case "enchant":
                return handleEnchant(player, Arrays.copyOfRange(args, 1, args.length));
            case "attribute":
                return handleAttribute(player, Arrays.copyOfRange(args, 1, args.length));
            case "save":
                return handleSave(player, Arrays.copyOfRange(args, 1, args.length));
            case "give":
                return handleGive(player, Arrays.copyOfRange(args, 1, args.length));
            case "reload":
                return handleReload(player);
            default:
                sendUsage(player);
                return true;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Использование: /i <name|lore|amount|enchant|attribute|save|give|reload>"));
    }

    private boolean handleName(Player player, String[] args) {
        if (!player.hasPermission("managerfix.items.name")) {
            sendMessage(player, "no-permission");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(player, "no-item");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /i name <новое имя>"));
            return true;
        }

        String newName = String.join(" ", args);
        Component nameComponent = MessageUtil.parse(newName);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(nameComponent);
        item.setItemMeta(meta);

        sendMessage(player, "name-changed");
        return true;
    }

    private boolean handleLore(Player player, String[] args) {
        if (!player.hasPermission("managerfix.items.lore")) {
            sendMessage(player, "no-permission");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(player, "no-item");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /i lore <новое описание>"));
            return true;
        }

        String newLore = String.join(" ", args);
        List<Component> loreComponents = Arrays.stream(newLore.split("\\\\n"))
                .map(MessageUtil::parse)
                .collect(Collectors.toList());

        ItemMeta meta = item.getItemMeta();
        meta.lore(loreComponents);
        item.setItemMeta(meta);

        sendMessage(player, "lore-changed");
        return true;
    }

    private boolean handleAmount(Player player, String[] args) {
        if (!player.hasPermission("managerfix.items.amount")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Использование: /i amount <число>"));
            return true;
        }

        try {
            int amount = Integer.parseInt(args[0]);
            if (amount <= 0) {
                sendMessage(player, "invalid-number");
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                sendMessage(player, "no-item");
                return true;
            }

            item.setAmount(amount);
            sendMessage(player, "amount-changed");
        } catch (NumberFormatException e) {
            sendMessage(player, "invalid-number");
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleEnchant(Player player, String[] args) {
        if (!player.hasPermission("managerfix.items.enchant")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Component.text("Использование: /i enchant <зачарование> <уровень>"));
            return true;
        }

        Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(args[0].toLowerCase()));
        if (ench == null) {
            sendMessage(player, "invalid-enchantment");
            return true;
        }

        try {
            int level = Integer.parseInt(args[1]);
            if (level <= 0) {
                sendMessage(player, "invalid-number");
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                sendMessage(player, "no-item");
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(ench, level, true);
            item.setItemMeta(meta);

            sendMessage(player, "enchantment-added");
        } catch (NumberFormatException e) {
            sendMessage(player, "invalid-number");
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleAttribute(Player player, String[] args) {
        if (!player.hasPermission("managerfix.items.attribute")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (args.length < 2 || args.length > 4) {
            player.sendMessage(Component.text("Использование: /i attribute <атрибут> <значение> [операция] [слот]"));
            return true;
        }

        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(args[0].toLowerCase()));
        if (attr == null) {
            sendMessage(player, "invalid-attribute");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(player, "invalid-number");
            return true;
        }

        AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
        if (args.length >= 3) {
            try {
                operation = AttributeModifier.Operation.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Неверная операция. Используйте ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1"));
                return true;
            }
        }

        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.HAND;
        if (args.length >= 4) {
            String slotStr = args[3].toUpperCase();
            switch (slotStr) {
                case "HAND":
                    slotGroup = EquipmentSlotGroup.HAND;
                    break;
                case "FEET":
                    slotGroup = EquipmentSlotGroup.FEET;
                    break;
                case "LEGS":
                    slotGroup = EquipmentSlotGroup.LEGS;
                    break;
                case "CHEST":
                    slotGroup = EquipmentSlotGroup.CHEST;
                    break;
                case "HEAD":
                    slotGroup = EquipmentSlotGroup.HEAD;
                    break;
                case "BODY":
                    slotGroup = EquipmentSlotGroup.BODY;
                    break;
                default:
                    player.sendMessage(Component.text("Неверный слот. Используйте HAND, FEET, LEGS, CHEST, HEAD, BODY"));
                    return true;
            }
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(player, "no-item");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey modKey = NamespacedKey.minecraft("custom");
        AttributeModifier modifier = new AttributeModifier(modKey, amount, operation, slotGroup);
        meta.addAttributeModifier(attr, modifier);
        item.setItemMeta(meta);

        sendMessage(player, "attribute-added");
        return true;
    }

    private boolean handleSave(Player player, String[] args) {
        if (!player.hasPermission("managerfix.items.save")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Использование: /i save <имя>"));
            return true;
        }

        String name = args[0];
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sendMessage(player, "no-item");
            return true;
        }

        // Сохранить в конфиг или SQL
        if (module.isSqlStorage() && plugin instanceof ManagerFix mf) {
            var sqlStorage = mf.getSqlItemsStorage();
            if (sqlStorage != null) {
                sqlStorage.saveItemAsync(name, item.serialize(), () ->
                    sendMessage(player, "item-saved", Map.of("name", name)));
            }
            // Возвращаем true, команда обработана (результат асинхронно)
            return true;
        } else {
            FileConfiguration savedConfig = module.getSavedConfig();
            if (savedConfig != null) {
                savedConfig.set("saved." + name, item.serialize());
                module.saveSavedConfig();
            }
        }

        sendMessage(player, "item-saved", Map.of("name", name));
        return true;
    }

    private boolean handleGive(Player sender, String[] args) {
        if (!sender.hasPermission("managerfix.items.give")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(Component.text("Использование: /i give <ник> <предмет> [количество]"));
            return true;
        }

        String targetName = args[0];
        String itemName = args[1];
        int amount = 1;
        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sendMessage(sender, "invalid-number");
                    return true;
                }
            } catch (NumberFormatException e) {
                sendMessage(sender, "invalid-number");
                return true;
            }
        }

        // Найти целевого игрока
        Player target = sender.getServer().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("§cИгрок " + targetName + " не найден."));
            return true;
        }

        // Загрузить из сохранённых (SQL или YAML)
        if (module.isSqlStorage() && plugin instanceof ManagerFix mf) {
            var sqlStorage = mf.getSqlItemsStorage();
            if (sqlStorage != null) {
                final Player finalTarget = target;
                final String finalItemName = itemName;
                final int finalAmount = amount;
                final CommandSender finalSender = sender;

                sqlStorage.getItemAsync(finalItemName, optData -> {
                    if (optData.isPresent()) {
                        try {
                            ItemStack itemToGive = ItemStack.deserialize(optData.get());
                            itemToGive.setAmount(Math.min(finalAmount, itemToGive.getMaxStackSize()));
                            finalTarget.getInventory().addItem(itemToGive);
                            finalSender.sendMessage(Component.text("§aПредмет \"" + finalItemName + "\" выдан игроку " + finalTarget.getName()));
                        } catch (Exception e) {
                            finalSender.sendMessage(Component.text("§cОшибка загрузки предмета!"));
                        }
                    } else {
                        giveMaterialItem(finalSender, finalTarget, finalItemName, finalAmount);
                    }
                });
                // Возвращаем true, так как команда обработана (результат будет асинхронно)
                return true;
            }
        } else {
            FileConfiguration savedConfig = module.getSavedConfig();
            if (savedConfig != null) {
                ConfigurationSection section = savedConfig.getConfigurationSection("saved." + itemName);
                if (section != null) {
                    Map<String, Object> data = section.getValues(false);
                    try {
                        ItemStack itemToGive = ItemStack.deserialize(data);
                        itemToGive.setAmount(Math.min(amount, itemToGive.getMaxStackSize()));
                        target.getInventory().addItem(itemToGive);
                        sender.sendMessage(Component.text("§aПредмет \"" + itemName + "\" выдан игроку " + target.getName()));
                        return true;
                    } catch (Exception e) {
                        sender.sendMessage(Component.text("§cОшибка загрузки предмета!"));
                    }
                }
            }
        }

        // Выдать материал
        if (sender instanceof Player) {
            giveMaterialItem(sender, target, itemName, amount);
        }
        return true;
    }
    
    private void giveMaterialItem(CommandSender sender, Player target, String itemName, int amount) {
        Material mat = Material.matchMaterial(itemName);
        if (mat == null) {
            if (sender instanceof Player p) {
                sendMessage(p, "invalid-material");
            }
            return;
        }
        ItemStack itemToGive = new ItemStack(mat, amount);
        target.getInventory().addItem(itemToGive);
        sender.sendMessage(Component.text("§aПредмет \"" + itemName + "\" выдан игроку " + target.getName()));
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("managerfix.items.reload")) {
            sendMessage(player, "no-permission");
            return true;
        }

        // Перезагрузить конфиг
        module.reloadConfig();
        sendMessage(player, "config-reloaded");
        return true;
    }

    private void sendMessage(Player player, String key) {
        sendMessage(player, key, Map.of());
    }

    private void sendMessage(Player player, String key, Map<String, String> placeholders) {
        FileConfiguration config = module.getModuleConfig();
        if (config != null) {
            String msg = config.getString("messages." + key, "&cСообщение не найдено: " + key);
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            Component comp = MessageUtil.parse(msg);
            player.sendMessage(comp);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("name", "lore", "amount", "enchant", "attribute", "save", "give", "reload");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("enchant")) {
                return Arrays.stream(Enchantment.values())
                        .map(ench -> ench.getKey().getKey())
                        .collect(Collectors.toList());
            }
            if (sub.equals("attribute")) {
                return Registry.ATTRIBUTE.stream()
                        .map(attr -> attr.getKey().getKey())
                        .collect(Collectors.toList());
            }
            if (sub.equals("give")) {
                // Автодополнение ников игроков
                return sender.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && "give".equals(args[0].toLowerCase())) {
            // Автодополнение предметов (сохранённые + материалы)
            List<String> suggestions = new ArrayList<>();
            FileConfiguration savedConfig = module.getSavedConfig();
            if (savedConfig != null && savedConfig.contains("saved")) {
                suggestions.addAll(savedConfig.getConfigurationSection("saved").getKeys(false));
            }
            suggestions.addAll(Registry.MATERIAL.stream()
                    .map(Material::name)
                    .collect(Collectors.toList()));
            return suggestions;
        }
        if (args.length == 3 && "attribute".equals(args[0].toLowerCase())) {
            return Arrays.stream(AttributeModifier.Operation.values())
                    .map(AttributeModifier.Operation::name)
                    .collect(Collectors.toList());
        }
        if (args.length == 4 && "attribute".equals(args[0].toLowerCase())) {
            return Arrays.asList("HAND", "FEET", "LEGS", "CHEST", "HEAD", "BODY");
        }
        if (args.length == 4 && "give".equals(args[0].toLowerCase())) {
            return Arrays.asList("1", "16", "32", "64");
        }
        return new ArrayList<>();
    }
}