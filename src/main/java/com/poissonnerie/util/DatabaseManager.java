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

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static volatile boolean isInitialized = false;

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

    public static synchronized Connection getConnection() throws SQLException {
        if (!isInitialized) {
            initDatabase();
        }
        return DatabaseConnectionPool.getConnection();
    }

    public static synchronized void initDatabase() {
        if (isInitialized) {
            return;
        }

        LOGGER.info("Initialisation de la base de données...");

        try (Connection conn = DatabaseConnectionPool.getConnection();
             Statement stmt = conn.createStatement()) {

            // Configuration minimale SQLite
            stmt.execute("PRAGMA foreign_keys = OFF");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");

            String schema = loadSchemaFromResource();
            if (schema == null || schema.trim().isEmpty()) {
                throw new IllegalStateException("Schema SQL vide ou introuvable");
            }

            // Exécution du schéma
            for (String sql : schema.split(";")) {
                sql = sql.trim();
                if (!sql.isEmpty()) {
                    try {
                        stmt.execute(sql);
                        LOGGER.fine("Exécution SQL réussie: " + sql.substring(0, Math.min(sql.length(), 50)) + "...");
                    } catch (SQLException e) {
                        if (!e.getMessage().contains("table already exists")) {
                            LOGGER.warning("Erreur SQL: " + e.getMessage() + " pour la requête: " + sql);
                            throw e;
                        }
                    }
                }
            }

            // Insertion des données de test si la base est vide
            insertTestDataIfEmpty(conn);

            stmt.execute("PRAGMA foreign_keys = ON");
            isInitialized = true;
            LOGGER.info("Base de données initialisée avec succès");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation", e);
            throw new RuntimeException("Erreur d'initialisation: " + e.getMessage(), e);
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

            // Vérifier si la table clients est vide
            rs = stmt.executeQuery("SELECT COUNT(*) FROM clients");
            if (rs.next() && rs.getInt(1) == 0) {
                // Insérer quelques clients de test
                stmt.execute("INSERT INTO clients (nom, telephone, adresse) VALUES " +
                           "('Jean Dupont', '0123456789', '1 rue de la Mer')," +
                           "('Marie Martin', '0987654321', '15 avenue des Poissons')");
            }
        }
    }

    private static String loadSchemaFromResource() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql"),
                    StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur de chargement du schéma", e);
            throw new RuntimeException("Impossible de charger le schéma", e);
        }
    }

    public static void closeConnections() {
        DatabaseConnectionPool.closePool();
    }

    public static void checkDatabaseHealth() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA quick_check");
            stmt.execute("PRAGMA integrity_check");
            stmt.execute("PRAGMA foreign_key_check");
        }
    }
}