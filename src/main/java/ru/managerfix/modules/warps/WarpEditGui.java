package ru.managerfix.modules.warps;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.managerfix.gui.Button;
import ru.managerfix.gui.GuiBuilder;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

public class WarpEditGui {

    private final WarpsDataStorage dataStorage;

    public WarpEditGui(WarpsDataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    public void open(Player player, Warp warp) {
        GuiBuilder builder = GuiBuilder.of(27)
                .title("<gradient:#e67e22:#d35400>Редактирование: " + warp.getName() + "</gradient>")
                .frame(true)
                .holderId("warp_edit");

        // Toggle Enabled
        builder.button(10, createToggleButton("Состояние", warp.isEnabled(), e -> {
            warp.setEnabled(!warp.isEnabled());
            dataStorage.saveWarp(warp);
            open(player, warp);
        }));

        // Toggle Hidden
        builder.button(11, createToggleButton("Скрытый", warp.isHidden(), e -> {
            warp.setHidden(!warp.isHidden());
            dataStorage.saveWarp(warp);
            open(player, warp);
        }));

        // Icon (just info for now)
        builder.button(13, Button.builder(new ItemBuilder(warp.getIcon())
                        .name("<#FF4D00>Иконка: " + warp.getIcon().name())
                        .loreStrings("<#E0E0E0>Текущая иконка", "<#E0E0E0>Изменить можно через конфиг")
                        .build())
                .build());

        // Delete button
        builder.button(16, Button.builder(new ItemBuilder(Material.BARRIER)
                        .name("<#FF4D00>УДАЛИТЬ ВАРП</#FF4D00>")
                        .loreStrings("<#E0E0E0>Это действие необратимо!")
                        .build())
                .onClick(e -> {
                    dataStorage.deleteWarp(warp.getName());
                    player.closeInventory();
                    player.sendMessage(MessageUtil.parse("<#FAA300>Варп <#FFFFFF>" + warp.getName() + "</#FFFFFF> удалён."));
                })
                .build());

        builder.open(player);
    }

    private Button createToggleButton(String name, boolean enabled, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> onClick) {
        return Button.builder(new ItemBuilder(enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                        .name((enabled ? "<#FAA300>" : "<#FF4D00>") + name)
                        .loreStrings("",
                                "<#E0E0E0>Текущее состояние: " + (enabled ? "<#FAA300>ВКЛ" : "<#FF4D00>ВЫКЛ"),
                                "",
                                "<#FAA300>Нажмите, чтобы переключить")
                        .build())
                .onClick(onClick)
                .build();
    }
}
