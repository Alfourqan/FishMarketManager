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
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 300000; // 5 minutes
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final Object INSTANCE_LOCK = new Object();
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

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
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(true); // Pour cette vérification simple
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM users WHERE username = ? AND role = 'ADMIN'")) {
                    stmt.setString(1, "admin");
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        createDefaultAdmin(conn);
                    }
                    return;
                }
            } catch (SQLException e) {
                if (e.getMessage().contains("database is locked") && retries < MAX_RETRIES - 1) {
                    retries++;
                    LOGGER.warning("Database locked, retrying admin check... (attempt " + retries + "/" + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retries);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOGGER.warning("Interrupted while waiting for retry");
                    }
                    continue;
                }
                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de la base de données", e);
                throw new RuntimeException("Erreur d'initialisation de la base de données", e);
            }
        }
    }

    private void createDefaultAdmin(Connection existingConn) {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            Connection conn = null;
            try {
                conn = (existingConn != null) ? existingConn : DatabaseManager.getConnection();
                boolean shouldCommit = existingConn == null;

                if (shouldCommit) {
                    conn.setAutoCommit(false);
                }

                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password, role, active, force_password_reset) VALUES (?, ?, 'ADMIN', true, true)")) {

                    String hashedPassword = BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, BCrypt.gensalt(12));
                    LOGGER.info("Création de l'utilisateur admin avec mot de passe hashé");

                    stmt.setString(1, "admin");
                    stmt.setString(2, hashedPassword);
                    stmt.executeUpdate();

                    if (shouldCommit) {
                        conn.commit();
                    }
                    LOGGER.info("Utilisateur administrateur par défaut créé avec succès");
                    return;

                } catch (SQLException e) {
                    if (shouldCommit) {
                        try {
                            conn.rollback();
                        } catch (SQLException re) {
                            LOGGER.log(Level.WARNING, "Erreur lors du rollback", re);
                        }
                    }
                    throw e;
                } finally {
                    if (shouldCommit && conn != null) {
                        try {
                            conn.setAutoCommit(true);
                        } catch (SQLException e) {
                            LOGGER.log(Level.WARNING, "Erreur lors de la restauration de l'autocommit", e);
                        }
                    }
                }
            } catch (SQLException e) {
                if (e.getMessage().contains("database is locked") && retries < MAX_RETRIES - 1) {
                    retries++;
                    LOGGER.warning("Database locked, retrying admin creation... (attempt " + retries + "/" + MAX_RETRIES + ")");
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retries);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOGGER.warning("Interrupted while waiting for retry");
                    }
                    continue;
                }
                LOGGER.log(Level.SEVERE, "Erreur lors de la création de l'admin par défaut", e);
                throw new RuntimeException("Erreur lors de la création de l'admin par défaut", e);
            } finally {
                if (existingConn == null && conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
                    }
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
                 "SELECT password, active, force_password_reset FROM users WHERE username = ?")) {
            stmt.setString(1, username.trim());
            LOGGER.info("Tentative d'authentification pour l'utilisateur: " + username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean isActive = rs.getBoolean("active");
                boolean forceReset = rs.getBoolean("force_password_reset");
                if (!isActive) {
                    LOGGER.warning("Tentative de connexion sur un compte désactivé: " + username);
                    return false;
                }
                if (forceReset) {
                    LOGGER.warning("Réinitialisation du mot de passe obligatoire pour l'utilisateur : " + username);
                    return false;
                }

                String storedPassword = rs.getString("password");
                boolean authenticated = BCrypt.checkpw(password, storedPassword);

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

    public boolean changePassword(String username, String currentPassword, String newPassword) {
        if (!authenticate(username, currentPassword)) {
            LOGGER.warning("Tentative de changement de mot de passe avec mauvais mot de passe actuel");
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE users SET password = ? WHERE username = ?")) {
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);

            int updated = stmt.executeUpdate();
            boolean success = updated > 0;

            if (success) {
                LOGGER.info("Mot de passe changé avec succès pour l'utilisateur: " + username);
            } else {
                LOGGER.warning("Échec du changement de mot de passe pour l'utilisateur: " + username);
            }

            return success;
        } catch (SQLException e) {
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

    public boolean isPasswordResetRequired(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT force_password_reset FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("force_password_reset");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification du reset de mot de passe", e);
            return false;
        }
    }

    public boolean markPasswordResetComplete(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE users SET force_password_reset = false WHERE username = ?")) {
            stmt.setString(1, username);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                LOGGER.info("Reset de mot de passe marqué comme complété pour: " + username);
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du statut de reset", e);
            return false;
        }
    }
    public boolean setForcePasswordReset(String username, boolean forceReset) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE users SET force_password_reset = ? WHERE username = ?")) {
            stmt.setBoolean(1, forceReset);
            stmt.setString(2, username);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                LOGGER.info("Force password reset " + (forceReset ? "activé" : "désactivé") + " pour: " + username);
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du force password reset", e);
            return false;
        }
    }
}