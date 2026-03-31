package ru.managerfix.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized GUI manager: routes clicks, click throttle 200ms, UI_BUTTON_CLICK sound,
 * ActionBar confirmation, scheduled animation updates.
 */
public final class GuiManager implements Listener {

    public static final long CLICK_THROTTLE_MS = 200;

    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> updateTasks = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> lastClickTime = new ConcurrentHashMap<>();
    private Sound clickSound = Sound.UI_BUTTON_CLICK;
    private float clickVolume = 0.5f;
    private float clickPitch = 1f;

    public GuiManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setClickSound(Sound sound, float volume, float pitch) {
        this.clickSound = sound != null ? sound : Sound.UI_BUTTON_CLICK;
        this.clickVolume = volume;
        this.clickPitch = pitch;
    }

    /**
     * Opens inventory for player (built by GuiBuilder). Optionally schedule periodic update.
     */
    public void open(Player player, Inventory inventory) {
        player.openInventory(inventory);
    }

    /**
     * Schedules periodic update for an open GUI (e.g. every 20 ticks). Call when opening.
     * Update runs only while the open inventory holder is a GuiHolder.
     */
    public void scheduleUpdate(Player player, long intervalTicks, Runnable updateAction) {
        cancelUpdate(player);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (!player.isOnline()) return;
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                        updateAction.run();
                    }
                },
                intervalTicks,
                intervalTicks
        );
        updateTasks.put(player.getUniqueId(), task);
    }

    public void cancelUpdate(Player player) {
        BukkitTask task = updateTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /** Sends message to player's ActionBar. */
    public void sendActionBar(Player player, String miniMessage) {
        if (player == null || !player.isOnline()) return;
        Component comp = MessageUtil.parse(miniMessage != null ? miniMessage : "");
        player.sendActionBar(comp);
    }

    /** Sends green confirmation to ActionBar (success flash). */
    public void sendConfirmation(Player player, String message) {
        sendActionBar(player, UIThemeManager.GRADIENT_ACCENT + (message != null ? message : "✔ Готово") + "</gradient>");
    }

    private boolean throttle(Player player) {
        long now = System.currentTimeMillis();
        AtomicLong last = lastClickTime.computeIfAbsent(player.getUniqueId(), u -> new AtomicLong(0));
        if (now - last.get() < CLICK_THROTTLE_MS) return true;
        last.set(now);
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }

        // Исключение для GUI выбора иконки кита - НЕ отменяем события
        if (holder.getId().startsWith("kit_icon_")) {
            int slot = event.getRawSlot();
            
            // Если клик НЕ в верхнем GUI (инвентарь игрока) - разрешаем
            if (slot >= 54) {
                return;
            }
            
            // Если клик в слот 22 или 31 - отменяем и обрабатываем через Button
            if (slot == 22 || slot == 31) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    Button button = holder.getButton(slot);
                    if (button != null) {
                        if (throttle(player)) return;
                        player.playSound(player.getLocation(), clickSound, clickVolume, clickPitch);
                        button.handle(event);
                    }
                }
                return;
            }
            
            // Остальные слоты верхнего GUI - отменяем
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            if (event.getWhoClicked() instanceof Player player) {
                player.updateInventory();
            }
            return;
        }

        // Полная блокировка любого клика в остальные GUI
        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Принудительно обновляем инвентарь игрока, чтобы убрать "визуальные" предметы
        player.updateInventory();

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        Button button = holder.getButton(slot);
        if (button != null) {
            if (throttle(player)) return;
            player.playSound(player.getLocation(), clickSound, clickVolume, clickPitch);
            button.handle(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder holder) {
            // Исключение для GUI выбора иконки кита - разрешаем перетаскивание
            if (holder.getId().startsWith("kit_icon_")) {
                // Если перетаскивание НЕ в слот 22 - отменяем
                if (!event.getRawSlots().contains(22)) {
                    event.setCancelled(true);
                    event.setResult(org.bukkit.event.Event.Result.DENY);
                    if (event.getWhoClicked() instanceof Player player) {
                        player.updateInventory();
                    }
                    return;
                }
                
                // Перетаскивание в слот 22 - отменяем и обрабатываем через Button
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    Button button = holder.getButton(22);
                    if (button != null) {
                        if (throttle(player)) return;
                        player.playSound(player.getLocation(), clickSound, clickVolume, clickPitch);
                        // Создаём фейковое InventoryClickEvent для обработки
                        org.bukkit.event.inventory.InventoryClickEvent clickEvent = 
                            new org.bukkit.event.inventory.InventoryClickEvent(
                                event.getView(),
                                InventoryType.SlotType.CONTAINER,
                                22,
                                org.bukkit.event.inventory.ClickType.LEFT,
                                org.bukkit.event.inventory.InventoryAction.PLACE_ALL
                            );
                        button.handle(clickEvent);
                    }
                }
                return;
            }
            
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
            if (event.getWhoClicked() instanceof Player player) {
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder holder) {
            holder.onClose();
            if (event.getPlayer() instanceof Player player) {
                cancelUpdate(player);
            }
        }
    }
}
