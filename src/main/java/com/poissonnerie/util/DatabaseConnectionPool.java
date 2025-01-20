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
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(60000);
            config.setConnectionTimeout(5000);
            config.setPoolName("PoissonneriePool");
            config.setAutoCommit(true);

            // Configuration minimale SQLite
            config.addDataSourceProperty("pragma.journal_mode", "WAL");
            config.addDataSourceProperty("pragma.synchronous", "NORMAL");
            config.addDataSourceProperty("pragma.foreign_keys", "ON");
            config.addDataSourceProperty("busy_timeout", "5000");

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