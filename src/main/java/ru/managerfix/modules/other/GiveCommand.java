package ru.managerfix.modules.other;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class GiveCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public GiveCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.other.give")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(module.getPlugin(), sender, "usage", Map.of("usage", "/give <name> <item> <amount>"));
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        Material mat;
        try {
            mat = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.send(module.getPlugin(), sender, "other.give.invalid-item");
            return true;
        }
        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (Exception ignored) {
            }
        }
        ItemStack item = new ItemStack(mat, amount);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
        for (ItemStack left : overflow.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), left);
        }
        MessageUtil.send(module.getPlugin(), sender, "other.give.done", Map.of("player", target.getName(), "item", mat.name(), "amount", String.valueOf(amount)));
        module.logAdminAction("[Other] " + sender.getName() + " gave " + amount + " " + mat.name() + " to " + target.getName());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
        }
        if (args.length == 2) {
            String prefix = args[1].toUpperCase();
            return Stream.of(Material.values())
                    .map(Material::name)
                    .filter(n -> n.startsWith(prefix))
                    .map(String::toLowerCase)
                    .toList();
        }
        return Collections.emptyList();
    }
}
