package com.poissonnerie.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionPool.class.getName());
    private static HikariDataSource dataSource;
    private static final String DB_URL = "jdbc:sqlite:poissonnerie.db";
    private static final int CACHE_SIZE = 2000;
    private static final int PAGE_SIZE = 4096;

    private static void initializeDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setIdleTimeout(300000); // 5 minutes
            config.setConnectionTimeout(10000); // 10 secondes
            config.setMaxLifetime(1800000); // 30 minutes

            // Propriétés spécifiques SQLite pour optimisation
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("cache_size", CACHE_SIZE);
            config.addDataSourceProperty("page_size", PAGE_SIZE);
            config.addDataSourceProperty("foreign_keys", "ON");
            config.addDataSourceProperty("busy_timeout", 30000);

            dataSource = new HikariDataSource(config);
            LOGGER.info("Pool de connexions initialisé avec succès");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initializeDataSource();
        }
        Connection conn = dataSource.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Configuration au niveau de la connexion
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = " + CACHE_SIZE);
            stmt.execute("PRAGMA page_size = " + PAGE_SIZE);
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = 30000");
        }
        return conn;
    }

    public static void closePool() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            LOGGER.info("Pool de connexions fermé");
        }
    }
}