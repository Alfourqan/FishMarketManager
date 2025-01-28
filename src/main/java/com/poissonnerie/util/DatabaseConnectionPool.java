package com.poissonnerie.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionPool.class.getName());
    private static volatile HikariDataSource dataSource;
    private static final String DB_NAME = "poissonnerie.db";
    private static final String DB_PATH = new File(System.getProperty("user.dir"), DB_NAME).getAbsolutePath();
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static final Object LOCK = new Object();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final AtomicInteger failureCount = new AtomicInteger(0);

    private static void initializeDataSource() {
        if (dataSource == null) {
            synchronized (LOCK) {
                if (dataSource == null) {
                    LOGGER.info("Initialisation du pool de connexions avec la base de données: " + DB_PATH);

                    try {
                        File dbFile = new File(DB_PATH);
                        if (!dbFile.exists()) {
                            LOGGER.warning("Base de données non trouvée, création du fichier: " + DB_PATH);
                            dbFile.createNewFile();
                        }

                        HikariConfig config = new HikariConfig();
                        config.setJdbcUrl(DB_URL);
                        config.setPoolName("PoissonneriePool");

                        // Configuration optimisée pour SQLite avec plus de robustesse
                        config.setMaximumPoolSize(20);        // Augmenté pour plus de disponibilité
                        config.setMinimumIdle(5);             // Augmenté pour maintenir plus de connexions
                        config.setConnectionTimeout(60000);    // 60 secondes pour timeout
                        config.setIdleTimeout(300000);        // 5 minutes
                        config.setMaxLifetime(1800000);       // 30 minutes
                        config.setAutoCommit(true);
                        config.setLeakDetectionThreshold(60000); // Détection des fuites après 60s

                        // Paramètres SQLite optimisés
                        config.addDataSourceProperty("pragma_settings", 
                            "PRAGMA journal_mode=WAL;" +
                            "PRAGMA synchronous=NORMAL;" +
                            "PRAGMA foreign_keys=ON;" +
                            "PRAGMA cache_size=10000;" +     // Augmenté
                            "PRAGMA busy_timeout=60000;" +    // 60 secondes
                            "PRAGMA temp_store=MEMORY;" +
                            "PRAGMA mmap_size=536870912;" +  // 512MB
                            "PRAGMA page_size=4096;" +
                            "PRAGMA locking_mode=NORMAL"
                        );

                        dataSource = new HikariDataSource(config);
                        verifyConnection();
                        failureCount.set(0); // Réinitialiser le compteur après succès

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
                LOGGER.info("Connexion à la base de données SQLite vérifiée avec succès");
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
                        resetPool(); // Reset après 5 échecs consécutifs
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

    private static boolean isPoolError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("pool") || message.contains("timeout") || 
               message.contains("connection") || e.getErrorCode() == 17002 ||
               message.contains("database is locked");
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