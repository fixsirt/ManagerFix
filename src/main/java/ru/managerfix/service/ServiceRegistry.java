package ru.managerfix.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal DI-lite: register and resolve services by class.
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    /**
     * Registers a service instance. Replaces existing for the same class.
     */
    public <T> void register(Class<T> type, T instance) {
        if (type == null || instance == null) return;
        services.put(type, instance);
    }

    /**
     * Returns the service for the given class, or empty.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Class<T> type) {
        if (type == null) return Optional.empty();
        Object instance = services.get(type);
        if (instance == null) {
            for (Map.Entry<Class<?>, Object> e : services.entrySet()) {
                if (type.isAssignableFrom(e.getKey())) {
                    return Optional.of((T) e.getValue());
                }
            }
            return Optional.empty();
        }
        return Optional.of((T) instance);
    }

    /**
     * Returns the service or null if not found.
     */
    public <T> T getOrNull(Class<T> type) {
        return get(type).orElse(null);
    }

    /**
     * Unregisters the service for the given class.
     */
    public void unregister(Class<?> type) {
        if (type != null) {
            services.remove(type);
        }
    }

    public void clear() {
        services.clear();
    }
}
