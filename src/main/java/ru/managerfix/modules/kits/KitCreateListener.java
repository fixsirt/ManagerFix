package ru.managerfix.modules.kits;

import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.utils.MessageUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listener для создания нового кита через чат.
 */
public final class KitCreateListener implements Listener {

    private final ManagerFix plugin;
    private final KitsModule kitsModule;
    private final Player player;

    public KitCreateListener(ManagerFix plugin, KitsModule kitsModule, Player player) {
        this.plugin = plugin;
        this.kitsModule = kitsModule;
        this.player = player;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player)) return;

        event.setCancelled(true);
        String kitName = event.getMessage().trim();

        if (kitName.length() < 2 || kitName.length() > 20) {
            player.sendMessage(MessageUtil.parse("<#FF3366>Название должно быть от 2 до 20 символов!"));
            return;
        }

        // Проверяем, нет ли уже такого кита
        KitManager kitManager = kitsModule.getKitManager();
        if (kitManager.getKit(kitName).isPresent()) {
            player.sendMessage(MessageUtil.parse("<#FF3366>Кит с таким названием уже существует!"));
            return;
        }

        // Создаём новый кит с предметами из рук игрока
        ItemStack[] contents = player.getInventory().getContents();
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }

        if (items.isEmpty()) {
            player.sendMessage(MessageUtil.parse("<#FF3366>У вас нет предметов в инвентаре! Возьмите предметы и используйте /kit create <name>"));
            return;
        }

        KitData newKit = new KitData(kitName, 86400, "managerfix.kits.kit." + kitName.toLowerCase(), items);
        LoggerUtil.debug("[KitCreate] Creating kit: " + kitName + " with " + items.size() + " items");
        kitManager.saveKit(newKit);
        LoggerUtil.debug("[KitCreate] Kit save requested for: " + kitName);

        player.sendMessage(MessageUtil.parse("<#00C8FF>✓ Кит <#F0F4F8>" + kitName + "</#F0F4F8> создан!"));
        player.sendMessage(MessageUtil.parse("<#F0F4F8>Предметов: <#F0F4F8>" + items.size() + "</#F0F4F8></#F0F4F8>"));
        player.sendMessage(MessageUtil.parse("<#F0F4F8>КД: <#F0F4F8>24 часа</#F0F4F8> (измените через /editkits)</#F0F4F8>"));
        
        // Отключаем слушатель после обработки
        HandlerList.unregisterAll(this);
    }
}
