package ru.managerfix.api.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Формат чата.
 */
public class ChatFormat {
    
    public static final ChatFormat GLOBAL = new ChatFormat(
        "<#F0F4F8>[Глобальный] <#F0F4F8>{player}: {message}</#F0F4F8></#F0F4F8>",
        NamedTextColor.WHITE
    );

    public static final ChatFormat LOCAL = new ChatFormat(
        "<#F0F4F8>[Локальный] <#F0F4F8>{player}: {message}</#F0F4F8></#F0F4F8> <#F0F4F8>({radius}м)</#F0F4F8>",
        NamedTextColor.GRAY
    );

    public static final ChatFormat PRIVATE = new ChatFormat(
        "<#F0F4F8>[ЛС] <#F0F4F8>{sender} → {target}: {message}</#F0F4F8></#F0F4F8>",
        NamedTextColor.GRAY
    );

    public static final ChatFormat CHANNEL = new ChatFormat(
        "<#F0F4F8>[{channel}] <#F0F4F8>{player}: {message}</#F0F4F8></#F0F4F8>",
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
