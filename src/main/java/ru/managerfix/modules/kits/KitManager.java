package ru.managerfix.modules.kits;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Cached kit definitions. giveKit checks permission and cooldown.
 */
public final class KitManager {

    private final KitStorage storage;
    private final ProfileManager profileManager;
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final Map<String, KitData> cache = new ConcurrentHashMap<>();

    public KitManager(KitStorage storage, ProfileManager profileManager, org.bukkit.plugin.java.JavaPlugin plugin) {
        this.storage = storage;
        this.profileManager = profileManager;
        this.plugin = plugin;
    }

    public void reload() {
        cache.clear();
        for (String name : storage.listKitNames()) {
            storage.loadKit(name).ifPresent(kit -> cache.put(kit.getName().toLowerCase(), kit));
        }
    }

    public Optional<KitData> getKit(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        KitData cached = cache.get(name.toLowerCase());
        if (cached != null) return Optional.of(cached);
        Optional<KitData> loaded = storage.loadKit(name);
        loaded.ifPresent(kit -> cache.put(kit.getName().toLowerCase(), kit));
        return loaded;
    }

    public List<String> getKitNames() {
        if (cache.isEmpty()) {
            for (String name : storage.listKitNames()) {
                storage.loadKit(name).ifPresent(kit -> cache.put(kit.getName().toLowerCase(), kit));
            }
        }
        // Сортируем: сначала по приоритету (возрастание), потом по имени (алфавит)
        List<KitData> sorted = new ArrayList<>(cache.values());
        sorted.sort(Comparator.comparingInt(KitData::getPriority).thenComparing(KitData::getName));
        List<String> result = new ArrayList<>();
        for (KitData kit : sorted) {
            result.add(kit.getName());
        }
        return result;
    }

    /** Returns true if kit was given, false if cooldown/permission/not found. */
    public boolean giveKit(Player player, String name) {
        Optional<KitData> opt = getKit(name);
        if (opt.isEmpty()) {
            MessageUtil.send(plugin, player, "kits.not-found", Map.of("name", name));
            return false;
        }
        KitData kit = opt.get();
        if (!player.hasPermission(kit.getPermission())) {
            MessageUtil.send(plugin, player, "kits.no-permission");
            return false;
        }
        
        // Проверка на одноразовый кит
        if (kit.isOneTime()) {
            String oneTimeKey = "kit_received_" + kit.getName().toLowerCase();
            PlayerProfile profile = profileManager.getProfile(player);
            Object received = profile.getMetadata(oneTimeKey).orElse(false);
            if (received instanceof Boolean b && b) {
                MessageUtil.send(plugin, player, "kits.one-time-already-received", Map.of("name", kit.getName()));
                return false;
            }
        }
        
        String cooldownKey = "kit_" + kit.getName().toLowerCase();
        PlayerProfile profile = profileManager.getProfile(player);
        if (kit.getCooldownSeconds() > 0 && !player.hasPermission("managerfix.bypass.cooldown")) {
            if (profile.hasCooldown(cooldownKey)) {
                long remaining = profile.getCooldownRemaining(cooldownKey);
                MessageUtil.send(plugin, player, "kits.cooldown", Map.of(
                        "name", kit.getName(),
                        "seconds", String.valueOf((remaining + 999) / 1000)));
                return false;
            }
            profile.setCooldown(cooldownKey, kit.getCooldownSeconds() * 1000L);
        }
        
        // Выдаём предметы
        for (ItemStack item : kit.getItems()) {
            if (item != null && !item.getType().isAir()) {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
                for (ItemStack left : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }
        }
        
        // Помечаем кит как полученный (для one_time)
        if (kit.isOneTime()) {
            String oneTimeKey = "kit_received_" + kit.getName().toLowerCase();
            profile.setMetadata(oneTimeKey, true);
        }
        
        MessageUtil.send(plugin, player, "kits.received", Map.of("name", kit.getName()));
        return true;
    }

    public void saveKit(KitData kit) {
        storage.saveKit(kit);
        cache.put(kit.getName().toLowerCase(), kit);
    }

    public void deleteKit(String name) {
        storage.deleteKit(name);
        cache.remove(name.toLowerCase());
    }

    public KitStorage getStorage() {
        return storage;
    }
}
