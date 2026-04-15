package ru.managerfix.modules.warps;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.managerfix.gui.Button;
import ru.managerfix.gui.GuiBuilder;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WarpGui {

    private final WarpConfig config;
    private final WarpService service;
    private final WarpsDataStorage dataStorage;
    private final WarpEditGui editGui;
    private final GuiTemplate template;
    private static final int ITEMS_PER_PAGE = 28; // 4 rows of 7 items (excluding frame)

    public WarpGui(WarpConfig config, WarpService service, WarpEditGui editGui, WarpsDataStorage dataStorage) {
        this(config, service, editGui, dataStorage, new GuiTemplate(new ru.managerfix.gui.theme.UIThemeManager()));
    }

    public WarpGui(WarpConfig config, WarpService service, WarpEditGui editGui, WarpsDataStorage dataStorage, GuiTemplate template) {
        this.config = config;
        this.service = service;
        this.dataStorage = dataStorage;
        this.editGui = editGui;
        this.template = template != null ? template : new GuiTemplate(new ru.managerfix.gui.theme.UIThemeManager());
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<Warp> visibleWarps = dataStorage.getWarps().values().stream()
                .filter(w -> !w.isHidden())
                .sorted(Comparator.comparing(Warp::getName))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) visibleWarps.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        final int finalPage = page;
        GuiBuilder builder = GuiBuilder.of(54)
                .title("<gradient:#7000FF:#00C8FF>Варпы сервера (" + (page + 1) + "/" + totalPages + ")</gradient>")
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, 54))
                .holderId("warps_list");

        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, visibleWarps.size());

        int slot = 10; // First content slot inside frame
        for (int i = startIdx; i < endIdx; i++) {
            // Skip frame slots
            while (slot % 9 == 0 || slot % 9 == 8 || slot < 10 || slot > 43) {
                slot++;
            }
            
            Warp warp = visibleWarps.get(i);
            addWarpButton(builder, player, warp, slot);
            slot++;
        }

        // Navigation buttons
        if (page > 0) {
            builder.button(45, Button.builder(new ItemBuilder(Material.ARROW)
                    .name("<#00C8FF>Предыдущая страница")
                    .build())
                    .onClick(e -> open(player, finalPage - 1))
                    .build());
        }

        if (page < totalPages - 1) {
            builder.button(53, Button.builder(new ItemBuilder(Material.ARROW)
                    .name("<#00C8FF>Следующая страница")
                    .build())
                    .onClick(e -> open(player, finalPage + 1))
                    .build());
        }

        builder.open(player);
    }

    private void addWarpButton(GuiBuilder builder, Player player, Warp warp, int slot) {
        boolean hasPerm = player.hasPermission("managerfix.warp.use") ||
                         (warp.getPermission() != null && !warp.getPermission().isEmpty() && player.hasPermission(warp.getPermission()));

        ItemBuilder ib = hasPerm ? new ItemBuilder(warp.getIcon()) : new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE);
        ib.name((hasPerm ? "<#00C8FF>" : "<#FF3366>") + warp.getName());

        List<String> lore = new ArrayList<>(warp.getDescription());
        lore.add("");
        lore.add("<#F0F4F8>Мир: <#F0F4F8>" + (warp.getLocation().getWorld() != null ? warp.getLocation().getWorld().getName() : "???"));
        lore.add("<#F0F4F8>Задержка: <#F0F4F8>" + warp.getTeleportDelay() + " сек.");
        if (warp.getOwner() != null) {
            String ownerName = org.bukkit.Bukkit.getOfflinePlayer(warp.getOwner()).getName();
            lore.add("<#F0F4F8>Владелец: <#F0F4F8>" + (ownerName != null ? ownerName : "???"));
        }
        lore.add("");
        if (hasPerm) {
            lore.add("<#00C8FF>Нажмите, чтобы телепортироваться");
        } else {
            lore.add("<#FF3366>Нет доступа");
        }
        ib.loreStrings(lore.toArray(new String[0]));

        builder.button(slot, 
            Button.builder(ib.build())
                .onClick(e -> {
                    if (hasPerm) {
                        player.closeInventory();
                        service.teleportToWarp(player, warp.getName());
                    }
                })
                .build());
    }
}
