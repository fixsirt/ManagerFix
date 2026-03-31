package ru.managerfix.gui.theme;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import ru.managerfix.utils.ItemBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Decorative frame templates for GUI: alternating orange/red glass, corners, dividers.
 * Цветовая палитра: Grief Orange (#FF4D00), Solar Flare (#FAA300), Scorched Earth (#1A120B)
 * Size 54 (6 rows): top row 0-8, bottom row 45-53, left column 9,18,27,36, right column 17,26,35,44.
 */
public final class GuiTemplate {

    // Огненная палитра: оранжевое, красное, янтарное стекло
    private static final Material ORANGE = Material.ORANGE_STAINED_GLASS_PANE;
    private static final Material RED = Material.RED_STAINED_GLASS_PANE;
    private static final Material MAGENTA = Material.MAGENTA_STAINED_GLASS_PANE;
    private static final Material CORNER = Material.BASALT; // Тёмный блок для углов (Scorched Earth стиль)
    private static final Material DIVIDER = Material.GRAY_STAINED_GLASS_PANE;

    private final UIThemeManager theme;

    public GuiTemplate(UIThemeManager theme) {
        this.theme = theme != null ? theme : new UIThemeManager();
    }

    /**
     * Returns a map of slot -> material for a styled frame.
     * 54: full border, corners BASALT, alternating ORANGE/RED/MAGENTA.
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
        int rows = size / 9;
        // Top row (0-8): corners; rest alternating orange/red/magenta
        map.put(0, CORNER);
        map.put(8, CORNER);
        for (int i = 1; i <= 7; i++) {
            map.put(i, cycle(i, ORANGE, RED, MAGENTA));
        }
        // Bottom row (45-53)
        map.put(45, CORNER);
        map.put(53, CORNER);
        for (int i = 46; i <= 52; i++) {
            map.put(i, cycle(i, ORANGE, RED, MAGENTA));
        }
        // Left column (9, 18, 27, 36)
        for (int r = 1; r <= 4; r++) {
            map.put(r * 9, cycle(r, ORANGE, RED, MAGENTA));
        }
        // Right column (17, 26, 35, 44)
        for (int r = 1; r <= 4; r++) {
            map.put(r * 9 + 8, cycle(r, MAGENTA, RED, ORANGE));
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
        Material mat = ORANGE;
        for (int i = 0; i < 9; i++) map.put(i, mat);
        for (int i = size - 9; i < size; i++) map.put(i, mat);
        for (int r = 1; r < rows - 1; r++) {
            map.put(r * 9, mat);
            map.put(r * 9 + 8, mat);
        }
        return map;
    }

    /** 45 or 54: border with corners BASALT, rest alternating ORANGE/RED/MAGENTA. */
    private static Map<Integer, Material> getSimpleFrameWithCorners(int size) {
        Map<Integer, Material> map = new HashMap<>();
        int rows = size / 9;
        map.put(0, CORNER);
        map.put(8, CORNER);
        for (int i = 1; i <= 7; i++) map.put(i, cycle(i, ORANGE, RED, MAGENTA));
        int lastRow = size - 9;
        map.put(lastRow, CORNER);
        map.put(lastRow + 8, CORNER);
        for (int i = 1; i <= 7; i++) map.put(lastRow + i, cycle(i, ORANGE, RED, MAGENTA));
        for (int r = 1; r < rows - 1; r++) {
            map.put(r * 9, cycle(r, ORANGE, RED, MAGENTA));
            map.put(r * 9 + 8, cycle(r, MAGENTA, RED, ORANGE));
        }
        return map;
    }

    public UIThemeManager getTheme() {
        return theme;
    }
}
