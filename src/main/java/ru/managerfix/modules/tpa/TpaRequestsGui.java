package ru.managerfix.modules.tpa;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.Button;
import ru.managerfix.gui.GuiBuilder;
import ru.managerfix.gui.GuiManager;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.UUID;

/**
 * GUI для отображения всех входящих заявок TPA.
 */
public final class TpaRequestsGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int CONTENT_PER_PAGE = CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int CLOSE_SLOT = 49;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;
    private final TpaService service;
    private final TpaGui tpaGui;

    public TpaRequestsGui(ManagerFix plugin, GuiManager guiManager, TpaService service, TpaGui tpaGui) {
        this.plugin = plugin;
        this.guiManager = guiManager != null ? guiManager : plugin.getGuiManager();
        this.theme = plugin.getUIThemeManager() != null ? plugin.getUIThemeManager() : new UIThemeManager();
        this.template = plugin.getGuiTemplate() != null ? plugin.getGuiTemplate() : new GuiTemplate(theme);
        this.service = service;
        this.tpaGui = tpaGui;
    }

    public void open(Player player, int page) {
        UUID playerUuid = player.getUniqueId();
        List<TpaRequest> requests = service.getRequests(playerUuid);
        int totalPages = Math.max(1, (int) Math.ceil((double) requests.size() / CONTENT_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * CONTENT_PER_PAGE;
        int to = Math.min(from + CONTENT_PER_PAGE, requests.size());

        String title = "<gradient:#7000FF:#00C8FF>Заявки на телепортацию (" + requests.size() + ")</gradient>";

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(MessageUtil.parse(title))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("tpa_requests_" + safePage);

        if (requests.isEmpty()) {
            ItemStack empty = new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.parse("<#FF3366>Нет входящих заявок</#FF3366>"))
                    .addLore(MessageUtil.parse("<#F0F4F8>Здесь появятся заявки на телепортацию"))
                    .hideFlags(true)
                    .build();
            builder.button(22, Button.builder(empty).build());
        } else {
            for (int i = from; i < to; i++) {
                TpaRequest request = requests.get(i);
                int contentIndex = i - from;
                int slot = CONTENT_SLOTS[contentIndex];
                ItemStack icon = buildRequestIcon(request);
                Button btn = Button.builder(icon)
                        .onClick(e -> {
                            if (!(e.getWhoClicked() instanceof Player p)) return;
                            acceptRequest(p, request);
                        })
                        .onRightClick(e -> {
                            if (!(e.getWhoClicked() instanceof Player p)) return;
                            denyRequest(p, request);
                        })
                        .build();
                builder.button(slot, btn);
            }
        }

        if (totalPages > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse("<#00C8FF>◂ Предыдущая страница"))
                    .hideFlags(true)
                    .build();
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse("<#00C8FF>Следующая страница ▸"))
                    .hideFlags(true)
                    .build();
            builder.button(PREV_SLOT, Button.builder(prev).onClick(e -> {
                if (safePage > 0) open(player, safePage - 1);
            }).build());
            builder.button(NEXT_SLOT, Button.builder(next).onClick(e -> {
                if (safePage < totalPages - 1) open(player, safePage + 1);
            }).build());
        }

        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.parse("<#FF3366>Закрыть</#FF3366>"))
                .hideFlags(true)
                .build();
        builder.button(49, Button.builder(close).onClick(e -> {
            if (e.getWhoClicked() instanceof Player p) p.closeInventory();
        }).build());

        guiManager.open(player, builder.build());
    }

    private ItemStack buildRequestIcon(TpaRequest request) {
        String senderName = Bukkit.getOfflinePlayer(request.getFrom()).getName();
        if (senderName == null) senderName = "?";
        boolean tpaHere = request.isTpaHere();

        Material material = tpaHere ? Material.ENDER_EYE : Material.ENDER_PEARL;
        String typeText = tpaHere ? "TPA Here" : "TPA";
        String actionText = tpaHere ? "Игрок хочет телепортировать вас к себе" : "Вы телепортируетесь к игроку";

        long timeLeft = request.getExpiresAt() - System.currentTimeMillis();
        long seconds = Math.max(0, timeLeft / 1000);
        String timeText = seconds > 60 ? (seconds / 60 + " мин") : (seconds + " сек");

        return new ItemBuilder(material)
                .name(MessageUtil.parse("<gradient:#00C8FF:#7000FF>" + senderName + "</gradient>"))
                .addLore(MessageUtil.parse("<#F0F4F8>Тип: <#00C8FF>" + typeText))
                .addLore(MessageUtil.parse("<#F0F4F8>" + actionText))
                .addLore(MessageUtil.parse("<#F0F4F8>Истекает: <#00C8FF>" + timeText))
                .addLore(MessageUtil.parse("<#F0F4F8>"))
                .addLore(MessageUtil.parse("<#00C8FF>▸ ЛКМ — принять"))
                .addLore(MessageUtil.parse("<#FF3366>▸ ПКМ — отклонить"))
                .hideFlags(true)
                .build();
    }

    private void acceptRequest(Player player, TpaRequest request) {
        UUID senderUuid = request.getFrom();
        
        var removed = service.removeRequest(player.getUniqueId(), senderUuid);
        if (removed.isEmpty()) {
            player.sendMessage(MessageUtil.parse("<#FF3366>Заявка уже обработана.</#FF3366>"));
            open(player, 0);
            return;
        }
        
        Player sender = Bukkit.getPlayer(senderUuid);
        boolean tpaHere = request.isTpaHere();

        if (sender == null || !sender.isOnline()) {
            String senderName = Bukkit.getOfflinePlayer(senderUuid).getName();
            player.sendMessage(MessageUtil.parse("<#FF3366>Игрок " + (senderName != null ? senderName : "?") + " вышел с сервера.</#FF3366>"));
            open(player, 0);
            return;
        }

        if (tpaHere) {
            Location dest = sender.getLocation().clone();
            service.scheduleTeleport(player, dest, () -> {});
        } else {
            Location dest = player.getLocation().clone();
            service.scheduleTeleport(sender, dest, () -> {});
        }

        player.sendMessage(MessageUtil.parse("<#00C8FF>Заявка принята!</#00C8FF>"));
        sender.sendMessage(MessageUtil.parse("<#00C8FF>Ваша заявка на телепортацию принята!</#00C8FF>"));
        player.closeInventory();
    }

    private void denyRequest(Player player, TpaRequest request) {
        UUID senderUuid = request.getFrom();
        
        var removed = service.removeRequest(player.getUniqueId(), senderUuid);
        if (removed.isEmpty()) {
            player.sendMessage(MessageUtil.parse("<#FF3366>Заявка уже обработана.</#FF3366>"));
            open(player, 0);
            return;
        }
        
        Player sender = Bukkit.getPlayer(senderUuid);

        player.sendMessage(MessageUtil.parse("<#FF3366>Заявка отклонена.</#FF3366>"));
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(MessageUtil.parse("<#FF3366>Ваша заявка на телепортацию отклонена.</#FF3366>"));
        }
        open(player, 0);
    }

    public int getRequestCount(UUID playerUuid) {
        return service.getRequests(playerUuid).size();
    }
}
