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
    private static final int CACHE_SIZE = 20000;
    private static final int PAGE_SIZE = 4096;

    private static void initializeDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setMaximumPoolSize(5);  // Réduit car SQLite ne supporte pas beaucoup de connexions simultanées
            config.setMinimumIdle(2);      // Maintient un minimum de connexions
            config.setIdleTimeout(30000);   // 30 secondes
            config.setMaxLifetime(60000);   // 1 minute
            config.setConnectionTimeout(5000); // 5 secondes
            config.setPoolName("PoissonneriePool");

            // Optimisations SQLite
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("cache_size", CACHE_SIZE);
            config.addDataSourceProperty("page_size", PAGE_SIZE);
            config.addDataSourceProperty("temp_store", "MEMORY");
            config.addDataSourceProperty("mmap_size", "30000000000");
            config.addDataSourceProperty("foreign_keys", "ON");
            config.addDataSourceProperty("busy_timeout", 5000);

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
            stmt.execute("PRAGMA temp_store = MEMORY");
            stmt.execute("PRAGMA mmap_size = 30000000000");
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            LOGGER.info("Pool de connexions fermé");
        }
    }
}