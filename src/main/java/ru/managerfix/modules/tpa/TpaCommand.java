package ru.managerfix.modules.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
import ru.managerfix.gui.GuiManager;
import ru.managerfix.utils.MessageUtil;
import ru.managerfix.utils.NickResolver;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TPA commands: /tpa, /tpahere, /tpaccept, /tpadeny, /tpatoggle, /tpablacklist, /tpareply.
 */
public final class TpaCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE = "managerfix.tpa.use";
    private static final String PERM_BYPASS_COOLDOWN = "managerfix.tpa.bypass.cooldown";

    private final ManagerFix plugin;
    private final TpaService service;
    private final TpaGui tpaGui;

    public TpaCommand(ManagerFix plugin, TpaService service, GuiManager guiManager) {
        this.plugin = plugin;
        this.service = service;
        this.tpaGui = new TpaGui(plugin, guiManager, service);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if ("tpareply".equals(cmd)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            Player player = (Player) sender;
            if (!CommandManager.checkCommandPermission(sender, "tpareply", PERM_USE, plugin)) return true;
            tpaGui.openIfHasRequest(player);
            return true;
        }

        if ("tpatoggle".equals(cmd)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "tpatoggle", PERM_USE, plugin)) return true;
            Player player = (Player) sender;
            boolean now = !service.isAcceptEnabled(player.getUniqueId());
            service.setAcceptEnabled(player.getUniqueId(), now);
            String key = now ? "toggle-enabled" : "toggle-disabled";
            sendTpaMessage(player, key, null);
            return true;
        }

        if ("tpablacklist".equals(cmd)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "tpablacklist", PERM_USE, plugin)) return true;
            return handleBlacklist((Player) sender, args);
        }

        if ("tpa".equals(cmd) || "tpahere".equals(cmd)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, cmd, PERM_USE, plugin)) return true;
            if (args.length < 1) {
                MessageUtil.send(plugin, sender, "tpa.usage");
                return true;
            }
            return handleTpaRequest((Player) sender, args[0], "tpahere".equals(cmd));
        }

        if ("tpaccept".equals(cmd)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "tpaccept", PERM_USE, plugin)) return true;
            return handleAccept((Player) sender);
        }

        if ("tpadeny".equals(cmd) || "tpdeny".equals(cmd)) {
            if (!CommandManager.checkPlayer(sender, plugin)) return true;
            if (!CommandManager.checkCommandPermission(sender, "tpadeny", PERM_USE, plugin)) return true;
            return handleDeny((Player) sender);
        }

        return false;
    }

    private boolean handleTpaRequest(Player sender, String targetName, boolean tpaHere) {
        Player target = NickResolver.resolve(targetName);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", targetName));
            return true;
        }

        Optional<String> err = service.validateSend(sender, target);
        if (err.isPresent()) {
            String key = err.get();
            if ("tpa.self".equals(key)) {
                MessageUtil.send(plugin, sender, key);
            } else if ("target-disabled".equals(key) || "in-blacklist".equals(key) || "already-request".equals(key)) {
                sendTpaMessage(sender, key, null);
            } else {
                MessageUtil.send(plugin, sender, key);
            }
            return true;
        }

        int cooldownSec = service.getConfig().getCooldownSeconds();
        if (cooldownSec > 0 && !sender.hasPermission(PERM_BYPASS_COOLDOWN)) {
            if (service.hasCooldown(sender)) {
                long rem = service.getCooldownRemaining(sender);
                sendTpaMessage(sender, "cooldown", Map.of("seconds", String.valueOf((rem + 999) / 1000)));
                return true;
            }
            service.setCooldown(sender, cooldownSec * 1000L);
        }

        int timeoutSec = service.getConfig().getRequestTimeoutSeconds();
        service.addRequest(sender.getUniqueId(), target.getUniqueId(), timeoutSec * 1000L, tpaHere);
        String targetDisplay = NickResolver.plainDisplayName(target);
        String senderDisplay = NickResolver.plainDisplayName(sender);
        sendTpaMessage(sender, "request-sent", Map.of("target", targetDisplay));

        Component requestMsg = buildRequestReceivedMessage(senderDisplay);
        target.sendMessage(requestMsg);
        // Меню открывается только по клику на сообщение (/tpareply), не автоматически
        return true;
    }

    private Component buildRequestReceivedMessage(String senderName) {
        String text = service.getConfig().getMessage("request-received", "<gradient:#FF4D00:#FAA300>Игрок {sender} хочет телепортироваться к вам</gradient>");
        text = text.replace("{sender}", senderName);
        Component main = MessageUtil.parse(text);
        String clickHint = service.getConfig().getMessage("click-to-reply", "Нажмите, чтобы ответить.");
        Component hint = MessageUtil.parse("<#E0E0E0>" + clickHint);
        Component full = main.append(Component.newline()).append(hint);
        return full.clickEvent(ClickEvent.runCommand("/tpareply"))
                .hoverEvent(HoverEvent.showText(MessageUtil.parse("<#FAA300>Открыть меню ответа</#FAA300>")));
    }

    private boolean handleAccept(Player accepter) {
        var reqOpt = service.removeRequest(accepter.getUniqueId());
        if (reqOpt.isEmpty()) {
            sendTpaMessage(accepter, "no-request", null);
            return true;
        }
        TpaRequest req = reqOpt.get();
        Player from = NickResolver.getPlayerByUuid(req.getFrom());
        if (from == null || !from.isOnline()) {
            sendTpaMessage(accepter, "request-expired", null);
            return true;
        }
        sendTpaMessage(accepter, "accepted", null);
        sendTpaMessage(from, "accepted", null);

        if (req.isTpaHere()) {
            service.scheduleTeleport(accepter, from.getLocation().clone(), () ->
                    sendTpaMessage(accepter, "cancelled-move", null));
        } else {
            service.scheduleTeleport(from, accepter.getLocation().clone(), () ->
                    sendTpaMessage(from, "cancelled-move", null));
        }
        return true;
    }

    private boolean handleDeny(Player denier) {
        var reqOpt = service.removeRequest(denier.getUniqueId());
        if (reqOpt.isEmpty()) {
            sendTpaMessage(denier, "no-request", null);
            return true;
        }
        sendTpaMessage(denier, "denied", null);
        TpaRequest req = reqOpt.get();
        Player from = NickResolver.getPlayerByUuid(req.getFrom());
        if (from != null && from.isOnline()) {
            sendTpaMessage(from, "denied", null);
        }
        return true;
    }

    private boolean handleBlacklist(Player player, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(plugin, player, "tpa.usage");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("list".equals(sub)) {
            Set<UUID> bl = service.getBlacklist(player.getUniqueId());
            if (bl.isEmpty()) {
                sendTpaMessage(player, "blacklist-empty", null);
                return true;
            }
            String list = bl.stream()
                    .map(u -> {
                        Player p = NickResolver.getPlayerByUuid(u);
                        return p != null ? NickResolver.plainDisplayName(p) : Bukkit.getOfflinePlayer(u).getName();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            sendTpaMessage(player, "blacklist-list", Map.of("list", list));
            return true;
        }
        if ("remove".equals(sub)) {
            if (args.length < 2) {
                MessageUtil.send(plugin, player, "tpa.usage");
                return true;
            }
            Player target = NickResolver.resolve(args[1]);
            if (target == null) {
                MessageUtil.send(plugin, player, "player-not-found", Map.of("player", args[1]));
                return true;
            }
            service.removeFromBlacklist(player.getUniqueId(), target.getUniqueId());
            sendTpaMessage(player, "blacklist-remove", Map.of("player", NickResolver.plainDisplayName(target)));
            return true;
        }
        // add: /tpablacklist add <player> or /tpablacklist <player>
        String playerArg = "add".equals(sub) && args.length > 1 ? args[1] : args[0];
        Player target = NickResolver.resolve(playerArg);
        if (target == null) {
            MessageUtil.send(plugin, player, "player-not-found", Map.of("player", playerArg));
            return true;
        }
        service.addToBlacklist(player.getUniqueId(), target.getUniqueId());
        sendTpaMessage(player, "blacklist-add", Map.of("player", NickResolver.plainDisplayName(target)));
        return true;
    }

    private void sendTpaMessage(CommandSender sender, String key, @Nullable Map<String, String> placeholders) {
        String msg = service.getConfig().getMessage(key, key);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                msg = msg.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        sender.sendMessage(MessageUtil.parse(msg));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (!(sender instanceof Player player)) return Collections.emptyList();
        UUID selfUuid = player.getUniqueId();

        if ("tpa".equals(cmd) || "tpahere".equals(cmd)) {
            if (args.length == 1) return NickResolver.tabComplete(args[0], selfUuid);
            return Collections.emptyList();
        }
        if ("tpablacklist".equals(cmd)) {
            if (args.length == 1) return Arrays.asList("add", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
            if (args.length == 2 && "remove".equals(args[0].toLowerCase())) {
                return NickResolver.tabComplete(args[1], selfUuid);
            }
            if (args.length >= 1 && !"list".equals(args[0].toLowerCase())) {
                return NickResolver.tabComplete(args[args.length - 1], selfUuid);
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
