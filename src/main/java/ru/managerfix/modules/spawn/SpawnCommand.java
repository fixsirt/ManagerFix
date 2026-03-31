package ru.managerfix.modules.spawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final SpawnService spawnService;
    private final SpawnConfig config;
    private final SpawnEditGui editGui;

    public SpawnCommand(SpawnService spawnService, SpawnConfig config, SpawnEditGui editGui) {
        this.spawnService = spawnService;
        this.config = config;
        this.editGui = editGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.parse("<#FF4D00>Эта команда только для игроков."));
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("setspawn")) {
            if (!player.hasPermission("managerfix.spawn.set")) {
                player.sendMessage(MessageUtil.parse("<#FF4D00>У вас нет прав на установку спавна!"));
                return true;
            }
            config.setSpawnLocation(player.getLocation());
            player.sendMessage(MessageUtil.parse("<#FAA300>Спавн сервера установлен в вашей позиции."));
            return true;
        }

        if (cmdName.equals("editspawn")) {
            if (!player.hasPermission("managerfix.spawn.edit")) {
                player.sendMessage(MessageUtil.parse("<#FF4D00>У вас нет прав на редактирование настроек спавна!"));
                return true;
            }
            editGui.open(player);
            return true;
        }

        if (cmdName.equals("spawn")) {
            if (!player.hasPermission("managerfix.spawn.use")) {
                player.sendMessage(MessageUtil.parse("<#FF4D00>У вас нет прав на использование команды спавна!"));
                return true;
            }
            spawnService.teleportToSpawn(player);
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
