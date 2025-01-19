package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_FILE = "poissonnerie.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private static volatile SQLiteDataSource dataSource;
    private static boolean isInitialized = false;

    private static void initializeDataSource() {
        synchronized (DatabaseManager.class) {
            if (dataSource == null) {
                try {
                    SQLiteConfig config = new SQLiteConfig();
                    config.enforceForeignKeys(true);
                    config.setBusyTimeout(30000);
                    config.setReadOnly(false);
                    config.setJournalMode(SQLiteConfig.JournalMode.WAL);
                    config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
                    config.setCacheSize(2000);
                    config.setPageSize(4096);

                    dataSource = new SQLiteDataSource(config);
                    dataSource.setUrl(DB_URL);

                    LOGGER.info("DataSource initialisé avec succès");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du DataSource", e);
                    throw new RuntimeException("Impossible d'initialiser le DataSource", e);
                }
            }
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initializeDataSource();
        }

        Connection conn = connectionHolder.get();
        if (conn == null || conn.isClosed()) {
            conn = dataSource.getConnection();
            connectionHolder.set(conn);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
            }

            LOGGER.info("Nouvelle connexion créée et configurée");
        }
        return conn;
    }

    public static void initDatabase() {
        if (isInitialized) {
            LOGGER.info("La base de données est déjà initialisée");
            return;
        }

        LOGGER.info("Début de l'initialisation de la base de données");

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            String schema = loadSchemaFromResource();

            if (schema == null || schema.trim().isEmpty()) {
                throw new IllegalStateException("Le fichier schema.sql est vide ou introuvable");
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF");

                for (String sql : schema.split(";")) {
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                            LOGGER.info("Exécution réussie: " + sql.substring(0, Math.min(50, sql.length())) + "...");
                        } catch (SQLException e) {
                            if (!e.getMessage().contains("table already exists")) {
                                LOGGER.log(Level.SEVERE, "Erreur lors de l'exécution de la requête: " + sql, e);
                                throw e;
                            }
                            LOGGER.info("Table déjà existante, continue...");
                        }
                    }
                }

                stmt.execute("PRAGMA foreign_keys = ON");
            }

            conn.commit();
            isInitialized = true;
            LOGGER.info("Base de données initialisée avec succès");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Erreur fatale lors de l'initialisation de la base de données: " + e.getMessage(), e);
        }
    }

    private static String loadSchemaFromResource() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql"),
                        StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement du schéma SQL", e);
            throw new RuntimeException("Impossible de charger le schéma SQL", e);
        }
    }

    public static void closeConnections() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    LOGGER.info("Connexion fermée avec succès");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
            } finally {
                connectionHolder.remove();
            }
        }
    }

    public static void resetConnection() {
        LOGGER.info("Réinitialisation de la connexion à la base de données");
        closeConnections();
        try {
            getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation de la connexion", e);
            throw new RuntimeException("Erreur lors de la réinitialisation de la connexion", e);
        }
    }

    public static void checkDatabaseHealth() throws SQLException {
        LOGGER.info("Vérification de la santé de la base de données...");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Vérifier si la base de données répond
            stmt.execute("SELECT 1");

            // Vérifier les paramètres PRAGMA essentiels
            stmt.execute("PRAGMA integrity_check");
            stmt.execute("PRAGMA foreign_key_check");

            LOGGER.info("Vérification de la base de données terminée avec succès");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de la base de données", e);
            throw e;
        }
    }
}