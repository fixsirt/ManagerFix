package ru.managerfix.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.Optional;

/**
 * Styled main admin panel: MiniMessage title, themed module icons (status/stats/control blocks),
 * styled frame, refresh in place. Throttle 200ms in GuiManager.
 */
public final class ManagerFixMainMenu {

    private static final String PERMISSION_ADMIN = "managerfix.admin";
    private static final String PERMISSION_MODULE_ADMIN = "managerfix.module.";
    private static final int SIZE = 54;

    private static final List<ModuleEntry> MODULES = List.of(
            new ModuleEntry("afk", Material.CLOCK),
            new ModuleEntry("announcer", Material.BELL),
            new ModuleEntry("ban", Material.IRON_BARS),
            new ModuleEntry("chat", Material.WRITABLE_BOOK),
            new ModuleEntry("homes", Material.RED_BED),
            new ModuleEntry("kits", Material.CHEST),
            new ModuleEntry("names", Material.PAPER),
            new ModuleEntry("other", Material.NETHER_STAR),
            new ModuleEntry("rtp", Material.COMPASS),
            new ModuleEntry("spawn", Material.OAK_SAPLING),
            new ModuleEntry("tab", Material.NAME_TAG),
            new ModuleEntry("tpa", Material.PLAYER_HEAD),
            new ModuleEntry("warps", Material.ENDER_PEARL),
            new ModuleEntry("worlds", Material.GRASS_BLOCK)
    );

