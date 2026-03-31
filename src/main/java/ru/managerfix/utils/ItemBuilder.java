package ru.managerfix.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds ItemStack with Adventure Component name/lore. Minimal style, clean lores.
 */
public final class ItemBuilder {

    private final ItemStack stack;
    private Component displayName;
    private List<Component> lore;
    private boolean hideFlags = true;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.stack = new ItemStack(material, amount);
        this.lore = new ArrayList<>();
    }

    public ItemBuilder(ItemStack base) {
        this.stack = base.clone();
        this.lore = new ArrayList<>();
        ItemMeta m = base.getItemMeta();
        if (m != null && m.hasDisplayName()) {
            this.displayName = m.displayName();
        }
        if (m != null && m.hasLore() && m.lore() != null) {
            this.lore = new ArrayList<>(m.lore());
        }
    }

    public ItemBuilder name(Component name) {
        this.displayName = name;
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        this.displayName = MessageUtil.parse(miniMessage);
        return this;
    }

    public ItemBuilder lore(Component... lines) {
        this.lore = new ArrayList<>(Arrays.asList(lines));
        return this;
    }

    public ItemBuilder lore(List<Component> lines) {
        this.lore = new ArrayList<>(lines);
        return this;
    }

    public ItemBuilder loreStrings(String... miniMessageLines) {
        List<Component> list = new ArrayList<>();
        for (String s : miniMessageLines) {
            list.add(MessageUtil.parse(s));
        }
        this.lore = list;
        return this;
    }

    public ItemBuilder addLore(Component line) {
        this.lore.add(line);
        return this;
    }

    public ItemBuilder addLore(String miniMessage) {
        this.lore.add(MessageUtil.parse(miniMessage));
        return this;
    }

    /** Декоративный предмет без названия (рамка, разделители). */
    public static ItemStack decoration(Material material) {
        return new ItemBuilder(material).name(Component.empty()).hideFlags(true).build();
    }

    public ItemBuilder hideFlags(boolean hide) {
        this.hideFlags = hide;
        return this;
    }

    public ItemBuilder editMeta(Consumer<ItemMeta> consumer) {
        ItemMeta m = stack.getItemMeta();
        if (m != null) {
            consumer.accept(m);
            stack.setItemMeta(m);
        }
        return this;
    }

    /**
     * Sets skull owner for PLAYER_HEAD (by name). For texture use editMeta and SkullMeta.
     */
    public ItemBuilder skullOwner(String ownerName) {
        if (stack.getType() != Material.PLAYER_HEAD) return this;
        editMeta(meta -> {
            if (meta instanceof SkullMeta sm) {
                com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.getServer().createProfile(ownerName);
                if (profile != null) {
                    sm.setPlayerProfile(profile);
                }
            }
        });
        return this;
    }

    public ItemStack build() {
        ItemMeta m = stack.getItemMeta();
        if (m != null) {
            if (displayName != null) {
                m.displayName(displayName);
            }
            if (!lore.isEmpty()) {
                m.lore(lore);
            }
            if (hideFlags) {
                m.addItemFlags(ItemFlag.values());
            }
        }
        stack.setItemMeta(m);
        return stack;
    }
}
