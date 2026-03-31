package ru.managerfix.gui.theme;

import net.kyori.adventure.text.Component;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized UI theme: MiniMessage gradients and colors, Unicode symbols.
 * Цветовая палитра: Grief Orange (#FF4D00), Solar Flare (#FAA300), Scorched Earth (#1A120B), Rust & Blood (#C0280F)
 * Акценты: Flame Core (#FF6A22), Hot Ember (#F5D742), Dried Blood (#7A2E1A)
 * Премиум: Ancient Bronze (#E38B30), Pure Light (#FFFFFF/#E0E0E0)
 * One instance per plugin (no static singletons).
 */
public final class UIThemeManager {

    // ─── Градиенты (огненная палитра) ───
    public static final String GRADIENT_MAIN = "<gradient:#FF4D00:#FAA300>";
    public static final String GRADIENT_ACCENT = "<gradient:#FF6A22:#F5D742>";
    public static final String GRADIENT_ERROR = "<gradient:#C0280F:#FF4D00>";
    public static final String GRADIENT_WARNING = "<gradient:#FAA300:#F5D742>";
    public static final String GRADIENT_SUCCESS = "<gradient:#F5D742:#E38B30>";

    // ─── Цвета (огненная палитра для меню) ───
    public static final String COLOR_ACCENT = "<#FF6A22>";
    public static final String COLOR_ERROR = "<#C0280F>";
    public static final String COLOR_WARNING = "<#FAA300>";
    public static final String COLOR_INFO = "<#E0E0E0>";
    public static final String COLOR_MAIN = "<#FF4D00>";
    public static final String COLOR_DARK = "<#1A120B>";

    // ─── Unicode символы ───
    public static final String SYM_BULLET = "●";
    public static final String SYM_ARROW = "▸";
    public static final String SYM_STAR = "✦";
    public static final String SYM_GEAR = "⚙";
    public static final String SYM_OK = "✔";
    public static final String SYM_CROSS = "✖";
    public static final String SYM_ARROW_R = "➜";
    public static final String SYM_HOURGLASS = "⌛";
    public static final String SYM_ZAP = "⚡";
    public static final String SYM_MENU = "☰";
    public static final String SYM_WARN = "⚠";

    public UIThemeManager() {
    }

    public String titleMainPanel() {
        return GRADIENT_MAIN + "✦ ManagerFix Control Panel ✦</gradient>";
    }

    public String titleModuleSettings(String moduleName) {
        return GRADIENT_ACCENT + "⚙ Настройки: " + moduleName + "</gradient>";
    }

    /** Заголовок любого меню в едином стиле (градиент + символы). */
    public String titleMenu(String displayText) {
        return GRADIENT_MAIN + "✦ " + displayText + " ✦</gradient>";
    }

    public String statusActive() {
        return COLOR_ACCENT + SYM_ARROW_R + " " + SYM_OK + " Активен</#FF6A22>";
    }

    public String statusDisabled() {
        return COLOR_ERROR + SYM_ARROW_R + " " + SYM_CROSS + " Отключён</#C0280F>";
    }

    public String blockStatus(boolean active) {
        return SYM_STAR + " Статус\n" + (active ? statusActive() : statusDisabled());
    }

    public String blockStats(String statsLine) {
        if (statsLine == null || statsLine.isEmpty() || "-".equals(statsLine)) {
            return SYM_STAR + " Статистика\n" + COLOR_INFO + SYM_ARROW_R + " —</#E0E0E0>";
        }
        return SYM_STAR + " Статистика\n" + COLOR_INFO + SYM_ARROW_R + " " + statsLine + "</#E0E0E0>";
    }

    public String blockControl() {
        return SYM_STAR + " Управление\n"
                + COLOR_INFO + "▸ ЛКМ — Вкл/Выкл\n▸ ПКМ — Настройки\n▸ Shift+ЛКМ — Reload</#E0E0E0>";
    }

    public String dependencyMissing(String moduleNames) {
        return COLOR_ERROR + SYM_WARN + " Отсутствуют зависимости:\n" + COLOR_WARNING + "- " + moduleNames + "</#FAA300>";
    }

    public String loreSpacer() {
        return " ";
    }

    /** Builds lore components from theme blocks (each block can contain \\n). Adds spacer between blocks. */
    public List<Component> loreFromBlocks(String... blocks) {
        List<Component> out = new ArrayList<>();
        for (int i = 0; i < blocks.length; i++) {
            if (i > 0) out.add(MessageUtil.parse(loreSpacer()));
            String block = blocks[i];
            if (block == null) continue;
            for (String line : block.split("\n")) {
                if (!line.isEmpty()) out.add(MessageUtil.parse(line));
            }
        }
        return out;
    }
}
