package ru.managerfix.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI list of worlds. Styled frame. Click to teleport to world spawn.
 */
public final class WorldsMenuGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int CONTENT_PER_PAGE = CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public WorldsMenuGui(ManagerFix plugin, GuiManager guiManager) {
        this(plugin, guiManager, plugin.getUIThemeManager(), plugin.getGuiTemplate());
    }

    public WorldsMenuGui(ManagerFix plugin, GuiManager guiManager,
                        UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player player, int page) {
        List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
        int totalPages = Math.max(1, (int) Math.ceil((double) worlds.size() / CONTENT_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * CONTENT_PER_PAGE;
        int to = Math.min(from + CONTENT_PER_PAGE, worlds.size());

        String raw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), "menu.worlds-title");
        String displayTitle = raw != null ? raw : "Миры";

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleMenu(displayTitle))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("worlds_" + safePage);

        for (int i = from; i < to; i++) {
            World w = worlds.get(i);
            int contentIndex = i - from;
            int slot = CONTENT_SLOTS[contentIndex];
            Material iconMat = w.getEnvironment() == World.Environment.NETHER ? Material.NETHERRACK : (w.getEnvironment() == World.Environment.THE_END ? Material.END_STONE : Material.GRASS_BLOCK);
            ItemStack item = new ItemBuilder(iconMat)
                    .name(MessageUtil.parse(UIThemeManager.GRADIENT_ACCENT + w.getName() + "</gradient>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Игроков: " + w.getPlayers().size() + "</#e9d5ff>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — телепорт</#e9d5ff>"))
                    .hideFlags(true)
                    .build();
            builder.button(slot, Button.builder(item).onClick(e -> {
                if (!player.hasPermission("managerfix.worlds.teleport")) {
                    MessageUtil.send(plugin, player, "no-permission");
                    return;
                }
                player.closeInventory();
                player.teleport(w.getSpawnLocation());
                MessageUtil.send(plugin, player, "worlds.teleported", Map.of("world", w.getName()));
            }).build());
        }

        if (totalPages > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Предыдущая страница</#e9d5ff>"))
                    .hideFlags(true).build();
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Следующая страница</#e9d5ff>"))
                    .hideFlags(true).build();
            builder.button(PREV_SLOT, Button.builder(prev).onClick(e -> { if (safePage > 0) open(player, safePage - 1); }).build());
            builder.button(NEXT_SLOT, Button.builder(next).onClick(e -> { if (safePage < totalPages - 1) open(player, safePage + 1); }).build());
        }

        guiManager.open(player, builder.build());
    }
}
