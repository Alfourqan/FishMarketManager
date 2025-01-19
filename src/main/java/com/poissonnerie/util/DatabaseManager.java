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
import java.util.HashSet;
import java.util.Set;

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
    private static final Set<String> ALLOWED_SQL_KEYWORDS = new HashSet<>(Arrays.asList(
        "CREATE", "TABLE", "IF", "NOT", "EXISTS", "DROP", "ALTER", "PRIMARY", "KEY",
        "FOREIGN", "REFERENCES", "INTEGER", "TEXT", "REAL", "DOUBLE", "BOOLEAN",
        "DEFAULT", "NULL", "AUTOINCREMENT", "INDEX", "UNIQUE", "CHECK", "CONSTRAINT",
        "ON", "DELETE", "CASCADE", "UPDATE", "SET", "PRAGMA", "INSERT", "OR", "IGNORE",
        "INTO", "VALUES", "BIGINT", "IN", "WHERE", "AND", "BETWEEN", "LIKE", "AS",
        "ORDER", "BY", "DESC", "ASC", "LIMIT", "OFFSET", "GROUP", "HAVING", "JOIN",
        "LEFT", "RIGHT", "INNER", "OUTER", "USING", "DISTINCT", "COUNT", "SUM", "AVG",
        "MIN", "MAX", "CASE", "WHEN", "THEN", "ELSE", "END"
    ));

    // Pattern pour détecter les tentatives d'injection SQL malveillantes
    private static final Pattern MALICIOUS_SQL_PATTERN = Pattern.compile(
        "(?i)(exec\\b|xp_\\w+|sp_\\w+|waitfor\\b|shutdown\\b|" +
        "drop\\s+database\\b|;\\s*;|@@version|system\\b|" +
        "convert\\(|cast\\(|declare\\s+@|bulk\\s+insert)"
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

    private static boolean validateSchemaContent(String schema) {
        if (schema == null || schema.trim().isEmpty()) {
            LOGGER.severe("Le schéma SQL est vide");
            return false;
        }

        // Supprimer les commentaires et les espaces superflus
        String cleanSchema = schema.replaceAll("--[^\n]*\n", "\n")
                                 .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                                 .replaceAll("\\s+", " ")
                                 .trim();

        // Vérifier les motifs malveillants
        if (MALICIOUS_SQL_PATTERN.matcher(cleanSchema).find()) {
            LOGGER.severe("Motif SQL malveillant détecté dans le schéma");
            return false;
        }

        // Diviser en commandes individuelles
        String[] commands = cleanSchema.split(";");
        for (String command : commands) {
            command = command.trim();
            if (command.isEmpty()) continue;

            // Vérifier chaque mot du command pour s'assurer qu'il est autorisé
            String[] words = command.split("\\s+");
            String firstWord = words[0].toUpperCase();

            // Vérifier que la commande commence par un mot-clé valide
            if (!firstWord.matches("^(CREATE|ALTER|DROP|INSERT|PRAGMA)$")) {
                LOGGER.warning("Commande non autorisée détectée: " + firstWord);
                return false;
            }

            // Pour les commandes CREATE TABLE, vérifier la syntaxe détaillée
            if (firstWord.equals("CREATE") && words.length > 1 && words[1].equalsIgnoreCase("TABLE")) {
                if (!validateCreateTableSyntax(command)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean validateCreateTableSyntax(String command) {
        // Vérifie la structure basique d'une commande CREATE TABLE
        if (!command.matches("(?i)^CREATE\\s+TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+)?\\w+\\s*\\(.*\\)\\s*$")) {
            LOGGER.warning("Syntaxe CREATE TABLE invalide");
            return false;
        }

        // Extrait et vérifie chaque définition de colonne
        String columnDefinitions = command.replaceAll("(?i)^CREATE\\s+TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+)?\\w+\\s*\\((.*)\\)\\s*$", "$2");
        String[] columns = columnDefinitions.split(",");

        for (String column : columns) {
            column = column.trim();
            String[] parts = column.split("\\s+");

            // Vérifie que chaque mot dans la définition de la colonne est autorisé
            for (String part : parts) {
                part = part.toUpperCase().replaceAll("[(),]", "");
                if (!part.isEmpty() && !ALLOWED_SQL_KEYWORDS.contains(part) && !part.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
                    LOGGER.warning("Mot-clé non autorisé dans la définition de colonne: " + part);
                    return false;
                }
            }
        }

        return true;
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