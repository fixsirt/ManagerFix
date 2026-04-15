package ru.managerfix.gui;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.*;

/**
 * Admin GUI to view and manage another player's homes.
 * LKM — teleport admin to target's home; PKM — delete target's home.
 */
public final class AdminHomesMenuGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int CONTENT_PER_PAGE = CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;
    private final UUID targetUuid;
    private final String targetName;

    public AdminHomesMenuGui(ManagerFix plugin, GuiManager guiManager, UUID targetUuid, String targetName) {
        this(plugin, guiManager, targetUuid, targetName, plugin.getUIThemeManager(), plugin.getGuiTemplate());
    }

    public AdminHomesMenuGui(ManagerFix plugin, GuiManager guiManager, UUID targetUuid, String targetName,
                             UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player admin, int page) {
        ProfileManager profileManager = plugin.getProfileManager();
        PlayerProfile profile = profileManager.getProfile(targetUuid);
        List<String> names = new ArrayList<>(new TreeSet<>(profile.getHomeNames()));
        int totalPages = Math.max(1, (int) Math.ceil((double) names.size() / CONTENT_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * CONTENT_PER_PAGE;
        int to = Math.min(from + CONTENT_PER_PAGE, names.size());

        String displayTitle = "Дома: " + targetName;
        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleMenu(displayTitle))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("admin_homes_" + targetName + "_" + safePage);

        for (int i = from; i < to; i++) {
            String name = names.get(i);
            int contentIndex = i - from;
            int slot = CONTENT_SLOTS[contentIndex];
            Location loc = profile.getHome(name).orElse(null);
            ItemStack icon = buildHomeIcon(name, loc);
            Button btn = Button.builder(icon)
                    .onClick(e -> teleportAdmin(admin, loc))
                    .onRightClick(e -> deleteHome(admin, profileManager, profile, name))
                    .build();
            builder.button(slot, btn);
        }

        if (totalPages > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Предыдущая страница</#F0F4F8>"))
                    .hideFlags(true)
                    .build();
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Следующая страница</#F0F4F8>"))
                    .hideFlags(true)
                    .build();
            builder.button(PREV_SLOT, Button.builder(prev).onClick(e -> {
                if (safePage > 0) open(admin, safePage - 1);
            }).build());
            builder.button(NEXT_SLOT, Button.builder(next).onClick(e -> {
                if (safePage < totalPages - 1) open(admin, safePage + 1);
            }).build());
        }

        guiManager.open(admin, builder.build());
    }

    private ItemStack buildHomeIcon(String name, Location loc) {
        ItemBuilder ib = new ItemBuilder(Material.ORANGE_BED)
                .name(MessageUtil.parse(UIThemeManager.GRADIENT_ACCENT + name + "</gradient>"))
                .hideFlags(true);
        if (loc != null && loc.getWorld() != null) {
            ib.addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Мир: " + loc.getWorld().getName() + "</#F0F4F8>"));
            ib.addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — телепорт (админ)</#F0F4F8>"));
            ib.addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ПКМ — удалить дом</#F0F4F8>"));
        }
        return ib.build();
    }

    private void teleportAdmin(Player admin, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        var tpaMod = plugin.getModuleManager().getEnabledModule("tpa")
            .filter(m -> m instanceof ru.managerfix.modules.tpa.TpaModule)
            .map(m -> (ru.managerfix.modules.tpa.TpaModule) m)
            .orElse(null);
        if (tpaMod != null && tpaMod.getTpaService() != null) {
            tpaMod.getTpaService().scheduleTeleport(admin, loc.clone(), null);
        } else {
            admin.teleport(loc.clone());
        }
    }

    private void deleteHome(Player admin, ProfileManager pm, PlayerProfile profile, String name) {
        if (!admin.hasPermission("managerfix.homes.admin")) {
            MessageUtil.send(plugin, admin, "no-permission");
            return;
        }
        if (profile.removeHome(name)) {
            pm.saveProfileAsync(profile.getUuid());
            open(admin, 0);
        }
    }
}
