package ru.managerfix.modules.names;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.LoggerUtil;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Map;
import java.util.UUID;

/**
 * Centralizes nick change flow: apply, notify, broadcast, debug log.
 * Used by NickCommand and NickAdminCommand.
 */
public final class NamesService {

    private final ManagerFix plugin;
    private final NamesModule module;
    private final ProfileManager profileManager;

    public NamesService(ManagerFix plugin, NamesModule module, ProfileManager profileManager) {
        this.plugin = plugin;
        this.module = module;
        this.profileManager = profileManager;
    }

    /** Apply nick for self (/nick). Call after validation. Fires event with changedBy=null. */
    public void setNickForSelf(Player player, String newNickRaw) {
        module.setNick(player, profileManager, newNickRaw, null, () -> {
            MessageUtil.send(plugin, player, "names.changed-self", Map.of("nick", newNickRaw != null ? newNickRaw : ""));
            LoggerUtil.debug("[Names] Player " + player.getName() + " changed own nick to " + (newNickRaw != null ? newNickRaw : "(reset)"));
        });
    }

    /** Reset nick for self (/nick reset). Sends names.reset message. */
    public void setNickForSelfReset(Player player) {
        module.setNick(player, profileManager, null, null, () -> {
            MessageUtil.send(plugin, player, "names.reset");
            LoggerUtil.debug("[Names] Player " + player.getName() + " reset own nick");
        });
    }

    /** Apply or reset nick by admin. Target may be online or offline (profile only). Notifies admin and target, optional broadcast. */
    public void setNickByAdmin(Player admin, UUID targetUuid, String newNickOrReset) {
        final String targetName;
        {
            String n = Bukkit.getOfflinePlayer(targetUuid).getName();
            targetName = n != null ? n : targetUuid.toString();
        }

        final Player target = Bukkit.getPlayer(targetUuid);
        final String displayNick = "reset".equalsIgnoreCase(newNickOrReset) || newNickOrReset == null || newNickOrReset.isEmpty()
                ? targetName : newNickOrReset;

        String oldNick = null;
        if (target != null && target.isOnline()) {
            oldNick = module.getStoredNick(target, profileManager);
        } else {
            oldNick = profileManager.getCachedProfile(targetUuid)
                    .flatMap(p -> p.getMetadata(NamesModule.getNickMetadataKey()))
                    .filter(v -> v instanceof String).map(v -> (String) v).orElse(null);
        }
        final String oldNickFinal = oldNick;
        final boolean isReset = "reset".equalsIgnoreCase(newNickOrReset) || newNickOrReset == null || newNickOrReset.isEmpty();

        Runnable afterSave = () -> {
            MessageUtil.send(plugin, admin, "names.changed-admin",
                    Map.of("player", targetName, "nick", displayNick));
            if (target != null && target.isOnline()) {
                String msgNick = isReset ? targetName : newNickOrReset;
                MessageUtil.send(plugin, target, "names.changed-notify", Map.of("nick", msgNick));
            }
            if (module.isAdminChangeBroadcast()) {
                Component broadcast = MessageUtil.get(plugin, admin, "names.admin-change-broadcast-msg",
                        Map.of("admin", admin.getName(), "player", targetName, "nick", displayNick));
                Bukkit.getServer().sendMessage(broadcast);
            }
            LoggerUtil.debug("[Names] Admin " + admin.getName() + " changed nick of " + targetName + " from " + (oldNickFinal != null ? oldNickFinal : "(none)") + " to " + displayNick);
        };

        String toSet = isReset ? null : newNickOrReset;
        if (target != null && target.isOnline()) {
            module.setNick(target, profileManager, toSet, admin.getUniqueId(), afterSave);
        } else {
            module.setNickOffline(targetUuid, profileManager, toSet, admin.getUniqueId(), afterSave);
        }
    }
}
