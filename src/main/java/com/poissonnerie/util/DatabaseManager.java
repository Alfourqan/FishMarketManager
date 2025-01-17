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

public class DatabaseManager {
    private static final String DB_FILE = "poissonnerie.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static Connection connection;

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                File dbFile = new File(DB_FILE);
                boolean needInit = !dbFile.exists();

                connection = DriverManager.getConnection(DB_URL);
                System.out.println("Connexion à la base de données établie");

                // Active les clés étrangères
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }

                // Si la base vient d'être créée, on l'initialise
                if (needInit) {
                    initDatabase();
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Driver SQLite non trouvé: " + e.getMessage());
                throw new SQLException("Driver SQLite non trouvé", e);
            }
        }
        return connection;
    }

    public static void initDatabase() {
        System.out.println("Initialisation de la base de données...");

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

                // Exécution des requêtes SQL
                try (Statement stmt = conn.createStatement()) {
                    // Activation des clés étrangères
                    stmt.execute("PRAGMA foreign_keys = ON");

                    // Exécution de chaque requête SQL
                    for (String sql : schema.split(";")) {
                        if (!sql.trim().isEmpty()) {
                            try {
                                stmt.execute(sql.trim());
                                System.out.println("Exécution réussie: " + sql.trim().substring(0, Math.min(50, sql.trim().length())) + "...");
                            } catch (SQLException e) {
                                System.err.println("Erreur lors de l'exécution de la requête: " + sql.trim());
                                System.err.println("Message d'erreur: " + e.getMessage());
                                throw e;
                            }
                        }
                    }

                    // Vérification des tables
                    String[] tables = {"configurations", "produits", "clients", "ventes", "lignes_vente", "mouvements_caisse"};
                    for (String table : tables) {
                        try {
                            stmt.executeQuery("SELECT 1 FROM " + table + " LIMIT 1");
                            System.out.println("Table " + table + " créée avec succès.");
                        } catch (SQLException e) {
                            System.err.println("Erreur lors de la vérification de la table " + table + ": " + e.getMessage());
                            throw e;
                        }
                    }

                    // Vérification et insertion des configurations par défaut
                    try {
                        stmt.execute("INSERT OR IGNORE INTO configurations (cle, valeur, description) VALUES " +
                                   "('TAUX_TVA', '20.0', 'Taux de TVA en pourcentage'), " +
                                   "('NOM_ENTREPRISE', '', 'Nom de l''entreprise'), " +
                                   "('ADRESSE_ENTREPRISE', '', 'Adresse de l''entreprise'), " +
                                   "('TELEPHONE_ENTREPRISE', '', 'Numéro de téléphone de l''entreprise'), " +
                                   "('PIED_PAGE_RECU', 'Merci de votre visite !', 'Message en pied de page des reçus')");
                        System.out.println("Configurations par défaut vérifiées/insérées avec succès.");
                    } catch (SQLException e) {
                        System.err.println("Erreur lors de la vérification/insertion des configurations par défaut: " + e.getMessage());
                        throw e;
                    }

                    conn.commit();
                    System.out.println("Base de données initialisée avec succès.");
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Erreur fatale lors de l'initialisation de la base de données:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Connexion à la base de données fermée.");
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void resetConnection() {
        closeConnection();
        try {
            getConnection();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la réinitialisation de la connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}