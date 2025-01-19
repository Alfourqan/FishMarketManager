package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;
import java.util.Properties;
import org.sqlite.SQLiteConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private static final long CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    private static final int LOGIN_TIMEOUT_SECONDS = 30;
    private static final Object LOCK = new Object();
    private static volatile SQLiteDataSource dataSource;
    private static boolean isInitialized = false;

    private static final Set<String> ALLOWED_SQL_KEYWORDS = new HashSet<>(Arrays.asList(
        "CREATE", "TABLE", "IF", "NOT", "EXISTS", "DROP", "ALTER", "PRIMARY", "KEY",
        "FOREIGN", "REFERENCES", "INTEGER", "TEXT", "REAL", "DOUBLE", "BOOLEAN",
        "DEFAULT", "NULL", "AUTOINCREMENT", "INDEX", "UNIQUE", "CHECK", "CONSTRAINT",
        "ON", "DELETE", "CASCADE", "UPDATE", "SET", "PRAGMA", "INSERT", "OR", "IGNORE",
        "INTO", "VALUES", "BIGINT", "IN", "WHERE", "AND", "BETWEEN", "LIKE", "AS",
        "ORDER", "BY", "DESC", "ASC", "LIMIT", "OFFSET", "GROUP", "HAVING", "JOIN",
        "LEFT", "RIGHT", "INNER", "OUTER", "USING", "DISTINCT", "COUNT", "SUM", "AVG",
        "MIN", "MAX", "CASE", "WHEN", "THEN", "ELSE", "END", "TRUE", "FALSE", "0", "1",
        "DATETIME", "NOW", "LOCALTIME", "STRFTIME", "'ACTIF'", "'NOW'", "'LOCALTIME'",
        "'ENTREE'", "'SORTIE'", "'OUVERTURE'", "'CLOTURE'", "*"
    ));

    private static final Pattern MALICIOUS_SQL_PATTERN = Pattern.compile(
        "(?i)(exec\\b|xp_\\w+|sp_\\w+|waitfor\\b|shutdown\\b|" +
        "drop\\s+database\\b|;\\s*;|@@version|system\\b|" +
        "convert\\(|cast\\(|declare\\s+@|bulk\\s+insert)"
    );

    static {
        initializeDataSource();
    }

    private static void initializeDataSource() {
        synchronized (LOCK) {
            if (dataSource == null) {
                try {
                    SQLiteConfig config = new SQLiteConfig();
                    config.enforceForeignKeys(true);
                    config.setBusyTimeout(30000);
                    config.setReadOnly(false);
                    config.setJournalMode(SQLiteConfig.JournalMode.WAL);
                    config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
                    config.setCacheSize(2000);
                    config.setPageSize(4096);

                    dataSource = new SQLiteDataSource(config);
                    dataSource.setUrl(DB_URL);
                    dataSource.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);

                    LOGGER.info("DataSource initialisé avec succès");
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du DataSource", e);
                    throw new RuntimeException("Impossible d'initialiser le DataSource", e);
                }
            }
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();

        if (conn == null || conn.isClosed() || !conn.isValid(5)) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de l'ancienne connexion", e);
                }
            }

            conn = dataSource.getConnection();
            conn.setAutoCommit(true);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA busy_timeout = 30000");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
            }

            connectionHolder.set(conn);
            LOGGER.info("Nouvelle connexion créée et configurée");
        }

        return conn;
    }

    public static synchronized void initDatabase() {
        if (isInitialized) {
            LOGGER.info("La base de données est déjà initialisée");
            return;
        }

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

                if (schema == null || schema.trim().isEmpty()) {
                    throw new IllegalStateException("Le fichier schema.sql est vide ou introuvable");
                }

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
                    isInitialized = true;
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

    public static void checkDatabaseHealth() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SELECT 1");
            LOGGER.info("Vérification de la santé de la base de données : OK");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "La base de données n'est pas en bonne santé", e);
            throw new RuntimeException("Erreur lors de la vérification de la base de données", e);
        }
    }

    private static boolean validateSchemaContent(String schema) {
        if (schema == null || schema.trim().isEmpty()) {
            LOGGER.severe("Le schéma SQL est vide");
            return false;
        }

        String cleanSchema = schema.replaceAll("--[^\n]*\n", "\n")
                                 .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                                 .replaceAll("\\s+", " ")
                                 .trim();

        if (MALICIOUS_SQL_PATTERN.matcher(cleanSchema).find()) {
            LOGGER.severe("Motif SQL malveillant détecté dans le schéma");
            return false;
        }

        String[] commands = cleanSchema.split(";");
        for (String command : commands) {
            command = command.trim();
            if (command.isEmpty()) continue;

            String[] words = command.split("\\s+");
            String firstWord = words[0].toUpperCase();

            if (!firstWord.matches("^(CREATE|ALTER|DROP|INSERT|PRAGMA)$")) {
                LOGGER.warning("Commande non autorisée détectée: " + firstWord);
                return false;
            }

            if (firstWord.equals("CREATE") && words.length > 1 && words[1].equalsIgnoreCase("TABLE")) {
                if (!validateCreateTableSyntax(command)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean validateCreateTableSyntax(String command) {
        if (!command.matches("(?i)^CREATE\\s+TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+)?\\w+\\s*\\(.*\\)\\s*$")) {
            LOGGER.warning("Syntaxe CREATE TABLE invalide");
            return false;
        }

        String columnDefinitions = command.replaceAll("(?i)^CREATE\\s+TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+)?\\w+\\s*\\((.*)\\)\\s*$", "$2");
        String[] columns = columnDefinitions.split(",");

        for (String column : columns) {
            column = column.trim();
            if (column.toUpperCase().startsWith("CONSTRAINT") ||
                column.toUpperCase().startsWith("PRIMARY KEY") ||
                column.toUpperCase().startsWith("FOREIGN KEY")) {
                continue;
            }

            String[] parts = column.split("\\s+");
            for (String part : parts) {
                part = part.toUpperCase().replaceAll("[(),]", "");
                if (!part.isEmpty() && 
                    !ALLOWED_SQL_KEYWORDS.contains(part) && 
                    !part.matches("^[A-Za-z_][A-Za-z0-9_]*$") &&
                    !part.matches("^\\d+$")) {
                    LOGGER.info("Validation de partie: " + part);
                }
            }
        }

        return true;
    }

    public static void closeConnections() {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    LOGGER.info("Connexion fermée avec succès");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
            } finally {
                connectionHolder.remove();
            }
        }
    }

    public static void resetConnection() {
        LOGGER.info("Réinitialisation de la connexion à la base de données");
        closeConnections();
        try {
            getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation de la connexion", e);
            throw new RuntimeException("Erreur lors de la réinitialisation de la connexion", e);
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