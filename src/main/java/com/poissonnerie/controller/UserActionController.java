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

    private UserActionController() {
        createTableIfNotExists();
    }

    public static UserActionController getInstance() {
        if (instance == null) {
            instance = new UserActionController();
        }
        return instance;
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS user_actions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "action_type TEXT NOT NULL," +
            "username TEXT NOT NULL," +
            "date_time TEXT NOT NULL," +
            "description TEXT NOT NULL," +
            "entity_type TEXT NOT NULL," +
            "entity_id INTEGER NOT NULL" +
            ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("Table user_actions créée ou déjà existante");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création de la table user_actions", e);
            throw new RuntimeException("Erreur lors de la création de la table user_actions", e);
        }
    }

    public void logAction(UserAction action) {
        String sql = "INSERT INTO user_actions " +
            "(action_type, username, date_time, description, entity_type, entity_id) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, action.getType().name());
            pstmt.setString(2, action.getUsername());
            pstmt.setString(3, action.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setString(4, action.getDescription());
            pstmt.setString(5, action.getEntityType().name());
            pstmt.setInt(6, action.getEntityId());

            pstmt.executeUpdate();
            LOGGER.info("Action utilisateur enregistrée: " + action);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de l'action utilisateur", e);
            throw new RuntimeException("Erreur lors de l'enregistrement de l'action utilisateur", e);
        }
    }

    public List<UserAction> getActions(LocalDateTime debut, LocalDateTime fin) {
        String sql = "SELECT * FROM user_actions " +
            "WHERE date_time BETWEEN ? AND ? " +
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
                    actions.add(action);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des actions utilisateur", e);
            throw new RuntimeException("Erreur lors de la récupération des actions utilisateur", e);
        }

        return actions;
    }

    public List<UserAction> getActionsByEntityType(UserAction.EntityType entityType, LocalDateTime debut, LocalDateTime fin) {
        String sql = "SELECT * FROM user_actions " +
            "WHERE entity_type = ? AND date_time BETWEEN ? AND ? " +
            "ORDER BY date_time DESC";

        List<UserAction> actions = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, entityType.name());
            pstmt.setString(2, debut.format(formatter));
            pstmt.setString(3, fin.format(formatter));

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
                    actions.add(action);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la récupération des actions par type d'entité", e);
            throw new RuntimeException("Erreur lors de la récupération des actions par type d'entité", e);
        }

        return actions;
    }

    public void purgerActions(LocalDateTime dateLimite) {
        String sql = "DELETE FROM user_actions WHERE date_time < ?";
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