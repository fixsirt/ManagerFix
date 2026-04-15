package ru.managerfix.modules.tpa;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.managerfix.ManagerFix;
import java.util.List;
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
        List<TpaRequest> reqs = service.getRequests(target.getUniqueId());
        if (reqs.isEmpty()) return;
        TpaRequest req = reqs.get(0);
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
                .name(MessageUtil.parse("<green>✔ Принять"))
                .addLore(MessageUtil.parse("<gray>" + fromName + " ➜ к вам"))
                .hideFlags(true)
                .build();
        org.bukkit.inventory.ItemStack denyBtn = new ItemBuilder(Material.RED_WOOL)
                .name(MessageUtil.parse("<red>✖ Отклонить"))
                .addLore(MessageUtil.parse("<gray>" + fromName))
                .hideFlags(true)
                .build();

        builder.button(ACCEPT_SLOT, Button.builder(acceptBtn).onClick(e -> {
            if (!(e.getWhoClicked() instanceof Player player)) return;
            var removed = service.removeFirstRequest(player.getUniqueId());
            if (removed.isEmpty()) {
                player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("no-request", "<#FF3366>У вас нет входящих запросов.</#FF3366>")));
                player.closeInventory();
                return;
            }
            TpaRequest r = removed.get();
            Player from = ru.managerfix.utils.NickResolver.getPlayerByUuid(r.getFrom());
            if (from == null || !from.isOnline()) {
                player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("request-expired", "<#FF3366>Запрос истёк.</#FF3366>")));
                player.closeInventory();
                return;
            }
            player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("accepted", "<#00C8FF>Запрос принят!</#00C8FF>")));
            from.sendMessage(MessageUtil.parse(service.getConfig().getMessage("accepted", "<#00C8FF>Запрос принят!</#00C8FF>")));
            if (r.isTpaHere()) {
                service.scheduleTeleport(player, from.getLocation().clone(), () ->
                        player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("cancelled-move", "<#FF3366>Телепортация отменена: вы сдвинулись.</#FF3366>"))));
            } else {
                service.scheduleTeleport(from, player.getLocation().clone(), () ->
                        from.sendMessage(MessageUtil.parse(service.getConfig().getMessage("cancelled-move", "<#FF3366>Телепортация отменена: вы сдвинулись.</#FF3366>"))));
            }
            player.closeInventory();
        }).build());

        builder.button(DENY_SLOT, Button.builder(denyBtn).onClick(e -> {
            if (!(e.getWhoClicked() instanceof Player player)) return;
            var removed = service.removeFirstRequest(player.getUniqueId());
            if (removed.isEmpty()) {
                player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("no-request", "<#FF3366>У вас нет входящих запросов.</#FF3366>")));
                player.closeInventory();
                return;
            }
            player.sendMessage(MessageUtil.parse(service.getConfig().getMessage("denied", "<#FF3366>Запрос отклонён.</#FF3366>")));
            Player from = ru.managerfix.utils.NickResolver.getPlayerByUuid(removed.get().getFrom());
            if (from != null && from.isOnline()) {
                from.sendMessage(MessageUtil.parse(service.getConfig().getMessage("denied", "<#FF3366>Запрос отклонён.</#FF3366>")));
            }
            player.closeInventory();
        }).build());

        Inventory inv = builder.build();
        guiManager.open(target, inv);
    }
}
