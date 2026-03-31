package ru.managerfix.modules.other;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class InvseeHolder implements InventoryHolder {

    private final UUID targetUuid;
    private Inventory inventory;

    public InvseeHolder(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @Nullable Inventory getInventory() {
        return inventory;
    }
}
