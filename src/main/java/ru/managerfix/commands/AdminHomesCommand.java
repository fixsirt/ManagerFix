package ru.managerfix.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.AdminHomesMenuGui;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /adminhomes <player> — open target's homes GUI
 * /adminsethome <player> <home_name> — set home for target at admin's location
 * Permissions: managerfix.homes.admin, managerfix.homes.admin.set
 */
public final class AdminHomesCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "managerfix.homes.admin";
    private static final String PERM_ADMIN_SET = "managerfix.homes.admin.set";

    private final ManagerFix plugin;
    private final GuiManager guiManager;

    public AdminHomesCommand(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase(Locale.ROOT);
        if ("adminhomes".equals(cmdName)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "adminhomes", PERM_ADMIN, plugin)) return true;
            Player admin = (Player) sender;
            if (args.length < 1) {
                MessageUtil.send(plugin, admin, "player-not-found");
                return true;
            }
            String targetName = args[0];
            UUID targetUuid = resolveUuid(targetName);
            if (targetUuid == null) {
                MessageUtil.send(plugin, admin, "player-not-found", Map.of("player", targetName));
                return true;
            }
            String display = resolveDisplayName(targetUuid, targetName);
            // Ensure profile load kicks off
            plugin.getProfileManager().getProfile(targetUuid);
            new AdminHomesMenuGui(plugin, guiManager, targetUuid, display).open(admin, 0);
            return true;
        }

        if ("adminsethome".equals(cmdName)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "adminsethome", PERM_ADMIN_SET, plugin)) return true;
            Player admin = (Player) sender;
            if (args.length < 2) {
                MessageUtil.send(plugin, admin, "homes.usage-delhome"); // reuse generic usage-style message
                return true;
            }
            String targetName = args[0];
            String homeName = args[1];
            UUID targetUuid = resolveUuid(targetName);
            if (targetUuid == null) {
                MessageUtil.send(plugin, admin, "player-not-found", Map.of("player", targetName));
                return true;
            }
            ProfileManager pm = plugin.getProfileManager();
            PlayerProfile profile = pm.getProfile(targetUuid);
            profile.setHome(homeName, admin.getLocation());
            pm.saveProfileAsync(targetUuid);
            MessageUtil.send(plugin, admin, "homes.set", Map.of("name", homeName));
            return true;
        }
        return false;
    }

    private @Nullable UUID resolveUuid(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null && p.isOnline()) return p.getUniqueId();
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off != null && (off.isOnline() || off.hasPlayedBefore())) {
            return off.getUniqueId();
        }
        return null;
    }

    private @NotNull String resolveDisplayName(UUID uuid, String fallback) {
        Player p = ru.managerfix.utils.NickResolver.getPlayerByUuid(uuid);
        if (p != null) return ru.managerfix.utils.NickResolver.plainDisplayName(p);
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n != null ? n : fallback;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase(Locale.ROOT);
        if ("adminhomes".equals(cmdName) || "adminsethome".equals(cmdName)) {
            if (!(sender instanceof Player)) return Collections.emptyList();
            if (args.length == 1) {
                String prefix = args[0].toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList());
            }
            if ("adminsethome".equals(cmdName) && args.length == 2) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                List<String> suggest = new ArrayList<>(List.of("home", "base", "farm", "mine"));
                suggest.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(prefix));
                suggest.sort(String.CASE_INSENSITIVE_ORDER);
                return suggest;
            }
        }
        return Collections.emptyList();
    }
}
