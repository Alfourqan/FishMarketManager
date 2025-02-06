package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static volatile boolean isInitialized = false;
    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static final String DB_FILE = "poissonnerie.db";
    private static SQLiteConfig config;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final int BUSY_TIMEOUT_MS = 30000;
    private static Connection singletonConnection = null;
    private static final Object CONNECTION_LOCK = new Object();

    static {
        config = new SQLiteConfig();
        config.setOpenMode(SQLiteOpenMode.READWRITE);
        config.setBusyTimeout(BUSY_TIMEOUT_MS);
        config.setCacheSize(2000);
        config.setPageSize(4096);
        config.enforceForeignKeys(true);
        // Force le mode WAL pour une meilleure gestion des transactions concurrentes
        try (Connection conn = createNewConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du mode WAL", e);
        }
    }

    private static synchronized Connection getSingletonConnection() throws SQLException {
        synchronized (CONNECTION_LOCK) {
            if (singletonConnection == null || singletonConnection.isClosed()) {
                singletonConnection = createNewConnection();
                initializePragmas(singletonConnection);
                LOGGER.info("Nouvelle connexion créée");
            } else {
                try {
                    // Vérifier si la connexion est vraiment valide
                    if (!singletonConnection.isValid(5)) {
                        singletonConnection.close();
                        singletonConnection = createNewConnection();
                        initializePragmas(singletonConnection);
                        LOGGER.info("Connexion recréée car invalide");
                    }
                } catch (SQLException e) {
                    LOGGER.warning("Erreur lors de la vérification de la validité de la connexion: " + e.getMessage());
                    singletonConnection = createNewConnection();
                    initializePragmas(singletonConnection);
                }
            }
            return singletonConnection;
        }
    }

    public static void reinitializeDatabase() {
        LOGGER.info("Réinitialisation forcée de la base de données...");
        synchronized (CONNECTION_LOCK) {
            try {
                closeConnection();
                isInitialized = false;

                // Suppression du fichier de base de données existant
                File dbFile = new File(DB_FILE);
                if (dbFile.exists()) {
                    if (dbFile.delete()) {
                        LOGGER.info("Ancien fichier de base de données supprimé");
                    } else {
                        LOGGER.warning("Impossible de supprimer l'ancien fichier de base de données");
                    }
                }

                // Réinitialisation complète
                initDatabase();
                LOGGER.info("Base de données réinitialisée avec succès");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation de la base de données", e);
                throw new RuntimeException("Erreur lors de la réinitialisation de la base de données", e);
            }
        }
    }

    private static void initializePragmas(Connection conn) throws SQLException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("La connexion est null ou fermée");
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);
            stmt.execute("PRAGMA cache_size = 2000");
            stmt.execute("PRAGMA page_size = 4096");
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA locking_mode = NORMAL");
            stmt.execute("PRAGMA temp_store = MEMORY");
            LOGGER.info("PRAGMAs initialisés avec succès");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation des PRAGMAs", e);
            throw e;
        }
    }

    private static Connection createNewConnection() throws SQLException {
        int retries = 0;
        SQLException lastException = null;

        while (retries < MAX_RETRIES) {
            try {
                Connection conn = config.createConnection("jdbc:sqlite:" + DB_FILE);
                conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                LOGGER.info("Nouvelle connexion créée avec succès");
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
        ensureInitialized();
        return getSingletonConnection();
    }

    private static void ensureInitialized() {
        if (!isInitialized) {
            INIT_LOCK.lock();
            try {
                if (!isInitialized) {
                    initDatabase();
                }
            } finally {
                INIT_LOCK.unlock();
            }
        }
    }

    public static void initDatabase() {
        if (isInitialized) {
            LOGGER.info("Base de données déjà initialisée");
            return;
        }

        INIT_LOCK.lock();
        try {
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

            try {
                Connection conn = getSingletonConnection();
                initializePragmas(conn);

                boolean originalAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                try {
                    String schema = loadSchemaFromResource();
                    if (schema == null || schema.trim().isEmpty()) {
                        throw new IllegalStateException("Schema SQL vide ou introuvable");
                    }

                    try (Statement stmt = conn.createStatement()) {
                        String[] statements = schema.split(";");
                        for (String sql : statements) {
                            sql = sql.trim();
                            if (!sql.isEmpty() && !sql.toUpperCase().startsWith("PRAGMA")) {
                                try {
                                    LOGGER.info("Exécution de la requête: " + sql);
                                    stmt.execute(sql);
                                } catch (SQLException e) {
                                    if (!e.getMessage().contains("table already exists")) {
                                        throw e;
                                    }
                                }
                            }
                        }
                    }

                    conn.commit();
                    isInitialized = true;
                    LOGGER.info("Base de données initialisée avec succès");
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(Level.WARNING, "Erreur lors du rollback", re);
                    }
                    throw e;
                } finally {
                    try {
                        conn.setAutoCommit(originalAutoCommit);
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la restauration de l'autocommit", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation", e);
                throw new RuntimeException("Erreur d'initialisation: " + e.getMessage(), e);
            }
        } finally {
            INIT_LOCK.unlock();
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

    public static void checkDatabaseHealth() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA quick_check");
            stmt.execute("PRAGMA integrity_check");
            stmt.execute("PRAGMA foreign_key_check");
            LOGGER.info("Vérification de la santé de la base de données réussie");
        }
    }

    private static void closeConnection() {
        if (singletonConnection != null) {
            try {
                if (!singletonConnection.isClosed()) {
                    singletonConnection.close();
                    LOGGER.info("Connexion fermée avec succès");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
            } finally {
                singletonConnection = null;
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("init")) {
            LOGGER.info("Initialisation forcée de la base de données...");
            reinitializeDatabase();
        } else {
            LOGGER.info("Vérification de la base de données...");
            try {
                initDatabase();
                checkDatabaseHealth();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de la base de données", e);
                System.exit(1);
            }
        }
    }
}