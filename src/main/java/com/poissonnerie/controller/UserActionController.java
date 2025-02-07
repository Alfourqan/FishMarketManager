package com.poissonnerie.controller;

import com.poissonnerie.model.UserAction;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class UserActionController {
    private static final Logger LOGGER = Logger.getLogger(UserActionController.class.getName());
    private static final String TABLE_NAME = "user_actions";
    private static UserActionController instance;
    private Integer currentUserId;
    private String currentUsername;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    private UserActionController() {
        try {
            migrateTable();
            LOGGER.info("UserActionController initialisé avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de UserActionController", e);
            throw new RuntimeException("Erreur lors de l'initialisation de UserActionController", e);
        }
    }

    public static UserActionController getInstance() {
        if (instance == null) {
            synchronized (UserActionController.class) {
                if (instance == null) {
                    instance = new UserActionController();
                }
            }
        }
        return instance;
    }

    public void setCurrentUser(Integer userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
        LOGGER.info("Utilisateur courant défini: " + username + " (ID: " + userId + ")");
    }

    private void migrateTable() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTables(null, null, TABLE_NAME, null);

            if (!rs.next()) {
                createTable(conn);
            }
        }
    }

    private void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "action_type TEXT NOT NULL," +
                "username TEXT NOT NULL," +
                "date_time TEXT NOT NULL," +
                "description TEXT NOT NULL," +
                "entity_type TEXT NOT NULL," +
                "entity_id INTEGER NOT NULL," +
                "user_id INTEGER" +
                ")");
        }
    }

    public void logAction(UserAction action) {
        if (action == null) {
            LOGGER.warning("Tentative de journalisation d'une action null");
            return;
        }

        if (currentUserId == null) {
            LOGGER.warning("Tentative de journalisation sans utilisateur connecté");
            action.setUsername("SYSTEM");
        } else {
            action.setUsername(currentUsername);
            action.setUserId(currentUserId);
        }

        String sql = "INSERT INTO " + TABLE_NAME + 
            " (username, action_type, entity_type, entity_id, description, date_time, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        for (int retry = 0; retry < MAX_RETRIES; retry++) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, action.getUsername());
                    stmt.setString(2, action.getType().getValue());
                    stmt.setString(3, action.getEntityType().getValue());
                    stmt.setInt(4, action.getEntityId());
                    stmt.setString(5, action.getDescription());
                    stmt.setString(6, action.getDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    stmt.setObject(7, action.getUserId());

                    stmt.executeUpdate();
                    conn.commit();
                    LOGGER.info("Action utilisateur enregistrée avec succès");
                    return;
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                    }
                    if (retry < MAX_RETRIES - 1) {
                        LOGGER.warning("Tentative " + (retry + 1) + " échouée: " + e.getMessage());
                        Thread.sleep(RETRY_DELAY_MS * (1L << retry));
                    } else {
                        throw new RuntimeException("Échec de l'enregistrement après " + MAX_RETRIES + " tentatives", e);
                    }
                }
            } catch (SQLException e) {
                if (retry == MAX_RETRIES - 1) {
                    throw new RuntimeException("Erreur de connexion à la base de données", e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interruption pendant la tentative", e);
            }
        }
    }

    public List<UserAction> getActions(LocalDateTime debut, LocalDateTime fin) {
        List<UserAction> actions = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE date_time BETWEEN ? AND ? ORDER BY date_time DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, debut.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setString(2, fin.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserAction action = new UserAction(
                        UserAction.ActionType.valueOf(rs.getString("action_type")),
                        rs.getString("username"),
                        rs.getString("description"),
                        UserAction.EntityType.valueOf(rs.getString("entity_type")),
                        rs.getInt("entity_id")
                    );
                    action.setDateTime(LocalDateTime.parse(
                        rs.getString("date_time"), 
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    ));
                    Object userId = rs.getObject("user_id");
                    if (userId != null) {
                        action.setUserId((Integer) userId);
                    }
                    actions.add(action);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des actions", e);
            throw new RuntimeException("Erreur lors de la récupération des actions", e);
        }
        return actions;
    }
    public void purgerActions(LocalDateTime dateLimite) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE date_time < ?";
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, dateLimite.format(formatter));
            int nbSuppression = pstmt.executeUpdate();

            LOGGER.info(String.format("Purge des actions utilisateur : %d entrées supprimées", nbSuppression));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la purge des actions utilisateur", e);
            throw new RuntimeException("Erreur lors de la purge des actions utilisateur", e);
        }
    }
}