package ru.managerfix.modules.chat;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /ignore add &lt;player&gt;, /ignore remove &lt;player&gt;, /ignore list — blacklist for PM (ignored players cannot PM you).
 */
public final class IgnoreCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_IGNORE = "managerfix.command.ignore";
    private static final String METADATA_PM_IGNORE = "pm_ignore";

    private final ManagerFix plugin;
    private final ProfileManager profileManager;

    public IgnoreCommand(ManagerFix plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    private static List<String> getIgnoreList(PlayerProfile profile) {
        Object raw = profile.getMetadata(METADATA_PM_IGNORE).orElse(null);
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) out.add(o.toString());
            }
            return out;
        }
        return new ArrayList<>();
    }

    private static void setIgnoreList(PlayerProfile profile, List<String> uuids) {
        profile.setMetadata(METADATA_PM_IGNORE, new ArrayList<>(uuids));
    }

    public static boolean isIgnored(UUID receiverUuid, UUID senderUuid, ProfileManager profileManager) {
        PlayerProfile receiver = profileManager.getCachedProfile(receiverUuid).orElse(null);
        if (receiver == null) return false;
        List<String> list = getIgnoreList(receiver);
        String senderStr = senderUuid.toString();
        return list.contains(senderStr);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        if (!CommandManager.checkPermission(sender, PERM_IGNORE, plugin)) return true;
        Player player = (Player) sender;
        if (args.length < 1) {
            MessageUtil.send(plugin, player, "chat.ignore.usage");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("list".equals(sub)) {
            List<String> list = getIgnoreList(profileManager.getProfile(player));
            if (list.isEmpty()) {
                MessageUtil.send(plugin, player, "chat.ignore.list-empty");
                return true;
            }
            List<String> names = new ArrayList<>();
            for (String uuidStr : list) {
                try {
                    UUID u = UUID.fromString(uuidStr);
                    Player p = Bukkit.getPlayer(u);
                    names.add(p != null && p.isOnline() ? p.getName() : uuidStr.substring(0, 8) + "...");
                } catch (Exception e) {
                    names.add(uuidStr);
                }
            }
            MessageUtil.send(plugin, player, "chat.ignore.list", java.util.Map.of("list", String.join(", ", names)));
            return true;
        }
        if ("add".equals(sub) || "remove".equals(sub)) {
            if (args.length < 2) {
                MessageUtil.send(plugin, player, "chat.ignore.usage");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                MessageUtil.send(plugin, player, "player-not-found", Map.of("player", args[1]));
                return true;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                MessageUtil.send(plugin, player, "chat.ignore.self");
                return true;
            }
            List<String> list = getIgnoreList(profileManager.getProfile(player));
            String targetUuid = target.getUniqueId().toString();
            if ("add".equals(sub)) {
                if (list.contains(targetUuid)) {
                    MessageUtil.send(plugin, player, "chat.ignore.already-in", Map.of("player", target.getName()));
                    return true;
                }
                list.add(targetUuid);
                setIgnoreList(profileManager.getProfile(player), list);
                plugin.getProfileManager().saveProfileAsync(player.getUniqueId());
                MessageUtil.send(plugin, player, "chat.ignore.added", Map.of("player", target.getName()));
            } else {
                if (!list.remove(targetUuid)) {
                    MessageUtil.send(plugin, player, "chat.ignore.not-in", Map.of("player", target.getName()));
                    return true;
                }
                setIgnoreList(profileManager.getProfile(player), list);
                plugin.getProfileManager().saveProfileAsync(player.getUniqueId());
                MessageUtil.send(plugin, player, "chat.ignore.removed", Map.of("player", target.getName()));
            }
            return true;
        }
        MessageUtil.send(plugin, player, "chat.ignore.usage");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (!sender.hasPermission(PERM_IGNORE)) return List.of();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Arrays.asList("add", "remove", "list").stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && ("add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
            String prefix = args[1].toLowerCase();
            Player self = (Player) sender;
            if ("remove".equalsIgnoreCase(args[0])) {
                List<String> list = getIgnoreList(plugin.getProfileManager().getProfile(self));
                List<String> names = new ArrayList<>();
                for (String uuidStr : list) {
                    try {
                        Player p = Bukkit.getPlayer(UUID.fromString(uuidStr));
                        if (p != null && p.isOnline() && p.getName().toLowerCase().startsWith(prefix))
                            names.add(p.getName());
                    } catch (Exception ignored) {}
                }
                return names.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
            }
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(self.getUniqueId()))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
