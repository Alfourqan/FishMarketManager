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
import java.io.File;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteOpenMode;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static volatile boolean isInitialized = false;
    private static final Object LOCK = new Object();
    private static final String DB_FILE = "poissonnerie.db";
    private static SQLiteConfig config;

    static {
        config = new SQLiteConfig();
        config.setOpenMode(SQLiteOpenMode.READWRITE);
        config.setJournalMode(SQLiteConfig.JournalMode.DELETE);
        config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);
        config.setBusyTimeout(30000);
        config.setCacheSize(2000);
        config.setPageSize(4096);
        config.enforceForeignKeys(true);
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            synchronized (LOCK) {
                if (!isInitialized) {
                    try {
                        initDatabase();
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Échec de l'initialisation de la base de données", e);
                        throw new SQLException("Échec de l'initialisation de la base de données", e);
                    }
                }
            }
        }

        try {
            return config.createConnection("jdbc:sqlite:" + DB_FILE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création de la connexion", e);
            throw e;
        }
    }

    public static void initDatabase() {
        if (isInitialized) {
            LOGGER.info("Base de données déjà initialisée");
            return;
        }

        synchronized (LOCK) {
            if (isInitialized) {
                return;
            }

            LOGGER.info("Initialisation de la base de données...");
            File dbFile = new File(DB_FILE);

            if (!dbFile.exists()) {
                LOGGER.info("Création du fichier de base de données...");
                try {
                    if (!dbFile.createNewFile()) {
                        throw new IOException("Impossible de créer le fichier de base de données");
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Erreur lors de la création du fichier de base de données", e);
                }
            }

            try (Connection conn = getConnection()) {
                String schema = loadSchemaFromResource();
                if (schema == null || schema.trim().isEmpty()) {
                    throw new IllegalStateException("Schema SQL vide ou introuvable");
                }

                try (Statement stmt = conn.createStatement()) {
                    String[] statements = schema.split(";");
                    for (String sql : statements) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            try {
                                stmt.execute(sql);
                            } catch (SQLException e) {
                                if (!e.getMessage().contains("table already exists")) {
                                    throw e;
                                }
                            }
                        }
                    }
                    insertTestDataIfEmpty(conn);
                }
                isInitialized = true;
                LOGGER.info("Base de données initialisée avec succès");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation", e);
                throw new RuntimeException("Erreur d'initialisation: " + e.getMessage(), e);
            }
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
                LOGGER.info("Schéma SQL chargé avec succès");
                return schema;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la lecture du fichier schema.sql", e);
            throw new RuntimeException("Erreur lors de la lecture du schéma: " + e.getMessage(), e);
        }
    }

    private static void insertTestDataIfEmpty(Connection conn) throws SQLException {
        LOGGER.info("Vérification et insertion des données de test...");
        try (Statement stmt = conn.createStatement()) {
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