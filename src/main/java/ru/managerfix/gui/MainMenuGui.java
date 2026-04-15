package ru.managerfix.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.managerfix.ManagerFix;
import ru.managerfix.gui.theme.GuiTemplate;
import ru.managerfix.gui.theme.UIThemeManager;
import ru.managerfix.utils.ItemBuilder;
import ru.managerfix.utils.MessageUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main ManagerFix menu: 54 slots, styled frame (as admin panel), one icon per module.
 * LMB: if enabled → open module menu; if disabled → message.
 * Shift+RMB: toggle module (managerfix.module.toggle), update config, reload module.
 */
public final class MainMenuGui {

    private static final String PERMISSION_TOGGLE = "managerfix.module.toggle";
    private static final int SIZE = 54;
    private static final AtomicInteger titleTick = new AtomicInteger(0);
    private static final List<ModuleEntry> MODULES = List.of(
            new ModuleEntry("warps", Material.ENDER_PEARL, "menu.modules.warps"),
            new ModuleEntry("homes", Material.RED_BED, "menu.modules.homes"),
            new ModuleEntry("spawn", Material.OAK_SAPLING, "menu.modules.spawn"),
            new ModuleEntry("chat", Material.WRITABLE_BOOK, "menu.modules.chat"),
            new ModuleEntry("tpa", Material.PLAYER_HEAD, "menu.modules.tpa"),
            new ModuleEntry("rtp", Material.COMPASS, "menu.modules.rtp"),
            new ModuleEntry("ban", Material.IRON_BARS, "menu.modules.ban"),
            new ModuleEntry("afk", Material.CLOCK, "menu.modules.afk"),
            new ModuleEntry("kits", Material.CHEST, "menu.modules.kits"),
            new ModuleEntry("names", Material.PAPER, "menu.modules.names"),
            new ModuleEntry("worlds", Material.GRASS_BLOCK, "menu.modules.worlds"),
            new ModuleEntry("other", Material.NETHER_STAR, "menu.modules.other"),
            new ModuleEntry("tab", Material.NAME_TAG, "menu.modules.tab"),
            new ModuleEntry("announcer", Material.BELL, "menu.modules.announcer")
    );

    private static final int[] CONTENT_SLOTS = GuiTemplate.getContentSlots54();

    private final ManagerFix plugin;
    private final GuiManager guiManager;
    private final UIThemeManager theme;
    private final GuiTemplate template;

    public MainMenuGui(ManagerFix plugin, GuiManager guiManager) {
        this(plugin, guiManager,
                plugin.getUIThemeManager(), plugin.getGuiTemplate());
    }

    public MainMenuGui(ManagerFix plugin, GuiManager guiManager,
                       UIThemeManager theme, GuiTemplate template) {
        this.plugin = plugin;
        this.guiManager = guiManager;
        this.theme = theme != null ? theme : new UIThemeManager();
        this.template = template != null ? template : new GuiTemplate(this.theme);
    }

