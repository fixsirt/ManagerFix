package ru.managerfix.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * GUI animations: cycling materials (loading icon), gradient titles.
 */
public final class AnimationUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private AnimationUtil() {
    }

    /**
     * Returns material at index % materials.length (for cycling).
     */
    public static Material cycleMaterial(Material[] materials, int index) {
        if (materials == null || materials.length == 0) return Material.GRAY_STAINED_GLASS_PANE;
        return materials[Math.floorMod(index, materials.length)];
    }

    /**
     * Common loading cycle: rotating through materials (огненная палитра).
     */
    public static final Material[] LOADING_MATERIALS = {
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE,
            Material.FIRE,
            Material.LAVA_BUCKET,
            Material.BLAZE_POWDER,
            Material.MAGMA_BLOCK,
            Material.ORANGE_TERRACOTTA
    };

    /**
     * Builds a "loading" item that cycles materials. Pass tick index from scheduler.
     */
    public static ItemStack loadingIcon(int tickIndex, String titleMiniMessage) {
        Material mat = cycleMaterial(LOADING_MATERIALS, tickIndex);
        ItemBuilder ib = new ItemBuilder(mat).hideFlags(true);
        if (titleMiniMessage != null && !titleMiniMessage.isEmpty()) {
            ib.name(titleMiniMessage);
        } else {
            ib.name(MessageUtil.parse("<#FAA300>Загрузка..."));
        }
        return ib.build();
    }

    /**
     * Simple gradient title: cycles through prefix strings. index = tick/interval.
     */
    public static Component animateTitle(String[] gradientTitles, int index) {
        if (gradientTitles == null || gradientTitles.length == 0) {
            return Component.empty();
        }
        String raw = gradientTitles[Math.floorMod(index, gradientTitles.length)];
        try {
            return MINI_MESSAGE.deserialize(raw);
        } catch (Exception e) {
            return LEGACY.deserialize(raw);
        }
    }

    /**
     * Default gradient titles for main menu (огненная палитра).
     */
    public static final String[] DEFAULT_TITLE_GRADIENT = {
            "<#1A120B>ManagerFix",
            "<#C0280F>ManagerFix",
            "<#FF4D00>ManagerFix",
            "<#FAA300>ManagerFix"
    };
}
