package ru.managerfix.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Clickable GUI element: icon + click handler (LMB, RMB, SHIFT+RMB).
 */
public final class Button {

    private final ItemStack icon;
    private final Consumer<InventoryClickEvent> onClick;
    private final Consumer<InventoryClickEvent> onRightClick;
    private final Consumer<InventoryClickEvent> onShiftRightClick;

    private Button(ItemStack icon,
                   Consumer<InventoryClickEvent> onClick,
                   Consumer<InventoryClickEvent> onRightClick,
                   Consumer<InventoryClickEvent> onShiftRightClick) {
        this.icon = icon;
        this.onClick = onClick;
        this.onRightClick = onRightClick;
        this.onShiftRightClick = onShiftRightClick;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void handle(InventoryClickEvent event) {
        if (event.getClick().isShiftClick() && event.getClick().isRightClick()) {
            if (onShiftRightClick != null) {
                onShiftRightClick.accept(event);
            }
            return;
        }
        if (event.getClick().isRightClick()) {
            if (onRightClick != null) {
                onRightClick.accept(event);
            }
            return;
        }
        if (event.getClick().isLeftClick()) {
            if (onClick != null) {
                onClick.accept(event);
            }
        }
    }

    public static Builder builder(ItemStack icon) {
        return new Builder(icon);
    }

    public static class Builder {
        private final ItemStack icon;
        private Consumer<InventoryClickEvent> onClick;
        private Consumer<InventoryClickEvent> onRightClick;
        private Consumer<InventoryClickEvent> onShiftRightClick;

        public Builder(ItemStack icon) {
            this.icon = icon;
        }

        public Builder onClick(Consumer<InventoryClickEvent> handler) {
            this.onClick = handler;
            return this;
        }

        public Builder onRightClick(Consumer<InventoryClickEvent> handler) {
            this.onRightClick = handler;
            return this;
        }

        public Builder onShiftRightClick(Consumer<InventoryClickEvent> handler) {
            this.onShiftRightClick = handler;
            return this;
        }

        public Button build() {
            return new Button(icon, onClick, onRightClick, onShiftRightClick);
        }
    }
}
