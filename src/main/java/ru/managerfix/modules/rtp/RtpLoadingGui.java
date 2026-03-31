package ru.managerfix.modules.rtp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple loading GUI shown during RTP search (animation: rotating item or message).
 */
public final class RtpLoadingGui {

    private static final int SIZE = 27;
    private static final Map<UUID, BukkitTask> tasks = new HashMap<>();

    public static void open(ManagerFix plugin, Player player) {
        Component title = MessageUtil.parse("<dark_gray>Поиск места...");
        Inventory inv = Bukkit.createInventory(new Holder(), SIZE, title);
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, glass);
        }
        inv.setItem(13, new ItemStack(Material.COMPASS));
        player.openInventory(inv);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !(player.getOpenInventory().getTopInventory().getHolder() instanceof Holder)) {
                cancel(player);
                return;
            }
            // Simple animation: rotate center item
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getItem(13) != null) {
                Material m = top.getItem(13).getType();
                Material next = m == Material.COMPASS ? Material.MAP : Material.COMPASS;
                top.setItem(13, new ItemStack(next));
            }
        }, 10L, 10L);
        tasks.put(player.getUniqueId(), task);
    }

    public static void close(Player player) {
        cancel(player);
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof Holder) {
            player.closeInventory();
        }
    }

    private static void cancel(Player player) {
        BukkitTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return Bukkit.createInventory(null, SIZE);
        }
    }
}
