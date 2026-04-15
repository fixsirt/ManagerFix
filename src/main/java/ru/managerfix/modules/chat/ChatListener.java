package ru.managerfix.modules.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.managerfix.ManagerFix;
import ru.managerfix.modules.ban.BanModule;
import ru.managerfix.modules.ban.MuteManager;
import ru.managerfix.modules.chat.filter.ProfanityFilter;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.service.ExternalApiService;
import ru.managerfix.utils.MessageUtil;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


/**
 * Chat: format (MiniMessage), local by default (radius), prefix ! for global.
 * Оптимизирован: использует кэшированный ExternalApiService вместо reflection.
 */
public final class ChatListener implements Listener {

    private final ChatModule module;
    private final ProfileManager profileManager;
    private final ExternalApiService externalApiService;

    public ChatListener(ChatModule module, ProfileManager profileManager, ExternalApiService externalApiService) {
        this.module = module;
        this.profileManager = profileManager;
        this.externalApiService = externalApiService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Получаем сообщение
        String messagePlain = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        // Пропускаем сообщения с префиксом % (клановый чат от ClansFix)
        if (messagePlain != null && messagePlain.startsWith("%")) {
            return;
        }

        // Получаем BanModule прямо здесь
        BanModule banModule = null;
        if (module.getPlugin() instanceof ManagerFix mf) {
            banModule = mf.getModuleManager().getEnabledModule("ban")
                .filter(m -> m instanceof BanModule)
                .map(m -> (BanModule) m)
                .orElse(null);
        }
        
        // Проверка на мут через BanModule
        MuteManager muteManager = banModule != null ? banModule.getMuteManager() : null;
        
        if (muteManager != null && muteManager.isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            var rec = muteManager.getMute(player.getUniqueId()).orElse(null);
            String until = "Навсегда";
            if (rec != null && !rec.isPermanent()) {
                try {
                    until = java.time.Instant.ofEpochMilli(rec.getExpiresAt())
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                } catch (Exception ignored) {
                    until = String.valueOf(new java.util.Date(rec.getExpiresAt()));
                }
            }
            player.sendMessage(MessageUtil.parse("<#FF3366>⛔ Вы замучены!</#FF3366> <#F0F4F8>До:</#F0F4F8> <#00C8FF>" + until + "</#00C8FF>"));
            return;
        }

        // Проверка фильтра мата (с возможностью обхода по разрешению)
        if (module.isFilterEnabled() && module.getProfanityFilter() != null && 
            !player.hasPermission("managerfix.chat.bypass.filter")) {
            ProfanityFilter.FilterResult result = module.getProfanityFilter().check(
                    player.getName(), messagePlain);

            if (result.isBlocked()) {
                if (result.getAction() == ProfanityFilter.Action.BLOCK) {
                    event.setCancelled(true);
                    player.sendMessage(module.getProfanityFilter().getMessageForAction(result.getAction()));
                    return;
                } else if (result.getAction() == ProfanityFilter.Action.CENSOR) {
                    String censored = result.getCensoredMessage();
                    if (censored != null) {
                        event.message(net.kyori.adventure.text.Component.text(censored));
                    }
                    player.sendMessage(module.getProfanityFilter().getMessageForAction(result.getAction()));
                } else if (result.getAction() == ProfanityFilter.Action.WARN) {
                    player.sendMessage(module.getProfanityFilter().getMessageForAction(result.getAction()));
                }
            }
        }

        if (!player.hasPermission("managerfix.chat.use")) {
            event.setCancelled(true);
            MessageUtil.send(module.getPlugin(), player, "chat.no-permission");
            return;
        }
        int cooldownSec = module.getSpamCooldownSeconds();
        if (cooldownSec > 0 && !player.hasPermission("managerfix.chat.bypass.cooldown")) {
            PlayerProfile profile = profileManager.getProfile(player);
            if (profile.hasCooldown("chat")) {
                event.setCancelled(true);
                long remaining = profile.getCooldownRemaining("chat");
                MessageUtil.send(module.getPlugin(), player, "chat.spam", Map.of("seconds", String.valueOf((remaining + 999) / 1000)));
                return;
            }
            profile.setCooldown("chat", cooldownSec * 1000L);
        }
        
        // ! in front = global chat; otherwise = local (radius)
        boolean isGlobal = messagePlain != null && messagePlain.startsWith("!");
        String displayMessage = isGlobal ? messagePlain.substring(1).trim() : messagePlain;

        // CAPS auto-lowercase: если 6+ букв капсом - переводим в lowercase
        if (displayMessage != null && displayMessage.length() >= 6) {
            String lettersOnly = displayMessage.replaceAll("[^A-Za-zA-Zа-яА-ЯёЁ]", "");
            if (lettersOnly.length() >= 6) {
                boolean allUpperCase = true;
                for (char c : lettersOnly.toCharArray()) {
                    if (Character.isLetter(c) && !Character.isUpperCase(c)) {
                        allUpperCase = false;
                        break;
                    }
                }
                if (allUpperCase) {
                    displayMessage = displayMessage.toLowerCase();
                }
            }
        }

        // Only this plain text must be copied to clipboard (no nickname, no format, no path)
        final String messageTextToCopy = displayMessage != null ? displayMessage : "";
        String badge = isGlobal ? module.getBadgeGlobal() : module.getBadgeLocal();
        String formatTemplate = isGlobal ? module.getFormatGlobal() : module.getFormatLocal();
        String formattedMessage = (module.getMessageFormat() != null ? module.getMessageFormat() : "{text}").replace("{text}", displayMessage != null ? displayMessage : "");
        String format = formatTemplate.replace("{badge}", badge).replace("{message}", formattedMessage);
        
        // Используем кэшированный ExternalApiService вместо reflection
        if (externalApiService != null && externalApiService.hasChat()) {
            String prefix = externalApiService.getPlayerPrefix(player);
            String suffix = externalApiService.getPlayerSuffix(player);
            if (prefix != null) format = format.replace("{prefix}", prefix);
            if (suffix != null) format = format.replace("{suffix}", suffix);
        }
        format = format.replace("{prefix}", "").replace("{suffix}", "");

        // Nick Component (preserves ALL colors from the nick) and plain text for lookup
        Component displayNameComp = player.displayName();
        String displayNamePlain = PlainTextComponentSerializer.plainText().serialize(displayNameComp);
        if (displayNamePlain == null || displayNamePlain.isEmpty()) {
            displayNamePlain = player.getName();
            displayNameComp = Component.text(displayNamePlain);
        }

        // Click on the nick in chat → suggest /pm <nick> (using custom nick, not real name)
        String pmSuggest = "/pm " + displayNamePlain + " ";

        Component formatted;
        if (module.isHoverEnabled() && format.contains("{player}")) {
            String hoverFormat = module.getHoverFormat();
            if (hoverFormat != null && !hoverFormat.isEmpty()) {
                String balanceStr = MessageUtil.convertLegacyColors(getBalanceString(player, externalApiService));
                String hoverText = MessageUtil.replace(hoverFormat, Map.of("player", displayNamePlain, "balance", balanceStr));
                hoverText = MessageUtil.setPlaceholders(player, hoverText);
                Component hoverComponent = MessageUtil.parse(hoverText.trim());

                // Nick Component keeps its original colors — no wrapping in format color tags
                Component nameComponent = displayNameComp
                        .hoverEvent(HoverEvent.showText(hoverComponent))
                        .clickEvent(ClickEvent.suggestCommand(pmSuggest));

                formatted = buildChatFormatted(format, player, nameComponent, messageTextToCopy, module);
            } else {
                // No hover format — still insert nick as Component to preserve colors
                Component nameComponent = displayNameComp.clickEvent(ClickEvent.suggestCommand(pmSuggest));
                formatted = buildChatFormatted(format, player, nameComponent, messageTextToCopy, module);
            }
        } else {
            // No hover — insert nick as Component to preserve colors
            Component nameComponent = displayNameComp.clickEvent(ClickEvent.suggestCommand(pmSuggest));
            formatted = buildChatFormatted(format, player, nameComponent, messageTextToCopy, module);
        }
        int radius = module.getLocalRadius();

        if (radius > 0 && !isGlobal) {
            // Local: only players in radius (+ chatspy)
            event.setCancelled(true);
            org.bukkit.Location loc = player.getLocation();
            java.util.Set<Player> recipients = new java.util.HashSet<>();
            for (Player other : player.getWorld().getPlayers()) {
                if (other.getLocation().distance(loc) <= radius) {
                    other.sendMessage(formatted);
                    recipients.add(other);
                }
            }

            // Проверяем, есть ли другие получатели (кроме отправителя)
            recipients.remove(player);
            if (recipients.isEmpty()) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Вас никто не услышал.</#FF3366>"));
            } else {
                recipients.add(player); // возвращаем для звуков
                if (module.isLocalChatSoundsEnabled()) {
                    String sendSound = module.getLocalSoundSend();
                    String receiveSound = module.getLocalSoundReceive();
                    if (sendSound != null && !sendSound.isEmpty() && !"none".equalsIgnoreCase(sendSound.trim())) {
                        playLocalSound(player, sendSound);
                    }
                    for (Player other : recipients) {
                        if (!other.equals(player) && receiveSound != null && !receiveSound.isEmpty() && !"none".equalsIgnoreCase(receiveSound.trim())) {
                            playLocalSound(other, receiveSound);
                        }
                    }
                }
                recipients.remove(player); // убираем для spy
                String lang = module.getPlugin() instanceof ru.managerfix.ManagerFix mf
                        ? mf.getConfigManager().getDefaultLanguage() : null;
                String spyRaw = MessageUtil.getRaw(module.getPlugin(), lang, "chat.spy-prefix");
                Component spyPrefix = (spyRaw != null && !spyRaw.isEmpty()) ? MessageUtil.parse(spyRaw) : MessageUtil.parse("<dark_gray>[Spy] </dark_gray>");
                for (Player other : module.getPlugin().getServer().getOnlinePlayers()) {
                    if (other.equals(player) || recipients.contains(other)) continue;
                    if (Boolean.TRUE.equals(profileManager.getProfile(other).getMetadata("chatspy").orElse(false))
                            && other.hasPermission("managerfix.chat.spy")) {
                        other.sendMessage(net.kyori.adventure.text.Component.empty().append(spyPrefix).append(formatted));
                    }
                }
            }
        } else {
            // Global: cancel and broadcast ourselves so delivery isn't blocked by other plugins (e.g. "ошибка проверки чата")
            event.setCancelled(true);
            java.util.Set<Player> recipients = new java.util.HashSet<>();
            for (Player other : module.getPlugin().getServer().getOnlinePlayers()) {
                other.sendMessage(formatted);
                recipients.add(other);
            }

            // Проверяем, есть ли другие получатели (кроме отправителя)
            recipients.remove(player);
            if (recipients.isEmpty()) {
                player.sendMessage(MessageUtil.parse("<#FF3366>Вас никто не услышал.</#FF3366>"));
            }
        }
    }

    /**
     * Splits the format string at {player}, inserts nameComponent as a sibling (not child),
     * so nick colors are NOT overridden by surrounding format colors.
     * Uses Component.empty() as root → all parts are siblings → no style inheritance.
     */
    private static Component buildChatFormatted(String format, Player player, Component nameComponent,
                                                String messageTextToCopy, ChatModule module) {
        String resolved = MessageUtil.setPlaceholders(player, format);
        String[] parts = resolved.split("\\{player\\}", 2);
        if (parts.length == 2) {
            String after = parts[1].replaceFirst("^(\\s*</[^>]+>)+", "");
            Component beforeComp = MessageUtil.parse(parts[0]);
            Component afterComp = MessageUtil.parse(after);
            if (module.isMessageHoverEnabled()) {
                Component msgHover = buildMessageHoverComponent(player, module);
                if (msgHover != null) {
                    afterComp = afterComp.hoverEvent(HoverEvent.showText(msgHover))
                            .clickEvent(ClickEvent.copyToClipboard(messageTextToCopy));
                }
            }
            // Component.empty() as root: all three are siblings → no color inheritance
            return Component.empty().append(beforeComp).append(nameComponent).append(afterComp);
        }
        // Fallback: no {player} placeholder
        String plain = resolved.replace("{player}",
                PlainTextComponentSerializer.plainText().serialize(nameComponent));
        Component result = MessageUtil.parse(plain);
        if (module.isMessageHoverEnabled()) {
            Component msgHover = buildMessageHoverComponent(player, module);
            if (msgHover != null) {
                result = result.hoverEvent(HoverEvent.showText(msgHover))
                        .clickEvent(ClickEvent.copyToClipboard(messageTextToCopy));
            }
        }
        return result;
    }

    private static void playLocalSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty() || "none".equalsIgnoreCase(soundName.trim())) return;
        Sound sound = resolveSound(soundName);
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    private static Sound resolveSound(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        NamespacedKey key = trimmed.contains(":")
                ? NamespacedKey.fromString(trimmed)
                : NamespacedKey.minecraft(trimmed.toLowerCase());
        return key != null ? Registry.SOUNDS.get(key) : null;
    }

    private static Component buildMessageHoverComponent(Player player, ChatModule module) {
        String format = module.getMessageHoverFormat();
        if (format == null || format.isEmpty()) return null;
        String timePattern = module.getMessageHoverTimeFormat();
        if (timePattern == null) timePattern = "HH:mm";
        try {
            String timeStr = ZonedDateTime.now().format(DateTimeFormatter.ofPattern(timePattern));
            String text = MessageUtil.replace(format, Map.of("time", timeStr));
            text = MessageUtil.setPlaceholders(player, text);
            return MessageUtil.parse(text);
        } catch (Exception e) {
            return MessageUtil.parse(MessageUtil.replace(format, Map.of("time", "--:--")));
        }
    }

    private static String getBalanceString(Player player, ExternalApiService externalApiService) {
        if (externalApiService == null) return "—";
        return externalApiService.getFormattedBalance(player);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Логируем сообщение в чат (всегда видно в консоли)
        module.getPlugin().getLogger().info("[CHAT] " + player.getName() + ": " + message);

        // Пропускаем сообщения с префиксом % (клановый чат от ClansFix)
        if (message != null && message.startsWith("%")) {
            return;
        }

        event.setCancelled(true);
        event.getRecipients().clear();
    }
}
