package com.poissonnerie.controller;

import com.poissonnerie.model.MouvementCaisse;
import com.poissonnerie.model.UserAction;
import com.poissonnerie.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CaisseController {
    private static final Logger LOGGER = Logger.getLogger(CaisseController.class.getName());
    private final List<MouvementCaisse> mouvements = new ArrayList<>();
    private double soldeCaisse = 0.0;
    private final UserActionController userActionController = UserActionController.getInstance();

    public List<MouvementCaisse> getMouvements() {
        return new ArrayList<>(mouvements);
    }

    public double getSoldeCaisse() {
        return soldeCaisse;
    }

    public boolean isCaisseOuverte() {
        if (mouvements.isEmpty()) {
            return false;
        }
        return mouvements.stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.OUVERTURE ||
                        m.getType() == MouvementCaisse.TypeMouvement.CLOTURE)
            .findFirst()
            .map(m -> m.getType() == MouvementCaisse.TypeMouvement.OUVERTURE)
            .orElse(false);
    }

    public void chargerMouvements() {
        mouvements.clear();
        soldeCaisse = 0.0;
        String sql = "SELECT * FROM mouvements_caisse ORDER BY date DESC";
        LOGGER.info("Chargement des mouvements de caisse...");

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                LocalDateTime date = LocalDateTime.parse(rs.getString("date"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                MouvementCaisse mouvement = new MouvementCaisse(
                    rs.getInt("id"),
                    date,
                    MouvementCaisse.TypeMouvement.fromString(rs.getString("type")),
                    rs.getDouble("montant"),
                    rs.getString("description")
                );
                mouvements.add(mouvement);

                updateSoldeAndState(mouvement);
            }
            LOGGER.info(String.format("Mouvements de caisse chargés avec succès: %d mouvements", mouvements.size()));
            LOGGER.info(String.format("État actuel de la caisse - Solde: %.2f€, Ouverte: %b", soldeCaisse, isCaisseOuverte()));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des mouvements", e);
            throw new RuntimeException("Erreur lors du chargement des mouvements", e);
        }
    }

    private void updateSoldeAndState(MouvementCaisse mouvement) {
        switch (mouvement.getType()) {
            case OUVERTURE:
                soldeCaisse = mouvement.getMontant();
                break;
            case CLOTURE:
                soldeCaisse = 0.0;
                break;
            case ENTREE:
                soldeCaisse += mouvement.getMontant();
                break;
            case SORTIE:
                soldeCaisse -= mouvement.getMontant();
                break;
        }
    }

    public void ajouterMouvement(MouvementCaisse mouvement) {
        LOGGER.info(String.format("Ajout d'un nouveau mouvement: Type=%s, Montant=%.2f€",
            mouvement.getType(),
            mouvement.getMontant()));

        String sql = "INSERT INTO mouvements_caisse (date, type, montant, description) VALUES (datetime('now', 'localtime'), ?, ?, ?)";
        String getIdSql = "SELECT last_insert_rowid() as id";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, mouvement.getType().getValue());
                pstmt.setDouble(2, mouvement.getMontant());
                pstmt.setString(3, mouvement.getDescription());
                pstmt.executeUpdate();

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(getIdSql)) {
                    if (rs.next()) {
                        mouvement.setId(rs.getInt("id"));
                        mouvements.add(0, mouvement);
                        updateSoldeAndState(mouvement);

                        UserAction action = new UserAction(
                            UserAction.ActionType.CREATION,
                            "", // Sera défini par UserActionController
                            String.format("Mouvement de caisse %s : %.2f€ - %s",
                                mouvement.getType().getValue(),
                                mouvement.getMontant(),
                                mouvement.getDescription()),
                            UserAction.EntityType.CAISSE,
                            mouvement.getId()
                        );
                        userActionController.logAction(action);
                    }
                }
                conn.commit();
                LOGGER.info(String.format("Mouvement de caisse ajouté avec succès: %s", mouvement));
                LOGGER.info(String.format("Nouvel état de la caisse - Solde: %.2f€, Ouverte: %b", soldeCaisse, isCaisseOuverte()));
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du mouvement", e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'ajout du mouvement", e);
            throw new RuntimeException("Erreur lors de l'ajout du mouvement", e);
        }
    }

    public String exporterMouvementsCSV(LocalDateTime debut, LocalDateTime fin) {
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        csv.append("Date,Type,Montant,Description\n");

        mouvements.stream()
            .filter(m -> !m.getDate().isBefore(debut) && !m.getDate().isAfter(fin))
            .forEach(m -> csv.append(String.format("%s,%s,%.2f,\"%s\"\n",
                m.getDate().format(dateFormatter),
                m.getType(),
                m.getMontant(),
                m.getDescription().replace("\"", "\"\"")
            )));

        return csv.toString();
    }

    public List<MouvementCaisse> getMouvementsDuJour(LocalDateTime date) {
        return mouvements.stream()
            .filter(m -> m.getDate().toLocalDate().equals(date.toLocalDate()))
            .collect(Collectors.toList());
    }

    public List<MouvementCaisse> rechercherMouvementsParDate(LocalDateTime dateDebut, LocalDateTime dateFin) {
        return mouvements.stream()
            .filter(m -> !m.getDate().isBefore(dateDebut.toLocalDate().atStartOfDay()) &&
                        !m.getDate().isAfter(dateFin.toLocalDate().atTime(23, 59, 59)))
            .sorted((m1, m2) -> m2.getDate().compareTo(m1.getDate()))
            .collect(Collectors.toList());
    }
}