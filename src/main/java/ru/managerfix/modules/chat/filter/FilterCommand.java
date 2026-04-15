package ru.managerfix.modules.chat.filter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.managerfix.ManagerFix;
import ru.managerfix.utils.MessageUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FilterCommand implements CommandExecutor, TabCompleter {

    private final ManagerFix plugin;

    public FilterCommand(ManagerFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("managerfix.chat.filter")) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reload(sender);
                break;
            case "toggle":
                toggle(sender);
                break;
            case "status":
                status(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void reload(CommandSender sender) {
        var chatModule = plugin.getModuleManager().getEnabledModule("chat")
                .filter(m -> m instanceof ru.managerfix.modules.chat.ChatModule)
                .map(m -> (ru.managerfix.modules.chat.ChatModule) m)
                .orElse(null);

        if (chatModule != null && chatModule.getProfanityFilter() != null) {
            chatModule.getProfanityFilter().load();
            sender.sendMessage(MessageUtil.parse("<#00C8FF>Фильтр мата перезагружён.</#00C8FF>"));
        } else {
            sender.sendMessage(MessageUtil.parse("<#FF3366>Фильтр мата не найден.</#FF3366>"));
        }
    }

    private void toggle(CommandSender sender) {
        var chatModule = plugin.getModuleManager().getEnabledModule("chat")
                .filter(m -> m instanceof ru.managerfix.modules.chat.ChatModule)
                .map(m -> (ru.managerfix.modules.chat.ChatModule) m)
                .orElse(null);

        if (chatModule != null && chatModule.getProfanityFilter() != null) {
            boolean enabled = chatModule.getProfanityFilter().isEnabled();
            chatModule.getProfanityFilter().setEnabled(!enabled);
            String state = !enabled ? "<#00C8FF>включён</#00C8FF>" : "<#FF3366>выключен</#FF3366>";
            sender.sendMessage(MessageUtil.parse("<#F0F4F8>Фильтр мата " + state + ".</#F0F4F8>"));
        } else {
            sender.sendMessage(MessageUtil.parse("<#FF3366>Фильтр мата не найден.</#FF3366>"));
        }
    }

    private void status(CommandSender sender) {
        var chatModule = plugin.getModuleManager().getEnabledModule("chat")
                .filter(m -> m instanceof ru.managerfix.modules.chat.ChatModule)
                .map(m -> (ru.managerfix.modules.chat.ChatModule) m)
                .orElse(null);

        if (chatModule != null && chatModule.getProfanityFilter() != null) {
            ProfanityFilter filter = chatModule.getProfanityFilter();
            String state = filter.isEnabled() ? "<#00C8FF>ВКЛ</#00C8FF>" : "<#FF3366>ВЫКЛ</#FF3366>";
            String action = filter.getAction().name();

            sender.sendMessage(MessageUtil.parse("<#F0F4F8>─── Фильтр мата ───</#F0F4F8>"));
            sender.sendMessage(MessageUtil.parse("<#F0F4F8>Статус: " + state + "</#F0F4F8>"));
            sender.sendMessage(MessageUtil.parse("<#F0F4F8>Действие: <#00C8FF>" + action + "</#00C8FF></#F0F4F8>"));
        } else {
            sender.sendMessage(MessageUtil.parse("<#FF3366>Фильтр мата не найден.</#FF3366>"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.parse("<#F0F4F8>─── Фильтр мата ───</#F0F4F8>"));
        sender.sendMessage(MessageUtil.parse("<#F0F4F8>/filter reload - перезагрузить</#F0F4F8>"));
        sender.sendMessage(MessageUtil.parse("<#F0F4F8>/filter toggle - вкл/выкл</#F0F4F8>"));
        sender.sendMessage(MessageUtil.parse("<#F0F4F8>/filter status - статус</#F0F4F8>"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "toggle", "status");
        }
        return Collections.emptyList();
    }
}
