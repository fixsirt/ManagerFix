package ru.managerfix.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.LoggerUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * HikariCP-based database manager for MySQL/MariaDB.
 * Creates tables on init if they do not exist.
 */
public final class DatabaseManager {

    private static final String TABLE_PROFILES = "CREATE TABLE IF NOT EXISTS profiles (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "name VARCHAR(50)," +
            "last_activity BIGINT NOT NULL," +
            "metadata TEXT," +
            "cooldowns TEXT," +
            "last_ip VARCHAR(45)," +
            "INDEX idx_last_ip (last_ip)" +
            ")";

    private static final String TABLE_WARPS = "CREATE TABLE IF NOT EXISTS warps (" +
            "name VARCHAR(50) PRIMARY KEY," +
            "world VARCHAR(50) NOT NULL," +
            "x DOUBLE NOT NULL," +
            "y DOUBLE NOT NULL," +
            "z DOUBLE NOT NULL," +
            "yaw FLOAT NOT NULL," +
            "pitch FLOAT NOT NULL," +
            "permission VARCHAR(100)," +
            "category VARCHAR(50) DEFAULT 'default'," +
            "icon VARCHAR(50) DEFAULT 'ENDER_PEARL'," +
            "slot INT DEFAULT -1," +
            "description TEXT," +
            "teleport_delay INT DEFAULT 5," +
            "enabled BOOLEAN DEFAULT TRUE," +
            "hidden BOOLEAN DEFAULT FALSE," +
            "owner VARCHAR(36)" +
            ")";

    private static final String TABLE_KITS = "CREATE TABLE IF NOT EXISTS kits (" +
            "name VARCHAR(50) PRIMARY KEY," +
            "cooldown INT NOT NULL DEFAULT 86400," +
            "permission VARCHAR(100)," +
            "items TEXT," +
            "priority INT DEFAULT 0," +
            "one_time BOOLEAN DEFAULT FALSE," +
            "icon_material VARCHAR(100)" +
            ")";

    // ALTER TABLE ... ADD COLUMN IF NOT EXISTS поддерживается только в MariaDB 10.6+
    // Для MySQL проверяем существование колонки через DatabaseMetaData
    private static final String ALTER_KITS_ADD_ICON = "ALTER TABLE kits ADD COLUMN icon_material VARCHAR(100)";

    private static final String TABLE_BANS = "CREATE TABLE IF NOT EXISTS bans (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "name VARCHAR(50) NOT NULL," +
            "reason VARCHAR(255) NOT NULL," +
            "source VARCHAR(50) NOT NULL," +
            "created_at BIGINT NOT NULL," +
            "expires_at BIGINT," +
            "ip_address VARCHAR(45)" +
            ")";

    private static final String TABLE_IP_BANS = "CREATE TABLE IF NOT EXISTS ip_bans (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "uuid VARCHAR(36) NOT NULL," +
            "ip_address VARCHAR(45) NOT NULL," +
            "reason VARCHAR(255) NOT NULL," +
            "source VARCHAR(50) NOT NULL," +
            "created_at BIGINT NOT NULL," +
            "expires_at BIGINT," +
            "INDEX idx_ip (ip_address)," +
            "INDEX idx_uuid (uuid)" +
            ")";

    private static final String TABLE_MUTES = "CREATE TABLE IF NOT EXISTS mutes (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "name VARCHAR(50) NOT NULL," +
            "reason VARCHAR(255) NOT NULL," +
            "source VARCHAR(50) NOT NULL," +
            "created_at BIGINT NOT NULL," +
            "expires_at BIGINT" +
            ")";

    private static final String TABLE_ITEMS = "CREATE TABLE IF NOT EXISTS saved_items (" +
            "name VARCHAR(50) PRIMARY KEY," +
            "data TEXT NOT NULL" +
            ")";

    private static final String TABLE_HOMES = "CREATE TABLE IF NOT EXISTS homes (" +
            "uuid VARCHAR(36) NOT NULL," +
            "name VARCHAR(50) NOT NULL," +
            "world VARCHAR(50) NOT NULL," +
            "x DOUBLE NOT NULL," +
            "y DOUBLE NOT NULL," +
            "z DOUBLE NOT NULL," +
            "yaw FLOAT NOT NULL DEFAULT 0," +
            "pitch FLOAT NOT NULL DEFAULT 0," +
            "PRIMARY KEY (uuid, name)" +
            ")";

    private static final String TABLE_BAN_HISTORY = "CREATE TABLE IF NOT EXISTS ban_history (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "uuid VARCHAR(36) NOT NULL," +
            "name VARCHAR(50) NOT NULL," +
            "action VARCHAR(20) NOT NULL," +
            "reason VARCHAR(255) NOT NULL," +
            "source VARCHAR(50) NOT NULL," +
            "created_at BIGINT NOT NULL," +
            "expires_at BIGINT," +
            "INDEX idx_uuid (uuid)" +
            ")";

