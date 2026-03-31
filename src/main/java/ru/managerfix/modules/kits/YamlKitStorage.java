package ru.managerfix.modules.kits;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.FileManager;
import ru.managerfix.core.LoggerUtil;

import java.io.File;
import java.util.*;

/**
 * YAML-based kit storage: data/kits.yml. Kits cached in memory by KitManager.
 */
public final class YamlKitStorage implements KitStorage {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    public YamlKitStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        FileManager fm = plugin instanceof ru.managerfix.ManagerFix
                ? ((ru.managerfix.ManagerFix) plugin).getFileManager()
                : null;
        if (fm != null) {
            configFile = new File(fm.getDataFolderStorage(), "kits.yml");
        } else {
            configFile = new File(plugin.getDataFolder(), "data/kits.yml");
        }
        configFile.getParentFile().mkdirs();
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.WARNING, "Could not create kits.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public void shutdown() {
        config = null;
        configFile = null;
    }

    @Override
    public Optional<KitData> loadKit(String name) {
        if (config == null || name == null) return Optional.empty();
        String path = "kits." + name;
        if (!config.contains(path)) return Optional.empty();
        int cooldown = config.getInt(path + ".cooldown", 86400);
        String permission = config.getString(path + ".permission", "managerfix.kits.kit." + name.toLowerCase());
        String iconMaterial = config.getString(path + ".icon-material");
        List<?> list = config.getList(path + ".items");
        List<ItemStack> items = new ArrayList<>();
        if (list != null) {
            for (Object o : list) {
                if (o instanceof Map) {
                    try {
                        @SuppressWarnings("unchecked")
                        ItemStack stack = ItemStack.deserialize((Map<String, Object>) o);
                        if (stack != null && !stack.getType().isAir()) {
                            items.add(stack);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        KitData kit = new KitData(name, cooldown, permission, items);
        if (iconMaterial != null) {
            kit.setIconMaterial(iconMaterial);
        }
        return Optional.of(kit);
    }

    @Override
    public void saveKit(KitData kit) {
        if (config == null || kit == null) return;
        String path = "kits." + kit.getName();
        config.set(path + ".cooldown", kit.getCooldownSeconds());
        config.set(path + ".permission", kit.getPermission());
        config.set(path + ".priority", kit.getPriority());
        config.set(path + ".one_time", kit.isOneTime());
        if (kit.getIconMaterial() != null) {
            config.set(path + ".icon-material", kit.getIconMaterial());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (ItemStack stack : kit.getItems()) {
            if (stack != null && !stack.getType().isAir()) {
                list.add(stack.serialize());
            }
        }
        config.set(path + ".items", list);
        try {
            config.save(configFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save kits.yml", e);
        }
    }

    @Override
    public void deleteKit(String name) {
        if (config == null || name == null) return;
        config.set("kits." + name, null);
        try {
            config.save(configFile);
        } catch (Exception e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Could not save kits.yml", e);
        }
    }

    @Override
    public List<String> listKitNames() {
        if (config == null || !config.contains("kits")) return Collections.emptyList();
        ConfigurationSection section = config.getConfigurationSection("kits");
        return section != null ? new ArrayList<>(section.getKeys(false)) : Collections.emptyList();
    }
}
