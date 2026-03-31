package ru.managerfix.modules.kits;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Kit definition: name, cooldown, permission, items.
 */
public final class KitData {

    private final String name;
    private int cooldownSeconds;
    private String permission;
    private final List<ItemStack> items;
    private int priority;
    private boolean oneTime;
    private String iconMaterial;  // Название материала иконки

    public KitData(String name, int cooldownSeconds, String permission, List<ItemStack> items) {
        this.name = name;
        this.cooldownSeconds = cooldownSeconds;
        this.permission = permission != null ? permission : "managerfix.kits.kit." + name.toLowerCase();
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.priority = 0;
        this.oneTime = false;
        this.iconMaterial = null;  // По умолчанию авто-выбор
    }

    public String getName() {
        return name;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<ItemStack> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Заменяет все предметы кита.
     * Используйте этот метод вместо getItems().clear()/addAll() — getItems() возвращает копию.
     */
    public void setItems(List<ItemStack> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public void setOneTime(boolean oneTime) {
        this.oneTime = oneTime;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    /**
     * Получает Material иконки кита.
     */
    public Material getIcon() {
        if (iconMaterial != null) {
            try {
                return Material.valueOf(iconMaterial);
            } catch (IllegalArgumentException e) {
                // Если материал не найден, используем авто-выбор
            }
        }
        // Авто-выбор по названию кита
        String lower = name.toLowerCase();
        if (lower.contains("warrior") || lower.contains("fight")) return Material.DIAMOND_SWORD;
        if (lower.contains("miner") || lower.contains("pick")) return Material.DIAMOND_PICKAXE;
        if (lower.contains("archer") || lower.contains("bow")) return Material.BOW;
        if (lower.contains("armor") || lower.contains("def")) return Material.DIAMOND_CHESTPLATE;
        if (lower.contains("food") || lower.contains("eat")) return Material.APPLE;
        if (lower.contains("vip") || lower.contains("premium")) return Material.GOLD_INGOT;
        return Material.CHEST;
    }
}
