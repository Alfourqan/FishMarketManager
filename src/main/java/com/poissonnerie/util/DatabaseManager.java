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
    private static boolean isInitialized = false;

    public static synchronized Connection getConnection() throws SQLException {
        return DatabaseConnectionPool.getConnection();
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
        DatabaseConnectionPool.closePool();
    }

    public static void resetConnection() {
        LOGGER.info("Réinitialisation de la connexion à la base de données");
        DatabaseConnectionPool.closePool();
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