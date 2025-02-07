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
    private static final String DB_FILE = "poissonnerie.db";
    private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static SQLiteConfig config;
    private static volatile Connection singletonConnection = null;
    private static final Object CONNECTION_LOCK = new Object();

    static {
        try {
            // Configuration de base de SQLite
            config = new SQLiteConfig();
            config.setOpenMode(SQLiteOpenMode.READWRITE);
            config.setBusyTimeout(30000);

            // Configuration de sécurité
            config.enforceForeignKeys(true);
            config.setTempStore(SQLiteConfig.TempStore.MEMORY);
            config.setCacheSize(2000);
            config.setPageSize(4096);

            LOGGER.info("Configuration SQLite initialisée");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la configuration SQLite", e);
            throw new RuntimeException("Échec de l'initialisation de la configuration SQLite", e);
        }
    }

    private DatabaseManager() {
        // Constructeur privé pour empêcher l'instanciation
    }

    public static Connection getConnection() throws SQLException {
        if (!isInitialized.get()) {
            initializeDatabase();
        }
        Connection conn = createNewConnection();
        // Configurer la connexion avant utilisation
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("PRAGMA synchronous=NORMAL");
        }
        conn.setAutoCommit(true);
        return conn;
    }

    private static Connection createNewConnection() throws SQLException {
        try {
            String url = "jdbc:sqlite:" + DB_FILE;
            Connection conn = DriverManager.getConnection(url, config.toProperties());

            if (conn == null) {
                throw new SQLException("Impossible de créer une connexion à la base de données");
            }

            // Configuration des paramètres après la connexion
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
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
                singletonConnection.setAutoCommit(false);
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
        try {
            conn = createNewConnection();

            // Configuration PRAGMA avant toute transaction
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA busy_timeout = 5000");
                stmt.execute("PRAGMA foreign_keys = OFF");
            }

            conn.setAutoCommit(false);
            try {
                String schema = loadSchemaFromResource();
                String[] statements = schema.split(";");

                // Exécution du schéma
                for (String sql : statements) {
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql);
                        } catch (SQLException e) {
                            if (!e.getMessage().contains("table already exists")) {
                                throw e;
                            }
                        }
                    }
                }

                // Réactivation des clés étrangères après la création du schéma
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }

                conn.commit();
                LOGGER.info("Schéma de base de données créé avec succès");
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                }
                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
                throw e;
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture de la connexion", e);
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