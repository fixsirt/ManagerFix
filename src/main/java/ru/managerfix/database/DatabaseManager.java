package ru.managerfix.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.managerfix.core.ConfigManager;
import ru.managerfix.core.LoggerUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Database manager supporting MySQL/MariaDB and SQLite.
 * SQLite stores data in a local file — no server required.
 * Creates tables on init if they do not exist.
 */
public final class DatabaseManager {

    private static final String TABLE_PROFILES = "CREATE TABLE IF NOT EXISTS profiles (" +
            "uuid TEXT PRIMARY KEY," +
            "name TEXT," +
            "last_activity INTEGER NOT NULL," +
            "metadata TEXT," +
            "cooldowns TEXT," +
            "last_ip TEXT" +
            ")";

    private static final String TABLE_WARPS = "CREATE TABLE IF NOT EXISTS warps (" +
            "name TEXT PRIMARY KEY," +
            "world TEXT NOT NULL," +
            "x REAL NOT NULL," +
            "y REAL NOT NULL," +
            "z REAL NOT NULL," +
            "yaw REAL NOT NULL," +
            "pitch REAL NOT NULL," +
            "permission TEXT," +
            "category TEXT DEFAULT 'default'," +
            "icon TEXT DEFAULT 'RECOVERY_COMPASS'," +
            "slot INTEGER DEFAULT -1," +
            "description TEXT," +
            "teleport_delay INTEGER DEFAULT 5," +
            "enabled INTEGER DEFAULT 1," +
            "hidden INTEGER DEFAULT 0," +
            "owner TEXT" +
            ")";

    private static final String TABLE_KITS = "CREATE TABLE IF NOT EXISTS kits (" +
            "name TEXT PRIMARY KEY," +
            "cooldown INTEGER NOT NULL DEFAULT 86400," +
            "permission TEXT," +
            "items TEXT," +
            "priority INTEGER DEFAULT 0," +
            "one_time INTEGER DEFAULT 0," +
            "icon_material TEXT" +
            ")";

    private static final String ALTER_KITS_ADD_ICON = "ALTER TABLE kits ADD COLUMN icon_material TEXT";

    private static final String TABLE_BANS = "CREATE TABLE IF NOT EXISTS bans (" +
            "uuid TEXT PRIMARY KEY," +
            "name TEXT NOT NULL," +
            "reason TEXT NOT NULL," +
            "source TEXT NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "expires_at INTEGER," +
            "ip_address TEXT" +
            ")";

    private static final String TABLE_IP_BANS = "CREATE TABLE IF NOT EXISTS ip_bans (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "ip_address TEXT NOT NULL," +
            "reason TEXT NOT NULL," +
            "source TEXT NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "expires_at INTEGER" +
            ")";

    private static final String TABLE_MUTES = "CREATE TABLE IF NOT EXISTS mutes (" +
            "uuid TEXT PRIMARY KEY," +
            "name TEXT NOT NULL," +
            "reason TEXT NOT NULL," +
            "source TEXT NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "expires_at INTEGER" +
            ")";

    private static final String TABLE_ITEMS = "CREATE TABLE IF NOT EXISTS saved_items (" +
            "name TEXT PRIMARY KEY," +
            "data TEXT NOT NULL" +
            ")";

    private static final String TABLE_HOMES = "CREATE TABLE IF NOT EXISTS homes (" +
            "uuid TEXT NOT NULL," +
            "name TEXT NOT NULL," +
            "world TEXT NOT NULL," +
            "x REAL NOT NULL," +
            "y REAL NOT NULL," +
            "z REAL NOT NULL," +
            "yaw REAL NOT NULL DEFAULT 0," +
            "pitch REAL NOT NULL DEFAULT 0," +
            "PRIMARY KEY (uuid, name)" +
            ")";

    private static final String TABLE_BAN_HISTORY = "CREATE TABLE IF NOT EXISTS ban_history (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "name TEXT NOT NULL," +
            "action TEXT NOT NULL," +
            "reason TEXT NOT NULL," +
            "source TEXT NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "expires_at INTEGER" +
            ")";

