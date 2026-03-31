package ru.managerfix.core;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Менеджер отладки и метрик производительности.
 * Собирает статистику по времени выполнения операций.
 */
public final class DebugManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    /** Runtime override: if non-null, overrides config. */
    private volatile Boolean debugOverride;
    
    // Метрики производительности
    private final Map<String, Metric> metrics = new ConcurrentHashMap<>();

    public DebugManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean isDebug() {
        return debugOverride != null ? debugOverride : configManager.isDebug();
    }

    /** Toggle debug at runtime (does not save to config). */
    public void setDebugEnabled(boolean enabled) {
        this.debugOverride = enabled;
    }

    /** Clear runtime override so isDebug() uses config again. */
    public void clearDebugOverride() {
        this.debugOverride = null;
    }

    public void logModuleLoad(String moduleName, long nanos) {
        if (!isDebug()) return;
        LoggerUtil.info("[DEBUG] Module loaded: " + moduleName + " in " + (nanos / 1_000_000) + " ms");
    }

    public void logCommandExecution(String command, long nanos) {
        if (!isDebug()) return;
        LoggerUtil.info("[DEBUG] Command " + command + " executed in " + (nanos / 1_000_000) + " ms");
    }

    public void logAsyncOp(String operation, long nanos) {
        if (!isDebug()) return;
        LoggerUtil.info("[DEBUG] Async " + operation + " took " + (nanos / 1_000_000) + " ms");
    }

    public void log(String message) {
        if (!isDebug()) return;
        LoggerUtil.info("[DEBUG] " + message);
    }

    public void logEvent(String eventName) {
        if (!isDebug()) return;
        LoggerUtil.info("[DEBUG] Event: " + eventName);
    }

    public void logDependencyResolution(String moduleName, boolean satisfied, String missingDeps) {
        if (!isDebug()) return;
        LoggerUtil.info("[DEBUG] Dependencies for " + moduleName + ": " + (satisfied ? "OK" : "missing " + missingDeps));
    }

    // ==================== Метрики производительности ====================

    /**
     * Записывает метрику времени выполнения.
     */
    public void recordMetric(String name, long nanos) {
        if (!isDebug()) return;
        metrics.computeIfAbsent(name, k -> new Metric()).record(nanos);
    }

    /**
     * Записывает метрику с тегами (например, для разных команд).
     */
    public void recordMetric(String name, String tag, long nanos) {
        if (!isDebug()) return;
        recordMetric(name + "." + tag, nanos);
    }

    /**
     * Возвращает отчёт по метрикам в формате "name: avg ms (count calls)".
     */
    public String getMetricsReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performance Metrics ===\n");
        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
            Metric m = entry.getValue();
            double avgMs = m.count() > 0 ? (m.totalNanos() / m.count()) / 1_000_000.0 : 0;
            sb.append(entry.getKey())
              .append(": avg ")
              .append(String.format("%.2f", avgMs))
              .append(" ms (")
              .append(m.count())
              .append(" calls)\n");
        }
        return sb.toString();
    }

    /**
     * Сбрасывает все метрики.
     */
    public void resetMetrics() {
        metrics.clear();
    }

    /**
     * Возвращает среднее время выполнения операции в мс.
     */
    public double getAverageMs(String name) {
        Metric m = metrics.get(name);
        return m != null && m.count() > 0 ? (m.totalNanos() / m.count()) / 1_000_000.0 : 0.0;
    }

    /**
     * Возвращает количество вызовов операции.
     */
    public long getCount(String name) {
        Metric m = metrics.get(name);
        return m != null ? m.count() : 0;
    }

    /**
     * Простая потокобезопасная метрика с использованием LongAdder.
     */
    private static final class Metric {
        private final LongAdder count = new LongAdder();
        private final AtomicLong totalNanos = new AtomicLong(0);

        public void record(long nanos) {
            count.increment();
            totalNanos.addAndGet(nanos);
        }

        public long count() {
            return count.sum();
        }

        public long totalNanos() {
            return totalNanos.get();
        }
    }
}
