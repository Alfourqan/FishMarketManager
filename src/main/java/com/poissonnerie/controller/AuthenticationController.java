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
            initializeDatabase();
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

    private void initializeDatabase() {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // Supprime l'ancien compte admin s'il existe
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM users WHERE username = ?")) {
                deleteStmt.setString(1, "admin");
                deleteStmt.executeUpdate();
                LOGGER.info("Ancien compte admin supprimé");
            }

            // Hash avec un coût moins élevé pour le debug
            String salt = BCrypt.gensalt(10);
            String hashedPassword = BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, salt);

            // Crée le nouveau compte admin
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
                    LOGGER.info("Compte admin créé avec ID: " + userId);

                    // Crée et attribue le rôle admin
                    Role adminRole = new Role("ADMIN", "Administrateur système");
                    adminRole = roleController.creerRole(adminRole);
                    roleController.attribuerRoleUtilisateur(userId, adminRole.getId());

                    conn.commit();
                    LOGGER.info("Rôle admin attribué avec succès");
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Database initialization error", e);
            throw new RuntimeException("Failed to initialize database", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close database connection", e);
                }
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
            LOGGER.info("Mise à jour de la dernière connexion pour l'utilisateur: " + userId);
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