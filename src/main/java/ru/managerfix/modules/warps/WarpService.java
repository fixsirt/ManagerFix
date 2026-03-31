package ru.managerfix.modules.warps;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.modules.tpa.TpaModule;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.UUID;

public class WarpService {

    private final JavaPlugin plugin;
    private final WarpConfig config;
    private final WarpsDataStorage dataStorage;

    public WarpService(JavaPlugin plugin, WarpConfig config, WarpsDataStorage dataStorage) {
        this.plugin = plugin;
        this.config = config;
        this.dataStorage = dataStorage;
    }

    public void teleportToWarp(Player player, String warpName) {
        Warp warp = dataStorage.getWarp(warpName);
        if (warp == null || !warp.isEnabled()) {
            player.sendMessage(MessageUtil.parse("<#FF4D00>Варп <#FFFFFF>" + warpName + "</#FFFFFF> не существует или выключен!"));
            return;
        }

        // Permission check
        if (!warp.getPermission().isEmpty() && !player.hasPermission(warp.getPermission())) {
            player.sendMessage(MessageUtil.parse("<#FF4D00>У вас нет прав для доступа к этому варпу!"));
            return;
        }

        // Делегируем анимацию/ожидание TPA-сервису (5 сек и та же логика отмены)
        if (plugin instanceof ManagerFix mf) {
            var tpaModule = mf.getModuleManager().getEnabledModule("tpa")
                .filter(m -> m instanceof TpaModule)
                .map(m -> (TpaModule) m)
                .orElse(null);
            if (tpaModule != null && tpaModule.getTpaService() != null) {
                // Проверяем задержку - если 0, то телепортируем мгновенно
                if (tpaModule.getTpaService().getTeleportDelay() <= 0) {
                    performTeleport(player, warp.getLocation());
                } else {
                    tpaModule.getTpaService().scheduleTeleport(player, warp.getLocation(), null);
                }
                return;
            }
        }
        // Фоллбек: мгновенный телепорт (если TPA модуль недоступен)
        performTeleport(player, warp.getLocation());
    }
    
    private void performTeleport(Player player, org.bukkit.Location loc) {
        try {
            player.teleport(loc);
            player.sendMessage(MessageUtil.parse("<#FAA300>Телепортация на варп <#FFFFFF>" + loc.getWorld().getName() + "</#FFFFFF>."));
        } catch (Exception e) {
            player.sendMessage(MessageUtil.parse("<#FF4D00>Не удалось телепортироваться: " + e.getMessage()));
            plugin.getLogger().warning("Warp teleport failed: " + e.getMessage());
        }
    }

    public void cancelTeleport(UUID uuid) {
        // Делегировано в TPA: перемещение отменяется его листенером
        // Метод оставлен для совместимости вызовов
    }

    public static void clearTask(UUID uuid) {
        // Нет локальных задач — очистка не требуется
    }
}
