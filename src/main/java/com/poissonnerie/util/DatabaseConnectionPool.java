
package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

public class DatabaseConnectionPool {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnectionPool.class.getName());
    private static final String DB_FILE = "poissonnerie.db";
    private static SQLiteConfig config;

    static {
        try {
            config = new SQLiteConfig();
            config.setOpenMode(SQLiteOpenMode.READWRITE);
            config.setBusyTimeout(30000);
            config.enforceForeignKeys(true);
            config.setTempStore(SQLiteConfig.TempStore.MEMORY);
            config.setCacheSize(2000);
            config.setPageSize(4096);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la configuration SQLite", e);
            throw new RuntimeException("Échec de l'initialisation de la configuration SQLite", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + DB_FILE;
        Connection conn = DriverManager.getConnection(url, config.toProperties());
        if (conn == null) {
            throw new SQLException("Impossible de créer une connexion à la base de données");
        }
        return conn;
    }

    private DatabaseConnectionPool() {
        // Constructeur privé pour empêcher l'instanciation
    }
}
