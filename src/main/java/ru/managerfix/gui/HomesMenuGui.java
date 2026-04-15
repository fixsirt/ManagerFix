package ru.managerfix.gui;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.modules.homes.HomesModule;
import ru.managerfix.profile.PlayerProfile;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Homes GUI: styled frame, list homes. LKM — teleport, PKM — delete, Shift+PKM — rename.
 */
public final class HomesMenuGui {

    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();
    private static final int CONTENT_PER_PAGE = CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final HomesModule homesModule;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public HomesMenuGui(ManagerFix plugin, GuiManager guiManager, HomesModule homesModule) {
        this(plugin, guiManager, homesModule, plugin.getUIThemeManager(), plugin.getGuiTemplate());
    }

    public HomesMenuGui(ManagerFix plugin, GuiManager guiManager, HomesModule homesModule,
                        UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.homesModule = homesModule;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player player, int page) {
        ProfileManager profileManager = plugin.getProfileManager();
        PlayerProfile profile = profileManager.getProfile(player);
        List<String> names = new ArrayList<>(new TreeSet<>(profile.getHomeNames()));
        int totalPages = Math.max(1, (int) Math.ceil((double) names.size() / CONTENT_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * CONTENT_PER_PAGE;
        int to = Math.min(from + CONTENT_PER_PAGE, names.size());

        String titleKey = "menu.homes-title";
        String raw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), titleKey);
        String displayTitle = raw != null ? raw : "Дома";

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleMenu(displayTitle))
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("homes_" + safePage);

        for (int i = from; i < to; i++) {
            String name = names.get(i);
            int contentIndex = i - from;
            int slot = CONTENT_SLOTS[contentIndex];
            Location loc = profile.getHome(name).orElse(null);
            ItemStack icon = buildHomeIcon(name, loc);
            Button btn = Button.builder(icon)
                    .onClick(e -> teleport(player, profile, name))
                    .onRightClick(e -> deleteHome(player, profile, name))
                    .onShiftRightClick(e -> renameHome(player, profile, name, e))
                    .build();
            builder.button(slot, btn);
        }

        if (totalPages > 1) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Предыдущая страница</#F0F4F8>"))
                    .hideFlags(true)
                    .build();
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.parse(UIThemeManager.COLOR_INFO + "➜ Следующая страница</#F0F4F8>"))
                    .hideFlags(true)
                    .build();
            builder.button(PREV_SLOT, Button.builder(prev).onClick(e -> {
                if (safePage > 0) open(player, safePage - 1);
            }).build());
            builder.button(NEXT_SLOT, Button.builder(next).onClick(e -> {
                if (safePage < totalPages - 1) open(player, safePage + 1);
            }).build());
        }

        guiManager.open(player, builder.build());
    }

    private ItemStack buildHomeIcon(String name, Location loc) {
        ItemBuilder ib = new ItemBuilder(Material.LIGHT_BLUE_BED)
                .name("<#00C8FF>" + name)
                .hideFlags(true);
        if (loc != null && loc.getWorld() != null) {
            ib.addLore(MessageUtil.parse("<#F0F4F8>Мир: <#F0F4F8>" + loc.getWorld().getName()));
            ib.addLore(MessageUtil.parse("<#F0F4F8>▸ ЛКМ — телепорт"));
            ib.addLore(MessageUtil.parse("<#F0F4F8>▸ ПКМ — удалить"));
            ib.addLore(MessageUtil.parse("<#F0F4F8>▸ Shift+ПКМ — переименовать"));
        }
        return ib.build();
    }

    private void teleport(Player player, PlayerProfile profile, String name) {
        if (!player.hasPermission("managerfix.homes.teleport")) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        var locOpt = profile.getHome(name);
        if (locOpt.isEmpty()) {
            MessageUtil.send(plugin, player, "homes.not-found-name", Map.of("name", name));
            return;
        }
        int cooldownSec = homesModule.getCooldownSeconds();
        if (!player.hasPermission("managerfix.homes.bypass.cooldown") && cooldownSec > 0) {
            if (profile.hasCooldown("home")) {
                long remaining = profile.getCooldownRemaining("home");
                MessageUtil.send(plugin, player, "homes.cooldown", Map.of("seconds", String.valueOf((remaining + 999) / 1000)));
                return;
            }
            profile.setCooldown("home", cooldownSec * 1000L);
        }
        // Делегируем телепортацию TPA-сервису: та же анимация и ожидание 5 сек
        player.closeInventory();
        var tpaMod = plugin.getModuleManager().getEnabledModule("tpa")
            .filter(m -> m instanceof ru.managerfix.modules.tpa.TpaModule)
            .map(m -> (ru.managerfix.modules.tpa.TpaModule) m)
            .orElse(null);
        if (tpaMod != null && tpaMod.getTpaService() != null) {
            tpaMod.getTpaService().scheduleTeleport(player, locOpt.get(), null);
        } else {
            // Фоллбек: без задержки, если модуль TPA недоступен
            player.teleport(locOpt.get());
        }
    }

    private void deleteHome(Player player, PlayerProfile profile, String name) {
        if (!player.hasPermission("managerfix.homes.delete")) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        if (profile.removeHome(name)) {
            plugin.getProfileManager().saveProfileAsync(player.getUniqueId());
            MessageUtil.send(plugin, player, "homes.deleted", Map.of("name", name));
            open(player, 0);
        }
    }

    private void renameHome(Player player, PlayerProfile profile, String oldName, InventoryClickEvent e) {
        if (!player.hasPermission("managerfix.homes.rename")) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        // Simple rename: prompt via chat would require conversation API. For now we just send message to use /sethome <new> and /delhome <old>.
        MessageUtil.send(plugin, player, "homes.rename-hint", Map.of("name", oldName));
    }
}
