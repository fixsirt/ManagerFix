package ru.managerfix.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.Module;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Universal module settings GUI: Toggle, Reload, Stats, Clear data, Config path.
 * Permission: managerfix.admin or managerfix.module.<name>.admin.
 */
public final class ModuleSettingsGui {

    private static final long CLICK_THROTTLE_MS = 300;
    private static final int SIZE = 45;
    private static final int SLOT_TOGGLE = 10;
    private static final int SLOT_RELOAD = 12;
    private static final int SLOT_STATS = 14;
    private static final int SLOT_CLEAR = 16;
    private static final int SLOT_BACK = 31;
    private static final ConcurrentHashMap<java.util.UUID, AtomicLong> lastClick = new ConcurrentHashMap<>();

    private final ManagerFix plugin;
    private final GuiManager guiManager;

    public ModuleSettingsGui(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public void open(Player player, String moduleId) {
        Optional<Module> moduleOpt = plugin.getModuleManager().getModule(moduleId);
        if (moduleOpt.isEmpty()) {
            MessageUtil.send(plugin, player, "module-disabled");
            return;
        }
        Module module = moduleOpt.get();
        boolean enabled = module.isEnabled();
        String stats = plugin.getModuleManager().getModuleStats(moduleId);
        Optional<String> reasonOpt = plugin.getModuleManager().getDependencyFailureReason(moduleId);

        String nameKey = "menu.modules." + moduleId;
        String nameRaw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), nameKey);
        String displayName = nameRaw != null ? nameRaw : moduleId;

        String titleRaw = UIThemeManager.GRADIENT_MAIN + "Настройки: " + displayName + "</gradient>";
        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(MessageUtil.parse(titleRaw))
                .frame(true)
                .frameMaterial(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
                .holderId("module_settings_" + moduleId);

        builder.button(SLOT_TOGGLE, Button.builder(
                new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                        .name(MessageUtil.parse(enabled ? "<#FAA300>Включить: ДА</#FAA300>" : "<#FF4D00>Включить: НЕТ</#FF4D00>"))
                        .addLore(MessageUtil.parse("<#E0E0E0>ЛКМ — переключить модуль"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> throttle(player, () -> {
            plugin.getConfigManager().setModuleEnabled(moduleId, !enabled);
            plugin.getModuleManager().reloadModule(moduleId);
            MessageUtil.send(plugin, player, enabled ? "module-disabled" : "module-enabled");
            open(player, moduleId);
        })).build());

        builder.button(SLOT_RELOAD, Button.builder(
                new ItemBuilder(Material.REDSTONE)
                        .name(MessageUtil.parse("<#FAA300>Перезагрузить модуль</#FAA300>"))
                        .addLore(MessageUtil.parse("<#E0E0E0>ЛКМ — hot reload"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> throttle(player, () -> {
            plugin.getModuleManager().reloadModule(moduleId);
            MessageUtil.send(plugin, player, "reload");
            open(player, moduleId);
        })).build());

        ItemBuilder statsIb = new ItemBuilder(Material.PAPER)
                .name(MessageUtil.parse("<#FFFFFF>Статистика</#FFFFFF>"))
                .addLore(MessageUtil.parse("<#E0E0E0>" + (stats != null && !stats.equals("-") ? stats : "—")))
                .addLore(MessageUtil.parse("<#E0E0E0>Статус: " + (enabled ? "Enabled" : "Disabled")))
                .hideFlags(true);
        reasonOpt.ifPresent(r -> statsIb.addLore(MessageUtil.parse("<#FF4D00>" + r)));
        builder.button(SLOT_STATS, Button.builder(statsIb.build()).build());

        builder.button(SLOT_CLEAR, Button.builder(
                new ItemBuilder(Material.TNT)
                        .name(MessageUtil.parse("<#FF4D00>Очистить данные</#FF4D00>"))
                        .addLore(MessageUtil.parse("<#E0E0E0>ЛКМ — очистить кэш/данные модуля"))
                        .addLore(MessageUtil.parse("<#FF4D00>Только для опытных!</#FF4D00>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> throttle(player, () -> {
            try {
                module.clearData();
                MessageUtil.send(plugin, player, "reload");
            } catch (Exception ex) {
                MessageUtil.send(plugin, player, "module-disabled");
            }
            open(player, moduleId);
        })).build());

        builder.button(SLOT_BACK, Button.builder(
                new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.parse("<#E0E0E0>← Назад в панель</#E0E0E0>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> throttle(player, () -> new ManagerFixPanelGui(plugin, guiManager).open(player))).build());

        guiManager.open(player, builder.build());
    }

    private void throttle(Player player, Runnable action) {
        long now = System.currentTimeMillis();
        AtomicLong last = lastClick.computeIfAbsent(player.getUniqueId(), u -> new AtomicLong(0));
        if (now - last.get() < CLICK_THROTTLE_MS) return;
        last.set(now);
        action.run();
    }
}
