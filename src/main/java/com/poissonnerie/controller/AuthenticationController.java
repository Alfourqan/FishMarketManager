package com.poissonnerie.controller;

import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.Instant;
import org.mindrot.jbcrypt.BCrypt;

public class AuthenticationController {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());
    private static AuthenticationController instance;
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final Object INSTANCE_LOCK = new Object();

    private AuthenticationController() {
        initializeDatabase();
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
        try (Connection conn = DatabaseManager.getConnection()) {
            String salt = BCrypt.gensalt(12);
            String hashedPassword = BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, salt);
            LOGGER.info("Création du compte admin avec mot de passe hashé");

            // Supprime l'ancien compte admin s'il existe
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM users WHERE username = ?")) {
                deleteStmt.setString(1, "admin");
                deleteStmt.executeUpdate();
            }

            // Crée le nouveau compte admin
            try (PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO users (username, password, role, active, force_password_reset) VALUES (?, ?, ?, ?, ?)")) {
                insertStmt.setString(1, "admin");
                insertStmt.setString(2, hashedPassword);
                insertStmt.setString(3, "ADMIN");
                insertStmt.setBoolean(4, true);
                insertStmt.setBoolean(5, false);
                insertStmt.executeUpdate();

                // Vérifie que le mot de passe peut être vérifié
                if (BCrypt.checkpw(DEFAULT_ADMIN_PASSWORD, hashedPassword)) {
                    LOGGER.info("Compte admin créé avec succès, vérification du hash réussie");
                } else {
                    LOGGER.severe("Erreur de vérification du hash du mot de passe admin");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Erreur lors de l'initialisation de la base de données", e);
        }
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            LOGGER.warning("Tentative d'authentification avec des identifiants vides");
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT password, active FROM users WHERE username = ?")) {
            stmt.setString(1, username.trim());
            LOGGER.info("Tentative d'authentification pour l'utilisateur: " + username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                boolean isActive = rs.getBoolean("active");

                if (!isActive) {
                    LOGGER.warning("Tentative de connexion sur un compte désactivé: " + username);
                    return false;
                }

                boolean authenticated = BCrypt.checkpw(password, storedPassword);
                LOGGER.info("Vérification du mot de passe pour " + username + 
                          " - Hash stocké: " + storedPassword + 
                          " - Authentification: " + (authenticated ? "réussie" : "échouée"));

                if (authenticated) {
                    updateLastLogin(conn, username);
                    LOGGER.info("Authentification réussie pour l'utilisateur: " + username);
                } else {
                    LOGGER.warning("Échec d'authentification pour l'utilisateur: " + username);
                }

                return authenticated;
            } else {
                LOGGER.warning("Utilisateur non trouvé: " + username);
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'authentification", e);
            return false;
        }
    }

    private void updateLastLogin(Connection conn, String username) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET last_login = ? WHERE username = ?")) {
            stmt.setLong(1, Instant.now().toEpochMilli());
            stmt.setString(2, username);
            stmt.executeUpdate();
            LOGGER.info("Mise à jour de la dernière connexion pour: " + username);
        }
    }

    public String getUserRole(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT role FROM users WHERE username = ?")) {
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération du rôle", e);
        }
        return null;
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