package com.poissonnerie.controller;

import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.Instant;

public class AuthenticationController {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());
    private static AuthenticationController instance;
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 300000; // 5 minutes

    private AuthenticationController() {
        initializeDatabase();
    }

    public static AuthenticationController getInstance() {
        if (instance == null) {
            instance = new AuthenticationController();
        }
        return instance;
    }

    private void initializeDatabase() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM users WHERE username = ? AND role = 'ADMIN'")) {
            stmt.setString(1, "admin");
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                // Créer l'utilisateur admin par défaut si non existant
                createDefaultAdmin();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Erreur d'initialisation de la base de données", e);
        }
    }

    private void createDefaultAdmin() {
        int retries = 3;
        while (retries > 0) {
            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn == null) {
                    retries--;
                    Thread.sleep(1000);
                    continue;
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, role) VALUES (?, ?, 'ADMIN')")) {
                    String hashedPassword = hashPassword("admin"); // Hash le mot de passe par défaut
                    LOGGER.info("Création de l'utilisateur admin avec mot de passe hashé");
                    stmt.setString(1, "admin");
                    stmt.setString(2, hashedPassword);
                    stmt.executeUpdate();
                    LOGGER.info("Utilisateur administrateur par défaut créé avec succès");
                    return; //Exit after successful creation.
                } catch (SQLException | NoSuchAlgorithmException e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la création de l'admin par défaut", e);
                    retries--;
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Thread interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new RuntimeException("Impossible de créer l'utilisateur admin après plusieurs tentatives.");
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
                boolean isActive = rs.getBoolean("active");
                if (!isActive) {
                    LOGGER.warning("Tentative de connexion sur un compte désactivé: " + username);
                    return false;
                }

                String storedHashedPassword = rs.getString("password");
                String providedHashedPassword = hashPassword(password);
                boolean authenticated = storedHashedPassword.equals(providedHashedPassword);

                LOGGER.info("Comparaison des mots de passe pour " + username + 
                          "\nStored hash: " + storedHashedPassword.substring(0, 10) + "..." +
                          "\nProvided hash: " + providedHashedPassword.substring(0, 10) + "...");

                if (authenticated) {
                    updateLastLogin(conn, username);
                    LOGGER.info("Authentification réussie pour l'utilisateur: " + username);
                } else {
                    LOGGER.warning("Échec d'authentification pour l'utilisateur: " + username + " (mot de passe incorrect)");
                }

                return authenticated;
            } else {
                LOGGER.warning("Utilisateur non trouvé: " + username);
                return false;
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
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

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes());
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public boolean changePassword(String username, String currentPassword, String newPassword) {
        if (!authenticate(username, currentPassword)) {
            LOGGER.warning("Tentative de changement de mot de passe avec mauvais mot de passe actuel");
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE users SET password = ? WHERE username = ?")) {
            stmt.setString(1, hashPassword(newPassword));
            stmt.setString(2, username);

            int updated = stmt.executeUpdate();
            boolean success = updated > 0;

            if (success) {
                LOGGER.info("Mot de passe changé avec succès pour l'utilisateur: " + username);
            } else {
                LOGGER.warning("Échec du changement de mot de passe pour l'utilisateur: " + username);
            }

            return success;
        } catch (SQLException | NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du changement de mot de passe", e);
            return false;
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