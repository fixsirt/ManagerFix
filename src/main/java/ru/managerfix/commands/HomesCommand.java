package ru.managerfix.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.HomesMenuGui;
import ru.managerfix.modules.homes.HomesModule;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /sethome [name], /home [name], /delhome [name], /homes — open GUI.
 * Permissions: managerfix.homes.set, managerfix.homes.teleport, managerfix.homes.delete,
 * managerfix.homes.limit.X (max homes), managerfix.homes.bypass.cooldown
 */
public final class HomesCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_SET = "managerfix.homes.set";
    private static final String PERM_TELEPORT = "managerfix.homes.teleport";
    private static final String PERM_DELETE = "managerfix.homes.delete";
    private static final String PERM_USE = "managerfix.homes.use";
    private static final String PERM_LIMIT_PREFIX = "managerfix.homes.limit.";
    private static final String PERM_BYPASS_COOLDOWN = "managerfix.homes.bypass.cooldown";

    private final ManagerFix plugin;
    private final GuiManager guiManager;

    public HomesCommand(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        Player player = (Player) sender;

        var moduleOpt = plugin.getModuleManager().getEnabledModule("homes");
        if (moduleOpt.isEmpty()) {
            MessageUtil.send(plugin, sender, "module-disabled");
            return true;
        }
        HomesModule homesModule = moduleOpt.filter(m -> m instanceof HomesModule)
            .map(m -> (HomesModule) m)
            .orElse(null);
        if (homesModule == null) {
            MessageUtil.send(plugin, sender, "module-disabled");
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        if ("homes".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "homes", PERM_USE, plugin)) return true;
            new HomesMenuGui(plugin, guiManager, homesModule).open(player, 0);
            return true;
        }

        ProfileManager profileManager = plugin.getProfileManager();
        PlayerProfile profile = profileManager.getProfile(player);
        int cooldownSec = homesModule.getCooldownSeconds();
        int teleportDelay = homesModule.getTeleportDelaySeconds();

        if ("sethome".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "sethome", PERM_SET, plugin)) return true;
            String name = args.length > 0 ? args[0].trim() : "home";
            if (name.isEmpty()) name = "home";
            int limit = Math.max(homesModule.getMaxHomesFor(player), getHomeLimit(player, homesModule.getDefaultMaxHomes()));
            if (profile.getHomesCount() >= limit && !profile.getHomeNames().contains(name.toLowerCase())) {
                MessageUtil.send(plugin, sender, "homes.limit-reached", Map.of("limit", String.valueOf(limit)));
                return true;
            }
            profile.setHome(name, player.getLocation());
            profileManager.saveProfileAsync(player.getUniqueId());
            if (plugin.getEventBus() != null) {
                plugin.getEventBus().callEvent(new ru.managerfix.event.HomeCreateEvent(player, name, player.getLocation()));
            }
            MessageUtil.send(plugin, sender, "homes.set", Map.of("name", name));
            return true;
        }

        if ("home".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "home", PERM_TELEPORT, plugin)) return true;
            if (args.length < 1) {
                if (profile.getHomesCount() == 0) {
                    MessageUtil.send(plugin, sender, "homes.not-found");
                    return true;
                }
                if (profile.getHomesCount() == 1) {
                    String only = profile.getHomeNames().iterator().next();
                    teleportToHome(player, profile, only, homesModule, cooldownSec, teleportDelay);
                    return true;
                }
                new HomesMenuGui(plugin, guiManager, homesModule).open(player, 0);
                return true;
            }
            String name = args[0].trim();
            teleportToHome(player, profile, name, homesModule, cooldownSec, teleportDelay);
            return true;
        }

        if ("delhome".equals(cmdName)) {
            if (!CommandManager.checkCommandPermission(sender, "delhome", PERM_DELETE, plugin)) return true;
            if (args.length < 1) {
                MessageUtil.send(plugin, sender, "homes.usage-delhome");
                return true;
            }
            String name = args[0].trim();
            if (profile.removeHome(name)) {
                profileManager.saveProfileAsync(player.getUniqueId());
                MessageUtil.send(plugin, sender, "homes.deleted", Map.of("name", name));
            } else {
                MessageUtil.send(plugin, sender, "homes.not-found-name", Map.of("name", name));
            }
            return true;
        }

        return false;
    }

    private void teleportToHome(Player player, PlayerProfile profile, String name, HomesModule homesModule,
                                int cooldownSec, int teleportDelay) {
        var locOpt = profile.getHome(name);
        if (locOpt.isEmpty()) {
            MessageUtil.send(plugin, player, "homes.not-found-name", Map.of("name", name));
            return;
        }
        if (!player.hasPermission(PERM_BYPASS_COOLDOWN) && cooldownSec > 0) {
            if (profile.hasCooldown("home")) {
                long remaining = profile.getCooldownRemaining("home");
                MessageUtil.send(plugin, player, "homes.cooldown", Map.of("seconds", String.valueOf((remaining + 999) / 1000)));
                return;
            }
            profile.setCooldown("home", cooldownSec * 1000L);
        }
        var tpaMod = plugin.getModuleManager().getEnabledModule("tpa")
            .filter(m -> m instanceof ru.managerfix.modules.tpa.TpaModule)
            .map(m -> (ru.managerfix.modules.tpa.TpaModule) m)
            .orElse(null);
        if (tpaMod != null && tpaMod.getTpaService() != null) {
            tpaMod.getTpaService().scheduleTeleport(player, locOpt.get(), null);
            return;
        }
        Location loc = locOpt.get();
        if (plugin.getEventBus() != null) {
            plugin.getEventBus().callEvent(new ru.managerfix.event.HomeTeleportEvent(player, name, loc));
        }
        player.teleport(loc);
        MessageUtil.send(plugin, player, "homes.teleported", Map.of("name", name));
    }

    private int getHomeLimit(Player player, int defaultMax) {
        int max = defaultMax;
        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission(PERM_LIMIT_PREFIX + i)) max = i;
        }
        return max;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        var moduleOpt = plugin.getModuleManager().getEnabledModule("homes");
        if (moduleOpt.isEmpty()) return Collections.emptyList();

        PlayerProfile profile = plugin.getProfileManager().getProfile(player);
        String cmdName = command.getName().toLowerCase();

        if ("homes".equals(cmdName)) return Collections.emptyList();
        if ("sethome".equals(cmdName)) {
            if (args.length == 1) {
                String a = args[0].toLowerCase();
                List<String> list = new ArrayList<>(List.of("home", "default", "farm", "mine"));
                return list.stream().filter(s -> s.startsWith(a)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
        if ("home".equals(cmdName) && args.length == 1) {
            String a = args[0].toLowerCase();
            List<String> names = new ArrayList<>(profile.getHomeNames());
            names.removeIf(s -> !s.toLowerCase().startsWith(a));
            Collections.sort(names);
            return names;
        }
        if ("delhome".equals(cmdName) && args.length == 1) {
            String a = args[0].toLowerCase();
            List<String> names = new ArrayList<>(profile.getHomeNames());
            names.removeIf(s -> !s.toLowerCase().startsWith(a));
            Collections.sort(names);
            return names;
        }
        return Collections.emptyList();
    }
}
