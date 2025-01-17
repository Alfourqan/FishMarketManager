package com.poissonnerie.controller;

import com.poissonnerie.model.MouvementCaisse;
import com.poissonnerie.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CaisseController {
    private final List<MouvementCaisse> mouvements = new ArrayList<>();
    private double soldeCaisse = 0.0;

    public List<MouvementCaisse> getMouvements() {
        return new ArrayList<>(mouvements);
    }

    public double getSoldeCaisse() {
        return soldeCaisse;
    }

    public void chargerMouvements() {
        mouvements.clear();
        String sql = "SELECT * FROM mouvements_caisse ORDER BY date DESC";

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

                // Mettre à jour le solde
                if (mouvement.getType() == MouvementCaisse.TypeMouvement.ENTREE) {
                    soldeCaisse += mouvement.getMontant();
                } else {
                    soldeCaisse -= mouvement.getMontant();
                }
            }
            System.out.println("Mouvements de caisse chargés avec succès: " + mouvements.size() + " mouvements");
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des mouvements: " + e.getMessage());
            throw new RuntimeException("Erreur lors du chargement des mouvements", e);
        }
    }

    public void ajouterMouvement(MouvementCaisse mouvement) {
        String sql = "INSERT INTO mouvements_caisse (date, type, montant, description) VALUES (datetime('now', 'localtime'), ?, ?, ?)";
        String getIdSql = "SELECT last_insert_rowid() as id";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, mouvement.getType().getValue());
                pstmt.setDouble(2, mouvement.getMontant());
                pstmt.setString(3, mouvement.getDescription());
                pstmt.executeUpdate();

                // Récupérer l'ID généré
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(getIdSql)) {
                    if (rs.next()) {
                        mouvement.setId(rs.getInt("id"));
                        mouvements.add(0, mouvement); // Ajouter au début de la liste

                        // Mettre à jour le solde
                        if (mouvement.getType() == MouvementCaisse.TypeMouvement.ENTREE) {
                            soldeCaisse += mouvement.getMontant();
                        } else {
                            soldeCaisse -= mouvement.getMontant();
                        }
                    }
                }
                conn.commit();
                System.out.println("Mouvement de caisse ajouté avec succès: " + mouvement);
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erreur lors de l'ajout du mouvement: " + e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur fatale lors de l'ajout du mouvement: " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'ajout du mouvement", e);
        }
    }

    public String exporterMouvementsCSV(LocalDateTime debut, LocalDateTime fin) {
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        // En-tête CSV
        csv.append("Date,Type,Montant,Description\n");

        // Filtrer et formatter les mouvements
        mouvements.stream()
            .filter(m -> !m.getDate().isBefore(debut) && !m.getDate().isAfter(fin))
            .forEach(m -> csv.append(String.format("%s,%s,%.2f,\"%s\"\n",
                m.getDate().format(dateFormatter),
                m.getType(),
                m.getMontant(),
                m.getDescription().replace("\"", "\"\"") // Échapper les guillemets
            )));

        return csv.toString();
    }
}
