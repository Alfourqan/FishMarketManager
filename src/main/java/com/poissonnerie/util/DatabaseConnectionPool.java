package com.poissonnerie.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;

public class DatabaseConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionPool.class.getName());
    private static HikariDataSource dataSource;
    private static final String DB_NAME = "poissonnerie.db";
    private static final String DB_PATH = new File(System.getProperty("user.dir"), DB_NAME).getAbsolutePath();
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static void initializeDataSource() {
        if (dataSource == null) {
            LOGGER.info("Initialisation du pool de connexions avec la base de données: " + DB_PATH);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setPoolName("PoissonneriePool");

            // Configuration optimisée pour SQLite
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(5000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(600000);
            config.setAutoCommit(true);
            config.setLeakDetectionThreshold(60000);


            // Paramètres spécifiques SQLite
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.addDataSourceProperty("foreign_keys", "ON");
            config.addDataSourceProperty("cache_size", "2000");
            config.addDataSourceProperty("busy_timeout", "30000");

            try {
                dataSource = new HikariDataSource(config);
                // Vérification initiale de la connexion
                try (Connection conn = dataSource.getConnection()) {
                    LOGGER.info("Connexion à la base de données SQLite établie avec succès");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du pool de connexions", e);
                throw new RuntimeException("Impossible d'initialiser le pool de connexions", e);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }
        try {
            Connection conn = dataSource.getConnection();
            // Configurer la connexion pour chaque session
            try (var stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA busy_timeout = 30000");
            }
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'obtention d'une connexion", e);
            throw e;
        }
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                LOGGER.info("Pool de connexions fermé avec succès");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du pool", e);
            } finally {
                dataSource = null;
            }
        }
    }
}