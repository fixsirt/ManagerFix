package ru.managerfix.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.modules.kits.KitData;
import ru.managerfix.modules.kits.KitsModule;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Kits GUI: styled frame, list kits. LKM — receive kit.
 */
public final class KitsMenuGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int CONTENT_PER_PAGE = CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final KitsModule kitsModule;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public KitsMenuGui(ManagerFix plugin, GuiManager guiManager, KitsModule kitsModule) {
        this(plugin, guiManager, kitsModule, plugin.getUIThemeManager(), plugin.getGuiTemplate());
    }

    public KitsMenuGui(ManagerFix plugin, GuiManager guiManager, KitsModule kitsModule,
                       UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.kitsModule = kitsModule;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player player, int page) {
        List<String> names = kitsModule.getKitManager().getKitNames();
        int totalPages = Math.max(1, (int) Math.ceil((double) names.size() / CONTENT_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * CONTENT_PER_PAGE;
        int to = Math.min(from + CONTENT_PER_PAGE, names.size());

        String titleKey = "menu.kits-title";
        String raw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), titleKey);
        String displayTitle = raw != null ? raw : "Киты";

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleMenu(displayTitle))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("kits_" + safePage);

        for (int i = from; i < to; i++) {
            String name = names.get(i);
            int contentIndex = i - from;
            int slot = CONTENT_SLOTS[contentIndex];
            KitData kit = kitsModule.getKitManager().getKit(name).orElse(null);
            if (kit == null) continue;
            boolean hasKitPerm = player.hasPermission(kit.getPermission());
            ItemStack icon = buildKitIcon(kit, hasKitPerm);
            Button btn = Button.builder(icon)
                    .onClick(e -> {
                        if (hasKitPerm) {
                            if (kitsModule.getKitManager().giveKit(player, name)) {
                                player.closeInventory();
                            }
                        } else {
                            openPreview(player, kit);
                        }
                    })
                    .onRightClick(e -> openPreview(player, kit))
                    .build();
            builder.button(slot, btn);
        }

        if (totalPages > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Предыдущая страница</#e9d5ff>"))
                    .hideFlags(true)
                    .build();
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Следующая страница</#e9d5ff>"))
                    .hideFlags(true)
                    .build();
            builder.button(PREV_SLOT, Button.builder(prev).onClick(e -> {
                if (safePage > 0) open(player, safePage - 1);
            }).build());
            builder.button(NEXT_SLOT, Button.builder(next).onClick(e -> {
                if (safePage < totalPages - 1) open(player, safePage + 1);
            }).build());
        }

        guiManager.open(player, builder.build());
    }

    private ItemStack buildKitIcon(KitData kit, boolean hasPermission) {
        ItemBuilder ib = new ItemBuilder(kit.getIcon())
                .name((hasPermission ? "<#FAA300>" : "<#C0280F>") + kit.getName())
                .hideFlags(true);

        // Добавляем lore
        ib.addLore(MessageUtil.parse("<gray>▸ " + formatCooldown(kit.getCooldownSeconds(), kit.isOneTime())));

        if (kit.isOneTime()) {
            ib.addLore(MessageUtil.parse("<green>✓ Только один раз"));
        }

        ib.addLore(MessageUtil.parse("<#E0E0E0>" + (hasPermission ? "▸ ЛКМ — получить" : "▸ ЛКМ — предосмотр")));
        ib.addLore(MessageUtil.parse("<#E0E0E0>▸ ПКМ — предосмотр"));

        return ib.build();
    }

    /**
     * Форматирует кулдаун в читаемый формат.
     */
    private String formatCooldown(int seconds, boolean oneTime) {
        if (oneTime) {
            return "<green>Одноразовый кит</green>";
        }
        if (seconds <= 0) {
            return "<green>Кулдаун: Нет</green>";
        }
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        
        StringBuilder sb = new StringBuilder("<yellow>Кулдаун: </yellow>");
        if (hours > 0) {
            sb.append(hours).append("ч ");
        }
        if (minutes > 0 || hours == 0) {
            sb.append(minutes).append("м");
        }
        
        return sb.toString();
    }

    private void openPreview(Player player, KitData kit) {
        int size = 54;
        GuiBuilder preview = GuiBuilder.of(size)
                .title(UIThemeManager.GRADIENT_MAIN + "Предосмотр: " + kit.getName() + "</gradient>")
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, size))
                .holderId("kits_preview");
        int[] slots = GuiTemplate.getContentSlots54();
        int idx = 0;
        for (ItemStack it : kit.getItems()) {
            if (it == null || it.getType().isAir()) continue;
            if (idx >= slots.length) break;
            preview.button(slots[idx], Button.builder(it.clone()).build());
            idx++;
        }
        guiManager.open(player, preview.build());
    }
}
