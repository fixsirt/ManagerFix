package ru.managerfix.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.modules.names.NamesModule;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin GUI: list online players, LMB = set nick via chat, Shift+LMB = reset nick. Styled as MainMenu.
 */
public final class NamesGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int CONTENT_PER_PAGE = CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final NamesModule namesModule;

    public NamesGui(ManagerFix plugin, GuiManager guiManager, NamesModule namesModule) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.namesModule = namesModule;
    }

    public void open(Player admin, int page) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPages = Math.max(1, (int) Math.ceil((double) players.size() / CONTENT_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * CONTENT_PER_PAGE;
        int to = Math.min(from + CONTENT_PER_PAGE, players.size());

        String raw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), "menu.names-title");
        String displayTitle = raw != null ? raw : "Ники";

        UIThemeManager theme = plugin.getUIThemeManager();
        GuiTemplate template = plugin.getGuiTemplate();
        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleMenu(displayTitle))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("names_" + safePage);

        ProfileManager pm = plugin.getProfileManager();
        for (int i = from; i < to; i++) {
            Player target = players.get(i);
            int contentIndex = i - from;
            int slot = CONTENT_SLOTS[contentIndex];
            String currentNick = namesModule.getStoredNick(target, pm);
            String displayNick = currentNick != null && !currentNick.isEmpty() ? currentNick : target.getName();

            ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(target.getName())
                    .name(MessageUtil.parse(UIThemeManager.GRADIENT_ACCENT + target.getName() + "</gradient>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Ник: " + displayNick + "</#F0F4F8>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — ввести новый ник в чат</#F0F4F8>"))
                    .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ Shift+ЛКМ — сбросить ник</#F0F4F8>"))
                    .hideFlags(true)
                    .build();

            Player finalTarget = target;
            Button btn = Button.builder(head)
                    .onClick(e -> {
                        namesModule.setPendingNickTarget(admin.getUniqueId(), finalTarget.getUniqueId());
                        admin.closeInventory();
                        MessageUtil.send(plugin, admin, "names.enter-in-chat", java.util.Map.of("player", finalTarget.getName()));
                    })
                    .onShiftRightClick(e -> {
                        namesModule.setNick(finalTarget, pm, null, () -> {
                            guiManager.sendConfirmation(admin, "Ник сброшен");
                            open(admin, safePage);
                        });
                    })
                    .build();
            builder.button(slot, btn);
        }

        if (totalPages > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Предыдущая страница</#F0F4F8>"))
                    .hideFlags(true).build();
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Следующая страница</#F0F4F8>"))
                    .hideFlags(true).build();
            builder.button(PREV_SLOT, Button.builder(prev).onClick(e -> { if (safePage > 0) open(admin, safePage - 1); }).build());
            builder.button(NEXT_SLOT, Button.builder(next).onClick(e -> { if (safePage < totalPages - 1) open(admin, safePage + 1); }).build());
        }

        guiManager.open(admin, builder.build());
    }
}
