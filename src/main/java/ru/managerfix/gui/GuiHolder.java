package ru.managerfix.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Holder that stores button map for an open GUI. GuiManager uses this to route clicks.
 */
public final class GuiHolder implements InventoryHolder {

    private final String id;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Runnable onClose;
    private final Runnable onUpdate;
    private Inventory inventory;

    public GuiHolder(String id, Map<Integer, Button> buttons, Runnable onClose, Runnable onUpdate) {
        this.id = id;
        if (buttons != null) {
            this.buttons.putAll(buttons);
        }
        this.onClose = onClose;
        this.onUpdate = onUpdate;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getId() {
        return id;
    }

    public Button getButton(int slot) {
        return buttons.get(slot);
    }

    public void onClose() {
        if (onClose != null) {
            onClose.run();
        }
    }

    public void onUpdate() {
        if (onUpdate != null) {
            onUpdate.run();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory != null ? inventory : org.bukkit.Bukkit.createInventory(null, 9);
    }
}
