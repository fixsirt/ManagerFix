package ru.managerfix.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.utils.ItemBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Universal inventory builder: size, title (Adventure Component), frame, buttons.
 * Uses GuiHolder to store button handlers for GuiManager.
 */
public final class GuiBuilder {

    private final int size;
    private Component title;
    private boolean frame;
    private Material frameMaterial = Material.GRAY_STAINED_GLASS_PANE;
    /** When set, applied instead of single frameMaterial (styled frame). */
    private Map<Integer, Material> frameSlotMaterials;
    private Consumer<Inventory> frameApplicator;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private String holderId = "default";
    private Runnable onClose;
    private Runnable onUpdate;

    public GuiBuilder(int size) {
        if (size <= 0 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Size must be 9, 18, 27, 36, 45, or 54");
        }
        this.size = size;
    }

    public static GuiBuilder of(int size) {
        return new GuiBuilder(size);
    }

    public GuiBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public GuiBuilder title(String miniMessage) {
        this.title = ru.managerfix.utils.MessageUtil.parse(miniMessage);
        return this;
    }

    public GuiBuilder frame(boolean enabled) {
        this.frame = enabled;
        return this;
    }

    public GuiBuilder frameMaterial(Material material) {
        this.frameMaterial = material;
        return this;
    }

    /** Use custom slot->material map for frame (e.g. from GuiTemplate). */
    public GuiBuilder frameSlots(Map<Integer, Material> slotMaterials) {
        this.frameSlotMaterials = slotMaterials != null ? new HashMap<>(slotMaterials) : null;
        return this;
    }

    /** Use custom applicator to fill frame (e.g. GuiTemplate.applyStyledFrame). */
    public GuiBuilder frameApplicator(Consumer<Inventory> applicator) {
        this.frameApplicator = applicator;
        return this;
    }

    public GuiBuilder holderId(String id) {
        this.holderId = id;
        return this;
    }

    public GuiBuilder onClose(Runnable runnable) {
        this.onClose = runnable;
        return this;
    }

    public GuiBuilder onUpdate(Runnable runnable) {
        this.onUpdate = runnable;
        return this;
    }

    public GuiBuilder button(int slot, Button button) {
        if (slot >= 0 && slot < size) {
            buttons.put(slot, button);
        }
        return this;
    }

    public GuiBuilder button(int slot, ItemStack icon, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> onClick) {
        return button(slot, Button.builder(icon).onClick(onClick).build());
    }

    /**
     * Fills border slots with frame item (no display name).
     */
    private void applyFrame(Inventory inv, Material frameMat) {
        int rows = size / 9;
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, ItemBuilder.decoration(frameMat));
        }
        if (rows > 1) {
            for (int i = size - 9; i < size; i++) {
                inv.setItem(i, ItemBuilder.decoration(frameMat));
            }
        }
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * 9, ItemBuilder.decoration(frameMat));
            inv.setItem(r * 9 + 8, ItemBuilder.decoration(frameMat));
        }
    }

    public Inventory build() {
        GuiHolder holder = new GuiHolder(holderId, new HashMap<>(buttons), onClose, onUpdate);
        Inventory inv = Bukkit.createInventory(holder, size, title != null ? title : Component.empty());
        holder.setInventory(inv);
        if (frame) {
            if (frameApplicator != null) {
                frameApplicator.accept(inv);
            } else if (frameSlotMaterials != null && !frameSlotMaterials.isEmpty()) {
                for (Map.Entry<Integer, Material> e : frameSlotMaterials.entrySet()) {
                    if (e.getKey() >= 0 && e.getKey() < size) {
                        inv.setItem(e.getKey(), ItemBuilder.decoration(e.getValue()));
                    }
                }
            } else {
                applyFrame(inv, frameMaterial);
            }
        }
        for (Map.Entry<Integer, Button> e : buttons.entrySet()) {
            int slot = e.getKey();
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, e.getValue().getIcon());
            }
        }
        return inv;
    }

    /**
     * Builds and opens inventory for player.
     */
    public Inventory open(Player player) {
        Inventory inv = build();
        player.openInventory(inv);
        return inv;
    }
}
