package ru.managerfix.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.KitsMenuGui;
import ru.managerfix.modules.kits.KitData;
import ru.managerfix.modules.kits.KitManager;
import ru.managerfix.modules.kits.KitsModule;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /kit [name], /kits (GUI), /kit create <name> (admin).
 */
public final class KitsCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE = "managerfix.kits.use";
    private static final String PERM_CREATE = "managerfix.kits.create";

    private final ManagerFix plugin;
    private final GuiManager guiManager;

    public KitsCommand(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        Player player = (Player) sender;

        var moduleOpt = plugin.getModuleManager().getEnabledModule("kits");
        if (moduleOpt.isEmpty()) {
            MessageUtil.send(plugin, sender, "module-disabled");
            return true;
        }
        KitsModule kitsModule = moduleOpt.filter(m -> m instanceof KitsModule)
            .map(m -> (KitsModule) m)
            .orElse(null);
        if (kitsModule == null) {
            MessageUtil.send(plugin, sender, "module-disabled");
            return true;
        }
        KitManager kitManager = kitsModule.getKitManager();

        String cmdName = command.getName().toLowerCase();
        if ("kits".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "kits", PERM_USE, plugin)) return true;
            new KitsMenuGui(plugin, guiManager, kitsModule).open(player, 0);
            return true;
        }

        if ("kit".equals(cmdName)) {
            if (args.length == 0) {
                if (!CommandManager.checkCommandPermission(sender, "kit", PERM_USE, plugin)) return true;
                new KitsMenuGui(plugin, guiManager, kitsModule).open(player, 0);
                return true;
            }
            if ("create".equalsIgnoreCase(args[0])) {
                if (!CommandManager.checkCommandPermission(sender, "kits", PERM_CREATE, plugin)) return true;
                if (args.length < 2) {
                    MessageUtil.send(plugin, sender, "kits.create-usage");
                    return true;
                }
                String name = args[1].trim();
                if (name.isEmpty()) {
                    MessageUtil.send(plugin, sender, "kits.create-usage");
                    return true;
                }
                int defaultCooldown = kitsModule.getDefaultCooldownSeconds();
                List<ItemStack> contents = new ArrayList<>();
                for (ItemStack i : player.getInventory().getContents()) {
                    if (i != null && !i.getType().isAir()) contents.add(i.clone());
                }
                KitData kit = new KitData(name, defaultCooldown, "managerfix.kits.kit." + name.toLowerCase(), contents);
                kitManager.saveKit(kit);
                MessageUtil.send(plugin, sender, "kits.created", Map.of("name", name));
                return true;
            }
            if (!CommandManager.checkCommandPermission(sender, "kit", PERM_USE, plugin)) return true;
            String name = args[0].trim();
            kitManager.giveKit(player, name);
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        var moduleOpt = plugin.getModuleManager().getEnabledModule("kits");
        if (moduleOpt.isEmpty()) return Collections.emptyList();
        KitsModule kitsModule = moduleOpt.filter(m -> m instanceof KitsModule)
            .map(m -> (KitsModule) m)
            .orElse(null);
        if (kitsModule == null) return Collections.emptyList();
        KitManager kitManager = kitsModule.getKitManager();

        if ("kit".equals(command.getName().toLowerCase())) {
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                if (sender.hasPermission("managerfix.command.kit") || sender.hasPermission(PERM_USE)) {
                    list.addAll(kitManager.getKitNames());
                }
                if (sender.hasPermission("managerfix.command.kits") || sender.hasPermission(PERM_CREATE)) {
                    list.add("create");
                }
                String a = args[0].toLowerCase();
                return list.stream().filter(s -> s.toLowerCase().startsWith(a)).sorted().collect(Collectors.toList());
            }
            if (args.length == 2 && "create".equalsIgnoreCase(args[0])) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
