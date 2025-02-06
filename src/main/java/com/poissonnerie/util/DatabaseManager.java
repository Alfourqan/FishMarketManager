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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static final String DB_FILE = "poissonnerie.db";
    private static SQLiteConfig config;
    private static volatile Connection singletonConnection = null;
    private static final Object CONNECTION_LOCK = new Object();
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    static {
        config = new SQLiteConfig();
        config.setOpenMode(SQLiteOpenMode.READWRITE);
        config.setBusyTimeout(30000);
        config.setCacheSize(2000);
        config.setPageSize(4096);
        config.enforceForeignKeys(true);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
    }

    private DatabaseManager() {
        // Constructeur privé pour empêcher l'instanciation
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (!isInitialized.get()) {
            initializeDatabase();
        }
        return getOrCreateConnection();
    }

    private static Connection getOrCreateConnection() throws SQLException {
        synchronized (CONNECTION_LOCK) {
            if (singletonConnection == null || singletonConnection.isClosed()) {
                int retries = 0;
                SQLException lastException = null;

                while (retries < MAX_RETRIES) {
                    try {
                        singletonConnection = createNewConnection();
                        return singletonConnection;
                    } catch (SQLException e) {
                        lastException = e;
                        LOGGER.warning("Tentative " + (retries + 1) + " de connexion échouée: " + e.getMessage());
                        retries++;
                        if (retries < MAX_RETRIES) {
                            try {
                                Thread.sleep(RETRY_DELAY_MS * retries);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new SQLException("Interruption pendant la tentative de connexion", ie);
                            }
                        }
                    }
                }
                throw new SQLException("Échec de la connexion après " + MAX_RETRIES + " tentatives", lastException);
            }
            return singletonConnection;
        }
    }

    private static Connection createNewConnection() throws SQLException {
        Connection conn = config.createConnection("jdbc:sqlite:" + DB_FILE);
        initializePragmas(conn);
        LOGGER.info("Nouvelle connexion créée");
        return conn;
    }

    private static void initializePragmas(Connection conn) throws SQLException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("La connexion est null ou fermée");
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA cache_size = 2000");
            stmt.execute("PRAGMA page_size = 4096");
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA locking_mode = NORMAL");
            stmt.execute("PRAGMA busy_timeout = 30000");
            stmt.execute("PRAGMA temp_store = MEMORY");
            LOGGER.info("PRAGMAs initialisés avec succès");
        }
    }

    public static void initializeDatabase() throws SQLException {
        if (!isInitialized.get()) {
            INIT_LOCK.lock();
            try {
                if (!isInitialized.get()) {
                    initializeDatabaseInternal();
                    isInitialized.set(true);
                }
            } finally {
                INIT_LOCK.unlock();
            }
        }
    }

    private static void initializeDatabaseInternal() throws SQLException {
        File dbFile = new File(DB_FILE);
        if (!dbFile.exists()) {
            try {
                if (!dbFile.createNewFile()) {
                    throw new IOException("Impossible de créer le fichier de base de données");
                }
                LOGGER.info("Fichier de base de données créé");
            } catch (IOException e) {
                throw new SQLException("Erreur lors de la création du fichier de base de données", e);
            }
        }

        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = getOrCreateConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            String schema = loadSchemaFromResource();
            executeSchema(conn, schema);

            conn.commit();
            LOGGER.info("Base de données initialisée avec succès");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Erreur lors de la restauration de l'autocommit", e);
                }
            }
        }
    }

    private static String loadSchemaFromResource() throws SQLException {
        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new SQLException("Fichier schema.sql introuvable dans les ressources");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new SQLException("Erreur lors de la lecture du schéma", e);
        }
    }

    private static void executeSchema(Connection conn, String schema) throws SQLException {
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
            LOGGER.info("Schéma exécuté avec succès");
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

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("init")) {
            LOGGER.info("Initialisation forcée de la base de données...");
            try {
                initializeDatabase();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation forcée", e);
                System.exit(1);
            }
        }
    }
}