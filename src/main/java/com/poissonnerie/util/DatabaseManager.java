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
    private static final String DB_URL = "jdbc:h2:./poissonnerie;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void initDatabase() {
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
            System.exit(1);
        }
    }
}
