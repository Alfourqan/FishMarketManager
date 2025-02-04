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
            // Supprime l'ancien compte admin s'il existe
            try (PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM users WHERE username = ?")) {
                deleteStmt.setString(1, "admin");
                deleteStmt.executeUpdate();
                LOGGER.info("Ancien compte admin supprimé");
            }

            // Hash avec un coût moins élevé pour le debug
            String salt = BCrypt.gensalt(10); // Coût réduit pour le debug
            String hashedPassword = BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, salt);

            // Log détaillé pour le debug
            LOGGER.info("Création du compte admin:");
            LOGGER.info("Mot de passe: " + DEFAULT_ADMIN_PASSWORD);
            LOGGER.info("Salt: " + salt);
            LOGGER.info("Hash: " + hashedPassword);

            // Test de vérification avant insertion
            boolean preTest = BCrypt.checkpw(DEFAULT_ADMIN_PASSWORD, hashedPassword);
            LOGGER.info("Test pré-insertion: " + preTest);

            // Crée le nouveau compte admin
            try (PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO users (username, password, role, active) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, "admin");
                insertStmt.setString(2, hashedPassword);
                insertStmt.setString(3, "ADMIN");
                insertStmt.setBoolean(4, true);
                insertStmt.executeUpdate();

                // Vérifie l'insertion
                ResultSet rs = insertStmt.getGeneratedKeys();
                if (rs.next()) {
                    int userId = rs.getInt(1);
                    LOGGER.info("Compte admin créé avec ID: " + userId);

                    // Test de vérification post-insertion
                    try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT password FROM users WHERE id = ?")) {
                        checkStmt.setInt(1, userId);
                        ResultSet checkRs = checkStmt.executeQuery();
                        if (checkRs.next()) {
                            String storedHash = checkRs.getString("password");
                            boolean postTest = BCrypt.checkpw(DEFAULT_ADMIN_PASSWORD, storedHash);
                            LOGGER.info("Test post-insertion - Hash stocké: " + storedHash);
                            LOGGER.info("Test post-insertion - Résultat: " + postTest);
                        }
                    }
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
            LOGGER.info("Mot de passe fourni: " + password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                boolean isActive = rs.getBoolean("active");

                LOGGER.info("Hash stocké pour " + username + ": " + storedHash);

                if (!isActive) {
                    LOGGER.warning("Compte désactivé: " + username);
                    return false;
                }

                boolean authenticated = BCrypt.checkpw(password, storedHash);
                LOGGER.info("Résultat authentification pour " + username + ": " + authenticated);

                if (authenticated) {
                    updateLastLogin(conn, username);
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