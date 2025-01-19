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
import java.util.Arrays;
import java.util.List;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_FILE = "poissonnerie.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static Connection connection;
    private static long lastConnectionCheck = 0;
    private static final long CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
    private static final int LOGIN_TIMEOUT_SECONDS = 15;
    private static final Object LOCK = new Object();

    // Liste des mots-clés SQL autorisés
    private static final List<String> ALLOWED_SQL_KEYWORDS = Arrays.asList(
        "CREATE", "TABLE", "DROP", "ALTER", "PRIMARY", "KEY", "FOREIGN",
        "REFERENCES", "INTEGER", "TEXT", "DOUBLE", "BOOLEAN", "DEFAULT",
        "NOT", "NULL", "AUTOINCREMENT", "INDEX", "UNIQUE", "CHECK",
        "CONSTRAINT", "ON", "DELETE", "CASCADE", "UPDATE", "SET"
    );

    // Pattern pour détecter les tentatives d'injection SQL malveillantes
    private static final Pattern MALICIOUS_SQL_PATTERN = Pattern.compile(
        "(?i)(\\b(INSERT|UPDATE|DELETE)\\b.*\\bINTO\\b.*|" +
        "\\bDROP\\b.*\\bTABLE\\b.*|" +
        "\\bALTER\\b.*\\bTABLE\\b.*\\bDROP\\b|" +
        "--.*|/\\*.*\\*/|;.*;|@@|" +
        "\\bEXEC\\b|\\bXP_\\w+|" +
        "\\bSYSDATETIME\\b|\\bCURRENT_USER\\b)"
    );

    private static SQLiteDataSource dataSource;

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(15000);
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

    private static boolean validateSchemaContent(String schema) {
        // Vérifier si le schéma est vide
        if (schema == null || schema.trim().isEmpty()) {
            LOGGER.severe("Le schéma SQL est vide");
            return false;
        }

        // Vérifier les motifs malveillants
        if (MALICIOUS_SQL_PATTERN.matcher(schema).find()) {
            LOGGER.severe("Motif SQL malveillant détecté dans le schéma");
            return false;
        }

        // Vérifier que seuls les mots-clés autorisés sont utilisés
        String[] words = schema.toUpperCase().split("\\s+");
        for (String word : words) {
            if (word.matches("^[A-Z_]+$") && !ALLOWED_SQL_KEYWORDS.contains(word)) {
                LOGGER.warning("Mot-clé SQL non autorisé détecté : " + word);
                return false;
            }
        }

        return true;
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

                if (!validateSchemaContent(schema)) {
                    throw new SecurityException("Le schéma SQL contient des éléments non autorisés");
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
                throw new SQLException("Erreur lors de l'initialisation de la base de données: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Erreur fatale lors de l'initialisation de la base de données: " + e.getMessage(), e);
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
        if (MALICIOUS_SQL_PATTERN.matcher(input).find()) {
            throw new SecurityException("L'entrée SQL contient des motifs non autorisés");
        }
    }
}