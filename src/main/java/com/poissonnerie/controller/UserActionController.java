package com.poissonnerie.controller;

import com.poissonnerie.model.UserAction;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private UserActionController() {
        try {
            createTableIfNotExists();
            LOGGER.info("UserActionController initialisé avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de UserActionController", e);
            throw new RuntimeException("Erreur lors de l'initialisation de UserActionController", e);
        }
    }

    public static UserActionController getInstance() {
        if (instance == null) {
            instance = new UserActionController();
        }
        return instance;
    }

    public void setCurrentUser(Integer userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
        LOGGER.info("Utilisateur courant défini: " + username + " (ID: " + userId + ")");
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "action_type TEXT NOT NULL," +
            "username TEXT NOT NULL," +
            "date_time TEXT NOT NULL," +
            "description TEXT NOT NULL," +
            "entity_type TEXT NOT NULL," +
            "entity_id INTEGER NOT NULL," +
            "user_id INTEGER NOT NULL" +
            ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            // Création des index si nécessaire
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_date ON " + TABLE_NAME + "(date_time)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_type ON " + TABLE_NAME + "(action_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_entity ON " + TABLE_NAME + "(entity_type, entity_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_user ON " + TABLE_NAME + "(user_id)");

            LOGGER.info("Table " + TABLE_NAME + " et index créés ou déjà existants");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création de la table " + TABLE_NAME, e);
            throw new RuntimeException("Erreur lors de la création de la table " + TABLE_NAME, e);
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
        }

        String sql = "INSERT INTO " + TABLE_NAME +
            " (action_type, username, date_time, description, entity_type, entity_id, user_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            LOGGER.fine("Préparation de l'enregistrement de l'action: " + action);

            pstmt.setString(1, action.getType().name());
            pstmt.setString(2, action.getUsername());
            pstmt.setString(3, action.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setString(4, action.getDescription());
            pstmt.setString(5, action.getEntityType().name());
            pstmt.setInt(6, action.getEntityId());
            pstmt.setObject(7, currentUserId);

            int result = pstmt.executeUpdate();
            if (result > 0) {
                LOGGER.info("Action utilisateur enregistrée avec succès: " + action);
            } else {
                LOGGER.warning("Aucune ligne insérée pour l'action: " + action);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de l'action utilisateur: " + action, e);
            throw new RuntimeException("Erreur lors de l'enregistrement de l'action utilisateur", e);
        }
    }

    public List<UserAction> getActions(LocalDateTime debut, LocalDateTime fin) {
        String sql = "SELECT * FROM " + TABLE_NAME +
            " WHERE date_time BETWEEN ? AND ? " +
            "ORDER BY date_time DESC";

        List<UserAction> actions = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, debut.format(formatter));
            pstmt.setString(2, fin.format(formatter));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserAction action = new UserAction(
                        UserAction.ActionType.valueOf(rs.getString("action_type")),
                        rs.getString("username"),
                        rs.getString("description"),
                        UserAction.EntityType.valueOf(rs.getString("entity_type")),
                        rs.getInt("entity_id")
                    );
                    action.setId(rs.getInt("id"));
                    action.setDateTime(LocalDateTime.parse(rs.getString("date_time"), formatter));
                    action.setUserId(rs.getInt("user_id")); //Added to handle new column
                    actions.add(action);
                }
            }
            LOGGER.info("Récupération de " + actions.size() + " actions entre " + debut + " et " + fin);
            return actions;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des actions utilisateur", e);
            throw new RuntimeException("Erreur lors de la récupération des actions utilisateur", e);
        }
    }

    public void purgerActions(LocalDateTime dateLimite) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE date_time < ?";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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