    private static final String TABLE_TPA_DATA = "CREATE TABLE IF NOT EXISTS tpa_data (" +
            "uuid TEXT PRIMARY KEY," +
            "enabled INTEGER NOT NULL DEFAULT 1" +
            ")";

    private static final String TABLE_TPA_BLACKLIST = "CREATE TABLE IF NOT EXISTS tpa_blacklist (" +
            "owner_uuid TEXT NOT NULL," +
            "blacklisted_uuid TEXT NOT NULL," +
            "PRIMARY KEY (owner_uuid, blacklisted_uuid)" +
            ")";

    private static final String TABLE_SPAWN = "CREATE TABLE IF NOT EXISTS spawn (" +
            "id INTEGER PRIMARY KEY DEFAULT 1," +
            "world TEXT NOT NULL," +
            "x REAL NOT NULL," +
            "y REAL NOT NULL," +
            "z REAL NOT NULL," +
            "yaw REAL NOT NULL DEFAULT 0," +
            "pitch REAL NOT NULL DEFAULT 0" +
            ")";

    // MySQL-specific CREATE statements (with AUTO_INCREMENT, INDEX, etc.)
    private static final String MYSQL_TABLE_IP_BANS = "CREATE TABLE IF NOT EXISTS ip_bans (" +
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

    private static final String MYSQL_TABLE_BAN_HISTORY = "CREATE TABLE IF NOT EXISTS ban_history (" +
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

    private static final String SQLITE_TABLE_IP_BANS = "CREATE TABLE IF NOT EXISTS ip_bans (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "ip_address TEXT NOT NULL," +
            "reason TEXT NOT NULL," +
            "source TEXT NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "expires_at INTEGER" +
            ")";

