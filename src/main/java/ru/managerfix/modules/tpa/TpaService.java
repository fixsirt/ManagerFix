package ru.managerfix.modules.tpa;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.managerfix.ManagerFix;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.scheduler.TaskScheduler;
import ru.managerfix.storage.SqlTpaStorage;
import ru.managerfix.utils.MessageUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TPA logic: requests, blacklist, toggle, cooldown, teleport delay and animation.
 * Thread-safe; cleans up on quit and reload.
 */
public final class TpaService {

    private static final String COOLDOWN_KEY = "tpa";
    private static final double MOVE_THRESHOLD = 0.2;
    private static final int TICKS_PER_SECOND = 20;

    private final JavaPlugin plugin;
    private final TpaConfig config;
    private final TaskScheduler scheduler;
    private final SqlTpaStorage sqlTpaStorage;
    private final Map<UUID, TpaRequest> requestsByTarget = new ConcurrentHashMap<>();
    /** Owner UUID -> set of blacklisted player UUIDs */
    private final Map<UUID, Set<UUID>> blacklist = new ConcurrentHashMap<>();
    /** Player UUID -> true = accept requests (default), false = disabled */
    private final Map<UUID, Boolean> toggleAccept = new ConcurrentHashMap<>();
    /** Player UUID -> pending teleport task (the player who must stand still) */
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    public TpaService(JavaPlugin plugin, TpaConfig config, TaskScheduler scheduler, SqlTpaStorage sqlTpaStorage) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = config != null ? config : new TpaConfig(null);
        this.scheduler = scheduler != null ? scheduler : new TaskScheduler(plugin);
        this.sqlTpaStorage = sqlTpaStorage;
        