    public void open(Player player) {
        int tick = titleTick.getAndIncrement();
        String menuTitleKey = "menu.main-title";
        String raw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), menuTitleKey);
        String displayTitle = raw != null ? raw : "ManagerFix";
        net.kyori.adventure.text.Component titleComponent = ru.managerfix.utils.AnimationUtil.animateTitle(
                new String[]{ theme.titleMenu(displayTitle) }, tick);
        GuiBuilder builder = GuiBuilder.of(SIZE)
                .title(titleComponent)
                .frame(true)
                .frameApplicator(inv -> template.applyStyledFrame(inv, SIZE))
                .holderId("managerfix_main");

        for (int i = 0; i < MODULES.size() && i < CONTENT_SLOTS.length; i++) {
            int slot = CONTENT_SLOTS[i];
            ModuleEntry entry = MODULES.get(i);
            var moduleOpt = plugin.getModuleManager().getEnabledModule(entry.id);
            boolean enabled = moduleOpt.isPresent();
            ItemStack icon = buildModuleIcon(entry, enabled, player);
            Button btn = Button.builder(icon)
                    .onClick(e -> {
                        if (enabled) {
                            openModuleMenu(player, entry.id);
                        } else {
                            MessageUtil.send(plugin, player, "module-disabled");
                        }
                    })
                    .onShiftRightClick(e -> toggleModule(player, entry.id))
                    .build();
            builder.button(slot, btn);
        }

        guiManager.open(player, builder.build());

        int animTicks = plugin.getConfigManager().getGuiAnimationTicks();
        if (animTicks > 0) {
            guiManager.scheduleUpdate(player, animTicks, () -> refreshMenu(player));
        }
    }

    private void refreshMenu(Player player) {
        if (!player.isOnline()) return;
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder h) || !"managerfix_main".equals(h.getId())) {
            return;
        }
        open(player);
    }

    private void toggleModule(Player player, String moduleId) {
        if (!player.hasPermission(PERMISSION_TOGGLE)) {
            MessageUtil.send(plugin, player, "no-permission");
            return;
        }
        boolean currentlyEnabled = plugin.getModuleManager().getEnabledModule(moduleId).isPresent();
        plugin.getConfigManager().setModuleEnabled(moduleId, !currentlyEnabled);
        plugin.getModuleManager().reloadModule(moduleId);
        MessageUtil.send(plugin, player, currentlyEnabled ? "module-disabled" : "module-enabled");
        open(player);
    }

    private ItemStack buildModuleIcon(ModuleEntry entry, boolean enabled, Player player) {
        String name = getModuleName(entry.nameKey, player);
        String nameFormat = enabled
                ? UIThemeManager.GRADIENT_ACCENT + name + "</gradient>"
                : UIThemeManager.COLOR_ERROR + name + "</#FF3366>";
        ItemBuilder ib = new ItemBuilder(entry.material)
                .name(MessageUtil.parse(nameFormat))
                .hideFlags(true);
        if (enabled) {
            ib.addLore(MessageUtil.parse(UIThemeManager.COLOR_INFO + "▸ ЛКМ — открыть</#F0F4F8>"));
        } else {
            ib.addLore(MessageUtil.parse(UIThemeManager.COLOR_ERROR + "✖ Модуль отключён</#FF3366>"));
        }
        return ib.build();
    }

    private String getModuleName(String key, Player player) {
        String raw = MessageUtil.getRaw(plugin, plugin.getConfigManager().getDefaultLanguage(), key);
        return raw != null ? raw : key;
    }

    private void openModuleMenu(Player player, String moduleId) {
        if ("warps".equals(moduleId)) {
            var mod = plugin.getModuleManager().getEnabledModule("warps")
                    .map(m -> (ru.managerfix.modules.warps.WarpsModule) m)
                    .orElse(null);
            if (mod != null) {
                new ru.managerfix.modules.warps.WarpGui(mod.getConfig(), null, null, mod.getDataStorage()).open(player);
            }
        }
        if ("homes".equals(moduleId)) {
            HomesMenuGui homesMenu = new HomesMenuGui(plugin, guiManager,
                    plugin.getModuleManager().getEnabledModule("homes")
                            .map(m -> (ru.managerfix.modules.homes.HomesModule) m)
                            .orElse(null), theme, template);
            homesMenu.open(player, 0);
        }
        if ("kits".equals(moduleId)) {
            KitsMenuGui kitsMenu = new KitsMenuGui(plugin, guiManager,
                    plugin.getModuleManager().getEnabledModule("kits")
                            .map(m -> (ru.managerfix.modules.kits.KitsModule) m)
                            .orElse(null), theme, template);
            kitsMenu.open(player, 0);
        }
        if ("worlds".equals(moduleId)) {
            WorldsMenuGui worldsMenu = new WorldsMenuGui(plugin, guiManager, theme, template);
            worldsMenu.open(player, 0);
        }
        if ("names".equals(moduleId) && player.hasPermission("managerfix.names.admin")) {
            var namesMod = plugin.getModuleManager().getEnabledModule("names")
                .filter(m -> m instanceof ru.managerfix.modules.names.NamesModule)
                .map(m -> (ru.managerfix.modules.names.NamesModule) m)
                .orElse(null);
            if (namesMod != null) new NamesGui(plugin, guiManager, namesMod).open(player, 0);
        }
    }

    private record ModuleEntry(String id, Material material, String nameKey) {
    }
}
