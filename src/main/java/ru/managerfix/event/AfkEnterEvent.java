package ru.managerfix.event;

import org.bukkit.entity.Player;

/**
 * Fired when a player enters AFK state.
 */
public class AfkEnterEvent extends ManagerFixEvent {

    private final Player player;

    public AfkEnterEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
