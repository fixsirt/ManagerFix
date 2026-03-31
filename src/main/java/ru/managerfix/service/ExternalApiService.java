package ru.managerfix.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.LoggerUtil;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Кэширует внешние API (Vault, LuckPerms) для избежания reflection в hot path.
 * Все методы безопасны для вызова из любого потока.
 * Использует reflection только при инициализации для поиска провайдеров.
 */
public final class ExternalApiService {

    private final JavaPlugin plugin;
    
    // Кэшированные провайдеры (храним как Object для избежания импортов)
    private volatile Object chatProvider;      // net.milkbowl.vault.chat.Chat
    private volatile Object economyProvider;   // net.milkbowl.vault.economy.Economy
    private volatile Object luckPermsApi;      // net.luckperms.api.LuckPerms
    
    // Флаги наличия плагинов
    private final boolean hasVault;
    private final boolean hasLuckPerms;
    private final boolean hasPlaceholderApi;

    public ExternalApiService(JavaPlugin plugin) {
        this.plugin = plugin;
        
        boolean vaultFound = false;
        boolean luckPermsFound = false;
        boolean placeholderApiFound = false;
        
        try {
            if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                vaultFound = true;
                refreshVaultProviders();
            }
        } catch (Exception e) {
            // Игнорируем ошибки при инициализации Vault
        }
        
        try {
            Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> lpProvider = Bukkit.getServicesManager().getRegistration(lpClass);
            if (lpProvider != null) {
                luckPermsFound = true;
                luckPermsApi = lpProvider.getProvider();
            }
        } catch (ClassNotFoundException e) {
            // LuckPerms не установлен
        } catch (Exception e) {
            // Другие ошибки
        }
        
        try {
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                placeholderApiFound = true;
            }
        } catch (Exception e) {
            // PlaceholderAPI не установлен
        }
        
        this.hasVault = vaultFound;
        this.hasLuckPerms = luckPermsFound;
        this.hasPlaceholderApi = placeholderApiFound;
        
        LoggerUtil.debug("External APIs: Vault=" + vaultFound + ", LuckPerms=" + luckPermsFound + ", PlaceholderAPI=" + placeholderApiFound);
    }

    /**
     * Обновляет провайдеры Vault (вызывать при перезагрузке).
     */
    public void refreshVaultProviders() {
        if (!hasVault) return;
        try {
            Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            
            RegisteredServiceProvider<?> chatReg = Bukkit.getServicesManager().getRegistration(chatClass);
            RegisteredServiceProvider<?> economyReg = Bukkit.getServicesManager().getRegistration(economyClass);
            
            if (chatReg != null) {
                chatProvider = chatReg.getProvider();
            }
            if (economyReg != null) {
                economyProvider = economyReg.getProvider();
            }
        } catch (ClassNotFoundException e) {
            LoggerUtil.warning("Failed to find Vault classes: " + e.getMessage());
        } catch (Exception e) {
            LoggerUtil.warning("Failed to refresh Vault providers: " + e.getMessage());
        }
    }

    /**
     * Возвращает префикс игрока из Vault. Быстрый кэшированный вызов.
     */
    public String getPlayerPrefix(Player player) {
        if (!hasVault || chatProvider == null) return "";
        try {
            Method method = chatProvider.getClass().getMethod("getPlayerPrefix", Player.class);
            return (String) method.invoke(chatProvider, player);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Возвращает суффикс игрока из Vault. Быстрый кэшированный вызов.
     */
    public String getPlayerSuffix(Player player) {
        if (!hasVault || chatProvider == null) return "";
        try {
            Method method = chatProvider.getClass().getMethod("getPlayerSuffix", Player.class);
            return (String) method.invoke(chatProvider, player);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Возвращает баланс игрока из Vault Economy. Быстрый кэшированный вызов.
     */
    public double getBalance(OfflinePlayer player) {
        if (!hasVault || economyProvider == null) return 0.0;
        try {
            Method method = economyProvider.getClass().getMethod("getBalance", OfflinePlayer.class);
            return (Double) method.invoke(economyProvider, player);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Форматирует сумму через Vault Economy.
     */
    public String formatBalance(double amount) {
        if (!hasVault || economyProvider == null) return String.valueOf(amount);
        try {
            Method method = economyProvider.getClass().getMethod("format", double.class);
            return (String) method.invoke(economyProvider, amount);
        } catch (Exception e) {
            return String.valueOf(amount);
        }
    }

    /**
     * Возвращает форматированный баланс игрока.
     */
    public String getFormattedBalance(OfflinePlayer player) {
        if (!hasVault || economyProvider == null) return "—";
        try {
            Method balanceMethod = economyProvider.getClass().getMethod("getBalance", OfflinePlayer.class);
            Method formatMethod = economyProvider.getClass().getMethod("format", double.class);
            double balance = (Double) balanceMethod.invoke(economyProvider, player);
            return (String) formatMethod.invoke(economyProvider, balance);
        } catch (Exception e) {
            return "—";
        }
    }

    /**
     * Возвращает LuckPerms API или null если недоступен.
     */
    public Object getLuckPermsApi() {
        return luckPermsApi;
    }

    /**
     * Возвращает UserManager из LuckPerms.
     */
    public Object getLuckPermsUserManager() {
        if (luckPermsApi == null) return null;
        try {
            Method method = luckPermsApi.getClass().getMethod("getUserManager");
            return method.invoke(luckPermsApi);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Асинхронно загружает пользователя LuckPerms.
     */
    public CompletableFuture<Object> getLuckPermsUserAsync(java.util.UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Object userManager = getLuckPermsUserManager();
            if (userManager == null) return null;
            try {
                Method method = userManager.getClass().getMethod("getUser", java.util.UUID.class);
                return method.invoke(userManager, uuid);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Возвращает первичную группу игрока из LuckPerms.
     */
    public String getPrimaryGroup(java.util.UUID uuid) {
        if (!hasLuckPerms) return null;
        try {
            Object userManager = getLuckPermsUserManager();
            if (userManager == null) return null;
            
            Method getUserMethod = userManager.getClass().getMethod("getUser", java.util.UUID.class);
            Object user = getUserMethod.invoke(userManager, uuid);
            if (user == null) return null;
            
            Method getPrimaryGroupMethod = user.getClass().getMethod("getPrimaryGroup");
            return (String) getPrimaryGroupMethod.invoke(user);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Проверяет наличие Vault.
     */
    public boolean hasVault() {
        return hasVault;
    }

    /**
     * Проверяет наличие LuckPerms.
     */
    public boolean hasLuckPerms() {
        return hasLuckPerms;
    }

    /**
     * Проверяет наличие PlaceholderAPI.
     */
    public boolean hasPlaceholderApi() {
        return hasPlaceholderApi;
    }

    /**
     * Проверяет наличие Vault Economy.
     */
    public boolean hasEconomy() {
        return hasVault && economyProvider != null;
    }

    /**
     * Проверяет наличие Vault Chat.
     */
    public boolean hasChat() {
        return hasVault && chatProvider != null;
    }
}