    private static final int[] MODULE_SLOTS = GuiTemplate.getContentSlots54();
    private static final int SLOT_RELOAD_ALL = 45;
    private static final int SLOT_DEBUG = 47;
    private static final int SLOT_STORAGE = 49;
    private static final int SLOT_CLUSTER = 51;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public ManagerFixMainMenu(ManagerFix plugin, GuiManager guiManager,
                              UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player player) {
        if (!player.hasPermission(PERMISSION_ADMIN)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }

        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(theme.titleMainPanel())
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("managerfix_panel");

        for (int i = 0; i < MODULES.size() && i < MODULE_SLOTS.length; i++) {
            ModuleEntry entry = MODULES.get(i);
            int slot = MODULE_SLOTS[i];
            boolean enabled = plugin.getModuleManager().getEnabledModule(entry.id).isPresent();
            String stats = plugin.getModuleManager().getModuleStats(entry.id);
            Optional<String> reasonOpt = plugin.getModuleManager().getDependencyFailureReason(entry.id);
            String reason = reasonOpt.orElse(null);
            ItemStack icon = buildModuleIcon(entry, enabled, stats, reason);
            String moduleId = entry.id;
            boolean finalEnabled = enabled;
            Button btn = Button.builder(icon)
                    .onClick(e -> {
                        if (finalEnabled) openModuleMenu(player, moduleId);
                        else toggleModule(player, moduleId);
                    })
                    .onRightClick(e -> openModuleSettings(player, moduleId))
                    .onShiftRightClick(e -> reloadModule(player, moduleId))
                    .build();
            builder.button(slot, btn);
        }

        builder.button(SLOT_RELOAD_ALL, Button.builder(
                new ItemBuilder(Material.REDSTONE)
                        .name(MessageUtil.parse(UIThemeManager.GRADIENT_ERROR + "Reload All</gradient>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "Перезагрузить конфиг и все модули</#e9d5ff>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> {
            plugin.reload();
            guiManager.sendConfirmation(player, "Перезагрузка выполнена");
            open(player);
        }).build());

        boolean debugOn = plugin.getDebugManager() != null && plugin.getDebugManager().isDebug();
        builder.button(SLOT_DEBUG, Button.builder(
                new ItemBuilder(debugOn ? Material.LIME_DYE : Material.GRAY_DYE)
                        .name(MessageUtil.parse(debugOn ? UIThemeManager.GRADIENT_SUCCESS + "Debug: ON</gradient>" : UIThemeManager.COLOR_INFO + "Debug: OFF</#e9d5ff>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — переключить отладочный вывод</#e9d5ff>"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> {
            if (plugin.getDebugManager() != null) {
                plugin.getDebugManager().setDebugEnabled(!plugin.getDebugManager().isDebug());
                guiManager.sendConfirmation(player, "Debug " + (plugin.getDebugManager().isDebug() ? "вкл" : "выкл"));
                refreshContent(player);
            }
        }).build());

        String storageType = plugin.getConfigManager().getStorageType();
        builder.button(SLOT_STORAGE, Button.builder(
                new ItemBuilder(Material.BOOK)
                        .name(MessageUtil.parse(UIThemeManager.COLOR_WARNING + "Storage</#fbbf24>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + storageType + "</#e9d5ff>"))
                        .hideFlags(true)
                        .build()
        ).build());

        boolean clusterOn = plugin.getConfigManager().isClusterEnabled();
        builder.button(SLOT_CLUSTER, Button.builder(
                new ItemBuilder(clusterOn ? Material.EMERALD : Material.BARRIER)
                        .name(MessageUtil.parse(clusterOn ? UIThemeManager.GRADIENT_SUCCESS + "Cluster: ON</gradient>" : UIThemeManager.GRADIENT_ERROR + "Cluster: OFF</gradient>"))
                        .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "ID: " + plugin.getConfigManager().getServerId() + "</#e9d5ff>"))
                        .hideFlags(true)
                        .build()
        ).build());

        guiManager.open(player, builder.build());
    }

    /** Updates current panel inventory in place (no close). */
    public void refreshContent(Player player) {
        if (player == null || !player.isOnline()) return;
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof GuiHolder h) || !"managerfix_panel".equals(h.getId())) return;

        template.applyStyledFrame(inv, SIZE);
        for (int i = 0; i < MODULES.size() && i < MODULE_SLOTS.length; i++) {
            ModuleEntry entry = MODULES.get(i);
            int slot = MODULE_SLOTS[i];
            boolean enabled = plugin.getModuleManager().getEnabledModule(entry.id).isPresent();
            String stats = plugin.getModuleManager().getModuleStats(entry.id);
            Optional<String> reasonOpt = plugin.getModuleManager().getDependencyFailureReason(entry.id);
            inv.setItem(slot, buildModuleIcon(entry, enabled, stats, reasonOpt.orElse(null)));
        }
        boolean debugOn = plugin.getDebugManager() != null && plugin.getDebugManager().isDebug();
        inv.setItem(SLOT_DEBUG, new ItemBuilder(debugOn ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(MessageUtil.parse(debugOn ? UIThemeManager.GRADIENT_SUCCESS + "Debug: ON</gradient>" : UIThemeManager.COLOR_INFO + "Debug: OFF</#e9d5ff>"))
                .addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — переключить отладочный вывод</#e9d5ff>"))
                .hideFlags(true)
                .build());
    }

    private ItemStack buildModuleIcon(ModuleEntry entry, boolean enabled, String stats, String dependencyReason) {
        String nameKey = "menu.modules." + entry.id;
        String nameRaw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), nameKey);
        String displayName = nameRaw != null ? nameRaw : entry.id;

        Material mat = enabled ? entry.material : Material.RED_STAINED_GLASS_PANE;
        String nameFormat;
        if (dependencyReason != null && !enabled) {
            nameFormat = UIThemeManager.GRADIENT_ERROR + displayName + "</gradient>";
        } else if (enabled) {
            nameFormat = UIThemeManager.GRADIENT_ACCENT + displayName + "</gradient>";
        } else {
            nameFormat = UIThemeManager.COLOR_ERROR + displayName + "</#f87171>";
        }

        List<Component> lore = theme.loreFromBlocks(
                theme.blockStatus(enabled),
                theme.blockStats(stats),
                theme.blockControl()
        );
        if (dependencyReason != null && !enabled) {
            lore.add(MessageUtil.parse(theme.loreSpacer()));
            for (String line : theme.dependencyMissing(dependencyReason).split("\n")) {
                lore.add(MessageUtil.parse(line));
            }
        }

        return new ItemBuilder(mat)
                .name(MessageUtil.parse(nameFormat))
                .lore(lore)
                .hideFlags(true)
                .build();
    }

