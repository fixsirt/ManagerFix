package ru.managerfix.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TeleportService {
    void teleport(Player player, Location destination, Runnable onCancel);
}
