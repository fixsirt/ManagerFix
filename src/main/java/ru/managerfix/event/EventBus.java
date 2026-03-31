package ru.managerfix.event;

import ru.managerfix.core.LoggerUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Оптимизированный EventBus с использованием MethodHandle вместо reflection.
 * MethodHandle даёт производительность близкую к прямому вызову метода.
 * 
 * Потокобезопасность:
 * - listeners — CopyOnWriteArrayList для безопасной итерации
 * - allInvokers — снимок на момент вызова для консистентности
 */
public final class EventBus {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final List<Object> listeners = new CopyOnWriteArrayList<>();
    private volatile List<HandlerInvoker> allInvokers = Collections.emptyList();
    private final Object invokersLock = new Object();
    private final boolean debug;

    public EventBus(boolean debug) {
        this.debug = debug;
    }

    public void registerListener(Object listener) {
        if (listener == null) return;
        listeners.add(listener);
        invalidateInvokers();
    }

    public void unregisterListener(Object listener) {
        if (listener == null) return;
        listeners.remove(listener);
        invalidateInvokers();
    }

    /**
     * Dispatches the event synchronously to all registered handlers.
     * Использует снимок invokers на момент вызова для потокобезопасности.
     */
    public void callEvent(ManagerFixEvent event) {
        if (event == null) return;
        if (debug) {
            LoggerUtil.debug("[DEBUG] Event: " + event.getClass().getSimpleName());
        }
        Class<?> eventClass = event.getClass();
        
        // Снимок invokers на момент вызова для консистентности
        List<HandlerInvoker> snapshot;
        synchronized (invokersLock) {
            snapshot = new ArrayList<>(allInvokers);
        }
        
        for (HandlerInvoker invoker : snapshot) {
            if (!invoker.eventType.isAssignableFrom(eventClass)) continue;
            try {
                if (event instanceof CancellableManagerFixEvent c && c.isCancelled()) {
                    break;
                }
                invoker.invoke(event);
            } catch (Exception e) {
                LoggerUtil.log(java.util.logging.Level.WARNING, "Event handler error for " + event.getClass().getSimpleName(), e);
            }
        }
    }

    private void invalidateInvokers() {
        synchronized (invokersLock) {
            allInvokers = buildAllInvokers();
        }
    }

    private List<HandlerInvoker> buildAllInvokers() {
        List<HandlerInvoker> list = new ArrayList<>(listeners.size() * 2);
        for (Object listener : listeners) {
            for (Method m : listener.getClass().getDeclaredMethods()) {
                MFEventHandler ann = m.getAnnotation(MFEventHandler.class);
                if (ann == null) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> param = m.getParameterTypes()[0];
                if (!ManagerFixEvent.class.isAssignableFrom(param)) continue;
                
                // Создаём MethodHandle для быстрой инвокации
                MethodHandle handle = createHandle(m);
                if (handle != null) {
                    list.add(new HandlerInvoker(listener, handle, ann.priority(), param));
                }
            }
        }
        list.sort(Comparator.comparingInt(HandlerInvoker::priorityOrder));
        return list;
    }

    private MethodHandle createHandle(Method method) {
        try {
            method.setAccessible(true);
            // Unreflect создаёт MethodHandle с минимальными накладными расходами
            return LOOKUP.unreflect(method);
        } catch (IllegalAccessException e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, "Failed to create MethodHandle for " + method, e);
            return null;
        }
    }

    private static final class HandlerInvoker {
        private final Object target;
        private final MethodHandle handle;
        private final EventPriority priority;
        private final Class<?> eventType;

        HandlerInvoker(Object target, MethodHandle handle, EventPriority priority, Class<?> eventType) {
            this.target = target;
            this.handle = handle;
            this.priority = priority;
            this.eventType = eventType;
        }

        int priorityOrder() {
            return priority.getOrder();
        }

        void invoke(ManagerFixEvent event) {
            try {
                // MethodHandle.invoke может выбросить Throwable
                handle.invoke(target, event);
            } catch (Throwable e) {
                // Оборачиваем в Exception для совместимости
                throw new RuntimeException("Failed to invoke event handler", e);
            }
        }
    }
}