    private void toggleModule(Player player, String moduleId) {
        if (!hasModuleAdmin(player, moduleId)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        boolean currentlyEnabled = plugin.getModuleManager().getEnabledModule(moduleId).isPresent();
        plugin.getConfigManager().setModuleEnabled(moduleId, !currentlyEnabled);
        plugin.getModuleManager().reloadModule(moduleId);
        guiManager.sendConfirmation(player, currentlyEnabled ? "Модуль отключён" : "Модуль включён");
        refreshContent(player);
    }

    private void reloadModule(Player player, String moduleId) {
        if (!hasModuleAdmin(player, moduleId)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        plugin.getModuleManager().reloadModule(moduleId);
        guiManager.sendConfirmation(player, "Перезагрузка модуля");
        refreshContent(player);
    }

    private void openModuleMenu(Player player, String moduleId) {
        switch (moduleId) {
            case "warps" -> {
                var mod = plugin.getModuleManager().getEnabledModule("warps")
                    .filter(m -> m instanceof ru.managerfix.modules.warps.WarpsModule)
                    .map(m -> (ru.managerfix.modules.warps.WarpsModule) m)
                    .orElse(null);
                if (mod != null) {
                    new ru.managerfix.modules.warps.WarpGui(mod.getConfig(), null, null, mod.getDataStorage()).open(player);
                }
            }
            case "homes" -> {
                var mod = plugin.getModuleManager().getEnabledModule("homes")
                    .filter(m -> m instanceof ru.managerfix.modules.homes.HomesModule)
                    .map(m -> (ru.managerfix.modules.homes.HomesModule) m)
                    .orElse(null);
                new HomesMenuGui(plugin, guiManager, mod).open(player, 0);
            }
            case "kits" -> {
                var mod = plugin.getModuleManager().getEnabledModule("kits")
                    .filter(m -> m instanceof ru.managerfix.modules.kits.KitsModule)
                    .map(m -> (ru.managerfix.modules.kits.KitsModule) m)
                    .orElse(null);
                new KitsMenuGui(plugin, guiManager, mod).open(player, 0);
            }
            case "ban" -> {
                var banMod = plugin.getModuleManager().getEnabledModule("ban")
                    .filter(m -> m instanceof ru.managerfix.modules.ban.BanModule)
                    .map(m -> (ru.managerfix.modules.ban.BanModule) m)
                    .orElse(null);
                if (banMod != null) new BanListGui(plugin, guiManager, banMod).open(player, 0);
                else openModuleSettings(player, moduleId);
            }
            case "worlds" -> new WorldsMenuGui(plugin, guiManager).open(player, 0);
            case "names" -> {
                if (player.hasPermission("managerfix.names.admin")) {
                    var namesMod = plugin.getModuleManager().getEnabledModule("names")
                        .filter(m -> m instanceof ru.managerfix.modules.names.NamesModule)
                        .map(m -> (ru.managerfix.modules.names.NamesModule) m)
                        .orElse(null);
                    if (namesMod != null) new NamesGui(plugin, guiManager, namesMod).open(player, 0);
                    else openModuleSettings(player, moduleId);
                } else openModuleSettings(player, moduleId);
            }
            default -> openModuleSettings(player, moduleId);
        }
    }

    private void openModuleSettings(Player player, String moduleId) {
        if (!hasModuleAdmin(player, moduleId)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        new ModuleStyledMenu(plugin, guiManager, theme, template).open(player, moduleId);
    }

    private boolean hasModuleAdmin(Player player, String moduleId) {
        return player.hasPermission(PERMISSION_ADMIN)
                || player.hasPermission(PERMISSION_MODULE_ADMIN + moduleId + ".admin");
    }

    private record ModuleEntry(String id, Material material) {}
}
