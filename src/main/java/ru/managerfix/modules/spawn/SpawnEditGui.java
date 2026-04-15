package ru.managerfix.modules.spawn;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.managerfix.gui.Button;
import ru.managerfix.gui.GuiBuilder;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

public class SpawnEditGui {

    private final SpawnConfig config;

    public SpawnEditGui(SpawnConfig config) {
        this.config = config;
    }

    public void open(Player player) {
        GuiBuilder builder = GuiBuilder.of(27)
                .title(MessageUtil.parse("<gradient:#7000FF:#00C8FF>Настройки Spawn</gradient>"))
                .frame(true)
                .holderId("spawn_edit");

        // [Spawn on Join: ON/OFF] - Slot 10
        builder.button(10, createToggleButton("Spawn on Join", config.isSpawnOnJoin(), "spawn-on-join"));
        // [Spawn on Death: ON/OFF] - Slot 11
        builder.button(11, createToggleButton("Spawn on Death", config.isSpawnOnDeath(), "spawn-on-death"));
        // [First Join Only: ON/OFF] - Slot 12
        builder.button(12, createToggleButton("First Join Only", config.isSpawnFirstJoinOnly(), "spawn-first-join-only"));
        // [Cancel on Move: ON/OFF] - Slot 13
        builder.button(13, createToggleButton("Cancel on Move", config.isCancelOnMove(), "cancel-on-move"));
        // [Cancel on Damage: ON/OFF] - Slot 14
        builder.button(14, createToggleButton("Cancel on Damage", config.isCancelOnDamage(), "cancel-on-damage"));
        // [Safe Teleport: ON/OFF] - Slot 15
        builder.button(15, createToggleButton("Safe Teleport", config.isSafeTeleport(), "safe-teleport"));

        // [Teleport Delay: + / -] - Slot 16
        builder.button(16, Button.builder(new ItemBuilder(Material.CLOCK)
                        .name("<#00C8FF>Задержка телепортации")
                        .loreStrings("",
                                "<#F0F4F8>Текущее значение: <#F0F4F8>" + config.getTeleportDelaySeconds() + " сек.",
                                "",
                                "<#00C8FF>ЛКМ: +1 сек.",
                                "<#00C8FF>ПКМ: -1 сек.")
                        .build())
                .onClick(e -> {
                    int delay = config.getTeleportDelaySeconds();
                    if (e.isLeftClick()) delay++;
                    else if (e.isRightClick() && delay > 0) delay--;
                    config.setSetting("teleport-delay-seconds", delay);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    open(player);
                })
                .build());

        builder.open(player);
    }

    private Button createToggleButton(String name, boolean enabled, String configPath) {
        return Button.builder(new ItemBuilder(enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                        .name((enabled ? "<#00C8FF>" : "<#FF3366>") + name)
                        .loreStrings("",
                                "<#F0F4F8>Текущее состояние: " + (enabled ? "<#00C8FF>ВКЛ" : "<#FF3366>ВЫКЛ"),
                                "",
                                "<#00C8FF>Нажмите, чтобы переключить")
                        .build())
                .onClick(e -> {
                    config.setSetting(configPath, !enabled);
                    Player clicker = (Player) e.getWhoClicked();
                    clicker.playSound(clicker.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    open(clicker);
                })
                .build();
    }
}
