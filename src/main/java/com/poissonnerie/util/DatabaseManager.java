package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;
import java.util.Properties;
import org.sqlite.SQLiteConfig;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_FILE = "poissonnerie.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static Connection connection;
    private static long lastConnectionCheck = 0;
    private static final long CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    private static final int LOGIN_TIMEOUT_SECONDS = 30;
    private static final Object LOCK = new Object();

    public static synchronized Connection getConnection() throws SQLException {
        synchronized (LOCK) {
            if (needsNewConnection()) {
                initializeConnection();
            }
            return connection;
        }
    }

    private static boolean needsNewConnection() {
        if (connection == null) {
            return true;
        }

        try {
            // Vérifier si la connexion est fermée ou invalide
            if (connection.isClosed() || !connection.isValid(5)) {
                LOGGER.warning("Connexion détectée comme invalide ou fermée");
                return true;
            }

            // Vérifier le timeout de la connexion
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastConnectionCheck > CONNECTION_TIMEOUT) {
                LOGGER.info("Timeout de connexion atteint, nouvelle connexion requise");
                return true;
            }

            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la vérification de la connexion", e);
            return true;
        }
    }

    private static void initializeConnection() throws SQLException {
        LOGGER.info("Initialisation d'une nouvelle connexion à la base de données");
        closeConnection(); // Fermer la connexion existante si elle existe

        try {
            Class.forName("org.sqlite.JDBC");

            // Configuration de SQLite
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(30000); // 30 secondes
            config.setReadOnly(false);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);

            // Configuration des timeouts
            Properties props = config.toProperties();
            props.setProperty("timeout", String.valueOf(LOGIN_TIMEOUT_SECONDS));

            // Vérifier si la base de données existe
            boolean needInit = !new File(DB_FILE).exists();

            // Établir la connexion avec les propriétés configurées
            connection = DriverManager.getConnection(DB_URL, props);
            lastConnectionCheck = System.currentTimeMillis();

            // Configuration de la connexion
            connection.setAutoCommit(true);

            // Active les clés étrangères
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA busy_timeout = 30000");
                LOGGER.info("Configuration des PRAGMA SQLite effectuée");
            }

            if (needInit) {
                LOGGER.info("Nouvelle base de données détectée, initialisation...");
                initDatabase();
            }

            LOGGER.info("Connexion à la base de données établie avec succès");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Driver SQLite non trouvé", e);
            throw new SQLException("Driver SQLite non trouvé", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la connexion", e);
            throw e;
        }
    }

    public static void initDatabase() {
        LOGGER.info("Début de l'initialisation de la base de données");

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Lecture du fichier schema.sql
                String schema = new BufferedReader(
                    new InputStreamReader(
                        DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql"),
                        StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

                if (schema.trim().isEmpty()) {
                    throw new SQLException("Le fichier schema.sql est vide ou n'a pas pu être lu");
                }

                // Exécution des requêtes SQL
                try (Statement stmt = conn.createStatement()) {
                    // Activation des clés étrangères
                    stmt.execute("PRAGMA foreign_keys = ON");

                    // Exécution de chaque requête SQL
                    for (String sql : schema.split(";")) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            try {
                                stmt.execute(sql);
                                LOGGER.info("Exécution réussie: " + sql.substring(0, Math.min(50, sql.length())) + "...");
                            } catch (SQLException e) {
                                LOGGER.log(Level.SEVERE, "Erreur lors de l'exécution de la requête: " + sql, e);
                                throw e;
                            }
                        }
                    }

                    conn.commit();
                    LOGGER.info("Base de données initialisée avec succès");
                }
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation, rollback effectué", e);
                throw new SQLException("Erreur lors de l'initialisation de la base de données", e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Erreur fatale lors de l'initialisation de la base de données", e);
        }
    }

    public static void closeConnection() {
        synchronized (LOCK) {
            if (connection != null) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                        LOGGER.info("Connexion à la base de données fermée");
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
                } finally {
                    connection = null;
                    lastConnectionCheck = 0;
                }
            }
        }
    }

    public static void resetConnection() {
        LOGGER.info("Réinitialisation de la connexion à la base de données");
        synchronized (LOCK) {
            closeConnection();
            try {
                getConnection();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation de la connexion", e);
                throw new RuntimeException("Erreur lors de la réinitialisation de la connexion", e);
            }
        }
    }
}