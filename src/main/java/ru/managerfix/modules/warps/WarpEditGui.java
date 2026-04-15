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
                .title("<gradient:#7000FF:#00C8FF>Редактирование: " + warp.getName() + "</gradient>")
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
                        .name("<#FF3366>Иконка: " + warp.getIcon().name())
                        .loreStrings("<#F0F4F8>Текущая иконка", "<#F0F4F8>Изменить можно через конфиг")
                        .build())
                .build());

        // Delete button
        builder.button(16, Button.builder(new ItemBuilder(Material.BARRIER)
                        .name("<#FF3366>УДАЛИТЬ ВАРП</#FF3366>")
                        .loreStrings("<#F0F4F8>Это действие необратимо!")
                        .build())
                .onClick(e -> {
                    dataStorage.deleteWarp(warp.getName());
                    player.closeInventory();
                    player.sendMessage(MessageUtil.parse("<#00C8FF>Варп <#F0F4F8>" + warp.getName() + "</#F0F4F8> удалён."));
                })
                .build());

        builder.open(player);
    }

    private Button createToggleButton(String name, boolean enabled, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> onClick) {
        return Button.builder(new ItemBuilder(enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                        .name((enabled ? "<#00C8FF>" : "<#FF3366>") + name)
                        .loreStrings("",
                                "<#F0F4F8>Текущее состояние: " + (enabled ? "<#00C8FF>ВКЛ" : "<#FF3366>ВЫКЛ"),
                                "",
                                "<#00C8FF>Нажмите, чтобы переключить")
                        .build())
                .onClick(onClick)
                .build();
    }
}
