package com.poissonnerie.controller;

import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.Instant;
import org.mindrot.jbcrypt.BCrypt;
import com.poissonnerie.model.Role;
import com.poissonnerie.model.Permission;
import java.util.Set;

public class AuthenticationController {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());
    private static AuthenticationController instance;
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final Object INSTANCE_LOCK = new Object();
    private RoleController roleController;

    private AuthenticationController() {
        try {
            setupDatabase();
            this.roleController = RoleController.getInstance();
            LOGGER.info("AuthenticationController initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize AuthenticationController", e);
            throw new RuntimeException("Failed to initialize AuthenticationController", e);
        }
    }

    public static AuthenticationController getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new AuthenticationController();
                }
            }
        }
        return instance;
    }

    private void setupDatabase() throws SQLException {
        // Drop existing tables to ensure clean slate
        dropExistingTables();

        // Create schema and initialize data
        createSchema();
        createInitialData();
    }

    private void dropExistingTables() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = OFF");

            // Drop tables in reverse order of dependencies
            stmt.execute("DROP TABLE IF EXISTS user_roles");
            stmt.execute("DROP TABLE IF EXISTS roles");
            stmt.execute("DROP TABLE IF EXISTS users");

            stmt.execute("PRAGMA foreign_keys = ON");

            LOGGER.info("Existing tables dropped successfully");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error dropping tables", e);
        }
    }

    private void createSchema() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Set pragmas for better concurrency and reliability
            try (Statement pragmaStmt = conn.createStatement()) {
                pragmaStmt.execute("PRAGMA journal_mode = WAL");
                pragmaStmt.execute("PRAGMA busy_timeout = 5000");
                pragmaStmt.execute("PRAGMA synchronous = NORMAL");
                pragmaStmt.execute("PRAGMA foreign_keys = ON");
            }

            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                // Create roles table first
                stmt.execute("CREATE TABLE IF NOT EXISTS roles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "description TEXT, " +
                    "created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000))");

                // Create users table
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " +
                    "active BOOLEAN DEFAULT true, " +
                    "created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000), " +
                    "last_login INTEGER, " +
                    "force_password_reset BOOLEAN DEFAULT false)");

                // Create user_roles table last
                stmt.execute("CREATE TABLE IF NOT EXISTS user_roles (" +
                    "user_id INTEGER NOT NULL, " +
                    "role_id INTEGER NOT NULL, " +
                    "created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000), " +
                    "PRIMARY KEY (user_id, role_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id), " +
                    "FOREIGN KEY (role_id) REFERENCES roles(id))");

                conn.commit();
                LOGGER.info("Database schema created successfully");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error creating schema", e);
                conn.rollback();
                throw e;
            }
        }
    }

    private void createInitialData() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Create ADMIN role first
                int roleId;
                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO roles (name, description) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, "ADMIN");
                    stmt.setString(2, "Administrateur système");
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Failed to create ADMIN role");
                        }
                        roleId = rs.getInt(1);
                        LOGGER.info("Created ADMIN role with ID: " + roleId);
                    }
                }

                // Create admin user
                String hashedPassword = BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, BCrypt.gensalt(10));
                int userId;

                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password, active) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, "admin");
                    stmt.setString(2, hashedPassword);
                    stmt.setBoolean(3, true);
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Failed to create admin user");
                        }
                        userId = rs.getInt(1);
                        LOGGER.info("Created admin user with ID: " + userId);
                    }
                }

                // Assign admin role to user
                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, roleId);
                    stmt.executeUpdate();
                    LOGGER.info("Assigned ADMIN role to admin user");
                }

                conn.commit();
                LOGGER.info("Initial data created successfully");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error initializing data", e);
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            LOGGER.warning("Tentative d'authentification avec des identifiants vides");
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, password, active FROM users WHERE username = ?")) {

            stmt.setString(1, username.trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                boolean isActive = rs.getBoolean("active");
                int userId = rs.getInt("id");

                if (!isActive) {
                    LOGGER.warning("Compte désactivé: " + username);
                    return false;
                }

                boolean authenticated = BCrypt.checkpw(password, storedHash);
                if (authenticated) {
                    updateLastLogin(userId);
                    LOGGER.info("Authentification réussie pour: " + username);
                } else {
                    LOGGER.warning("Échec d'authentification pour: " + username);
                }

                return authenticated;
            }

            LOGGER.warning("Utilisateur non trouvé: " + username);
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'authentification", e);
            return false;
        }
    }

    private void updateLastLogin(int userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE users SET last_login = ? WHERE id = ?")) {
            stmt.setLong(1, Instant.now().toEpochMilli());
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la mise à jour de la dernière connexion", e);
        }
    }

    public Set<Role> getUserRoles(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id");
                return roleController.getRolesUtilisateur(userId);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des rôles", e);
        }
        return Set.of();
    }

    public boolean hasPermission(String username, String permissionCode) {
        Set<Role> roles = getUserRoles(username);
        return roles.stream()
            .anyMatch(role -> role.hasPermission(permissionCode));
    }

    public boolean isUserActive(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT active FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("active");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification du statut utilisateur", e);
            return false;
        }
    }
}