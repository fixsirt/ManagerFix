package ru.managerfix.modules.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.managerfix.utils.MessageUtil;


/**
 * Applies chat module format to join, quit and death messages (MiniMessage).
 * Runs at HIGH so other plugins (e.g. Vanish at HIGHEST) can still override.
 * <p>
 * The {player} placeholder is replaced by splitting the format around it and inserting
 * the player's displayName() as a Component, so nick colors are fully preserved.
 */
public final class ChatJoinQuitDeathListener implements Listener {

    private final ChatModule module;

    public ChatJoinQuitDeathListener(ChatModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        String format = module.getFormatJoin();
        if (format == null || format.isEmpty()) return;
        event.joinMessage(buildMessage(format, event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        String format = module.getFormatQuit();
        if (format == null || format.isEmpty()) return;
        event.quitMessage(buildMessage(format, event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        String format = module.getFormatDeath();
        if (format == null || format.isEmpty()) return;
        Player player = event.getEntity();
        String resolved = format.replace("{message}", "");
        event.deathMessage(buildMessage(resolved, player));
    }

    /**
     * Builds a Component from format, inserting player.displayName() as a Component
     * at the {player} position so nick colors are preserved.
     */
    private static Component buildMessage(String format, Player player) {
        format = MessageUtil.setPlaceholders(player, format);
        String[] parts = format.split("\\{player\\}", 2);
        if (parts.length == 2) {
            // Strip orphaned closing MiniMessage tags right after {player}
            String after = parts[1].replaceFirst("^(\\s*</[^>]+>)+", "");
            Component before = MessageUtil.parse(parts[0]);
            Component nick = player.displayName(); // Component with nick colors
            Component afterComp = MessageUtil.parse(after);
            // Component.empty() root: all three are siblings → no color inheritance
            return Component.empty().append(before).append(nick).append(afterComp);
        }
        // No {player} placeholder — just parse
        return MessageUtil.parse(format);
    }
}
