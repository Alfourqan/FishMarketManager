package com.poissonnerie.controller;

import com.poissonnerie.model.UserAction;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class UserActionController {
    private static final Logger LOGGER = Logger.getLogger(UserActionController.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;
    private static UserActionController instance;
    private Integer currentUserId;
    private String currentUsername;

    private UserActionController() {
        try {
            createTableIfNotExists();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de UserActionController", e);
            throw new RuntimeException(e);
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

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS user_actions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "action_type TEXT NOT NULL," +
                    "username TEXT NOT NULL," +
                    "date_time TEXT NOT NULL," +
                    "description TEXT NOT NULL," +
                    "entity_type TEXT NOT NULL," +
                    "entity_id INTEGER NOT NULL," +
                    "user_id INTEGER)";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void setCurrentUser(Integer userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;
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

        String sql = "INSERT INTO user_actions (username, action_type, entity_type, entity_id, description, date_time, user_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Connection conn = null;
            try {
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, action.getUsername());
                    stmt.setString(2, action.getType().getValue());
                    stmt.setString(3, action.getEntityType().getValue());
                    stmt.setInt(4, action.getEntityId());
                    stmt.setString(5, action.getDescription());
                    stmt.setString(6, action.getDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    if (action.getUserId() != null) {
                        stmt.setInt(7, action.getUserId());
                    } else {
                        stmt.setNull(7, java.sql.Types.INTEGER);
                    }

                    stmt.executeUpdate();
                    conn.commit();
                    return;
                }
            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                    }
                }
                LOGGER.warning("Tentative " + (attempt + 1) + " échouée: " + e.getMessage());

                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (1L << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interruption pendant l'attente", ie);
                    }
                } else {
                    throw new RuntimeException("Échec de l'enregistrement après " + MAX_RETRIES + " tentatives", e);
                }
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture de la connexion", e);
                    }
                }
            }
        }
    }
    public List<UserAction> getActions(LocalDateTime debut, LocalDateTime fin) {
        List<UserAction> actions = new ArrayList<>();
        String sql = "SELECT * FROM user_actions WHERE date_time BETWEEN ? AND ? ORDER BY date_time DESC";

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
        String sql = "DELETE FROM user_actions WHERE date_time < ?";
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