package ru.managerfix.api.chat;

import java.util.Date;

/**
 * Информация о муте.
 */
public class MuteInfo {
    
    private final String reason;
    private final String source;
    private final long createdAt;
    private final long expiresAt;
    
    public MuteInfo(String reason, String source, long createdAt, long expiresAt) {
        this.reason = reason;
        this.source = source;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Причина мута.
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Кто замутил.
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Время создания мута.
     */
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Время истечения мута.
     */
    public long getExpiresAt() {
        return expiresAt;
    }
    
    /**
     * Является ли мут перманентным.
     */
    public boolean isPermanent() {
        return expiresAt <= 0;
    }
    
    /**
     * Получить время истечения в читаемом формате.
     */
    public String getFormattedExpiresAt() {
        if (isPermanent()) {
            return "Навсегда";
        }
        return new Date(expiresAt).toString();
    }
}
