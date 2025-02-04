package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static volatile boolean isInitialized = false;
    private static final Object LOCK = new Object();
    private static final String DB_FILE = "poissonnerie.db";
    private static SQLiteConfig config;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final int BUSY_TIMEOUT_MS = 30000;

    static {
        config = new SQLiteConfig();
        config.setOpenMode(SQLiteOpenMode.READWRITE);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setBusyTimeout(BUSY_TIMEOUT_MS);
        config.setCacheSize(2000);
        config.setPageSize(4096);
        config.enforceForeignKeys(true);
        config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
    }

    private static Connection createConnection() throws SQLException {
        int retries = 0;
        SQLException lastException = null;

        while (retries < MAX_RETRIES) {
            try {
                Connection conn = config.createConnection("jdbc:sqlite:" + DB_FILE);
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);
                    stmt.execute("PRAGMA journal_mode = WAL");
                    stmt.execute("PRAGMA synchronous = NORMAL");
                    stmt.execute("PRAGMA locking_mode = EXCLUSIVE");
                    stmt.execute("PRAGMA cache_size = 2000");
                }

                return conn;
            } catch (SQLException e) {
                lastException = e;
                if (e.getMessage().contains("database is locked") && retries < MAX_RETRIES - 1) {
                    LOGGER.warning("Database locked, retrying... (attempt " + (retries + 1) + "/" + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (retries + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrupted while waiting for retry", ie);
                    }
                    retries++;
                } else {
                    throw e;
                }
            }
        }

        throw new SQLException("Failed to create connection after " + MAX_RETRIES + " attempts", lastException);
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized) {
            synchronized (LOCK) {
                if (!isInitialized) {
                    initDatabase();
                }
            }
        }
        return createConnection();
    }

    private static Connection getInitConnection() throws SQLException {
        SQLiteConfig initConfig = new SQLiteConfig();
        initConfig.setOpenMode(SQLiteOpenMode.READWRITE);
        initConfig.enforceForeignKeys(true);
        initConfig.setBusyTimeout(BUSY_TIMEOUT_MS);
        return initConfig.createConnection("jdbc:sqlite:" + DB_FILE);
    }

    public static void initDatabase() {
        if (isInitialized) {
            LOGGER.info("Base de données déjà initialisée");
            return;
        }

        synchronized (LOCK) {
            if (isInitialized) {
                return;
            }

            LOGGER.info("Initialisation de la base de données...");
            File dbFile = new File(DB_FILE);

            if (!dbFile.exists()) {
                LOGGER.info("Création du fichier de base de données...");
                try {
                    if (!dbFile.createNewFile()) {
                        throw new IOException("Impossible de créer le fichier de base de données");
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Erreur lors de la création du fichier de base de données", e);
                }
            }

            try (Connection conn = getInitConnection()) {
                String schema = loadSchemaFromResource();
                if (schema == null || schema.trim().isEmpty()) {
                    throw new IllegalStateException("Schema SQL vide ou introuvable");
                }

                try (Statement stmt = conn.createStatement()) {
                    String[] statements = schema.split(";");
                    for (String sql : statements) {
                        sql = sql.trim();
                        if (!sql.isEmpty()) {
                            try {
                                stmt.execute(sql);
                            } catch (SQLException e) {
                                if (!e.getMessage().contains("table already exists")) {
                                    throw e;
                                }
                            }
                        }
                    }
                    insertTestDataIfEmpty(conn);
                }
                isInitialized = true;
                LOGGER.info("Base de données initialisée avec succès");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation", e);
                throw new RuntimeException("Erreur d'initialisation: " + e.getMessage(), e);
            }
        }
    }

    private static String loadSchemaFromResource() {
        LOGGER.info("Chargement du schéma SQL depuis les ressources...");
        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("Fichier schema.sql introuvable dans les ressources");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String schema = reader.lines().collect(Collectors.joining("\n"));
                LOGGER.info("Schéma SQL chargé avec succès");
                return schema;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la lecture du fichier schema.sql", e);
            throw new RuntimeException("Erreur lors de la lecture du schéma: " + e.getMessage(), e);
        }
    }

    private static void insertTestDataIfEmpty(Connection conn) throws SQLException {
        LOGGER.info("Vérification et insertion des données de test...");
        try (Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM produits");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO produits (nom, categorie, prix_achat, prix_vente, stock, seuil_alerte) VALUES " +
                           "('Saumon frais', 'Poisson', 15.00, 25.00, 50, 10)," +
                           "('Thon rouge', 'Poisson', 20.00, 35.00, 30, 5)," +
                           "('Crevettes', 'Fruits de mer', 12.00, 18.00, 100, 20)");
            }
            LOGGER.info("Données de test insérées avec succès");
        }
    }

    public static void checkDatabaseHealth() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA quick_check");
            stmt.execute("PRAGMA integrity_check");
            stmt.execute("PRAGMA foreign_key_check");
            LOGGER.info("Vérification de la santé de la base de données réussie");
        }
    }
}