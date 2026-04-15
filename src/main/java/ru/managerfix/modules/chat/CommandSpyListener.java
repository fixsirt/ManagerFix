package ru.managerfix.modules.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.managerfix.ManagerFix;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

/**
 * Notifies players with commandspy enabled when another player runs a command.
 */
public final class CommandSpyListener implements Listener {

    private final ManagerFix plugin;
    private final ProfileManager profileManager;

    public CommandSpyListener(ManagerFix plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isBlank()) return;
        String lang = plugin.getConfigManager().getDefaultLanguage();
        String formatKey = "chat.commandspy-format";
        String raw = MessageUtil.getRaw(plugin, lang, formatKey);
        String format = raw != null ? raw : "<#F0F4F8>[CommandSpy] <#F0F4F8>{player}: <#F0F4F8>{command}</#F0F4F8></#F0F4F8>";
        String line = format.replace("{player}", sender.getName()).replace("{command}", message.trim());

        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (other.equals(sender)) continue;
            if (!other.hasPermission("managerfix.chat.commandspy")) continue;
            if (!CommandSpyCommand.isCommandSpyEnabled(other, profileManager)) continue;
            other.sendMessage(MessageUtil.parse(line));
        }
    }
}
