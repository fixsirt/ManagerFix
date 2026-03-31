package ru.managerfix.modules.other;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.MessageUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class FoodCommand implements CommandExecutor, TabCompleter {

    private static final String FOOD_GOD_KEY = "other.foodgod";

    private final OtherModule module;

    public FoodCommand(OtherModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player self)) {
            MessageUtil.send(module.getPlugin(), sender, "command-player-only");
            return true;
        }
        if (args.length == 0) {
            if (!sender.hasPermission("managerfix.other.food")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            feed(self);
            return true;
        }
        if ("god".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("managerfix.other.food.god")) {
                MessageUtil.send(module.getPlugin(), sender, "no-permission");
                return true;
            }
            boolean now = !module.isFoodGod(self.getUniqueId());
            module.setFoodGod(self.getUniqueId(), now);
            if (module.getOtherConfig().isFoodGodPersist()) {
                saveFoodGod(self, now);
            }
            self.sendMessage(MessageUtil.parse(now ? "<green>FoodGod включён.</green>" : "<red>FoodGod выключен.</red>"));
            return true;
        }
        if (!sender.hasPermission("managerfix.other.food.others")) {
            MessageUtil.send(module.getPlugin(), sender, "no-permission");
            return true;
        }
        Player target = ru.managerfix.utils.NickResolver.resolve(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.send(module.getPlugin(), sender, "player-not-found", Map.of("player", args[0]));
            return true;
        }
        feed(target);
        if (!target.equals(sender)) {
            module.logAdminAction("[Other] " + sender.getName() + " fed " + target.getName());
        }
        return true;
    }

    private void feed(Player target) {
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(MessageUtil.parse("<#3498db>Голод восполнен.</#3498db>"));
    }

    private void saveFoodGod(Player player, boolean state) {
        if (module.getPlugin() instanceof ManagerFix mf) {
            ProfileManager pm = mf.getProfileManager();
            PlayerProfile profile = pm.getProfile(player);
            profile.setMetadata(FOOD_GOD_KEY, state);
            pm.saveProfileAsync(player.getUniqueId());
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            if ("god".startsWith(args[0].toLowerCase()) && sender.hasPermission("managerfix.other.food.god")) {
                return List.of("god");
            }
            if (sender.hasPermission("managerfix.other.food.others")) {
                return ru.managerfix.utils.NickResolver.tabComplete(args[0], sender instanceof Player p ? p.getUniqueId() : null);
            }
        }
        return Collections.emptyList();
    }
}
