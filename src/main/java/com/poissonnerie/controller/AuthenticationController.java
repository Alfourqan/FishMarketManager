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
            this.roleController = RoleController.getInstance();
            createAdminIfNeeded();
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

    private void createAdminIfNeeded() throws SQLException {
        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;
        SQLException lastException = null;

        while (!success && retryCount < maxRetries) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Vérifier si la table existe
                    DatabaseMetaData meta = conn.getMetaData();
                    ResultSet tables = meta.getTables(null, null, "users", null);
                    if (!tables.next()) {
                        // Créer la table users si elle n'existe pas
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL UNIQUE," +
                                "password TEXT NOT NULL," +
                                "active BOOLEAN DEFAULT true," +
                                "created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000)," +
                                "last_login INTEGER," +
                                "force_password_reset BOOLEAN DEFAULT false)");
                        }
                    }

                    // Vérifier si l'admin existe
                    try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT id FROM users WHERE username = ?")) {
                        checkStmt.setString(1, "admin");
                        ResultSet rs = checkStmt.executeQuery();
                        if (!rs.next()) {
                            createAdmin(conn);
                        }
                    }

                    conn.commit();
                    success = true;
                    LOGGER.info("Admin initialization completed successfully");
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(Level.WARNING, "Error during rollback", re);
                    }
                    if (e.getMessage().contains("database is locked") && retryCount < maxRetries - 1) {
                        LOGGER.log(Level.WARNING, "Database locked, retrying... Attempt " + (retryCount + 1));
                        retryCount++;
                        Thread.sleep(1000 * (retryCount + 1)); // Exponential backoff
                        lastException = e;
                    } else {
                        throw e;
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for retry", ie);
            }
        }

        if (!success && lastException != null) {
            throw new SQLException("Failed to initialize admin after " + maxRetries + " attempts", lastException);
        }
    }

    private void createAdmin(Connection conn) throws SQLException {
        String salt = BCrypt.gensalt(10);
        String hashedPassword = BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, salt);

        try (PreparedStatement insertStmt = conn.prepareStatement(
            "INSERT INTO users (username, password, active) VALUES (?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {

            insertStmt.setString(1, "admin");
            insertStmt.setString(2, hashedPassword);
            insertStmt.setBoolean(3, true);
            insertStmt.executeUpdate();

            ResultSet rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                int userId = rs.getInt(1);
                LOGGER.info("Admin account created with ID: " + userId);

                // Créer et attribuer le rôle admin
                Role adminRole = roleController.creerRole(new Role("ADMIN", "Administrateur système"));
                roleController.attribuerRoleUtilisateur(userId, adminRole.getId());
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