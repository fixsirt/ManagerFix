package ru.managerfix.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.modules.afk.AfkManager;
import ru.managerfix.modules.afk.AfkManager.AfkTopEntry;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.List;

/**
 * Styled AFK top menu: players with most total AFK time. Same style as /managerfix menu.
 */
public final class AfkTopGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int MAX_ENTRIES = CONTENT_SLOTS.length;

    private final GuiManager guiManager;
    private final AfkManager afkManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public AfkTopGui(ManagerFix plugin, GuiManager guiManager, AfkManager afkManager) {
        this(guiManager, afkManager,
                plugin.getUIThemeManager(), plugin.getGuiTemplate());
    }

    public AfkTopGui(GuiManager guiManager, AfkManager afkManager,
                     UIThemeManager theme, GuiTemplate template) {
        this.guiManager = guiManager;
        this.afkManager = afkManager;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player player) {
        List<AfkTopEntry> top = afkManager.getTopAfkPlayers(MAX_ENTRIES);

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleMenu("Топ AFK"))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("afk_top");

        if (top.isEmpty()) {
            ItemStack empty = new ItemBuilder(Material.CLOCK)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Нет данных</#e9d5ff>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ В кэше только игроки на сервере</#e9d5ff>"))
                    .hideFlags(true)
                    .build();
            builder.button(CONTENT_SLOTS[CONTENT_SLOTS.length / 2], Button.builder(empty).build());
        } else {
            for (int i = 0; i < top.size() && i < CONTENT_SLOTS.length; i++) {
                AfkTopEntry entry = top.get(i);
                int rank = i + 1;
                int slot = CONTENT_SLOTS[i];
                ItemStack icon = buildEntryIcon(entry, rank);
                builder.button(slot, Button.builder(icon).build());
            }
        }

        guiManager.open(player, builder.build());
    }

    private ItemStack buildEntryIcon(AfkTopEntry entry, int rank) {
        String nameLine;
        if (rank == 1) {
            nameLine = UIThemeManager.GRADIENT_ACCENT + "#1 " + entry.getPlayerName() + "</gradient>";
        } else if (rank == 2) {
            nameLine = "<#c0c0c0>#2 " + entry.getPlayerName() + "</#c0c0c0>";
        } else if (rank == 3) {
            nameLine = UIThemeManager.COLOR_WARNING + "#3 " + entry.getPlayerName() + "</#fbbf24>";
        } else {
            nameLine = UIThemeManager.COLOR_INFO + "#" + rank + " " + entry.getPlayerName() + "</#e9d5ff>";
        }

        String timeStr = formatAfkTime(entry.totalSeconds());
        String loreLine = UIThemeManager.COLOR_INFO + "▸ Время в AFK: " + timeStr + "</#e9d5ff>";

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(entry.getPlayerName())
                .name(MessageUtil.parse(nameLine))
                .addLore(MessageUtil.parse(loreLine))
                .hideFlags(true)
                .build();
    }

    private static String formatAfkTime(long totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + " сек";
        }
        long minutes = totalSeconds / 60;
        if (minutes < 60) {
            return minutes + " мин";
        }
        long hours = minutes / 60;
        minutes = minutes % 60;
        if (hours < 24) {
            return hours + " ч " + (minutes > 0 ? minutes + " мин" : "");
        }
        long days = hours / 24;
        hours = hours % 24;
        return days + " д " + (hours > 0 ? hours + " ч" : "");
    }
}
