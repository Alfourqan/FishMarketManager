package com.poissonnerie.util;

import java.sql.*;
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
        try {
            config = new SQLiteConfig();
            config.setOpenMode(SQLiteOpenMode.READWRITE);
            config.setBusyTimeout(30000);
            config.setSharedCache(true);
            config.enableLoadExtension(true);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
            config.setTempStore(SQLiteConfig.TempStore.MEMORY);
            config.setCacheSize(2000);
            config.setPageSize(4096);
            LOGGER.info("Configuration SQLite initialisée");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la configuration SQLite", e);
            throw new RuntimeException(e);
        }
    }

    private DatabaseManager() {
        // Constructeur privé pour empêcher l'instanciation
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized.get()) {
            initializeDatabase();
        }
        return getOrCreateConnection();
    }

    private static Connection createNewConnection() throws SQLException {
        try {
            Connection conn = config.createConnection("jdbc:sqlite:" + DB_FILE);
            if (conn == null) {
                throw new SQLException("Impossible de créer une connexion à la base de données");
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA busy_timeout = 30000");
            }

            LOGGER.info("Nouvelle connexion créée avec succès");
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création de la connexion", e);
            throw e;
        }
    }

    private static Connection getOrCreateConnection() throws SQLException {
        synchronized (CONNECTION_LOCK) {
            if (singletonConnection == null || singletonConnection.isClosed()) {
                singletonConnection = createNewConnection();
            }
            return singletonConnection;
        }
    }

    public static void initializeDatabase() throws SQLException {
        if (isInitialized.get()) {
            return;
        }

        INIT_LOCK.lock();
        try {
            if (!isInitialized.get()) {
                createDatabaseFile();
                setupDatabase();
                isInitialized.set(true);
                LOGGER.info("Base de données initialisée avec succès");
            }
        } finally {
            INIT_LOCK.unlock();
        }
    }

    private static void createDatabaseFile() throws SQLException {
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
    }

    private static void setupDatabase() throws SQLException {
        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            // Créer une nouvelle connexion spécifique pour l'initialisation
            conn = createNewConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            String schema = loadSchemaFromResource();
            String[] statements = schema.split(";");

            try (Statement stmt = conn.createStatement()) {
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
            }

            conn.commit();
            LOGGER.info("Schéma de base de données créé avec succès");

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
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
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