    private static final String TABLE_TPA_DATA = "CREATE TABLE IF NOT EXISTS tpa_data (" +
            "uuid VARCHAR(36) PRIMARY KEY," +
            "enabled BOOLEAN NOT NULL DEFAULT TRUE" +
            ")";

    private static final String TABLE_TPA_BLACKLIST = "CREATE TABLE IF NOT EXISTS tpa_blacklist (" +
            "owner_uuid VARCHAR(36) NOT NULL," +
            "blacklisted_uuid VARCHAR(36) NOT NULL," +
            "PRIMARY KEY (owner_uuid, blacklisted_uuid)" +
            ")";

    private static final String TABLE_SPAWN = "CREATE TABLE IF NOT EXISTS spawn (" +
            "id INT PRIMARY KEY DEFAULT 1," +
            "world VARCHAR(50) NOT NULL," +
            "x DOUBLE NOT NULL," +
            "y DOUBLE NOT NULL," +
            "z DOUBLE NOT NULL," +
            "yaw FLOAT NOT NULL DEFAULT 0," +
            "pitch FLOAT NOT NULL DEFAULT 0" +
            ")";

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = config;
    }

    public void init() {
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "managerfix");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "password");
        int poolSize = config.getInt("database.pool-size", 20); // Увеличено с 10 до 20

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setMinimumIdle(Math.min(5, poolSize)); // Увеличено с 2 до 5

        // Настройка connection pooling для стабильности
        hikariConfig.setConnectionTimeout(10000); // Уменьшено с 30 до 10 секунд
        hikariConfig.setIdleTimeout(600000); // 10 минут простоя перед закрытием
        hikariConfig.setMaxLifetime(1800000); // 30 минут максимальное время жизни соединения
        hikariConfig.setConnectionInitSql("SELECT 1"); // Проверка соединения при инициализации
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("connectionTimeout", "10000");
        hikariConfig.addDataSourceProperty("validationTimeout", "5000");

        dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(TABLE_PROFILES);
            st.executeUpdate(TABLE_WARPS);
            st.executeUpdate(TABLE_KITS);
            st.executeUpdate(TABLE_BANS);
            st.executeUpdate(TABLE_IP_BANS);
            st.executeUpdate(TABLE_MUTES);
            st.executeUpdate(TABLE_ITEMS);
            st.executeUpdate(TABLE_HOMES);
            st.executeUpdate(TABLE_BAN_HISTORY);
            st.executeUpdate(TABLE_TPA_DATA);
            st.executeUpdate(TABLE_TPA_BLACKLIST);
            st.executeUpdate(TABLE_SPAWN);

            // Проверяем существование колонки перед добавлением
            addColumnIfNotExists(conn, "kits", "icon_material", "VARCHAR(100)");
            addColumnIfNotExists(conn, "profiles", "last_ip", "VARCHAR(45)");
            addColumnIfNotExists(conn, "profiles", "name", "VARCHAR(50)");
            
            // Добавляем uuid в таблицу ip_bans если нет
            addColumnIfNotExists(conn, "ip_bans", "uuid", "VARCHAR(36) NOT NULL");
            
            // Проверяем колонки в таблице warps
            addColumnIfNotExists(conn, "warps", "permission", "VARCHAR(100)");
            addColumnIfNotExists(conn, "warps", "category", "VARCHAR(50) DEFAULT 'default'");
            addColumnIfNotExists(conn, "warps", "icon", "VARCHAR(50) DEFAULT 'ENDER_PEARL'");
            addColumnIfNotExists(conn, "warps", "slot", "INT DEFAULT -1");
            addColumnIfNotExists(conn, "warps", "description", "TEXT");
            addColumnIfNotExists(conn, "warps", "teleport_delay", "INT DEFAULT 5");
            addColumnIfNotExists(conn, "warps", "enabled", "BOOLEAN DEFAULT TRUE");
            addColumnIfNotExists(conn, "warps", "hidden", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, "warps", "owner", "VARCHAR(36)");
            
            // Проверяем колонки в таблице bans
            addColumnIfNotExists(conn, "bans", "ip_address", "VARCHAR(45)");

            LoggerUtil.info("Database connected and tables verified.");
        } catch (SQLException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "Database init failed", e);
            throw new RuntimeException("Database init failed", e);
        }
    }

    /**
     * Добавляет колонку в таблицу, если она не существует.
     * Совместимо с MySQL и MariaDB.
     */
    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnType) {
        try {
            // Проверяем существование колонки через DatabaseMetaData
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
                if (!rs.next()) {
                    // Колонка не существует, добавляем
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                        LoggerUtil.info("Added column " + columnName + " to table " + tableName);
                    }
                }
            }
        } catch (SQLException e) {
            LoggerUtil.log(java.util.logging.Level.WARNING, 
                "Failed to check/add column " + columnName + " to table " + tableName, e);
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            LoggerUtil.info("Database connection pool closed.");
        }
    }

    /**
     * Returns a connection from the pool. Caller must close it.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager is not initialized");
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Database connection pool is closed or unavailable", e);
        }
    }

    public boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }
}
