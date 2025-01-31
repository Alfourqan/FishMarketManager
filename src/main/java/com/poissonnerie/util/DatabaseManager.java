package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static final int INIT_TIMEOUT_SECONDS = 10;

    public static Connection getConnection() throws SQLException {
        if (!isInitialized.get()) {
            boolean locked = INIT_LOCK.tryLock();
            try {
                if (locked && !isInitialized.get()) {
                    try {
                        initDatabase();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Échec de l'initialisation de la base de données", e);
                        throw new SQLException("Échec de l'initialisation de la base de données", e);
                    }
                } else if (!locked) {
                    // Attendre que l'initialisation soit terminée
                    long startTime = System.currentTimeMillis();
                    while (!isInitialized.get()) {
                        if (System.currentTimeMillis() - startTime > INIT_TIMEOUT_SECONDS * 1000) {
                            throw new SQLException("Timeout lors de l'attente de l'initialisation de la base de données");
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new SQLException("Interruption pendant l'attente de l'initialisation", e);
                        }
                    }
                }
            } finally {
                if (locked) {
                    INIT_LOCK.unlock();
                }
            }
        }
        return DatabaseConnectionPool.getConnection();
    }

    public static void initDatabase() {
        if (isInitialized.get()) {
            LOGGER.info("Base de données déjà initialisée");
            return;
        }

        boolean locked = INIT_LOCK.tryLock();
        if (!locked) {
            LOGGER.info("Initialisation déjà en cours par un autre thread");
            return;
        }

        try {
            LOGGER.info("Initialisation de la base de données...");
            Connection conn = null;
            long startTime = System.nanoTime();

            try {
                conn = DatabaseConnectionPool.getConnection();
                conn.setAutoCommit(false);

                try (Statement stmt = conn.createStatement()) {
                    // Configuration SQLite optimisée
                    stmt.execute("PRAGMA foreign_keys = ON");
                    stmt.execute("PRAGMA journal_mode = WAL");
                    stmt.execute("PRAGMA synchronous = NORMAL");
                    stmt.execute("PRAGMA cache_size = 1000");
                    stmt.execute("PRAGMA page_size = 4096");
                    stmt.execute("PRAGMA busy_timeout = 5000");

                    // Chargement et exécution du schéma
                    String schema = loadSchemaFromResource();
                    if (schema == null || schema.trim().isEmpty()) {
                        throw new IllegalStateException("Schema SQL vide ou introuvable");
                    }

                    String[] statements = schema.split(";");
                    for (String sql : statements) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            try {
                                stmt.execute(sql);
                                LOGGER.fine("Exécution SQL réussie: " + sql.substring(0, Math.min(sql.length(), 50)) + "...");
                            } catch (SQLException e) {
                                if (!e.getMessage().contains("table already exists")) {
                                    throw e;
                                }
                            }
                        }
                    }

                    // Insertion des données de test si nécessaire
                    insertTestDataIfEmpty(conn);
                    conn.commit();

                    long duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
                    LOGGER.info("Base de données initialisée avec succès en " + duration + " secondes");
                    isInitialized.set(true);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation", e);
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                    }
                }
                throw new RuntimeException("Erreur d'initialisation: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
                    }
                }
            }
        } finally {
            INIT_LOCK.unlock();
        }
    }

    private static void insertTestDataIfEmpty(Connection conn) throws SQLException {
        LOGGER.info("Vérification et insertion des données de test...");
        try (Statement stmt = conn.createStatement()) {
            // Vérification table produits
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM produits");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO produits (nom, categorie, prix_achat, prix_vente, stock, seuil_alerte) VALUES " +
                           "('Saumon frais', 'Poisson', 15.00, 25.00, 50, 10)," +
                           "('Thon rouge', 'Poisson', 20.00, 35.00, 30, 5)," +
                           "('Crevettes', 'Fruits de mer', 12.00, 18.00, 100, 20)");
            }
            LOGGER.info("Données de test insérées avec succès");
        }
    }

    private static String loadSchemaFromResource() {
        LOGGER.info("Chargement du schéma SQL depuis les ressources...");
        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("Fichier schema.sql introuvable dans les ressources");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String schema = reader.lines().collect(Collectors.joining("\n"));
                if (schema.trim().isEmpty()) {
                    throw new IllegalStateException("Le fichier schema.sql est vide");
                }
                LOGGER.info("Schéma SQL chargé avec succès");
                return schema;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la lecture du fichier schema.sql", e);
            throw new RuntimeException("Erreur lors de la lecture du schéma: " + e.getMessage(), e);
        }
    }

    public static void checkDatabaseHealth() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA quick_check");
            stmt.execute("PRAGMA integrity_check");
            stmt.execute("PRAGMA foreign_key_check");
            LOGGER.info("Vérification de la santé de la base de données réussie");
        }
    }
}