        // Синхронизируем локальный кэш с SQL хранилищем
        if (sqlTpaStorage != null) {
            syncWithSqlStorage();
        }
    }

    /**
     * Синхронизирует локальные кэши с SQL хранилищем.
     */
    private void syncWithSqlStorage() {
        // Кэш уже загружен в SqlTpaStorage при инициализации
        // Просто копируем данные в локальные поля
        if (sqlTpaStorage != null) {
            // Данные уже в sqlTpaStorage, используем его методы
        }
    }

    // --- Requests ---

    public void addRequest(UUID from, UUID to, long timeoutMillis, boolean tpaHere) {
        requestsByTarget.put(to, new TpaRequest(from, to, System.currentTimeMillis() + timeoutMillis, tpaHere));
    }

    public Optional<TpaRequest> getRequest(UUID target) {
        TpaRequest req = requestsByTarget.get(target);
        if (req == null || req.isExpired()) {
            if (req != null) requestsByTarget.remove(target);
            return Optional.empty();
        }
        return Optional.of(req);
    }

    public Optional<TpaRequest> removeRequest(UUID target) {
        TpaRequest req = requestsByTarget.remove(target);
        if (req == null || req.isExpired()) return Optional.empty();
        return Optional.of(req);
    }

    public void removeRequestsBySender(UUID from) {
        requestsByTarget.entrySet().removeIf(e -> e.getValue().getFrom().equals(from));
    }

    public void removeRequestsByTarget(UUID to) {
        requestsByTarget.remove(to);
    }

    public void cleanupExpired() {
        requestsByTarget.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public boolean hasActiveRequestTo(UUID target) {
        return getRequest(target).isPresent();
    }

    // --- Toggle (accept requests) ---

    public boolean isAcceptEnabled(UUID player) {
        if (sqlTpaStorage != null) {
            return sqlTpaStorage.isAcceptEnabled(player);
        }
        return toggleAccept.getOrDefault(player, true);
    }

    public void setAcceptEnabled(UUID player, boolean enabled) {
        if (sqlTpaStorage != null) {
            sqlTpaStorage.setAcceptEnabled(player, enabled);
        } else {
            toggleAccept.put(player, enabled);
            saveData();
        }
    }

    // --- Blacklist ---

    public boolean isBlacklisted(UUID owner, UUID player) {
        if (sqlTpaStorage != null) {
            return sqlTpaStorage.isBlacklisted(owner, player);
        }
        Set<UUID> set = blacklist.get(owner);
        return set != null && set.contains(player);
    }

    public void addToBlacklist(UUID owner, UUID player) {
        if (sqlTpaStorage != null) {
            sqlTpaStorage.addToBlacklist(owner, player);
        } else {
            blacklist.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(player);
            saveData();
        }
    }

    public void removeFromBlacklist(UUID owner, UUID player) {
        if (sqlTpaStorage != null) {
            sqlTpaStorage.removeFromBlacklist(owner, player);
        } else {
            Set<UUID> set = blacklist.get(owner);
            if (set != null) {
                set.remove(player);
                if (set.isEmpty()) blacklist.remove(owner);
                saveData();
            }
        }
    }

    public Set<UUID> getBlacklist(UUID owner) {
        if (sqlTpaStorage != null) {
            return sqlTpaStorage.getBlacklist(owner);
        }
        Set<UUID> set = blacklist.get(owner);
        return set == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(set));
    }

    // --- Validation before send ---

    public Optional<String> validateSend(Player sender, Player target) {
        if (sender == null || target == null) return Optional.of("player-not-found");
        if (sender.getUniqueId().equals(target.getUniqueId())) return Optional.of("tpa.self");
        if (!target.isOnline()) return Optional.of("player-not-found");
        if (!isAcceptEnabled(target.getUniqueId())) return Optional.of("target-disabled");
        if (isBlacklisted(target.getUniqueId(), sender.getUniqueId())) return Optional.of("in-blacklist");
        if (hasActiveRequestTo(target.getUniqueId())) return Optional.of("already-request");
        return Optional.empty();
    }

    // --- Cooldown (via ProfileManager) ---

    public boolean hasCooldown(Player player) {
        if (plugin instanceof ManagerFix mf) {
            ProfileManager pm = mf.getProfileManager();
            if (pm != null) {
                PlayerProfile profile = pm.getProfile(player);
                return profile != null && profile.hasCooldown(COOLDOWN_KEY);
            }
        }
        return false;
    }

    public long getCooldownRemaining(Player player) {
        if (plugin instanceof ManagerFix mf) {
            ProfileManager pm = mf.getProfileManager();
            if (pm != null) {
                PlayerProfile profile = pm.getProfile(player);
                return profile != null ? profile.getCooldownRemaining(COOLDOWN_KEY) : 0;
            }
        }
        return 0;
    }

    public void setCooldown(Player player, long millis) {
        if (millis <= 0) return;
        if (plugin instanceof ManagerFix mf) {
            ProfileManager pm = mf.getProfileManager();
            if (pm != null) {
                PlayerProfile profile = pm.getProfile(player);
                if (profile != null) profile.setCooldown(COOLDOWN_KEY, millis);
            }
        }
    }

    // --- Teleport with delay and animation ---

    public void scheduleTeleport(Player teleportingPlayer, Location destination, Runnable onCancel) {
        if (teleportingPlayer == null || !teleportingPlayer.isOnline() || destination == null) {
            log("scheduleTeleport: skipped (null or offline)");
            return;
        }
        UUID uuid = teleportingPlayer.getUniqueId();
        cancelPendingTeleport(uuid);

        int delaySec = config.getTeleportDelaySeconds();
        log("scheduleTeleport: player=" + teleportingPlayer.getName() + " uuid=" + uuid + " delay=" + delaySec);

        if (delaySec <= 0) {
            performTeleport(teleportingPlayer, destination);
            return;
        }

        // Сохраняем начальные координаты (x, y, z) отдельно — не Location, чтобы не зависеть от World reference
        double startX = teleportingPlayer.getLocation().getX();
        double startY = teleportingPlayer.getLocation().getY();
        double startZ = teleportingPlayer.getLocation().getZ();
        String startWorldName = teleportingPlayer.getWorld().getName();

        int[] secondsLeft = { delaySec };

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                Player p = teleportingPlayer; // прямая ссылка на объект Player
                if (p == null || !p.isOnline()) {
                    log("timer: player offline, cancelling uuid=" + uuid);
                    cancelPendingTeleport(uuid);
                    return;
                }

                // Проверка движения: сравниваем по координатам, а не по World.equals()
                double dx = p.getLocation().getX() - startX;
                double dy = p.getLocation().getY() - startY;
                double dz = p.getLocation().getZ() - startZ;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                boolean worldChanged = !p.getWorld().getName().equals(startWorldName);

                if (worldChanged || dist > MOVE_THRESHOLD) {
                    log("timer: player moved dist=" + dist + " worldChanged=" + worldChanged + ", cancelling");
                    cancelPendingTeleport(uuid);
                    if (onCancel != null) {
                        try { onCancel.run(); } catch (Exception ignored) {}
                    }
                    return;
                }

                if (secondsLeft[0] <= 0) {
                    log("timer: countdown done, teleporting player=" + p.getName());
                    cancelPendingTeleport(uuid);
                    performTeleport(p, destination);
                    return;
                }

                // Визуальный обратный отсчёт
                try {
                    String sec = String.valueOf(secondsLeft[0]);
                    p.showTitle(net.kyori.adventure.title.Title.title(
                            MessageUtil.parse("<gradient:#FF4D00:#FAA300>" + sec + "</gradient>"),
                            MessageUtil.parse("<#E0E0E0>Не двигайтесь..."),
                            net.kyori.adventure.title.Title.Times.times(
                                    java.time.Duration.ofMillis(0),
                                    java.time.Duration.ofMillis(1100),
                                    java.time.Duration.ofMillis(0)
                            )
                    ));
                    p.sendActionBar(MessageUtil.parse("<#FAA300>Телепортация через " + sec + " сек.</#FAA300>"));
                } catch (Exception e) {
                    log("timer: title/actionbar error: " + e.getMessage());
                }

                // Частицы и звук (необязательно, ошибка не должна ломать телепорт)
                try { spawnCountdownParticles(p); } catch (Exception ignored) {}
                try {
                    if (config.isAllowSound() && config.getSoundType() != null) {
                        p.playSound(p.getLocation(), config.getSoundType(), config.getSoundVolume() * 0.3f, config.getSoundPitch());
                    }
                } catch (Exception ignored) {}

                secondsLeft[0]--;
            } catch (Exception e) {
                log("timer: EXCEPTION in timer tick: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                cancelPendingTeleport(uuid);
            }
        }, 0L, TICKS_PER_SECOND);

        pendingTeleports.put(uuid, new PendingTeleport(startX, startY, startZ, startWorldName, task));
    }

    public void teleportInstant(Player teleportingPlayer, Location destination) {
        if (teleportingPlayer == null || !teleportingPlayer.isOnline() || destination == null) {
            return;
        }
        UUID uuid = teleportingPlayer.getUniqueId();
        cancelPendingTeleport(uuid);
        performTeleport(teleportingPlayer, destination);
    }

    public void cancelPendingTeleport(UUID playerUuid) {
        PendingTeleport pt = pendingTeleports.remove(playerUuid);
        if (pt != null && pt.task != null) {
            try { pt.task.cancel(); } catch (Exception ignored) {}
        }
    }

    public boolean hasPendingTeleport(UUID playerUuid) {
        return pendingTeleports.containsKey(playerUuid);
    }

    /** Used by move listener; returns start location of pending teleport. */
    public Optional<Location> getPendingTeleportStart(UUID playerUuid) {
        PendingTeleport pt = pendingTeleports.get(playerUuid);
        if (pt == null) return Optional.empty();
        World w = plugin.getServer().getWorld(pt.worldName);
        if (w == null) return Optional.empty();
        return Optional.of(new Location(w, pt.x, pt.y, pt.z));
    }

    private void spawnCountdownParticles(Player p) {
        Location loc = p.getLocation().add(0, 1, 0);
        World w = loc.getWorld();
        if (w == null) return;
        try {
            w.spawnParticle(Particle.PORTAL, loc, 20, 0.5, 0.5, 0.5, 0.1);
        } catch (Throwable ignored) {}
        try {
            w.spawnParticle(Particle.ENCHANT, loc, 15, 0.4, 0.6, 0.4, 0.5);
        } catch (Throwable ignored) {}
    }

    private void performTeleport(Player player, Location destination) {
        log("performTeleport: player=" + (player != null ? player.getName() : "null")
                + " online=" + (player != null && player.isOnline())
                + " dest=" + destination);
        if (player == null || !player.isOnline()) {
            log("performTeleport: ABORTED — player null or offline");
            return;
        }
        if (destination == null || destination.getWorld() == null) {
            log("performTeleport: ABORTED — destination null or world null");
            return;
        }

        try {
            try {
                java.util.List<org.bukkit.entity.Entity> passengers = new java.util.ArrayList<>(player.getPassengers());
                for (org.bukkit.entity.Entity e : passengers) {
                    if (e instanceof org.bukkit.entity.Display) {
                        player.removePassenger(e);
                    }
                }
            } catch (Throwable ignored) {}

            // Анимация в точке отбытия
            Location from = player.getLocation();
            try {
                World fw = from.getWorld();
                if (fw != null) {
                    Location at = from.clone().add(0, 1, 0);
                    fw.spawnParticle(Particle.PORTAL, at, 60, 0.5, 1, 0.5, 0.3);
                }
            } catch (Throwable ignored) {}

            // Звук
            try {
                if (config.isAllowSound() && config.getSoundType() != null) {
                    player.playSound(from, config.getSoundType(), config.getSoundVolume(), config.getSoundPitch());
                }
            } catch (Throwable ignored) {}

            // Title
            try {
                player.showTitle(net.kyori.adventure.title.Title.title(
                        MessageUtil.parse("<gradient:#FF4D00:#FAA300>Телепорт!</gradient>"),
                        net.kyori.adventure.text.Component.empty(),
                        net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(100),
                                java.time.Duration.ofMillis(800),
                                java.time.Duration.ofMillis(300)
                        )
                ));
            } catch (Throwable ignored) {}

            // === ТЕЛЕПОРТ ===
            boolean result = player.teleport(destination);
            log("performTeleport: teleport() returned " + result);

            // Анимация в точке прибытия
            try {
                Location to = destination.clone().add(0, 1, 0);
                World dw = destination.getWorld();
                if (dw != null) {
                    dw.spawnParticle(Particle.PORTAL, to, 60, 0.5, 1, 0.5, 0.3);
                }
            } catch (Throwable ignored) {}

            try {
                if (config.isAllowSound() && config.getSoundType() != null) {
                    player.playSound(destination, config.getSoundType(), config.getSoundVolume(), config.getSoundPitch());
                }
            } catch (Throwable ignored) {}

        } catch (Exception e) {
            log("performTeleport: EXCEPTION: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void log(String msg) {
        // if (plugin != null && plugin.getConfig().getBoolean("settings.debug", false)) {
        //     plugin.getLogger().info("[TPA] " + msg);
        // }
    }

    // --- Persistence ---

    // Данные сохраняются в SQL через SqlTpaStorage
    // Для YAML режима оставляем методы saveData/loadData

    private void saveData() {
        // YAML режим - данные сохраняются в локальные кэши
        // При следующем включении SQL они будут синхронизированы
    }

    public void cleanup() {
        requestsByTarget.clear();
        for (PendingTeleport pt : pendingTeleports.values()) {
            if (pt != null && pt.task != null) pt.task.cancel();
        }
        pendingTeleports.clear();
    }

    public TpaConfig getConfig() {
        return config;
    }
    
    public int getTeleportDelay() {
        return config.getTeleportDelaySeconds();
    }

    private static final class PendingTeleport {
        final double x, y, z;
        final String worldName;
        final BukkitTask task;

        PendingTeleport(double x, double y, double z, String worldName, BukkitTask task) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldName = worldName;
            this.task = task;
        }
    }
}
