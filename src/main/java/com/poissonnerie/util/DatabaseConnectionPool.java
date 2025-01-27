package com.poissonnerie.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionPool.class.getName());
    private static HikariDataSource dataSource;
    private static final String DB_URL = "jdbc:sqlite:poissonnerie.db";

    private static void initializeDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            // Reduced pool size for better resource usage
            config.setMaximumPoolSize(3);
            config.setMinimumIdle(1);
            // Optimized timeouts
            config.setIdleTimeout(60000); // 1 minute
            config.setMaxLifetime(300000); // 5 minutes
            config.setConnectionTimeout(3000); // 3 seconds
            config.setPoolName("PoissonneriePool");
            config.setAutoCommit(true);

            // SQLite optimizations
            config.addDataSourceProperty("pragma.journal_mode", "WAL");
            config.addDataSourceProperty("pragma.synchronous", "NORMAL");
            config.addDataSourceProperty("pragma.foreign_keys", "ON");
            config.addDataSourceProperty("cache_size", "2000"); // Increased cache
            config.addDataSourceProperty("busy_timeout", "3000");

            dataSource = new HikariDataSource(config);
            LOGGER.info("Pool de connexions initialisé avec succès");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initializeDataSource();
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            LOGGER.info("Pool de connexions fermé");
        }
    }
}