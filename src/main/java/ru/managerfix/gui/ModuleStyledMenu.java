package ru.managerfix.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.managerfix.ManagerFix;
import ru.managerfix.core.Module;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.Optional;

/**
 * Styled module settings GUI: sections [Общие настройки], [Параметры], [Администрирование], [Статистика],
 * themed title and buttons, 200ms throttle in GuiManager, ActionBar confirmation.
 */
public final class ModuleStyledMenu {

    private static final int SIZE = 45;
    private static final int SLOT_TOGGLE = 10;
    private static final int SLOT_RELOAD = 12;
    private static final int SLOT_STATS = 14;
    private static final int SLOT_CLEAR = 16;
    private static final int SLOT_BACK = 31;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public ModuleStyledMenu(ManagerFix plugin, GuiManager guiManager,
                            UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
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

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleModuleSettings(displayName))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("module_settings_" + moduleId);

        // [ Общие настройки ] — Toggle
        builder.button(SLOT_TOGGLE, Button.builder(
                new ItemBuilder(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
                        .name(MessageUtil.parse(enabled
                                ? UIThemeManager.GRADIENT_SUCCESS + "✔ Включить: ДА</gradient>"
                                : UIThemeManager.GRADIENT_ERROR + "✖ Включить: НЕТ</gradient>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — переключить модуль</#e9d5ff>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> {
            plugin.getConfigManager().setModuleEnabled(moduleId, !enabled);
            plugin.getModuleManager().reloadModule(moduleId);
            guiManager.sendConfirmation(player, enabled ? "Модуль отключён" : "Модуль включён");
            open(player, moduleId);
        }).build());

        // [ Параметры ] — Reload
        builder.button(SLOT_RELOAD, Button.builder(
                new ItemBuilder(Material.REDSTONE)
                        .name(MessageUtil.parse(UIThemeManager.GRADIENT_WARNING + "Перезагрузить модуль</gradient>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — hot reload</#e9d5ff>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> {
            plugin.getModuleManager().reloadModule(moduleId);
            guiManager.sendConfirmation(player, "Модуль перезагружен");
            open(player, moduleId);
        }).build());

        // [ Статистика ]
        ItemBuilder statsIb = new ItemBuilder(Material.PAPER)
                .name(MessageUtil.parse(UIThemeManager.GRADIENT_MAIN + "Статистика</gradient>"))
                .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + (stats != null && !stats.equals("-") ? stats : "—") + "</#e9d5ff>"));
        for (String line : theme.blockStatus(enabled).split("\n")) {
            statsIb.addLore(MessageUtil.parse(line));
        }
        reasonOpt.ifPresent(r -> statsIb.addLore(MessageUtil.parse(UIThemeManager.COLOR_ERROR + UIThemeManager.SYM_WARN + " " + r + "</#f87171>")));
        statsIb.hideFlags(true);
        builder.button(SLOT_STATS, Button.builder(statsIb.build()).build());

        // [ Администрирование ] — Clear data
        builder.button(SLOT_CLEAR, Button.builder(
                new ItemBuilder(Material.TNT)
                        .name(MessageUtil.parse(UIThemeManager.GRADIENT_ERROR + "Очистить данные</gradient>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — очистить кэш/данные модуля</#e9d5ff>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_WARNING + "Только для опытных!</#fbbf24>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> {
            try {
                module.clearData();
                guiManager.sendConfirmation(player, "Данные очищены");
            } catch (Exception ex) {
                MessageUtil.send(plugin, player, "module-disabled");
            }
            open(player, moduleId);
        }).build());

        builder.button(SLOT_BACK, Button.builder(
                new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Назад в панель</#e9d5ff>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> new ManagerFixMainMenu(plugin, guiManager, theme, template).open(player)).build());

        guiManager.open(player, builder.build());
    }
}
