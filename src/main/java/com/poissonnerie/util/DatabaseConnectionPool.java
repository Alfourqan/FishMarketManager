package com.poissonnerie.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionPool.class.getName());
    private static volatile HikariDataSource dataSource;
    private static final Object LOCK = new Object();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500;
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final String DB_FILE = "poissonnerie.db";

    private static void initializeDataSource() {
        if (dataSource == null) {
            synchronized (LOCK) {
                if (dataSource == null) {
                    LOGGER.info("Initialisation du pool de connexions à la base de données SQLite");

                    try {
                        HikariConfig config = new HikariConfig();
                        config.setJdbcUrl("jdbc:sqlite:" + DB_FILE);
                        config.setDriverClassName("org.sqlite.JDBC");
                        config.setPoolName("PoissonnerieSQLitePool");

                        // Configuration optimisée pour SQLite en mode single-connection
                        config.setMaximumPoolSize(1);
                        config.setMinimumIdle(1);
                        config.setConnectionTimeout(5000); // 5 secondes
                        config.setIdleTimeout(60000); // 1 minute
                        config.setMaxLifetime(300000); // 5 minutes
                        config.setAutoCommit(true);
                        config.setLeakDetectionThreshold(60000); // 1 minute

                        // Propriétés SQLite spécifiques
                        config.addDataSourceProperty("journal_mode", "WAL");
                        config.addDataSourceProperty("synchronous", "NORMAL");
                        config.addDataSourceProperty("foreign_keys", "true");
                        config.addDataSourceProperty("cache_size", "1000");
                        config.addDataSourceProperty("busy_timeout", "5000");

                        dataSource = new HikariDataSource(config);
                        verifyConnection();
                        failureCount.set(0);
                        LOGGER.info("Pool de connexions SQLite initialisé avec succès");

                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erreur critique lors de l'initialisation du pool", e);
                        throw new RuntimeException("Impossible d'initialiser le pool de connexions", e);
                    }
                }
            }
        }
    }

    private static void verifyConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                throw new SQLException("La connexion test n'est pas valide");
            }
            LOGGER.info("Connexion à SQLite vérifiée avec succès");
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }

        Connection conn = null;
        int attempts = 0;
        SQLException lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                conn = dataSource.getConnection();
                if (conn.isValid(5)) {
                    return conn;
                }
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion invalide", e);
                    }
                }
            } catch (SQLException e) {
                lastException = e;
                attempts++;

                if (attempts < MAX_RETRY_ATTEMPTS) {
                    LOGGER.warning(String.format("Tentative %d/%d échouée, nouvelle tentative dans %dms",
                            attempts, MAX_RETRY_ATTEMPTS, RETRY_DELAY_MS));
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interruption pendant la tentative de reconnexion", ie);
                    }
                }
            }
        }

        if (failureCount.incrementAndGet() > 3) {
            resetPool();
            failureCount.set(0);
        }

        throw new SQLException("Impossible d'obtenir une connexion valide après " + 
                            MAX_RETRY_ATTEMPTS + " tentatives", lastException);
    }

    private static synchronized void resetPool() {
        LOGGER.warning("Réinitialisation du pool de connexions...");
        closePool();
        initializeDataSource();
    }

    public static void closePool() {
        synchronized (LOCK) {
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
}