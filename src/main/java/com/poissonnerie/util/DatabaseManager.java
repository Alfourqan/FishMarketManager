package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final String DB_URL = System.getenv("DATABASE_URL");
    private static final String DB_USER = System.getenv("PGUSER");
    private static final String DB_PASSWORD = System.getenv("PGPASSWORD");

    private static boolean isDatabaseInitialized = false;

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Driver PostgreSQL chargé avec succès");
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur: Driver PostgreSQL non trouvé");
            throw new SQLException("Driver PostgreSQL non trouvé", e);
        }

        System.out.println("Tentative de connexion à la base de données...");
        System.out.println("URL de connexion: " + DB_URL);
        System.out.println("Utilisateur: " + DB_USER);

        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connexion établie avec succès à la base de données");
            return conn;
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données: " + e.getMessage());
            throw e;
        }
    }

    public static synchronized void initDatabase() {
        if (isDatabaseInitialized) {
            System.out.println("Base de données déjà initialisée");
            return;
        }

        System.out.println("Initialisation de la base de données PostgreSQL...");
        System.out.println("Variables d'environnement:");
        System.out.println("DATABASE_URL présent: " + (DB_URL != null));
        System.out.println("PGUSER présent: " + (DB_USER != null));
        System.out.println("PGPASSWORD présent: " + (DB_PASSWORD != null));

        try (Connection conn = getConnection()) {
            // Lecture du fichier schema.sql
            String schemaPath = "schema.sql";
            System.out.println("Lecture du fichier schema: " + schemaPath);

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    DatabaseManager.class.getClassLoader().getResourceAsStream(schemaPath),
                    StandardCharsets.UTF_8))) {

                String schema = reader.lines().collect(Collectors.joining("\n"));
                System.out.println("Contenu du schema SQL chargé: " + schema.length() + " caractères");

                // Exécution des requêtes SQL
                try (Statement stmt = conn.createStatement()) {
                    conn.setAutoCommit(false);

                    String[] queries = schema.split(";");
                    for (String sql : queries) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            try {
                                System.out.println("Exécution de la requête: " + sql);
                                stmt.execute(sql);
                                System.out.println("Requête exécutée avec succès");
                            } catch (SQLException e) {
                                System.err.println("Erreur lors de l'exécution de la requête: " + sql);
                                System.err.println("Message d'erreur: " + e.getMessage());
                                throw e;
                            }
                        }
                    }

                    conn.commit();
                    System.out.println("Initialisation de la base de données terminée avec succès");
                    isDatabaseInitialized = true;
                } catch (SQLException e) {
                    System.err.println("Erreur lors de l'exécution des requêtes SQL: " + e.getMessage());
                    conn.rollback();
                    throw e;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la lecture du fichier schema.sql: " + e.getMessage());
                throw new SQLException("Erreur lors de la lecture du schema", e);
            }
        } catch (SQLException e) {
            System.err.println("Erreur fatale lors de l'initialisation de la base de données: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec de l'initialisation de la base de données", e);
        }
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Test de connexion réussi");
        } catch (SQLException e) {
            System.err.println("Test de connexion échoué: " + e.getMessage());
            e.printStackTrace();
        }
    }
}