package ru.managerfix.modules.other;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class WeatherTimeCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public WeatherTimeCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        World world = player.getWorld();
        switch (cmd) {
            case "weather" -> {
                if (!sender.hasPermission("managerfix.other.weather")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                if (args.length < 1) {
                    MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/weather <clear|rain|thunder>"));
                    return true;
                }
                return setWeather(sender, world, args[0]);
            }
            case "day" -> {
                if (!sender.hasPermission("managerfix.other.time")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                world.setTime(1000);
                MessageUtil.send(module.getPlugin(), sender, "other.time.day");
                return true;
            }
            case "night" -> {
                if (!sender.hasPermission("managerfix.other.time")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                world.setTime(13000);
                MessageUtil.send(module.getPlugin(), sender, "other.time.night");
                return true;
            }
            case "sun" -> {
                if (!sender.hasPermission("managerfix.other.weather")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                return setWeather(sender, world, "clear");
            }
            case "rain" -> {
                if (!sender.hasPermission("managerfix.other.weather")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                return setWeather(sender, world, "rain");
            }
            case "thunder" -> {
                if (!sender.hasPermission("managerfix.other.weather")) {
                    MessageUtil.send(module.getPlugin(), sender, "no-permission");
                    return true;
                }
                return setWeather(sender, world, "thunder");
            }
            default -> {
                return false;
            }
        }
    }

    private boolean setWeather(CommandSender sender, World world, String type) {
        if (world == null) return true;
        switch (type.toLowerCase()) {
            case "clear" -> {
                world.setStorm(false);
                world.setThundering(false);
                MessageUtil.send(module.getPlugin(), sender, "other.weather.clear");
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
                MessageUtil.send(module.getPlugin(), sender, "other.weather.rain");
            }
            case "thunder" -> {
                world.setStorm(true);
                world.setThundering(true);
                MessageUtil.send(module.getPlugin(), sender, "other.weather.thunder");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if ("weather".equalsIgnoreCase(command.getName()) && args.length == 1) {
            return List.of("clear", "rain", "thunder").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
