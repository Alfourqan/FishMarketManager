package com.poissonnerie.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditLogger {
    private static final Logger LOGGER = Logger.getLogger(AuditLogger.class.getName());

    public static void logAction(Integer utilisateurId, String typeAction, String entite, String description, String details) {
        if (utilisateurId == null) {
            LOGGER.warning("Tentative de journalisation sans ID utilisateur");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO journal_actions (utilisateur_id, type_action, entite, description, details, date_action) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)"
             )) {
            stmt.setInt(1, utilisateurId);
            stmt.setString(2, typeAction);
            stmt.setString(3, entite);
            stmt.setString(4, description);
            stmt.setString(5, details);

            int result = stmt.executeUpdate();
            if (result > 0) {
                LOGGER.info(String.format("Action enregistrée avec succès - Utilisateur: %d, Action: %s", utilisateurId, typeAction));
            } else {
                LOGGER.warning("Échec de l'enregistrement de l'action - Aucune ligne insérée");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, String.format("Erreur lors de l'enregistrement de l'action - Utilisateur: %d, Action: %s", utilisateurId, typeAction), e);
            throw new RuntimeException("Erreur critique lors de l'enregistrement de l'action", e);
        }
    }
}