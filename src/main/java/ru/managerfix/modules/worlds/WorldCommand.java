package ru.managerfix.modules.worlds;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.WorldsMenuGui;
import ru.managerfix.utils.MessageUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * /world [tp <name>], /world create <name> [generator], /world delete <name>, /world (GUI).
 */
public final class WorldCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;
    private final GuiManager guiManager;

    public WorldCommand(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "world", "managerfix.worlds.teleport", plugin)) return true;
            var mod = plugin.getModuleManager().getEnabledModule("worlds");
            if (mod.isEmpty()) { MessageUtil.send(plugin, sender, "module-disabled"); return true; }
            new WorldsMenuGui(plugin, guiManager).open((Player) sender, 0);
            return true;
        }

        if (args.length == 1) {
            World w = Bukkit.getWorld(args[0]);
            if (w != null) {
                if (!CommandManager.checkPlayer(sender, plugin)) return true;
                if (!CommandManager.checkCommandPermission(sender, "world", "managerfix.worlds.teleport", plugin)) return true;
                ((Player) sender).teleport(w.getSpawnLocation());
                MessageUtil.send(plugin, sender, "worlds.teleported", Map.of("world", w.getName()));
                return true;
            }
        }

        String sub = args[0].toLowerCase();
        if ("tp".equals(sub)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "world", "managerfix.worlds.teleport", plugin)) return true;
            if (args.length < 2) { MessageUtil.send(plugin, sender, "worlds.tp-usage"); return true; }
            World w = Bukkit.getWorld(args[1]);
            if (w == null) { MessageUtil.send(plugin, sender, "worlds.not-found", Map.of("world", args[1])); return true; }
            ((Player) sender).teleport(w.getSpawnLocation());
            MessageUtil.send(plugin, sender, "worlds.teleported", Map.of("world", w.getName()));
            return true;
        }

        if ("create".equals(sub)) {
            if (!CommandManager.checkCommandPermission(sender, "world", "managerfix.worlds.create", plugin)) return true;
            if (args.length < 2) { MessageUtil.send(plugin, sender, "worlds.create-usage"); return true; }
            String name = args[1];
            if (Bukkit.getWorld(name) != null) {
                MessageUtil.send(plugin, sender, "worlds.already-exists", Map.of("world", name));
                return true;
            }
            String generator = args.length > 2 ? args[2] : "default";
            WorldCreator creator = WorldCreator.name(name);
            if ("flat".equalsIgnoreCase(generator)) creator.generator("minecraft:flat");
            else if ("void".equalsIgnoreCase(generator)) creator.generator("minecraft:void");
            plugin.getScheduler().runSync(() -> {
                World world = Bukkit.createWorld(creator);
                if (world != null) {
                    MessageUtil.send(plugin, sender, "worlds.created", Map.of("world", world.getName()));
                } else {
                    MessageUtil.send(plugin, sender, "worlds.create-failed", Map.of("world", name));
                }
            });
            return true;
        }

        if ("delete".equals(sub)) {
            if (!CommandManager.checkCommandPermission(sender, "world", "managerfix.worlds.delete", plugin)) return true;
            if (args.length < 2) { MessageUtil.send(plugin, sender, "worlds.delete-usage"); return true; }
            String name = args[1];
            World w = Bukkit.getWorld(name);
            if (w == null) { MessageUtil.send(plugin, sender, "worlds.not-found", Map.of("world", name)); return true; }
            String containerPath = plugin.getServer().getWorldContainer().getAbsolutePath();
            if (!w.getWorldFolder().getAbsolutePath().startsWith(containerPath)) {
                MessageUtil.send(plugin, sender, "worlds.delete-forbidden");
                return true;
            }
            for (Player p : w.getPlayers()) {
                p.teleport(Objects.requireNonNull(Bukkit.getWorlds().get(0).getSpawnLocation()));
            }
            plugin.getServer().unloadWorld(w, true);
            plugin.getScheduler().runAsync(() -> {
                File folder = w.getWorldFolder();
                deleteDirectory(folder);
                plugin.getScheduler().runSync(() -> MessageUtil.send(plugin, sender, "worlds.deleted", Map.of("world", name)));
            });
            return true;
        }

        return false;
    }

    private static void deleteDirectory(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteDirectory(c);
        }
        f.delete();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String a = args[0].toLowerCase();
            List<String> list = new ArrayList<>();
            if (sender.hasPermission("managerfix.command.world") || sender.hasPermission("managerfix.worlds.teleport")) list.addAll(List.of("tp"));
            if (sender.hasPermission("managerfix.command.world") || sender.hasPermission("managerfix.worlds.create")) list.add("create");
            if (sender.hasPermission("managerfix.command.world") || sender.hasPermission("managerfix.worlds.delete")) list.add("delete");
            if (sender.hasPermission("managerfix.command.world") || sender.hasPermission("managerfix.worlds.teleport")) {
                list.addAll(Bukkit.getWorlds().stream().map(World::getName).toList());
            }
            return list.stream().filter(s -> s.toLowerCase().startsWith(a)).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if ("tp".equals(args[0].toLowerCase())) {
                String a = args[1].toLowerCase();
                return Bukkit.getWorlds().stream().map(World::getName).filter(n -> n.toLowerCase().startsWith(a)).collect(Collectors.toList());
            }
            if ("delete".equals(args[0].toLowerCase())) {
                String a = args[1].toLowerCase();
                return Bukkit.getWorlds().stream().map(World::getName).filter(n -> n.toLowerCase().startsWith(a)).collect(Collectors.toList());
            }
        }
        if (args.length == 3 && "create".equals(args[0].toLowerCase())) {
            String a = args[2].toLowerCase();
            return List.of("default", "flat", "void").stream().filter(s -> s.startsWith(a)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
