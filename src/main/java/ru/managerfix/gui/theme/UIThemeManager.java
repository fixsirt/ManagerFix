package ru.managerfix.gui.theme;

import net.kyori.adventure.text.Component;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized UI theme: MiniMessage gradients and colors, Unicode symbols.
 * Цветовая палитра: Фон (#F0F4F8), Текст (#1E2A3A), Акцент 1 - Неоновый розовый (#FF3366)
 * Акцент 2 - Яркий циан (#00C8FF), Акцент 3 - Глубокий фиолетовый (#7000FF)
 * One instance per plugin (no static singletons).
 */
public final class UIThemeManager {

    // ─── Градиенты (современная палитра) ───
    public static final String GRADIENT_MAIN = "<gradient:#7000FF:#00C8FF>";
    public static final String GRADIENT_ACCENT = "<gradient:#FF3366:#7000FF>";
    public static final String GRADIENT_ERROR = "<gradient:#FF3366:#7000FF>";
    public static final String GRADIENT_WARNING = "<gradient:#00C8FF:#7000FF>";
    public static final String GRADIENT_SUCCESS = "<gradient:#00C8FF:#FF3366>";

    // ─── Цвета (современная палитра для меню) ───
    public static final String COLOR_ACCENT = "<#00C8FF>";
    public static final String COLOR_ERROR = "<#FF3366>";
    public static final String COLOR_WARNING = "<#00C8FF>";
    public static final String COLOR_INFO = "<#F0F4F8>";
    public static final String COLOR_MAIN = "<#FF3366>";
    public static final String COLOR_DARK = "<#F0F4F8>";

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
        return COLOR_ACCENT + SYM_ARROW_R + " " + SYM_OK + " Активен</#00C8FF>";
    }

    public String statusDisabled() {
        return COLOR_ERROR + SYM_ARROW_R + " " + SYM_CROSS + " Отключён</#FF3366>";
    }

    public String blockStatus(boolean active) {
        return SYM_STAR + " Статус\n" + (active ? statusActive() : statusDisabled());
    }

    public String blockStats(String statsLine) {
        if (statsLine == null || statsLine.isEmpty() || "-".equals(statsLine)) {
            return SYM_STAR + " Статистика\n" + COLOR_INFO + SYM_ARROW_R + " —</#F0F4F8>";
        }
        return SYM_STAR + " Статистика\n" + COLOR_INFO + SYM_ARROW_R + " " + statsLine + "</#F0F4F8>";
    }

    public String blockControl() {
        return SYM_STAR + " Управление\n"
                + COLOR_INFO + "▸ ЛКМ — Вкл/Выкл\n▸ ПКМ — Настройки\n▸ Shift+ЛКМ — Reload</#F0F4F8>";
    }

    public String dependencyMissing(String moduleNames) {
        return COLOR_ERROR + SYM_WARN + " Отсутствуют зависимости:\n" + COLOR_WARNING + "- " + moduleNames + "</#00C8FF>";
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
