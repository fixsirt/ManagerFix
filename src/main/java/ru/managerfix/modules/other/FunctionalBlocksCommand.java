package ru.managerfix.modules.other;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.utils.MessageUtil;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public final class FunctionalBlocksCommand implements CommandExecutor, TabCompleter {

    private final OtherModule module;

    public FunctionalBlocksCommand(OtherModule module) {
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
        String perm = "managerfix.other." + cmd;
        if (!sender.hasPermission(perm)) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        Component title = MessageUtil.get(module.getPlugin(), player, "other.blocks." + cmd);
        switch (cmd) {
            case "workbench" -> openBlock(player, "openWorkbench", InventoryType.WORKBENCH, title);
            case "anvil" -> openBlock(player, "openAnvil", InventoryType.ANVIL, title);
            case "stonecutter" -> openBlock(player, "openStonecutter", InventoryType.STONECUTTER, title);
            case "grindstone" -> openBlock(player, "openGrindstone", InventoryType.GRINDSTONE, title);
            case "cartography" -> openBlock(player, "openCartographyTable", InventoryType.CARTOGRAPHY, title);
            case "loom" -> openBlock(player, "openLoom", InventoryType.LOOM, title);
            case "enchanting" -> openBlock(player, "openEnchanting", InventoryType.ENCHANTING, title);
            default -> { return false; }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    private void openBlock(Player player, String methodName, InventoryType fallbackType, Component title) {
        try {
            Method method = player.getClass().getMethod(methodName, org.bukkit.Location.class, boolean.class);
            method.invoke(player, player.getLocation(), true);
            return;
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            module.getPlugin().getLogger().warning("Failed to open " + methodName + ": " + t.getMessage());
        }
        player.openInventory(Bukkit.createInventory(player, fallbackType, title));
    }
}
