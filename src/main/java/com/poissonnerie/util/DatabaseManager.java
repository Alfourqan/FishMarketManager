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

    public static Connection getConnection() throws SQLException {
        // Vérifier si le pilote SQLite est disponible
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver SQLite non trouvé", e);
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initDatabase() {
        // Créer le répertoire de la base de données si nécessaire
        File dbFile = new File(DB_FILE);
        if (!dbFile.exists()) {
            try (Connection conn = getConnection()) {
                // Lecture du fichier schema.sql
                String schema = new BufferedReader(
                    new InputStreamReader(
                        DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql"),
                        StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

                // Exécution des requêtes SQL
                try (Statement stmt = conn.createStatement()) {
                    for (String sql : schema.split(";")) {
                        if (!sql.trim().isEmpty()) {
                            stmt.execute(sql);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Erreur lors de l'initialisation de la base de données: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}