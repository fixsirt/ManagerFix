package ru.managerfix.modules.tpa;

import org.bukkit.Bukkit;
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

/**
 * GUI for TPA request: Accept / Deny. Opens on click from request message or /tpareply.
 */
public final class TpaGui {

    private static final int SIZE = 27;
    private static final int ACCEPT_SLOT = 11;
    private static final int DENY_SLOT = 15;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final TpaService service;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public TpaGui(ManagerFix plugin, GuiManager guiManager, TpaService service) {
        this.plugin = plugin;
        this.guiManager = guiManager != null ? guiManager : plugin.getGuiManager();
        this.service = service;
        this.theme = plugin.getUIThemeManager() != null ? plugin.getUIThemeManager() : new UIThemeManager();
        this.template = plugin.getGuiTemplate() != null ? plugin.getGuiTemplate() : new GuiTemplate(theme);
    }

    public void openIfHasRequest(Player target) {
        if (target == null || !target.isOnline()) return;
        var reqOpt = service.getRequest(target.getUniqueId());
        if (reqOpt.isEmpty()) return;
        TpaRequest req = reqOpt.get();
        String fromName = resolveName(req.getFrom());
        open(target, fromName, req);
    }

    private String resolveName(java.util.UUID uuid) {
        Player p = ru.managerfix.utils.NickResolver.getPlayerByUuid(uuid);
        if (p != null) return ru.managerfix.utils.NickResolver.plainDisplayName(p);
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n != null ? n : "?";
    }

    public void open(Player target, String fromName, TpaRequest req) {
        if (target == null || !target.isOnline()) return;
        String title = service.getConfig().getMessage("menu.tpa-confirm-title", "Запрос на телепортацию");
        if (title == null) title = "Запрос на телепортацию";

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(MessageUtil.parse(title))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("tpa_confirm");

        org.bukkit.inventory.ItemStack acceptBtn = new ItemBuilder(Material.LIME_WOOL)
                .name(MessageUtil.parse(UIThemeManager.GRADIENT_SUCCESS + "✔ Принять</gradient>"))
                .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + fromName + " ➜ к вам</#E0E0E0>"))
                .hideFlags(true)
                .build();
        org.bukkit.inventory.ItemStack denyBtn = new ItemBuilder(Material.RED_WOOL)
                .name(MessageUtil.parse(UIThemeManager.GRADIENT_ERROR + "✖ Отклонить</gradient>"))
                .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + fromName + "</#E0E0E0>"))
                .hideFlags(true)
                .build();

        builder.button(ACCEPT_SLOT, Button.builder(acceptBtn).onClick(e -> {
            if (!(e.getWhoClicked() instanceof Player player)) return;
            var removed = service.removeRequest(player.getUniqueId());
            if (removed.isEmpty()) {
                player.closeInventory();
                return;
            }
            TpaRequest r = removed.get();
            Player from = ru.managerfix.utils.NickResolver.getPlayerByUuid(r.getFrom());
            if (from == null || !from.isOnline()) {
                player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("request-expired", "<red>Запрос истёк.")));
                player.closeInventory();
                return;
            }
            player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("accepted", "<green>Запрос принят!")));
            from.sendMessage(MessageUtil.parse(service.getConfig().getMessage("accepted", "<green>Запрос принят!")));
            if (r.isTpaHere()) {
                service.scheduleTeleport(player, from.getLocation().clone(), () ->
                        player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("cancelled-move", "<red>Телепортация отменена: вы сдвинулись."))));
            } else {
                service.scheduleTeleport(from, player.getLocation().clone(), () ->
                        from.sendMessage(MessageUtil.parse(service.getConfig().getMessage("cancelled-move", "<red>Телепортация отменена: вы сдвинулись."))));
            }
            player.closeInventory();
        }).build());

        builder.button(DENY_SLOT, Button.builder(denyBtn).onClick(e -> {
            if (!(e.getWhoClicked() instanceof Player player)) return;
            var removed = service.removeRequest(player.getUniqueId());
            player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("denied", "<red>Запрос отклонён.")));
            if (removed.isPresent()) {
                Player from = ru.managerfix.utils.NickResolver.getPlayerByUuid(removed.get().getFrom());
                if (from != null && from.isOnline()) {
                    from.sendMessage(MessageUtil.parse(service.getConfig().getMessage("denied", "<red>Запрос отклонён.")));
                }
            }
            player.closeInventory();
        }).build());

        Inventory inv = builder.build();
        guiManager.open(target, inv);
    }
}
