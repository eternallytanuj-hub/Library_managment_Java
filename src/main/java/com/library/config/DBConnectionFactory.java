package com.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Thread-safe Singleton Connection Factory managing the application's HikariCP DataSource.
 * Provides connections to DAOs, automatically bootstraps the database on first acquisition,
 * and exposes lifecycle cleanup hooks.
 */
public final class DBConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(DBConnectionFactory.class.getName());

    private static volatile HikariDataSource dataSource = null;
    private static final Object LOCK = new Object();

    private DBConnectionFactory() {
        // Singleton pattern: prevent external instantiation
    }

    /**
     * Retrieves an active connection from the HikariCP pool.
     * Initializes the pool and triggers DDL/DML bootstrap on first invocation.
     *
     * @return pooled {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            synchronized (LOCK) {
                if (dataSource == null || dataSource.isClosed()) {
                    initDataSource();
                }
            }
        }
        return dataSource.getConnection();
    }

    /**
     * Exposes the underlying {@link DataSource} for testing or container integration.
     */
    public static DataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            synchronized (LOCK) {
                if (dataSource == null || dataSource.isClosed()) {
                    initDataSource();
                }
            }
        }
        return dataSource;
    }

    /**
     * Builds and starts the HikariDataSource using properties from {@link AppConfig}.
     */
    private static void initDataSource() {
        LOGGER.info("Initializing HikariCP DataSource...");

        String driver = AppConfig.getProperty("jdbc.driver", "org.h2.Driver");
        String url = AppConfig.getProperty("jdbc.url",
                "jdbc:h2:./data/librarydb;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        String user = AppConfig.getProperty("jdbc.user", "sa");
        String pass = AppConfig.getProperty("jdbc.password", "");

        HikariConfig config = new HikariConfig();
        config.setPoolName(AppConfig.getProperty("hikari.poolName", "LibraryHikariPool"));
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);

        config.setMinimumIdle(AppConfig.getInt("hikari.minimumIdle", 5));
        config.setMaximumPoolSize(AppConfig.getInt("hikari.maximumPoolSize", 20));
        config.setConnectionTimeout(AppConfig.getInt("hikari.connectionTimeout", 30000));
        config.setIdleTimeout(AppConfig.getInt("hikari.idleTimeout", 600000));
        config.setMaxLifetime(AppConfig.getInt("hikari.maxLifetime", 1800000));

        dataSource = new HikariDataSource(config);
        LOGGER.info("HikariCP pool initialized successfully with URL: " + url);

        // Run automatic bootstrap DDL/DML if tables absent
        try {
            DBInitializer.initialize(dataSource);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database bootstrap initialization failed!", e);
            throw new RuntimeException("Failed to bootstrap database schema and seed data", e);
        }
    }

    /**
     * Safely closes the connection pool. Called during web application shutdown or test teardown.
     */
    public static void closePool() {
        synchronized (LOCK) {
            if (dataSource != null && !dataSource.isClosed()) {
                LOGGER.info("Closing HikariCP connection pool...");
                dataSource.close();
                dataSource = null;
                LOGGER.info("HikariCP connection pool closed cleanly.");
            }
        }
    }

    /**
     * Programmatically reconfigures the connection pool for testing (e.g. In-Memory H2).
     * Closes the active pool and starts a fresh instance with custom parameters.
     */
    public static void reset(String jdbcUrl, String username, String password, String driverClass) {
        synchronized (LOCK) {
            closePool();
            HikariConfig config = new HikariConfig();
            config.setPoolName("TestHikariPool");
            config.setDriverClassName(driverClass);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMinimumIdle(2);
            config.setMaximumPoolSize(10);

            dataSource = new HikariDataSource(config);
            try {
                DBInitializer.initialize(dataSource);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize test database", e);
            }
        }
    }
}
