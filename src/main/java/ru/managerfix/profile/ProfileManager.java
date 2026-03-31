package ru.managerfix.profile;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.event.EventBus;
import ru.managerfix.event.ProfileLoadEvent;
import ru.managerfix.event.ProfileSaveEvent;
import ru.managerfix.storage.ProfileStorage;
import ru.managerfix.scheduler.TaskScheduler;

import java.util.*;
import java.util.concurrent.*;

/**
 * Оптимизированный менеджер профилей с CompletableFuture и защитой от гонок.
 * - Возвращает CompletableFuture для асинхронной загрузки
 * - Использует ConcurrentMap для потокобезопасности
 * - Слияние данных при загрузке после изменения в памяти
 */
public final class ProfileManager implements Listener {

    private final JavaPlugin plugin;
    private final ProfileStorage profileStorage;
    private final EventBus eventBus;
    private final ConcurrentHashMap<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    // Карта ожидающих загрузку профилей (для дедупликации запросов)
    private final ConcurrentHashMap<UUID, CompletableFuture<PlayerProfile>> pendingLoads = new ConcurrentHashMap<>();
    private final int autosaveMinutes;
    private volatile BukkitTask autosaveTask;
    private final TaskScheduler scheduler;

    public ProfileManager(JavaPlugin plugin, ProfileStorage profileStorage, int autosaveMinutes,
                         TaskScheduler scheduler, EventBus eventBus) {
        this.plugin = plugin;
        this.profileStorage = profileStorage;
        this.eventBus = eventBus;
        this.autosaveMinutes = Math.max(1, autosaveMinutes);
        this.scheduler = scheduler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Возвращает профиль игрока. Если профиль ещё не загружен, запускает асинхронную загрузку.
     * Возвращает временный профиль до завершения загрузки.
     */
    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    /**
     * Возвращает профиль по UUID. Если профиль ещё не загружен, запускает асинхронную загрузку.
     */
    public PlayerProfile getProfile(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> {
            PlayerProfile tempProfile = new PlayerProfile(u);
            loadProfileInternal(u, tempProfile);
            return tempProfile;
        });
    }

    /**
     * Возвращает CompletableFuture с профилем. Завершается когда данные загружены.
     */
    public CompletableFuture<PlayerProfile> getProfileAsync(UUID uuid) {
        // Проверяем, есть ли уже в кэше
        PlayerProfile cached = cache.get(uuid);
        if (cached != null && !cached.isModified()) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // Проверяем, есть ли уже ожидающая загрузка
        CompletableFuture<PlayerProfile> pending = pendingLoads.get(uuid);
        if (pending != null) {
            return pending;
        }
        
        // Создаём новый CompletableFuture для загрузки
        CompletableFuture<PlayerProfile> future = new CompletableFuture<>();
        pendingLoads.put(uuid, future);
        
        PlayerProfile profile = cache.computeIfAbsent(uuid, u -> new PlayerProfile(u));
        
        profileStorage.loadProfileAsync(uuid, result -> {
            pendingLoads.remove(uuid);
            if (result.isPresent()) {
                PlayerProfile loaded = result.get();
                PlayerProfile existing = cache.get(uuid);
                if (existing != null && existing.isModified()) {
                    // Слияние: загруженные данные + изменения в памяти
                    existing.mergeFrom(loaded);
                } else {
                    cache.put(uuid, loaded);
                    future.complete(loaded);
                    return;
                }
            }
            future.complete(cache.get(uuid));
            
            if (eventBus != null) {
                runSync(() -> eventBus.callEvent(new ProfileLoadEvent(uuid, result)));
            }
        });
        
        return future;
    }

    /**
     * Внутренняя загрузка профиля (не блокирующая).
     */
    private void loadProfileInternal(UUID uuid, PlayerProfile tempProfile) {
        profileStorage.loadProfileAsync(uuid, result -> {
            if (result.isPresent()) {
                PlayerProfile existing = cache.get(uuid);
                if (existing == null || !existing.isModified()) {
                    cache.put(uuid, result.get());
                } else {
                    existing.mergeFrom(result.get());
                }
            }
            if (eventBus != null) {
                runSync(() -> eventBus.callEvent(new ProfileLoadEvent(uuid, result)));
            }
        });
    }

    public Optional<PlayerProfile> getCachedProfile(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    /** Returns UUIDs of all profiles currently in cache (e.g. for AFK top). */
    public Set<UUID> getCachedUuids() {
        return new HashSet<>(cache.keySet());
    }

    public void loadProfileAsync(UUID uuid) {
        profileStorage.loadProfileAsync(uuid, result -> {
            if (result.isPresent()) {
                PlayerProfile existing = cache.get(uuid);
                if (existing == null || !existing.isModified()) {
                    cache.put(uuid, result.get());
                } else {
                    existing.mergeFrom(result.get());
                }
            }
            if (eventBus != null) {
                runSync(() -> eventBus.callEvent(new ProfileLoadEvent(uuid, result)));
            }
        });
    }

    public void saveProfileAsync(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return;
        Runnable onDone = () -> {
            profile.setModified(false);
            if (eventBus != null) {
                runSync(() -> eventBus.callEvent(new ProfileSaveEvent(profile)));
            }
        };
        profileStorage.saveProfileAsync(profile, onDone);
    }

    public void findUuidsByMetadataEquals(String key, String value, java.util.function.Consumer<java.util.Set<java.util.UUID>> callback) {
        if (profileStorage != null) {
            profileStorage.findUuidsByMetadataEquals(key, value, callback);
        } else {
            callback.accept(java.util.Collections.emptySet());
        }
    }

    private void runSync(Runnable r) {
        if (scheduler != null) {
            scheduler.runSync(r);
        } else {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    public void unloadProfile(UUID uuid) {
        // saveProfileAsync уже вызван перед этим в onPlayerQuit — не дублируем
        cache.remove(uuid);
        pendingLoads.remove(uuid);
    }

    public void startAutosave() {
        stopAutosave();
        long periodTicks = autosaveMinutes * 60 * 20L;
        Runnable saveAll = () -> {
            for (UUID uuid : new HashSet<>(cache.keySet())) {
                saveProfileAsync(uuid);
            }
        };
        if (scheduler != null) {
            autosaveTask = scheduler.runTimer(periodTicks, periodTicks, saveAll);
        } else {
            autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, saveAll, periodTicks, periodTicks);
        }
    }

    public void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = getProfile(player);
        try {
            String ip = player.getAddress() != null && player.getAddress().getAddress() != null
                    ? player.getAddress().getAddress().getHostAddress()
                    : null;
            if (ip != null && !ip.isEmpty()) {
                profile.setLastIpAddress(ip);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Получаем профиль ТОЛЬКО из кэша (не создаём новый и не загружаем из БД)
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) {
            // Профиля нет в кэше - нечего сохранять
            return;
        }
        
        try {
            Location loc = player.getLocation();
            if (loc != null && loc.getWorld() != null) {
                String lastLoc = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                profile.setMetadata("last_location", lastLoc);
            }
        } catch (Throwable ignored) {}
        try {
            GameMode gm = player.getGameMode();
            if (gm != null) {
                profile.setMetadata("last_gamemode", gm.name());
            }
            profile.setMetadata("last_fly", player.getAllowFlight());
        } catch (Throwable ignored) {}
        
        // Сохраняем профиль асинхронно
        saveProfileAsync(uuid);
        unloadProfile(uuid);
    }
}
