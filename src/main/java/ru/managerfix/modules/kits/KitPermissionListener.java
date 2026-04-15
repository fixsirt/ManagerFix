package ru.managerfix.modules.kits;

import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener для изменения разрешения кита через чат.
 */
public final class KitPermissionListener implements Listener {

    private final ManagerFix plugin;
    private final KitsModule kitsModule;
    private final Player player;
    private final KitData kit;

    public KitPermissionListener(ManagerFix plugin, KitsModule kitsModule, Player player, KitData kit) {
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
        String permission = event.getMessage().trim();

        if (permission.isEmpty() || permission.contains(" ")) {
            player.sendMessage(MessageUtil.parse("<#FF3366>Разрешение не должно содержать пробелов!"));
            return;
        }

        kit.setPermission(permission);
        kitsModule.getKitManager().saveKit(kit);

        player.sendMessage(MessageUtil.parse("<#00C8FF>✓ Разрешение установлено: <#F0F4F8>" + permission + "</#F0F4F8>"));
        
        // Отключаем слушатель после обработки
        HandlerList.unregisterAll(this);
    }
}
