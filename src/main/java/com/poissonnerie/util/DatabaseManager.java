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
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;
import java.util.Properties;
import org.sqlite.SQLiteConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.regex.Pattern;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_FILE = "poissonnerie.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static Connection connection;
    private static long lastConnectionCheck = 0;
    private static final long CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(2); // Réduit à 2 minutes
    private static final int LOGIN_TIMEOUT_SECONDS = 15; // Réduit à 15 secondes
    private static final Object LOCK = new Object();
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("[';\"\\-\\/*]");
    private static SQLiteDataSource dataSource;

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(15000); // 15 secondes
            config.setReadOnly(false);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);

            dataSource = new SQLiteDataSource(config);
            dataSource.setUrl(DB_URL);
            dataSource.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);

            LOGGER.info("DataSource initialisé avec succès");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du DataSource", e);
            throw new RuntimeException("Impossible d'initialiser le DataSource", e);
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        synchronized (LOCK) {
            if (needsNewConnection()) {
                initializeConnection();
            }
            return connection;
        }
    }

    private static boolean needsNewConnection() {
        if (connection == null) {
            return true;
        }

        try {
            if (connection.isClosed() || !connection.isValid(5)) {
                LOGGER.warning("Connexion détectée comme invalide ou fermée");
                return true;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastConnectionCheck > CONNECTION_TIMEOUT) {
                LOGGER.info("Timeout de connexion atteint, nouvelle connexion requise");
                return true;
            }

            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la vérification de la connexion", e);
            return true;
        }
    }

    private static void initializeConnection() throws SQLException {
        LOGGER.info("Initialisation d'une nouvelle connexion à la base de données");
        closeConnection();

        try {
            connection = dataSource.getConnection();
            lastConnectionCheck = System.currentTimeMillis();

            connection.setAutoCommit(true);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA busy_timeout = 15000");
                LOGGER.info("Configuration des PRAGMA SQLite effectuée");
            }

            if (!isDatabaseInitialized()) {
                LOGGER.info("Nouvelle base de données détectée, initialisation...");
                initDatabase();
            }

            LOGGER.info("Connexion à la base de données établie avec succès");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la connexion", e);
            throw e;
        }
    }

    private static boolean isDatabaseInitialized() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='configurations'")) {
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la vérification de l'initialisation de la base de données", e);
            return false;
        }
    }

    public static void initDatabase() {
        LOGGER.info("Début de l'initialisation de la base de données");

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String schema = new BufferedReader(
                    new InputStreamReader(
                        DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql"),
                        StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

                if (schema.trim().isEmpty()) {
                    throw new SQLException("Le fichier schema.sql est vide ou n'a pas pu être lu");
                }

                // Validation du schéma SQL
                if (SQL_INJECTION_PATTERN.matcher(schema).find()) {
                    throw new SecurityException("Le schéma SQL contient des caractères non autorisés");
                }

                try (Statement stmt = conn.createStatement()) {
                    for (String sql : schema.split(";")) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            try {
                                stmt.execute(sql);
                                LOGGER.info("Exécution réussie: " + sql.substring(0, Math.min(50, sql.length())) + "...");
                            } catch (SQLException e) {
                                LOGGER.log(Level.SEVERE, "Erreur lors de l'exécution de la requête: " + sql, e);
                                throw e;
                            }
                        }
                    }

                    conn.commit();
                    LOGGER.info("Base de données initialisée avec succès");
                }
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation, rollback effectué", e);
                throw new SQLException("Erreur lors de l'initialisation de la base de données", e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Erreur fatale lors de l'initialisation de la base de données", e);
        }
    }

    public static void closeConnection() {
        synchronized (LOCK) {
            if (connection != null) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                        LOGGER.info("Connexion à la base de données fermée");
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
                } finally {
                    connection = null;
                    lastConnectionCheck = 0;
                }
            }
        }
    }

    public static void resetConnection() {
        LOGGER.info("Réinitialisation de la connexion à la base de données");
        synchronized (LOCK) {
            closeConnection();
            try {
                getConnection();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation de la connexion", e);
                throw new RuntimeException("Erreur lors de la réinitialisation de la connexion", e);
            }
        }
    }

    public static void validateSqlInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("L'entrée SQL ne peut pas être vide");
        }
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            throw new SecurityException("L'entrée SQL contient des caractères non autorisés");
        }
    }
}