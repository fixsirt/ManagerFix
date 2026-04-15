package ru.managerfix.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Admin panel: 54 slots, gradient title, module buttons (status, stats, LMB enable, RMB settings, Shift+LMB reload).
 * Bottom: Reload All, Debug Toggle, Storage Info, Cluster Status.
 * Permission: managerfix.admin. Per-module: managerfix.module.<name>.admin for settings.
 */
public final class ManagerFixPanelGui {

    private static final String PERMISSION_ADMIN = "managerfix.admin";
    private static final String PERMISSION_MODULE_ADMIN = "managerfix.module.";
    private static final String TITLE_RAW = UIThemeManager.GRADIENT_MAIN + "ManagerFix Panel</gradient>";
    private static final int SIZE = 54;
    private static final long CLICK_THROTTLE_MS = 300;

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

    private static final int[] MODULE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int SLOT_RELOAD_ALL = 45;
    private static final int SLOT_DEBUG = 47;
    private static final int SLOT_STORAGE = 49;
    private static final int SLOT_CLUSTER = 51;

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private static final ConcurrentHashMap<java.util.UUID, AtomicLong> lastClickByPlayer = new ConcurrentHashMap<>();

    public ManagerFixPanelGui(ManagerFix plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public void open(Player player) {
        if (!player.hasPermission(PERMISSION_ADMIN)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }

        Component title = MessageUtil.parse(TITLE_RAW);
        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(title)
                .frame(true)
                .frameMaterial(Material.CYAN_STAINED_GLASS_PANE)
                .holderId("managerfix_panel");

        for (int i = 0; i < MODULES.size() && i < MODULE_SLOTS.length; i++) {
            ModuleEntry entry = MODULES.get(i);
            int slot = MODULE_SLOTS[i];
            boolean enabled = plugin.getModuleManager().getEnabledModule(entry.id).isPresent();
            String stats = plugin.getModuleManager().getModuleStats(entry.id);
            Optional<String> reasonOpt = plugin.getModuleManager().getDependencyFailureReason(entry.id);
            ItemStack icon = buildModuleIcon(entry, enabled, stats, reasonOpt.orElse(null));
            boolean finalEnabled = enabled;
            String moduleId = entry.id;
            Button btn = Button.builder(icon)
                    .onClick(e -> throttleClick(player, () -> {
                        if (finalEnabled) {
                            openModuleMenu(player, moduleId);
                        } else {
                            toggleModule(player, moduleId);
                        }
                    }))
                    .onRightClick(e -> throttleClick(player, () -> openModuleSettings(player, moduleId)))
                    .onShiftRightClick(e -> throttleClick(player, () -> reloadModule(player, moduleId)))
                    .build();
            builder.button(slot, btn);
        }

        builder.button(SLOT_RELOAD_ALL, Button.builder(
                new ItemBuilder(Material.REDSTONE)
                        .name(MessageUtil.parse("<gradient:#FF3366:#FF3366>Reload All</gradient>"))
                        .addLore(MessageUtil.parse("<#F0F4F8>Перезагрузить конфиг и все модули"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> throttleClick(player, () -> {
            plugin.reload();
            MessageUtil.send(plugin, player, "reload");
            open(player);
        })).build());

        boolean debugOn = plugin.getDebugManager() != null && plugin.getDebugManager().isDebug();
        builder.button(SLOT_DEBUG, Button.builder(
                new ItemBuilder(debugOn ? Material.LIME_DYE : Material.GRAY_DYE)
                        .name(MessageUtil.parse(debugOn ? "<#00C8FF>Debug: ON</#00C8FF>" : "<#F0F4F8>Debug: OFF</#F0F4F8>"))
                        .addLore(MessageUtil.parse("<#F0F4F8>ЛКМ — переключить отладочный вывод"))
                        .hideFlags(true)
                        .build()
        ).onClick(e -> throttleClick(player, () -> {
            if (plugin.getDebugManager() != null) {
                plugin.getDebugManager().setDebugEnabled(!plugin.getDebugManager().isDebug());
                open(player);
            }
        })).build());

        String storageType = plugin.getConfigManager().getStorageType();
        builder.button(SLOT_STORAGE, Button.builder(
                new ItemBuilder(Material.BOOK)
                        .name(MessageUtil.parse("<#00C8FF>Storage</#00C8FF>"))
                        .addLore(MessageUtil.parse("<#F0F4F8>" + storageType))
                        .hideFlags(true)
                        .build()
        ).build());

        boolean clusterOn = plugin.getConfigManager().isClusterEnabled();
        builder.button(SLOT_CLUSTER, Button.builder(
                new ItemBuilder(clusterOn ? Material.EMERALD : Material.BARRIER)
                        .name(MessageUtil.parse(clusterOn ? "<#00C8FF>Cluster: ON</#00C8FF>" : "<#FF3366>Cluster: OFF</#FF3366>"))
                        .addLore(MessageUtil.parse("<#F0F4F8>ID: " + plugin.getConfigManager().getServerId()))
                        .hideFlags(true)
                        .build()
        ).build());

        Inventory inv = builder.build();
        guiManager.open(player, inv);
    }

    private void throttleClick(Player player, Runnable action) {
        long now = System.currentTimeMillis();
        AtomicLong last = lastClickByPlayer.computeIfAbsent(player.getUniqueId(), u -> new AtomicLong(0));
        if (now - last.get() < CLICK_THROTTLE_MS) return;
        last.set(now);
        action.run();
    }

    private ItemStack buildModuleIcon(ModuleEntry entry, boolean enabled, String stats, String reason) {
        Material mat = enabled ? entry.material : Material.BARRIER;
        if (!enabled && entry.material != Material.BARRIER) mat = Material.RED_STAINED_GLASS_PANE;
        String nameKey = "menu.modules." + entry.id;
        String nameRaw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), nameKey);
        String name = nameRaw != null ? nameRaw : entry.id;
        ItemBuilder ib = new ItemBuilder(mat)
                .name(MessageUtil.parse(enabled ? "<#00C8FF>" + name + "</#00C8FF>" : "<#FF3366>" + name + "</#FF3366>"))
                .hideFlags(true);
        ib.addLore(MessageUtil.parse(enabled ? "<#F0F4F8>Статус: <#00C8FF>Enabled</#00C8FF>" : "<#F0F4F8>Статус: <#FF3366>Disabled</#FF3366>"));
        if (stats != null && !stats.equals("-")) ib.addLore(MessageUtil.parse("<#F0F4F8>" + stats));
        if (reason != null && !enabled) ib.addLore(MessageUtil.parse("<#FF3366>" + reason));
        ib.addLore(MessageUtil.parse("<#F0F4F8>ЛКМ — " + (enabled ? "меню" : "включить")));
        ib.addLore(MessageUtil.parse("<#F0F4F8>ПКМ — настройки"));
        ib.addLore(MessageUtil.parse("<#F0F4F8>Shift+ЛКМ — перезагрузка"));
        return ib.build();
    }

    private void toggleModule(Player player, String moduleId) {
        if (!hasModuleAdmin(player, moduleId)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        boolean currentlyEnabled = plugin.getModuleManager().getEnabledModule(moduleId).isPresent();
        plugin.getConfigManager().setModuleEnabled(moduleId, !currentlyEnabled);
        plugin.getModuleManager().reloadModule(moduleId);
        MessageUtil.send(plugin, player, currentlyEnabled ? "module-disabled" : "module-enabled");
        open(player);
    }

    private void reloadModule(Player player, String moduleId) {
        if (!hasModuleAdmin(player, moduleId)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        plugin.getModuleManager().reloadModule(moduleId);
        MessageUtil.send(plugin, player, "reload");
        open(player);
    }

    private void openModuleMenu(Player player, String moduleId) {
        if ("warps".equals(moduleId)) {
            var mod = plugin.getModuleManager().getEnabledModule("warps")
                .filter(m -> m instanceof ru.managerfix.modules.warps.WarpsModule)
                .map(m -> (ru.managerfix.modules.warps.WarpsModule) m)
                .orElse(null);
            if (mod != null) {
                new ru.managerfix.modules.warps.WarpGui(mod.getConfig(), null, null, mod.getDataStorage()).open(player);
            }
            return;
        }
        if ("homes".equals(moduleId)) {
            var mod = plugin.getModuleManager().getEnabledModule("homes")
                .filter(m -> m instanceof ru.managerfix.modules.homes.HomesModule)
                .map(m -> (ru.managerfix.modules.homes.HomesModule) m)
                .orElse(null);
            new HomesMenuGui(plugin, guiManager, mod).open(player, 0);
            return;
        }
        if ("kits".equals(moduleId)) {
            var mod = plugin.getModuleManager().getEnabledModule("kits")
                .filter(m -> m instanceof ru.managerfix.modules.kits.KitsModule)
                .map(m -> (ru.managerfix.modules.kits.KitsModule) m)
                .orElse(null);
            new KitsMenuGui(plugin, guiManager, mod).open(player, 0);
            return;
        }
        if ("ban".equals(moduleId)) {
            var banMod = plugin.getModuleManager().getEnabledModule("ban")
                .filter(m -> m instanceof ru.managerfix.modules.ban.BanModule)
                .map(m -> (ru.managerfix.modules.ban.BanModule) m)
                .orElse(null);
            if (banMod != null) {
                new BanListGui(plugin, guiManager, banMod).open(player, 0);
            } else {
                openModuleSettings(player, moduleId);
            }
            return;
        }
        if ("worlds".equals(moduleId)) {
            new WorldsMenuGui(plugin, guiManager).open(player, 0);
            return;
        }
        if ("names".equals(moduleId) && hasModuleAdmin(player, "names")) {
            var namesMod = plugin.getModuleManager().getEnabledModule("names")
                .filter(m -> m instanceof ru.managerfix.modules.names.NamesModule)
                .map(m -> (ru.managerfix.modules.names.NamesModule) m)
                .orElse(null);
            if (namesMod != null) new NamesGui(plugin, guiManager, namesMod).open(player, 0);
            else openModuleSettings(player, moduleId);
            return;
        }
        openModuleSettings(player, moduleId);
    }

    private void openModuleSettings(Player player, String moduleId) {
        if (!hasModuleAdmin(player, moduleId)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        new ModuleSettingsGui(plugin, guiManager).open(player, moduleId);
    }

    private boolean hasModuleAdmin(Player player, String moduleId) {
        return player.hasPermission(PERMISSION_ADMIN)
                || player.hasPermission(PERMISSION_MODULE_ADMIN + moduleId + ".admin");
    }

    private record ModuleEntry(String id, Material material) {}
}
