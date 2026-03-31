package ru.managerfix.modules.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.commands.CommandManager;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * /pm, /tell, /msg &lt;player&gt; &lt;message...&gt; — private messages with tab complete.
 */
public final class PmCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE = "managerfix.chat.use";

    private final ManagerFix plugin;
    private final ChatModule chatModule;

    public PmCommand(ManagerFix plugin, ChatModule chatModule) {
        this.plugin = plugin;
        this.chatModule = chatModule;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!CommandManager.checkPlayer(sender, plugin)) return true;
        if (!CommandManager.checkCommandPermission(sender, "pm", PERM_USE, plugin)) return true;
        Player player = (Player) sender;
        if (args.length < 2) {
            MessageUtil.send(plugin, player, "chat.pm.usage");
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(plugin, sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageUtil.send(plugin, sender, "chat.pm.self");
            return true;
        }
        ProfileManager pm = plugin.getProfileManager();
        if (PmBlockCommand.isPmBlocked(player, pm)) {
            MessageUtil.send(plugin, sender, "chat.pm.blocked");
            return true;
        }
        if (IgnoreCommand.isIgnored(target.getUniqueId(), player.getUniqueId(), pm)) {
            MessageUtil.send(plugin, sender, "chat.ignore.cannot-message");
            return true;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendPm(plugin, chatModule, pm, player, target, message);
        return true;
    }

    /**
     * Sends a PM from sender to target (format, sounds, spy, last-partner for /r).
     * Caller must have already checked block/ignore/self.
     */
    public static void sendPm(ManagerFix plugin, ChatModule chatModule, ProfileManager pm,
                              Player sender, Player target, String message) {
        String lang = plugin.getConfigManager().getDefaultLanguage();
        String youRaw = MessageUtil.getRaw(plugin, lang, "chat.pm.you");
        String you = (youRaw != null && !youRaw.isEmpty()) ? youRaw : "Вы";
        String badge = chatModule.getBadgePm();
        String formatTemplate = chatModule.getFormatPm();

        String messageFormattedStr = (chatModule.getMessageFormat() != null ? chatModule.getMessageFormat() : "{text}").replace("{text}", message != null ? message : "");

        // Use display names (nicks) instead of real names for PM formatting
        String senderDisplay = ru.managerfix.utils.NickResolver.plainDisplayName(sender);
        String targetDisplay = ru.managerfix.utils.NickResolver.plainDisplayName(target);

        // To sender: You -> Target: message (format-pm с {message})
        String toSender = formatTemplate
                .replace("{badge}", badge)
                .replace("{sender}", you)
                .replace("{receiver}", targetDisplay)
                .replace("{message}", messageFormattedStr);
        sender.sendMessage(MessageUtil.parse(MessageUtil.setPlaceholders(sender, toSender)));
        if (chatModule.isPmSoundsEnabled()) {
            playPmSound(sender, chatModule.getPmSoundSend());
        }

        // To target: Sender -> You: message
        String toTarget = formatTemplate
                .replace("{badge}", badge)
                .replace("{sender}", senderDisplay)
                .replace("{receiver}", you)
                .replace("{message}", messageFormattedStr);
        target.sendMessage(MessageUtil.parse(MessageUtil.setPlaceholders(target, toTarget)));
        if (chatModule.isPmSoundsEnabled()) {
            playPmSound(target, chatModule.getPmSoundReceive());
        }

        // ChatSpy
        String spyRaw = MessageUtil.getRaw(plugin, lang, "chat.pm-spy-prefix");
        if (spyRaw == null || spyRaw.isEmpty()) spyRaw = MessageUtil.getRaw(plugin, lang, "chat.spy-prefix");
        Component spyPrefix = (spyRaw != null && !spyRaw.isEmpty())
                ? MessageUtil.parse(spyRaw)
                : MessageUtil.parse("<dark_gray>[PM Spy] </dark_gray>");
        String toSpy = formatTemplate
                .replace("{badge}", badge)
                .replace("{sender}", senderDisplay)
                .replace("{receiver}", targetDisplay)
                .replace("{message}", messageFormattedStr);
        Component spyLine = MessageUtil.parse(toSpy);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(sender) || other.equals(target)) continue;
            if (Boolean.TRUE.equals(pm.getProfile(other).getMetadata("chatspy").orElse(false))
                    && other.hasPermission("managerfix.chat.spy")) {
                other.sendMessage(Component.empty().append(spyPrefix).append(spyLine));
            }
        }

        chatModule.setLastPmPartner(sender, target);
    }

    @SuppressWarnings("deprecation")
    private static void playPmSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty() || "none".equalsIgnoreCase(soundName.trim())) return;
        try {
            Sound sound = Sound.valueOf(soundName.trim().toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (!sender.hasPermission("managerfix.command.pm") && !sender.hasPermission(PERM_USE)) return List.of();
        if (args.length == 1) {
            Player self = (Player) sender;
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], self.getUniqueId());
        }
        return List.of();
    }
}
