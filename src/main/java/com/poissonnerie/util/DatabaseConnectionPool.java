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
    private static final long RETRY_DELAY_MS = 1000;
    private static final AtomicInteger failureCount = new AtomicInteger(0);

    private static void initializeDataSource() {
        if (dataSource == null) {
            synchronized (LOCK) {
                if (dataSource == null) {
                    LOGGER.info("Initialisation du pool de connexions à la base de données PostgreSQL");

                    try {
                        HikariConfig config = new HikariConfig();

                        // Configuration PostgreSQL avec variables d'environnement
                        config.setJdbcUrl(System.getenv("DATABASE_URL"));
                        config.setUsername(System.getenv("PGUSER"));
                        config.setPassword(System.getenv("PGPASSWORD"));
                        config.setPoolName("PoissonneriePool");

                        // Configuration optimisée pour PostgreSQL
                        config.setMaximumPoolSize(10);
                        config.setMinimumIdle(2);
                        config.setConnectionTimeout(30000);
                        config.setIdleTimeout(600000);
                        config.setMaxLifetime(1800000);
                        config.setAutoCommit(true);
                        config.setLeakDetectionThreshold(60000);

                        // Paramètres PostgreSQL optimisés
                        config.addDataSourceProperty("cachePrepStmts", "true");
                        config.addDataSourceProperty("prepStmtCacheSize", "250");
                        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                        config.addDataSourceProperty("useServerPrepStmts", "true");

                        dataSource = new HikariDataSource(config);
                        verifyConnection();
                        failureCount.set(0);

                        LOGGER.info("Pool de connexions PostgreSQL initialisé avec succès");

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
            if (conn.isValid(5)) {
                LOGGER.info("Connexion à PostgreSQL vérifiée avec succès");
            } else {
                throw new SQLException("La connexion test n'est pas valide");
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource();
        }

        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                Connection conn = dataSource.getConnection();
                if (conn.isValid(5)) {
                    return conn;
                }
                throw new SQLException("Connexion invalide obtenue du pool");
            } catch (SQLException e) {
                attempts++;
                if (attempts == MAX_RETRY_ATTEMPTS) {
                    LOGGER.severe("Échec de l'obtention d'une connexion après " + MAX_RETRY_ATTEMPTS + " tentatives");
                    if (failureCount.incrementAndGet() > 5) {
                        resetPool();
                        failureCount.set(0);
                    }
                    throw e;
                }
                LOGGER.warning("Tentative " + attempts + "/" + MAX_RETRY_ATTEMPTS + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interruption pendant la tentative de reconnexion", e);
                }
            }
        }
        throw new SQLException("Impossible d'obtenir une connexion valide après " + MAX_RETRY_ATTEMPTS + " tentatives");
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