    private static final String SQLITE_TABLE_BAN_HISTORY = "CREATE TABLE IF NOT EXISTS ban_history (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "uuid TEXT NOT NULL," +
            "name TEXT NOT NULL," +
            "action TEXT NOT NULL," +
            "reason TEXT NOT NULL," +
            "source TEXT NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "expires_at INTEGER" +
            ")";

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final String storageType;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin, FileConfiguration config, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin);
        this.config = config;
        this.storageType = configManager.getStorageType();
    }

    public void init() {
        if ("SQLITE".equalsIgnoreCase(storageType)) {
            initSQLite();
        } else {
            initMySQL();
        }
    }

    private void initSQLite() {
        File dataFolder = plugin.getDataFolder();
        File dbFile = new File(dataFolder, "data/managerfix.db");
        dbFile.getParentFile().mkdirs();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("busy_timeout", "5000");

        dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(TABLE_PROFILES);
            st.executeUpdate(TABLE_WARPS);
            st.executeUpdate(TABLE_KITS);
            st.executeUpdate(TABLE_BANS);
            st.executeUpdate(SQLITE_TABLE_IP_BANS);
            st.executeUpdate(TABLE_MUTES);
            st.executeUpdate(TABLE_ITEMS);
            st.executeUpdate(TABLE_HOMES);
            st.executeUpdate(SQLITE_TABLE_BAN_HISTORY);
            st.executeUpdate(TABLE_TPA_DATA);
            st.executeUpdate(TABLE_TPA_BLACKLIST);
            st.executeUpdate(TABLE_SPAWN);

            addColumnIfNotExists(conn, "kits", "icon_material", "TEXT");
            addColumnIfNotExists(conn, "profiles", "last_ip", "TEXT");
            addColumnIfNotExists(conn, "profiles", "name", "TEXT");
            addColumnIfNotExists(conn, "ip_bans", "uuid", "TEXT");
            addColumnIfNotExists(conn, "warps", "permission", "TEXT");
            addColumnIfNotExists(conn, "warps", "category", "TEXT DEFAULT 'default'");
            addColumnIfNotExists(conn, "warps", "icon", "TEXT DEFAULT 'RECOVERY_COMPASS'");
            addColumnIfNotExists(conn, "warps", "slot", "INTEGER DEFAULT -1");
            addColumnIfNotExists(conn, "warps", "description", "TEXT");
            addColumnIfNotExists(conn, "warps", "teleport_delay", "INTEGER DEFAULT 5");
            addColumnIfNotExists(conn, "warps", "enabled", "INTEGER DEFAULT 1");
            addColumnIfNotExists(conn, "warps", "hidden", "INTEGER DEFAULT 0");
            addColumnIfNotExists(conn, "warps", "owner", "TEXT");
            addColumnIfNotExists(conn, "bans", "ip_address", "TEXT");

            LoggerUtil.info("SQLite database initialized: " + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "SQLite init failed", e);
            throw new RuntimeException("SQLite init failed", e);
        }
    }

    private void initMySQL() {
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "managerfix");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "password");
        int poolSize = config.getInt("database.pool-size", 20);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setMinimumIdle(Math.min(5, poolSize));
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setConnectionInitSql("SELECT 1");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("connectionTimeout", "10000");
        hikariConfig.addDataSourceProperty("validationTimeout", "5000");

        dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(TABLE_PROFILES.replace("TEXT", "VARCHAR(255)").replace("INTEGER", "BIGINT").replace("REAL", "DOUBLE"));
            st.executeUpdate(TABLE_WARPS.replace("TEXT", "VARCHAR(255)").replace("INTEGER", "INT").replace("REAL", "DOUBLE"));
            st.executeUpdate(TABLE_KITS.replace("TEXT", "VARCHAR(255)").replace("INTEGER", "INT"));
            st.executeUpdate(TABLE_BANS.replace("TEXT", "VARCHAR(255)").replace("INTEGER", "BIGINT"));
            st.executeUpdate(MYSQL_TABLE_IP_BANS);
            st.executeUpdate(TABLE_MUTES.replace("TEXT", "VARCHAR(255)").replace("INTEGER", "BIGINT"));
            st.executeUpdate(TABLE_ITEMS.replace("TEXT", "VARCHAR(5000)"));
            st.executeUpdate(TABLE_HOMES.replace("TEXT", "VARCHAR(255)").replace("REAL", "DOUBLE"));
            st.executeUpdate(MYSQL_TABLE_BAN_HISTORY);
            st.executeUpdate(TABLE_TPA_DATA.replace("INTEGER", "BIGINT"));
            st.executeUpdate(TABLE_TPA_BLACKLIST.replace("TEXT", "VARCHAR(36)"));
            st.executeUpdate(TABLE_SPAWN.replace("TEXT", "VARCHAR(255)").replace("REAL", "DOUBLE"));

            addColumnIfNotExists(conn, "kits", "icon_material", "VARCHAR(100)");
            addColumnIfNotExists(conn, "profiles", "last_ip", "VARCHAR(45)");
            addColumnIfNotExists(conn, "profiles", "name", "VARCHAR(50)");
            addColumnIfNotExists(conn, "ip_bans", "uuid", "VARCHAR(36) NOT NULL");
            addColumnIfNotExists(conn, "warps", "permission", "VARCHAR(100)");
            addColumnIfNotExists(conn, "warps", "category", "VARCHAR(50) DEFAULT 'default'");
            addColumnIfNotExists(conn, "warps", "icon", "VARCHAR(50) DEFAULT 'RECOVERY_COMPASS'");
            addColumnIfNotExists(conn, "warps", "slot", "INT DEFAULT -1");
            addColumnIfNotExists(conn, "warps", "description", "TEXT");
            addColumnIfNotExists(conn, "warps", "teleport_delay", "INT DEFAULT 5");
            addColumnIfNotExists(conn, "warps", "enabled", "BOOLEAN DEFAULT TRUE");
            addColumnIfNotExists(conn, "warps", "hidden", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, "warps", "owner", "VARCHAR(36)");
            addColumnIfNotExists(conn, "bans", "ip_address", "VARCHAR(45)");

            LoggerUtil.info("MySQL database connected and tables verified.");
        } catch (SQLException e) {
            LoggerUtil.log(java.util.logging.Level.SEVERE, "MySQL init failed", e);
            throw new RuntimeException("MySQL init failed", e);
        }
    }

    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnType) {
        try {
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
                if (!rs.next()) {
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

    public String getStorageType() {
        return storageType;
    }
}
