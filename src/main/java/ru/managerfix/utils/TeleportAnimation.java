package ru.managerfix.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class TeleportAnimation {

    public enum AnimationType {
        RING, SWIRL, PULSE, PILLAR, CYCLONE, EXPLOSION, SNOW, RING_IN, DOUBLE_HELIX, ORBIT
    }

    public static BukkitTask playAnimation(Location loc, World world, AnimationType type, int durationTicks, JavaPlugin plugin) {
        if (world == null) return null;

        BukkitRunnable runnable = switch (type) {
            case RING -> createRingAnimation(loc, world, durationTicks);
            case SWIRL -> createSwirlAnimation(loc, world, durationTicks);
            case PULSE -> createPulseAnimation(loc, world, durationTicks);
            case PILLAR -> createPillarAnimation(loc, world, durationTicks);
            case CYCLONE -> createCycloneAnimation(loc, world, durationTicks);
            case EXPLOSION -> createExplosionAnimation(loc, world, durationTicks);
            case SNOW -> createSnowAnimation(loc, world, durationTicks);
            case RING_IN -> createRingInAnimation(loc, world, durationTicks);
            case DOUBLE_HELIX -> createDoubleHelixAnimation(loc, world, durationTicks);
            case ORBIT -> createOrbitAnimation(loc, world, durationTicks);
        };

        return runnable.runTaskTimer(plugin, 0L, 2L);
    }

    public static void playInstantDeparture(Location loc, World world) {
        if (world == null) return;
        world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.8, 0), 20, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.8, 0), 15, 0.2, 0.3, 0.2, 0.02);
        world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 0.8, 0), 25, 0.3, 0.5, 0.3, 0.03);
        world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1.2, 0), 10, 0.2, 0.4, 0.2, 0.03);
    }

    public static void playQuickArrival(Player player, Location destination) {
        if (player == null || !player.isOnline() || destination == null || destination.getWorld() == null) return;
        World world = destination.getWorld();
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("ManagerFix");

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 8 || player == null || !player.isOnline()) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 4; i++) {
                    double angle = (tick * 90 + i * 90) * Math.PI / 180;
                    double radius = 0.6 - (tick * 0.07);
                    if (radius < 0) radius = 0;
                    
                    double x = destination.getX() + Math.cos(angle) * radius;
                    double z = destination.getZ() + Math.sin(angle) * radius;
                    double y = destination.getY() + 0.5 + Math.sin(tick + i) * 0.15;

                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.03, 0.03, 0.03, 0.005);
                }

                if (tick < 5) {
                    world.spawnParticle(Particle.POOF, destination.clone().add(0, 0.5, 0), 2, 0.15, 0.2, 0.15, 0.01);
                }

                tick++;
            }
        }.runTaskLater(plugin, 2L);
    }

    // ============== RING ==============
    private static BukkitRunnable createRingAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double radius = 0.5 + Math.sin(progress * Math.PI * 2) * 0.3 + progress * 0.5;
                double y = loc.getY() + 0.5 + Math.sin(progress * Math.PI) * 0.3;

                for (int i = 0; i < 12; i++) {
                    double angle = (tick * 30 + i * 30) * Math.PI / 180;
                    Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * radius, y, loc.getZ() + Math.sin(angle) * radius);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.03, 0.03, 0.03, 0.005);
                }
                if (tick % 4 == 0) world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 1, 0.1, 0.1, 0.1, 0.01);
                tick++;
            }
        };
    }

    // ============== SWIRL ==============
    private static BukkitRunnable createSwirlAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double height = progress * 2.5;
                double radius = 0.8 - progress * 0.3;

                for (int i = 0; i < 8; i++) {
                    double angle = (tick * 45 + i * 45) * Math.PI / 180;
                    Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * radius, loc.getY() + 0.5 + height + Math.sin(tick * 0.3 + i) * 0.2, loc.getZ() + Math.sin(angle) * radius);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.03, 0.03, 0.03, 0.005);
                }
                if (tick % 3 == 0) world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 1, 0.15, 0.15, 0.15, 0.01);
                tick++;
            }
        };
    }

    // ============== PULSE ==============
    private static BukkitRunnable createPulseAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double pulse = Math.sin((double) tick / durationTicks * Math.PI);
                double radius = 0.3 + pulse * 0.8;

                for (int i = 0; i < 10; i++) {
                    double angle = (tick * 36 + i * 36) * Math.PI / 180;
                    Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * radius, loc.getY() + 0.5 + (i % 3) * 0.3, loc.getZ() + Math.sin(angle) * radius);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
                if (tick % 2 == 0) {
                    world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5 + pulse * 0.5, 0), 1, 0.1, 0.1, 0.1, 0.01);
                    world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 1, 0.15, 0.15, 0.15, 0.01);
                }
                tick++;
            }
        };
    }

    // ============== PILLAR ==============
    private static BukkitRunnable createPillarAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double height = progress * 3.0;
                double wobble = Math.sin(tick * 0.5) * 0.2;

                for (int i = 0; i < 4; i++) {
                    double angle = (tick * 30 + i * 90) * Math.PI / 180;
                    for (double h = 0; h < height; h += 0.5) {
                        Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * 0.3 + wobble, loc.getY() + 0.5 + h, loc.getZ() + Math.sin(angle) * 0.3);
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
                        if (tick % 2 == 0) world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.02, 0.02, 0.02, 0.005);
                    }
                }
                if (tick % 3 == 0) world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, height, 0), 2, 0.1, 0.2, 0.1, 0.02);
                tick++;
            }
        };
    }

    // ============== CYCLONE ==============
    private static BukkitRunnable createCycloneAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double height = progress * 3.5;
                double radius = 0.6 - progress * 0.3;

                for (int layer = 0; layer < 6; layer++) {
                    double layerHeight = Math.max(0, height - layer * 0.4);
                    double layerRadius = radius + layer * 0.1;
                    for (int i = 0; i < 4; i++) {
                        double angle = (tick * 60 + i * 90 + layer * 45) * Math.PI / 180;
                        Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * layerRadius, loc.getY() + 0.5 + layerHeight + Math.sin(tick * 0.3 + layer + i) * 0.15, loc.getZ() + Math.sin(angle) * layerRadius);
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.008);
                        if (tick % 2 == 0) world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.02, 0.02, 0.02, 0.003);
                    }
                }
                if (tick % 3 == 0) world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, height + 0.5, 0), 2, 0.1, 0.2, 0.1, 0.015);
                if (tick < 10) world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.015);
                tick++;
            }
        };
    }

    // ============== EXPLOSION ==============
    private static BukkitRunnable createExplosionAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double radius = 0.5 + progress * 2.0;
                double y = loc.getY() + 0.5 + progress * 0.5;

                for (int i = 0; i < 16; i++) {
                    double angle = (tick * 40 + i * 22.5) * Math.PI / 180;
                    double spread = 0.2 + progress * 0.3;
                    Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * radius + (Math.random() - 0.5) * spread, y, loc.getZ() + Math.sin(angle) * radius + (Math.random() - 0.5) * spread);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.03, 0.03, 0.03, 0.005);
                }
                if (tick < 15) world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 3, 0.15 * (1 + progress), 0.15 * (1 + progress), 0.15 * (1 + progress), 0.01);
                tick++;
            }
        };
    }

    // ============== SNOW ==============
    private static BukkitRunnable createSnowAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double height = 3.0 + progress * 1.5;

                for (int i = 0; i < 8; i++) {
                    double fallProgress = ((tick * 0.5 + i * 0.3) % 1.0);
                    Location particleLoc = new Location(world, loc.getX() + Math.sin(tick * 0.2 + i) * 0.3, loc.getY() + 0.5 + height * (1.0 - fallProgress), loc.getZ() + Math.cos(tick * 0.2 + i) * 0.3);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0.02, 0, 0.005);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.02, 0.02, 0.02, 0.003);
                }
                if (tick % 3 == 0) world.spawnParticle(Particle.POOF, loc.clone().add(0, height * 0.5, 0), 1, 0.2, 0.3, 0.2, 0.01);
                tick++;
            }
        };
    }

    // ============== RING_IN ==============
    private static BukkitRunnable createRingInAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double radius = 2.0 * (1.0 - progress);
                double y = loc.getY() + 0.5 + Math.sin(progress * Math.PI) * 0.5;

                for (int i = 0; i < 16; i++) {
                    double angle = (tick * 30 + i * 22.5) * Math.PI / 180;
                    double pulse = Math.sin(progress * Math.PI * 4) * 0.1;
                    Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * (radius + pulse), y, loc.getZ() + Math.sin(angle) * (radius + pulse));
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.03, 0.03, 0.03, 0.005);
                }
                if (tick % 2 == 0) world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 0.5, 0), 2, 0.2 * (1 - progress), 0.3 * (1 - progress), 0.2 * (1 - progress), 0.01);
                if (tick < 5) world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 2, 0.15, 0.15, 0.15, 0.01);
                tick++;
            }
        };
    }

    // ============== DOUBLE_HELIX ==============
    private static BukkitRunnable createDoubleHelixAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double height = progress * 3.0;
                double radius = 0.5;

                for (int strand = 0; strand < 2; strand++) {
                    double offset = strand * Math.PI;
                    for (int i = 0; i < 6; i++) {
                        double angle = (tick * 45 + i * 60 + offset) * Math.PI / 180;
                        Location particleLoc = new Location(world, loc.getX() + Math.cos(angle) * radius, loc.getY() + 0.5 + height + i * 0.3, loc.getZ() + Math.sin(angle) * radius);
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
                        if (tick % 2 == 0) world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.02, 0.02, 0.02, 0.003);
                    }
                }
                if (tick % 4 == 0) world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 1, 0.15, 0.15, 0.15, 0.01);
                if (tick > durationTicks * 0.7) world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, height + 1.0, 0), 3, 0.1, 0.2, 0.1, 0.015);
                tick++;
            }
        };
    }

    // ============== ORBIT ==============
    private static BukkitRunnable createOrbitAnimation(Location loc, World world, int durationTicks) {
        return new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks) { cancel(); return; }
                double progress = (double) tick / durationTicks;
                double height = loc.getY() + 0.5 + Math.sin(progress * Math.PI * 2) * 0.3;

                for (int orbit = 0; orbit < 3; orbit++) {
                    double orbitRadius = 0.4 + orbit * 0.35;
                    double orbitAngle = tick * 0.15 + orbit * (Math.PI * 2 / 3);
                    for (int i = 0; i < 4; i++) {
                        double particleAngle = orbitAngle + i * Math.PI / 2;
                        Location particleLoc = new Location(world, loc.getX() + Math.cos(particleAngle) * orbitRadius, height + Math.sin(tick * 0.3 + i) * 0.2, loc.getZ() + Math.sin(particleAngle) * orbitRadius);
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0.01);
                        if (tick % 2 == 0) world.spawnParticle(Particle.PORTAL, particleLoc, 1, 0.02, 0.02, 0.02, 0.003);
                    }
                }
                if (tick % 3 == 0) world.spawnParticle(Particle.POOF, loc.clone().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.01);
                tick++;
            }
        };
    }
}
