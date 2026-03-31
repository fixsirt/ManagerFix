package ru.managerfix.modules.kits;

import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener для подтверждения удаления кита через чат.
 */
public final class KitDeleteConfirmListener implements Listener {

    private final ManagerFix plugin;
    private final KitsModule kitsModule;
    private final Player player;
    private final KitData kit;

    public KitDeleteConfirmListener(ManagerFix plugin, KitsModule kitsModule, Player player, KitData kit) {
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
        String response = event.getMessage().trim().toLowerCase();

        if ("да".equals(response) || "yes".equals(response) || "confirm".equals(response)) {
            // Реально удаляем кит из БД
            kitsModule.getKitManager().deleteKit(kit.getName());
            player.sendMessage(MessageUtil.parse("<green>✓ Кит <white>" + kit.getName() + "</white> удалён!"));
        } else {
            player.sendMessage(MessageUtil.parse("<red>Удаление отменено."));
        }
        
        // Отключаем слушатель после обработки
        HandlerList.unregisterAll(this);
    }
}
