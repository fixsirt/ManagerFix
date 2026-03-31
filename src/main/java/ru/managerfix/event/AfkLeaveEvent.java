package ru.managerfix.event;

import org.bukkit.entity.Player;

/**
 * Fired when a player leaves AFK state.
 */
public class AfkLeaveEvent extends ManagerFixEvent {

    private final Player player;

    public AfkLeaveEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
