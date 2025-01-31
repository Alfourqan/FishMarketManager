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

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static volatile boolean isInitialized = false;
    private static final Object INIT_LOCK = new Object();

    public static void main(String[] args) {
        try {
            LOGGER.info("Démarrage de l'initialisation de la base de données...");
            initDatabase();
            LOGGER.info("Base de données initialisée avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
            System.exit(1);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            synchronized (INIT_LOCK) {
                if (!isInitialized) {
                    initDatabase();
                }
            }
        }
        return DatabaseConnectionPool.getConnection();
    }

    public static void initDatabase() {
        if (isInitialized) {
            return;
        }

        synchronized (INIT_LOCK) {
            if (isInitialized) {
                return;
            }

            LOGGER.info("Initialisation de la base de données...");
            Connection conn = null;

            try {
                conn = DatabaseConnectionPool.getConnection();

                // Chargement et exécution du schéma
                String schema = loadSchemaFromResource();
                if (schema == null || schema.trim().isEmpty()) {
                    throw new IllegalStateException("Schema SQL vide ou introuvable");
                }

                try (Statement stmt = conn.createStatement()) {
                    for (String sql : schema.split(";")) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            try {
                                stmt.execute("PRAGMA foreign_keys = ON");
                                stmt.execute("PRAGMA journal_mode = WAL");
                                stmt.execute("PRAGMA synchronous = NORMAL");
                                stmt.execute("PRAGMA cache_size = 2000");
                                stmt.execute("PRAGMA page_size = 4096");
                                stmt.execute(sql);
                                LOGGER.fine("Exécution SQL réussie: " + sql.substring(0, Math.min(sql.length(), 50)) + "...");
                            } catch (SQLException e) {
                                if (!e.getMessage().contains("table already exists")) {
                                    throw e;
                                }
                            }
                        }
                    }
                    LOGGER.info("Schéma de base de données appliqué avec succès");

                    // Insertion des données de test si nécessaire
                    insertTestDataIfEmpty(conn);
                }

                isInitialized = true;
                LOGGER.info("Base de données initialisée avec succès");

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation", e);
                throw new RuntimeException("Erreur d'initialisation: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
                    }
                }
            }
        }
    }

    private static void insertTestDataIfEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Vérifier si la table produits est vide
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM produits");
            if (rs.next() && rs.getInt(1) == 0) {
                // Insérer quelques produits de test
                stmt.execute("INSERT INTO produits (nom, categorie, prix_achat, prix_vente, stock, seuil_alerte) VALUES " +
                           "('Saumon frais', 'Poisson', 15.00, 25.00, 50, 10)," +
                           "('Thon rouge', 'Poisson', 20.00, 35.00, 30, 5)," +
                           "('Crevettes', 'Fruits de mer', 12.00, 18.00, 100, 20)");
            }

            // Vérifier si la table utilisateurs est vide
            rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                // Insérer un utilisateur admin par défaut
                stmt.execute("INSERT INTO users (username, password, role) VALUES " +
                           "('admin', '$2a$12$1234567890123456789012uuuu', 'ADMIN')");
            }

            LOGGER.info("Données de test insérées avec succès");
        }
    }

    private static String loadSchemaFromResource() {
        LOGGER.info("Chargement du schéma SQL depuis les ressources...");
        final String schemaPath = "schema.sql";

        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream(schemaPath)) {
            if (is == null) {
                LOGGER.severe("Fichier schema.sql introuvable dans les ressources");
                throw new IllegalStateException("Fichier schema.sql introuvable dans les ressources");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String schema = reader.lines().collect(Collectors.joining("\n"));
                if (schema.trim().isEmpty()) {
                    LOGGER.severe("Le fichier schema.sql est vide");
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