package ru.managerfix.gui.theme;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import ru.managerfix.utils.ItemBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Decorative frame templates for GUI: light blue theme.
 * Цветовая палитра: Фон (#F0F4F8), Текст (#1E2A3A), Акцент 1 - Неоновый розовый (#FF3366)
 * Акцент 2 - Яркий циан (#00C8FF), Акцент 3 - Глубокий фиолетовый (#7000FF)
 * Size 54 (6 rows): top row 0-8, bottom row 45-53, left column 9,18,27,36, right column 17,26,35,44.
 */
public final class GuiTemplate {

    // Светло-голубая палитра
    private static final Material CORNER = Material.LIGHT_BLUE_DYE;           // 0,8,45,53 - углы
    private static final Material SIDE = Material.BLUE_STAINED_GLASS_PANE;    // 9,18,27,36,17,26,35,44 - боковые
    private static final Material TOP_BOTTOM = Material.LIGHT_BLUE_STAINED_GLASS_PANE; // 1-7,46-52 - верх/низ
    private static final Material DIVIDER = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

    private final UIThemeManager theme;

    public GuiTemplate(UIThemeManager theme) {
        this.theme = theme != null ? theme : new UIThemeManager();
    }

    /**
     * Returns a map of slot -> material for a styled frame.
     * 54: full border, corners LIGHT_BLUE_DYE, sides BLUE_STAINED_GLASS_PANE, top/bottom LIGHT_BLUE_STAINED_GLASS_PANE.
     * 45: same border + row 2 (slots 18-26) as divider.
     */
    public Map<Integer, Material> getStyledFrameMaterials(int size) {
        if (size == 27) {
            return getSimpleFrameWithCorners(27);
        }
        if (size == 45) {
            Map<Integer, Material> map = getSimpleFrameWithCorners(45);
            for (int i = 18; i <= 26; i++) map.put(i, DIVIDER);
            return map;
        }
        if (size != 54) {
            return getSimpleFrame(size);
        }
        Map<Integer, Material> map = new HashMap<>();
        // Top row (0-8): corners + top/bottom glass
        map.put(0, CORNER);
        map.put(8, CORNER);
        for (int i = 1; i <= 7; i++) {
            map.put(i, TOP_BOTTOM);
        }
        // Bottom row (45-53): corners + top/bottom glass
        map.put(45, CORNER);
        map.put(53, CORNER);
        for (int i = 46; i <= 52; i++) {
            map.put(i, TOP_BOTTOM);
        }
        // Left column (9, 18, 27, 36) - side glass
        for (int r = 1; r <= 4; r++) {
            map.put(r * 9, SIDE);
        }
        // Right column (17, 26, 35, 44) - side glass
        for (int r = 1; r <= 4; r++) {
            map.put(r * 9 + 8, SIDE);
        }
        return map;
    }

    /**
     * Fills the inventory border with styled frame. Content slots (buttons) must be set after.
     */
    public void applyStyledFrame(Inventory inv, int size) {
        Map<Integer, Material> frame = getStyledFrameMaterials(size);
        for (Map.Entry<Integer, Material> e : frame.entrySet()) {
            if (e.getKey() >= 0 && e.getKey() < size) {
                inv.setItem(e.getKey(), ItemBuilder.decoration(e.getValue()));
            }
        }
    }

    /**
     * Content slots for 54-slot panel (modules area): 10-16, 19-25, 28-34, 37-43.
     */
    public static int[] getContentSlots54() {
        return new int[]{
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
    }

    /**
     * Bottom bar slots for 54-slot: 45, 47, 49, 51 (Reload, Debug, Storage, Cluster).
     */
    public static int[] getBottomBarSlots54() {
        return new int[]{45, 47, 49, 51};
    }

    private static Material cycle(int index, Material a, Material b, Material c) {
        switch (Math.floorMod(index, 3)) {
            case 0: return a;
            case 1: return b;
            default: return c;
        }
    }

    private static Map<Integer, Material> getSimpleFrame(int size) {
        Map<Integer, Material> map = new HashMap<>();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) map.put(i, TOP_BOTTOM);
        for (int i = size - 9; i < size; i++) map.put(i, TOP_BOTTOM);
        for (int r = 1; r < rows - 1; r++) {
            map.put(r * 9, SIDE);
            map.put(r * 9 + 8, SIDE);
        }
        return map;
    }

    /** 45 or 54: border with corners LIGHT_BLUE_DYE, rest TOP_BOTTOM/SIDE. */
    private static Map<Integer, Material> getSimpleFrameWithCorners(int size) {
        Map<Integer, Material> map = new HashMap<>();
        int rows = size / 9;
        map.put(0, CORNER);
        map.put(8, CORNER);
        for (int i = 1; i <= 7; i++) map.put(i, TOP_BOTTOM);
        int lastRow = size - 9;
        map.put(lastRow, CORNER);
        map.put(lastRow + 8, CORNER);
        for (int i = 1; i <= 7; i++) map.put(lastRow + i, TOP_BOTTOM);
        for (int r = 1; r < rows - 1; r++) {
            map.put(r * 9, SIDE);
            map.put(r * 9 + 8, SIDE);
        }
        return map;
    }

    public UIThemeManager getTheme() {
        return theme;
    }
}
