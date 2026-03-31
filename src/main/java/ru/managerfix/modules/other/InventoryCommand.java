package ru.managerfix.modules.other;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InventoryCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public InventoryCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if ("ec".equals(cmd) || "enderchest".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.ec")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (args.length > 0) {
                if (!sender.hasPermission("managerfix.other.ec.others")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
                if (target == null || !target.isOnline()) {
                    MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
                    return true;
                }
                player.openInventory(target.getEnderChest());
                module.logAdminAction("[Other] " + sender.getName() + " opened ender chest of " + target.getName());
                return true;
            }
            player.openInventory(player.getEnderChest());
            return true;
        }
        if ("invsee".equals(cmd)) {
            if (!sender.hasPermission("managerfix.other.invsee")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            if (args.length < 1) {
                MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/invsee <name>"));
                return true;
            }
            
            // Пробуем найти онлайн игрока
            Player targetOnline = ru.managerfix.utils.NickResolver.resolve(args[0]);
            OfflinePlayer target;
            
            if (targetOnline != null && targetOnline.isOnline()) {
                // Игрок онлайн - используем его инвентарь
                target = targetOnline;
            } else {
                // Игрок офлайн - загружаем из файла
                target = Bukkit.getOfflinePlayer(args[0]);
                if (target == null || !target.hasPlayedBefore()) {
                    MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
                    return true;
                }
            }
            
            InvseeHolder holder = new InvseeHolder(target.getUniqueId());
            Inventory inv = Bukkit.createInventory(holder, 45, Component.text("Invsee: " + target.getName()));
            holder.setInventory(inv);
            
            if (target.isOnline() && target.getPlayer() != null) {
                // Игрок онлайн - копируем инвентарь напрямую
                ItemStack[] contents = target.getPlayer().getInventory().getContents();
                for (int i = 0; i < 36 && i < contents.length; i++) {
                    ItemStack it = contents[i];
                    if (it != null && !it.getType().isAir()) {
                        inv.setItem(i, it.clone());
                    }
                }
                ItemStack[] armor = target.getPlayer().getInventory().getArmorContents();
                if (armor.length > 0 && armor[0] != null) inv.setItem(36, armor[0].clone());
                if (armor.length > 1 && armor[1] != null) inv.setItem(37, armor[1].clone());
                if (armor.length > 2 && armor[2] != null) inv.setItem(38, armor[2].clone());
                if (armor.length > 3 && armor[3] != null) inv.setItem(39, armor[3].clone());
                ItemStack offhand = target.getPlayer().getInventory().getItemInOffHand();
                if (offhand != null && !offhand.getType().isAir()) {
                    inv.setItem(40, offhand.clone());
                }
            } else {
                // Игрок офлайн - загружаем инвентарь из файла данных через NBT
                try {
                    File worldContainer = Bukkit.getServer().getWorldContainer();
                    File playerFile = new File(new File(worldContainer, "world"), 
                        "playerdata/" + target.getUniqueId() + ".dat");
                    if (!playerFile.exists()) {
                        // Пробуем другие миры
                        File nether = new File(new File(worldContainer, "world_nether"), 
                            "playerdata/" + target.getUniqueId() + ".dat");
                        File end = new File(new File(worldContainer, "world_the_end"), 
                            "playerdata/" + target.getUniqueId() + ".dat");
                        if (nether.exists()) playerFile = nether;
                        else if (end.exists()) playerFile = end;
                    }
                    
                    if (playerFile.exists()) {
                        // Используем reflection для загрузки NBT данных
                        var method = Bukkit.getServer().getClass().getMethod("loadPlayerData", UUID.class);
                        if (method != null) {
                            var nbtCompound = method.invoke(Bukkit.getServer(), target.getUniqueId());
                            if (nbtCompound != null) {
                                var getListMethod = nbtCompound.getClass().getMethod("getList", String.class, int.class);
                                var inventoryList = getListMethod.invoke(nbtCompound, "Inventory", (byte) 10);
                                if (inventoryList != null) {
                                    var sizeMethod = inventoryList.getClass().getMethod("size");
                                    int size = (int) sizeMethod.invoke(inventoryList);
                                    for (int i = 0; i < size; i++) {
                                        var getMethod = inventoryList.getClass().getMethod("get", int.class);
                                        var itemTag = getMethod.invoke(inventoryList, i);
                                        try {
                                            // Конвертируем NBT в Bukkit ItemStack через serialize
                                            var serializeMethod = itemTag.getClass().getMethod("serialize");
                                            var serialized = serializeMethod.invoke(itemTag);
                                            if (serialized instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> itemMap = (Map<String, Object>) serialized;
                                                var item = org.bukkit.inventory.ItemStack.deserialize(itemMap);
                                                var getByteMethod = itemTag.getClass().getMethod("getByte", String.class);
                                                byte slot = (byte) getByteMethod.invoke(itemTag, "Slot");
                                                if (slot >= 0 && slot < 36) {
                                                    inv.setItem(slot, item);
                                                } else if (slot == 100) {
                                                    inv.setItem(40, item); // offhand
                                                } else if (slot >= -106 && slot <= -99) {
                                                    int armorSlot = slot + 100;
                                                    if (armorSlot >= 0 && armorSlot <= 3) {
                                                        inv.setItem(36 + armorSlot, item);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Ignore invalid items
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Не удалось загрузить инвентарь, оставляем пустым
                }
            }
            
            player.openInventory(inv);
            module.logAdminAction("[Other] " + sender.getName() + " opened invsee for " + target.getName());
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if ("ec".equalsIgnoreCase(command.getName()) && args.length == 1) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        if ("invsee".equalsIgnoreCase(command.getName()) && args.length == 1) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        return Collections.emptyList();
    }
}
