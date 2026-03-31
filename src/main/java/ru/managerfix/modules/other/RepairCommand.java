package ru.managerfix.modules.other;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class RepairCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public RepairCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player self)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (!sender.hasPermission("managerfix.other.repair")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (!module.checkAndApplyCooldown(self, "repair", "managerfix.bypass.cooldown")) {
            return true;
        }
        boolean all = args.length > 0 && "all".equalsIgnoreCase(args[0]);
        Player target = self;
        if (args.length > 0 && !"all".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("managerfix.other.repair.others")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            target = ru.managerfix.utils.NickResolver.resolve(args[0]);
            if (target == null || !target.isOnline()) {
                MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
                return true;
            }
        }
        if (all && args.length > 1) {
            if (!sender.hasPermission("managerfix.other.repair.others")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            target = ru.managerfix.utils.NickResolver.resolve(args[1]);
            if (target == null || !target.isOnline()) {
                MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[1]));
                return true;
            }
        }
        if (all && !sender.hasPermission("managerfix.other.repair.all")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        if (all) {
            repairAll(target);
        } else {
            repairItem(target.getInventory().getItemInMainHand());
        }
        MessageUtil.send(module.getPlugin(), sender, "repair.done");
        if (!target.equals(sender)) {
            module.logAdminAction("[Other] " + sender.getName() + " repaired items for " + target.getName());
        }
        return true;
    }

    private void repairAll(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            repairItem(item);
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            repairItem(armor);
        }
        repairItem(player.getInventory().getItemInOffHand());
    }

    private void repairItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (!(item.getItemMeta() instanceof Damageable meta)) return;
        meta.setDamage(0);
        item.setItemMeta(meta);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            if ("all".startsWith(args[0].toLowerCase()) && sender.hasPermission("managerfix.other.repair.all")) {
                return List.of("all");
            }
            if (sender.hasPermission("managerfix.other.repair.others")) {
                return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
            }
        }
        return Collections.emptyList();
    }
}
