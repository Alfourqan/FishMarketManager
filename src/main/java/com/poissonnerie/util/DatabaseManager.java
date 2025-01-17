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

                // Vérifier si la base de données existe
                boolean needInit = !new File(DB_FILE).exists();

                connection = DriverManager.getConnection(DB_URL);
                System.out.println("Connexion à la base de données établie");

                // Active les clés étrangères
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }

                // Initialiser la base de données seulement si elle n'existe pas
                if (needInit) {
                    System.out.println("Nouvelle base de données détectée, initialisation...");
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
                            stmt.execute(sql.trim());
                            System.out.println("Exécution réussie: " + sql.trim().substring(0, Math.min(50, sql.trim().length())) + "...");
                        }
                    }

                    conn.commit();
                    System.out.println("Base de données initialisée avec succès.");
                }
            } catch (Exception e) {
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