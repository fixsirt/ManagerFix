package ru.managerfix.modules.rtp;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.Button;
import ru.managerfix.gui.GuiBuilder;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

final class RtpGui {

    private static final int SIZE = 27;
    private static final int SLOT_1000 = 11;
    private static final int SLOT_5000 = 13;
    private static final int SLOT_NEAR = 15;

    private static final String PERM_1000 = "managerfix.rtp.option.1000";
    private static final String PERM_5000 = "managerfix.rtp.option.5000";
    private static final String PERM_NEAR = "managerfix.rtp.option.randomplayer";
    private static final String CURRENCY = "$";

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;
    private final RtpService service;

    RtpGui(ManagerFix plugin, GuiManager guiManager, UIThemeManager theme, GuiTemplate template, RtpService service) {
        this.plugin = plugin;
        this.guiManager = guiManager != null ? guiManager : plugin.getGuiManager();
        this.theme = theme != null ? theme : plugin.getUIThemeManager();
        this.template = template != null ? template : plugin.getGuiTemplate();
        this.service = service;
    }

    void open(Player player) {
        String title = "<gradient:#7000FF:#00C8FF>Рандомный телепорт</gradient>";
        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(MessageUtil.parse(title))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("rtp_menu");

        String nearRange = service.getNearRtpMin() + "-" + service.getNearRtpMax();
        String farRange = service.getFarRtpMin() + "-" + service.getFarRtpMax();
        String playerRange = service.getPlayerRadiusMin() + "-" + service.getPlayerRadiusMax();

        var item1000 = new ItemBuilder(Material.ENDER_PEARL)
                .name(MessageUtil.parse(UIThemeManager.GRADIENT_MAIN + "Ближний RTP (" + nearRange + ")</gradient>"))
                .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Случайная точка вокруг спавна</#F0F4F8>"))
                .hideFlags(true)
                .build();
        var item5000 = new ItemBuilder(Material.ENDER_EYE)
                .name(MessageUtil.parse(UIThemeManager.GRADIENT_MAIN + "Дальний RTP (" + farRange + ")</gradient>"))
                .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Дальняя случайная точка</#F0F4F8>"))
                .hideFlags(true)
                .build();
        String costLine = "<#F0F4F8>Стоимость: <#00C8FF>" + (int) Math.round(service.getNearTeleportCost()) + CURRENCY + "</#00C8FF>";
        var itemNear = new ItemBuilder(Material.PLAYER_HEAD)
                .name(MessageUtil.parse(UIThemeManager.GRADIENT_SUCCESS + "К случайному игроку</gradient>"))
                .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "В радиусе " + playerRange + " блоков</#F0F4F8>"))
                .addLore(MessageUtil.parse(costLine))
                .hideFlags(true)
                .build();

        builder.button(SLOT_1000, Button.builder(item1000).onClick(e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (!p.hasPermission(PERM_1000)) {
                p.sendMessage(MessageUtil.parse("<red>Нет доступа."));
                return;
            }
            p.closeInventory();
            service.randomTeleport(p, "near");
        }).build());
        builder.button(SLOT_5000, Button.builder(item5000).onClick(e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (!p.hasPermission(PERM_5000)) {
                p.sendMessage(MessageUtil.parse("<red>Нет доступа."));
                return;
            }
            p.closeInventory();
            service.randomTeleport(p, "far");
        }).build());
        builder.button(SLOT_NEAR, Button.builder(itemNear).onClick(e -> {
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (!p.hasPermission(PERM_NEAR)) {
                p.sendMessage(MessageUtil.parse("<red>Нет доступа."));
                return;
            }
            p.closeInventory();
            service.teleportToRandomNearbyPlayer(p);
        }).build());

        Inventory inv = builder.build();
        guiManager.open(player, inv);
    }
}
