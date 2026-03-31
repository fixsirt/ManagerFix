package ru.managerfix.api.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Формат чата.
 */
public class ChatFormat {
    
    public static final ChatFormat GLOBAL = new ChatFormat(
        "<gray>[Глобальный] <white>{player}: {message}</white></gray>",
        NamedTextColor.WHITE
    );
    
    public static final ChatFormat LOCAL = new ChatFormat(
        "<gray>[Локальный] <white>{player}: {message}</white></gray> <gray>({radius}м)</gray>",
        NamedTextColor.GRAY
    );
    
    public static final ChatFormat PRIVATE = new ChatFormat(
        "<gray>[ЛС] <white>{sender} → {target}: {message}</white></gray>",
        NamedTextColor.GRAY
    );
    
    public static final ChatFormat CHANNEL = new ChatFormat(
        "<gray>[{channel}] <white>{player}: {message}</white></gray>",
        NamedTextColor.GRAY
    );
    
    private final String format;
    private final NamedTextColor color;
    
    public ChatFormat(String format, NamedTextColor color) {
        this.format = format;
        this.color = color;
    }
    
    public String getFormat() {
        return format;
    }
    
    public NamedTextColor getColor() {
        return color;
    }
    
    /**
     * Применить формат к сообщению.
     */
    public Component apply(String player, Component message) {
        String formatted = format
            .replace("{player}", player)
            .replace("{message}", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(message));
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(formatted);
    }
}
