package ru.managerfix.modules.kits;

import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener для переименования кита через чат.
 */
public final class KitRenameListener implements Listener {

    private final ManagerFix plugin;
    private final KitsModule kitsModule;
    private final Player player;
    private final KitData kit;

    public KitRenameListener(ManagerFix plugin, KitsModule kitsModule, Player player, KitData kit) {
        this.plugin = plugin;
        this.kitsModule = kitsModule;
        this.player = player;
        this.kit = kit;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player)) return;

        event.setCancelled(true);
        String newName = event.getMessage().trim();

        if (newName.length() < 2 || newName.length() > 20) {
            player.sendMessage(MessageUtil.parse("<#FF3366>Название должно быть от 2 до 20 символов!"));
            return;
        }

        // Проверяем, нет ли уже такого кита
        KitManager kitManager = kitsModule.getKitManager();
        if (kitManager.getKit(newName).isPresent() && !newName.equals(kit.getName())) {
            player.sendMessage(MessageUtil.parse("<#FF3366>Кит с таким названием уже существует!"));
            return;
        }

        // Создаём новый кит с новым именем и удаляем старый
        KitData newKit = new KitData(newName, kit.getCooldownSeconds(), kit.getPermission(), kit.getItems());
        newKit.setPriority(kit.getPriority());
        newKit.setOneTime(kit.isOneTime());
        kitManager.saveKit(newKit);

        // Удаляем старый кит
        kitManager.getStorage().saveKit(kit); // Сохраняем изменения (если были)

        player.sendMessage(MessageUtil.parse("<#00C8FF>✓ Кит переименован: <#F0F4F8>" + kit.getName() + "</#F0F4F8> → <#F0F4F8>" + newName + "</#F0F4F8>"));
        
        // Отключаем слушатель после обработки
        HandlerList.unregisterAll(this);
    }
}
