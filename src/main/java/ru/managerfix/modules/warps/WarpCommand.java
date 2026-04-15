package ru.managerfix.modules.warps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;
    private final WarpConfig config;
    private final WarpsDataStorage dataStorage;
    private final WarpEditGui editGui;
    private final WarpService warpService;
    private final GuiTemplate template;

    public WarpCommand(ManagerFix plugin, WarpConfig config, WarpsDataStorage dataStorage, WarpEditGui editGui, WarpService warpService) {
        this(plugin, config, dataStorage, editGui, warpService, null);
    }

    public WarpCommand(ManagerFix plugin, WarpConfig config, WarpsDataStorage dataStorage, WarpEditGui editGui, WarpService warpService, GuiTemplate template) {
        this.plugin = plugin;
        this.config = config;
        this.dataStorage = dataStorage;
        this.editGui = editGui;
        this.warpService = warpService;
        this.template = template;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.parse("<#FF3366>Только для игроков!"));
            return true;
        }

        String cmdName = command.getName().toLowerCase();


        // /setwarp <name>
        if (cmdName.equals("setwarp")) {
            if (!player.hasPermission("managerfix.warps.create")) {
                player.sendMessage(MessageUtil.parse("<#FF3366>У вас нет прав!"));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Использование: /setwarp <название>"));
                return true;
            }

            String name = args[0];
            Warp existing = dataStorage.getWarp(name);
            if (existing != null) {
                boolean isOwner = existing.getOwner() != null && existing.getOwner().equals(player.getUniqueId());
                if (!isOwner) {
                    player.sendMessage(MessageUtil.parse("<#FF3366>Варп с таким именем уже есть."));
                    return true;
                }
                existing.setLocation(player.getLocation());
                dataStorage.saveWarp(existing);
                player.sendMessage(MessageUtil.parse("<#00C8FF>Варп <#F0F4F8>" + name + "</#F0F4F8> обновлён."));
                return true;
            }

            int limit = config.getMaxWarpsFor(player);
            long owned = dataStorage.getWarps().values().stream()
                    .filter(w -> w.getOwner() != null && w.getOwner().equals(player.getUniqueId()))
                    .count();
            if (owned >= limit) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Вы достигли лимита варпов: <#F0F4F8>" + limit + "</#F0F4F8>"));
                return true;
            }

            Warp warp = new Warp(name, player.getLocation());
            warp.setOwner(player.getUniqueId());
            dataStorage.saveWarp(warp);
            player.sendMessage(MessageUtil.parse("<#00C8FF>Варп <#F0F4F8>" + name + "</#F0F4F8> успешно создан!"));
            return true;
        }

        // /delwarp <name>
        if (cmdName.equals("delwarp")) {
            if (args.length < 1) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Использование: /delwarp <название>"));
                return true;
            }

            String name = args[0];
            Warp warp = dataStorage.getWarp(name);
            if (warp == null) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Варп не найден!"));
                return true;
            }

            // Удалять через /delwarp может только владелец
            if (warp.getOwner() == null || !warp.getOwner().equals(player.getUniqueId())) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Вы не являетесь владельцем этого варпа!"));
                return true;
            }

            dataStorage.deleteWarp(name);
            player.sendMessage(MessageUtil.parse("<#00C8FF>Варп <#F0F4F8>" + name + "</#F0F4F8> удалён."));
            return true;
        }

        // /editwarp <name>
        if (cmdName.equals("editwarp")) {
            if (args.length < 1) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Использование: /editwarp <название>"));
                return true;
            }
            Warp warp = dataStorage.getWarp(args[0]);
            if (warp == null) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Варп не найден!"));
                return true;
            }
            if (!player.hasPermission("managerfix.warp.edit") && (warp.getOwner() == null || !warp.getOwner().equals(player.getUniqueId()))) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Вы не можете редактировать этот варп!"));
                return true;
            }
            editGui.open(player, warp);
            return true;
        }

        // /warps [name] [reload]
        if (cmdName.equals("warps")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("managerfix.warp.reload")) {
                    player.sendMessage(MessageUtil.parse("<#FF3366>У вас нет прав!"));
                    return true;
                }
                config.reload();
                player.sendMessage(MessageUtil.parse("<#00C8FF>Конфигурация варпов перезагружена."));
                return true;
            }
            if (args.length == 0) {
                if (!player.hasPermission("managerfix.command.warps")) {
                    player.sendMessage(MessageUtil.parse("<#FF3366>У вас нет прав!"));
                    return true;
                }
                // Открываем GUI со списком варпов
                new WarpGui(config, warpService, editGui, dataStorage, template).open(player);
                return true;
            }
            // /warps <name> — телепортация
            if (!player.hasPermission("managerfix.command.warp")) {
                player.sendMessage(MessageUtil.parse("<#FF3366>У вас нет прав!"));
                return true;
            }
            warpService.teleportToWarp(player, args[0]);
            return true;
        }

        // /warp [name]
        if (cmdName.equals("warp")) {
            if (args.length == 0) {
                if (!player.hasPermission("managerfix.command.warps")) {
                    player.sendMessage(MessageUtil.parse("<#FF3366>У вас нет прав!"));
                    return true;
                }
                // Открываем GUI со списком варпов
                new WarpGui(config, warpService, editGui, dataStorage, template).open(player);
                return true;
            }
            if (!player.hasPermission("managerfix.command.warp")) {
                player.sendMessage(MessageUtil.parse("<#FF3366>У вас нет прав!"));
                return true;
            }
            // Телепортация на варп через сервис
            warpService.teleportToWarp(player, args[0]);
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        // Tab complete для /warp <name> и /warps <name>
        if ((cmdName.equals("warp") || cmdName.equals("warps")) && args.length == 1) {
            String partial = args[0].toLowerCase();
            return dataStorage.getWarps().keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .sorted()
                    .toList();
        }

        return new ArrayList<>();
    }
}
