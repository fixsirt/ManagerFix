package ru.managerfix.api;

import ru.managerfix.api.chat.ChatManager;
import ru.managerfix.profile.ProfileManager;
import ru.managerfix.modules.ban.BanManager;
import ru.managerfix.modules.warps.WarpStorage;

/**
 * Главный API интерфейс ManagerFix.
 * Предоставляет доступ ко всем функциям плагина.
 */
public interface ManagerFixAPI {
    
    /**
     * Версия API.
     */
    String VERSION = "1.0.0";
    
    /**
     * Версия API (числовая для сравнения).
     */
    int API_VERSION = 1;
    
    /**
     * Получить версию API.
     */
    String getVersion();
    
    /**
     * Получить номер версии API.
     */
    int getApiVersion();
    
    /**
     * Проверить совместимость версии API.
     * @param minVersion минимальная требуемая версия
     * @return true если совместимо
     */
    boolean isCompatible(int minVersion);
    
    // ==================== Менеджеры ====================
    
    /**
     * Получить менеджер чата.
     */
    ChatManager getChatManager();
    
    /**
     * Получить менеджер профилей.
     */
    ProfileManager getProfileManager();
    
    /**
     * Получить менеджер банов.
     */
    BanManager getBanManager();
    
    /**
     * Получить хранилище варпов.
     */
    WarpStorage getWarpStorage();
    
    // ==================== Module API ====================
    
    /**
     * Проверить, включён ли модуль.
     * @param moduleName имя модуля
     * @return true если модуль включён
     */
    boolean isModuleEnabled(String moduleName);
    
    /**
     * Включить/выключить модуль.
     * @param moduleName имя модуля
     * @param enabled true для включения
     */
    void setModuleEnabled(String moduleName, boolean enabled);
    
    // ==================== Event API ====================
    
    /**
     * Зарегистрировать слушателя событий ManagerFix.
     * @param listener объект-слушатель с @EventHandler
     */
    void registerListener(Object listener);
    
    /**
     * Удалить слушателя событий.
     * @param listener объект-слушатель
     */
    void unregisterListener(Object listener);
}
