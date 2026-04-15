package ru.managerfix.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.modules.ban.BanModule;
import ru.managerfix.modules.ban.BanRecord;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI list of bans (read-only). Styled frame, pagination.
 */
public final class BanListGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int CONTENT_PER_PAGE = CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int SORT_SLOT = 47;
    private static final int FILTER_SLOT = 51;

    private enum SortMode { BY_DATE, BY_NAME }
    private enum FilterMode { ALL, PERM_ONLY, TEMP_ONLY }

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final BanModule banModule;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public BanListGui(ManagerFix plugin, GuiManager guiManager, BanModule banModule) {
        this(plugin, guiManager, banModule, plugin.getUIThemeManager(), plugin.getGuiTemplate());
    }

    public BanListGui(ManagerFix plugin, GuiManager guiManager, BanModule banModule,
                     UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.banModule = banModule;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player player, int page) {
        open(player, page, SortMode.BY_DATE, FilterMode.ALL);
    }

    private void open(Player player, int page, SortMode sort, FilterMode filter) {
        banModule.getBanManager().getBansAsync(all -> {
            List<BanRecord> list = new ArrayList<>(all);
            if (filter == FilterMode.PERM_ONLY) {
                list.removeIf(r -> !r.isPermanent());
            } else if (filter == FilterMode.TEMP_ONLY) {
                list.removeIf(BanRecord::isPermanent);
            }
            if (sort == SortMode.BY_DATE) {
                list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            } else {
                list.sort((a, b) -> {
                    String an = a.getTargetName() != null ? a.getTargetName() : "";
                    String bn = b.getTargetName() != null ? b.getTargetName() : "";
                    return an.compareToIgnoreCase(bn);
                });
            }
            int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / CONTENT_PER_PAGE));
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            int from = safePage * CONTENT_PER_PAGE;
            int to = Math.min(from + CONTENT_PER_PAGE, list.size());

            String titleKey = "menu.banlist-title";
            String raw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), titleKey);
            String displayTitle = raw != null ? raw : "Список банов";

            GuiBuilder builder = GuiBuilder.of(SIZE)
                    .title(theme.titleMenu(displayTitle))
                    .frame(true)
                    .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                    .holderId("banlist_" + safePage + "_" + sort + "_" + filter);

            if (list.isEmpty()) {
                int center = CONTENT_SLOTS[CONTENT_SLOTS.length / 2];
                ItemStack empty = new ItemBuilder(Material.BARRIER)
                        .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Нет активных банов</#F0F4F8>"))
                        .hideFlags(true)
                        .build();
                builder.button(center, Button.builder(empty).build());
            } else {
                for (int i = from; i < to; i++) {
                    BanRecord r = list.get(i);
                    int contentIndex = i - from;
                    int slot = CONTENT_SLOTS[contentIndex];
                    boolean permanent = r.isPermanent();
                    String nameLine = permanent
                            ? "<#FF3366>⛔ " + r.getTargetName() + "</#FF3366>"
                            : UIThemeManager.GRADIENT_ACCENT + r.getTargetName() + "</gradient>";
                    ItemBuilder ib = new ItemBuilder(Material.PLAYER_HEAD)
                            .skullOwner(r.getTargetName())
                            .name(MessageUtil.parse(nameLine))
                            .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Причина: " + (r.getReason() != null ? r.getReason() : "") + "</#F0F4F8>"))
                            .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Кем: " + (r.getSource() != null ? r.getSource() : "Console") + "</#F0F4F8>"));
                    if (permanent) {
                        ib.addLore(MessageUtil.parse("<#FF3366>Навсегда</#FF3366>"));
                    } else {
                        ib.addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "До: " + new java.util.Date(r.getExpiresAt()) + "</#F0F4F8>"));
                    }
                    ib.hideFlags(true);
                    ItemStack icon = ib.build();
                    builder.button(slot, Button.builder(icon).build());
                }
            }

            if (totalPages > 1) {
                ItemStack prev = new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Предыдущая страница</#F0F4F8>"))
                        .hideFlags(true).build();
                ItemStack next = new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Следующая страница</#F0F4F8>"))
                        .hideFlags(true).build();
                builder.button(PREV_SLOT, Button.builder(prev).onClick(e -> { if (safePage > 0) open(player, safePage - 1, sort, filter); }).build());
                builder.button(NEXT_SLOT, Button.builder(next).onClick(e -> { if (safePage < totalPages - 1) open(player, safePage + 1, sort, filter); }).build());
                ItemStack pageInfo = new ItemBuilder(Material.PAPER)
                        .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Страница " + (safePage + 1) + " / " + totalPages + "</#F0F4F8>"))
                        .hideFlags(true).build();
                builder.button(49, Button.builder(pageInfo).build());
            }

            ItemStack sortBtn = new ItemBuilder(Material.COMPASS)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Сортировка: " + (sort == SortMode.BY_DATE ? "дата" : "ник") + "</#F0F4F8>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — переключить</#F0F4F8>"))
                    .hideFlags(true).build();
            builder.button(SORT_SLOT, Button.builder(sortBtn).onClick(e -> {
                SortMode nextSort = (sort == SortMode.BY_DATE) ? SortMode.BY_NAME : SortMode.BY_DATE;
                open(player, 0, nextSort, filter);
            }).build());

            ItemStack filterBtn = new ItemBuilder(Material.HOPPER)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Фильтр: " + switch (filter) {
                        case ALL -> "все";
                        case PERM_ONLY -> "перманентные";
                        case TEMP_ONLY -> "временные";
                    } + "</#F0F4F8>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — переключить</#F0F4F8>"))
                    .hideFlags(true).build();
            builder.button(FILTER_SLOT, Button.builder(filterBtn).onClick(e -> {
                FilterMode nextFilter = switch (filter) {
                    case ALL -> FilterMode.PERM_ONLY;
                    case PERM_ONLY -> FilterMode.TEMP_ONLY;
                    case TEMP_ONLY -> FilterMode.ALL;
                };
                open(player, 0, sort, nextFilter);
            }).build());

            plugin.getScheduler().runSync(() -> guiManager.open(player, builder.build()));
        });
    }
}
