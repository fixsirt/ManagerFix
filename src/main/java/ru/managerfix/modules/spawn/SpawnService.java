package ru.managerfix.modules.spawn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.ManagerFix;
import ru.managerfix.modules.tpa.TpaModule;
import ru.managerfix.utils.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnService {

    private final JavaPlugin plugin;
    private final SpawnConfig config;

    public SpawnService(JavaPlugin plugin, SpawnConfig config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void teleportToSpawn(Player player) {
        Location spawnLoc = config.getSpawnLocation();
        if (spawnLoc == null) {
            player.sendMessage(MessageUtil.parse("<#FF4D00>Спавн не установлен!"));
            return;
        }

        if (config.isSafeTeleport()) {
            if (!isSafe(spawnLoc)) {
                Location safeLoc = findSafeLocation(spawnLoc);
                if (safeLoc != null) {
                    spawnLoc = safeLoc;
                } else {
                    player.sendMessage(MessageUtil.parse("<#FF4D00>Не удалось найти безопасное место для телепортации!"));
                    return;
                }
            }
        }

        // Делегируем анимацию/ожидание TPA-сервису (5 сек и та же логика отмены)
        if (plugin instanceof ManagerFix mf) {
            var tpaModule = mf.getModuleManager().getEnabledModule("tpa")
                .filter(m -> m instanceof TpaModule)
                .map(m -> (TpaModule) m)
                .orElse(null);
            if (tpaModule != null && tpaModule.getTpaService() != null) {
                tpaModule.getTpaService().scheduleTeleport(player, spawnLoc, null);
                return;
            }
        }
        // Фоллбек: мгновенный телепорт (если TPA модуль недоступен)
        player.teleport(spawnLoc);
        player.sendMessage(MessageUtil.parse("<#FAA300>Телепортация на спавн."));
    }

    public void cancelTeleport(UUID uuid) {
        if (plugin instanceof ManagerFix mf) {
            var tpaModule = mf.getModuleManager().getEnabledModule("tpa")
                .filter(m -> m instanceof TpaModule)
                .map(m -> (TpaModule) m)
                .orElse(null);
            if (tpaModule != null && tpaModule.getTpaService() != null) {
                tpaModule.getTpaService().cancelPendingTeleport(uuid);
            }
        }
    }

    private boolean isSafe(Location loc) {
        Block feet = loc.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        return !feet.getType().isSolid() && !head.getType().isSolid() && ground.getType().isSolid() 
                && ground.getType() != Material.LAVA && ground.getType() != Material.FIRE;
    }

    private Location findSafeLocation(Location loc) {
        for (int y = -3; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location check = loc.clone().add(x, y, z);
                    if (isSafe(check)) return check;
                }
            }
        }
        return null;
    }
}
