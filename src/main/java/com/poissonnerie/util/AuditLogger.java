
package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditLogger {
    private static final Logger LOGGER = Logger.getLogger(AuditLogger.class.getName());

    public static void logAction(Integer utilisateurId, String typeAction, String entite, String description, String details) {
        try (Connection conn = DatabaseConnectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO journal_actions (utilisateur_id, type_action, entite, description, details) VALUES (?, ?, ?, ?, ?)"
             )) {
            stmt.setObject(1, utilisateurId);
            stmt.setString(2, typeAction);
            stmt.setString(3, entite);
            stmt.setString(4, description);
            stmt.setString(5, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de l'action", e);
        }
    }
}
