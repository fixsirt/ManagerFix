package ru.managerfix.modules.other;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.managerfix.ManagerFix;
import ru.managerfix.modules.tpa.TpaModule;
import ru.managerfix.service.TeleportService;

public final class DefaultTeleportService implements TeleportService {

    private final ManagerFix plugin;

    public DefaultTeleportService(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public void teleport(Player player, Location destination, Runnable onCancel) {
        if (player == null || destination == null || destination.getWorld() == null) return;
        var tpaMod = plugin.getModuleManager().getEnabledModule("tpa")
            .filter(m -> m instanceof TpaModule)
            .map(m -> (TpaModule) m)
            .orElse(null);
        if (tpaMod != null && tpaMod.getTpaService() != null) {
            tpaMod.getTpaService().scheduleTeleport(player, destination, onCancel);
            return;
        }
        player.teleport(destination);
    }
}
