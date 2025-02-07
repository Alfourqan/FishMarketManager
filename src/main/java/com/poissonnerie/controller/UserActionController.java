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

    private void migrateTable() {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                DatabaseMetaData md = conn.getMetaData();
                ResultSet rs = md.getTables(null, null, TABLE_NAME, null);

                if (!rs.next()) {
                    createTable(conn);
                }
                conn.commit();
                LOGGER.info("Migration de la table " + TABLE_NAME + " effectuée avec succès");
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la migration de la table " + TABLE_NAME, e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de la migration de la table " + TABLE_NAME, e);
            throw new RuntimeException("Erreur lors de la migration de la table " + TABLE_NAME, e);
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

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_date ON " + TABLE_NAME + "(date_time)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_type ON " + TABLE_NAME + "(action_type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_entity ON " + TABLE_NAME + "(entity_type, entity_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_actions_user ON " + TABLE_NAME + "(user_id)");
        }
    }

    public synchronized void logAction(UserAction action) {
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
            " (action_type, username, date_time, description, entity_type, entity_id, user_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        int retries = 0;
        while (retries < MAX_RETRIES) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, action.getType().name());
                    pstmt.setString(2, action.getUsername());
                    pstmt.setString(3, action.getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    pstmt.setString(4, action.getDescription());
                    pstmt.setString(5, action.getEntityType().name());
                    pstmt.setInt(6, action.getEntityId());

                    if (currentUserId != null) {
                        pstmt.setInt(7, currentUserId);
                    } else {
                        pstmt.setNull(7, Types.INTEGER);
                    }

                    int result = pstmt.executeUpdate();
                    conn.commit();

                    if (result > 0) {
                        LOGGER.info("Action utilisateur enregistrée avec succès: " + action);
                        return;
                    } else {
                        LOGGER.warning("Aucune ligne insérée pour l'action: " + action);
                    }
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        LOGGER.log(Level.SEVERE, "Erreur lors du rollback", rollbackEx);
                    }

                    if (e.getMessage().contains("database is locked") && retries < MAX_RETRIES - 1) {
                        retries++;
                        LOGGER.info("Tentative de réessai " + retries + "/" + MAX_RETRIES + " après verrouillage de la base");
                        try {
                            Thread.sleep(RETRY_DELAY_MS * (1 << retries)); // Délai exponentiel
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interruption pendant l'attente entre les tentatives", ie);
                        }
                        continue;
                    }
                    throw e;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de l'action utilisateur: " + action, e);
                if (retries >= MAX_RETRIES - 1) {
                    throw new RuntimeException("Erreur lors de l'enregistrement de l'action utilisateur après " + MAX_RETRIES + " tentatives", e);
                }
                retries++;
            }
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

                    Object userId = rs.getObject("user_id");
                    if (userId != null) {
                        action.setUserId((Integer) userId);
                    }

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