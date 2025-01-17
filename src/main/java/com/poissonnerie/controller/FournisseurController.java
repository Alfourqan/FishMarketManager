package com.poissonnerie.controller;

import com.poissonnerie.model.Fournisseur;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FournisseurController {
    private List<Fournisseur> fournisseurs;

    public FournisseurController() {
        this.fournisseurs = new ArrayList<>();
    }

    public void chargerFournisseurs() {
        fournisseurs.clear();
        String sql = "SELECT * FROM fournisseurs WHERE supprime = false ORDER BY nom";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Fournisseur fournisseur = new Fournisseur(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("contact"),
                    rs.getString("telephone"),
                    rs.getString("email"),
                    rs.getString("adresse")
                );
                fournisseur.setStatut(rs.getString("statut"));
                fournisseurs.add(fournisseur);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des fournisseurs: " + e.getMessage(), e);
        }
    }

    public List<Fournisseur> getFournisseurs() {
        return fournisseurs;
    }

    public void ajouterFournisseur(Fournisseur fournisseur) {
        String sql = "INSERT INTO fournisseurs (nom, contact, telephone, email, adresse, statut) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, fournisseur.getNom());
            pstmt.setString(2, fournisseur.getContact());
            pstmt.setString(3, fournisseur.getTelephone());
            pstmt.setString(4, fournisseur.getEmail());
            pstmt.setString(5, fournisseur.getAdresse());
            pstmt.setString(6, fournisseur.getStatut());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    fournisseur.setId(generatedKeys.getInt(1));
                    fournisseurs.add(fournisseur);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du fournisseur: " + e.getMessage(), e);
        }
    }

    public void mettreAJourFournisseur(Fournisseur fournisseur) {
        String sql = "UPDATE fournisseurs SET nom = ?, contact = ?, telephone = ?, email = ?, adresse = ?, statut = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fournisseur.getNom());
            pstmt.setString(2, fournisseur.getContact());
            pstmt.setString(3, fournisseur.getTelephone());
            pstmt.setString(4, fournisseur.getEmail());
            pstmt.setString(5, fournisseur.getAdresse());
            pstmt.setString(6, fournisseur.getStatut());
            pstmt.setInt(7, fournisseur.getId());

            pstmt.executeUpdate();

            // Mettre à jour la liste en mémoire
            int index = fournisseurs.indexOf(fournisseur);
            if (index != -1) {
                fournisseurs.set(index, fournisseur);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du fournisseur: " + e.getMessage(), e);
        }
    }

    public void supprimerFournisseur(Fournisseur fournisseur) {
        // Suppression logique
        String sql = "UPDATE fournisseurs SET supprime = true WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, fournisseur.getId());
            pstmt.executeUpdate();

            // Retirer de la liste en mémoire
            fournisseurs.remove(fournisseur);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du fournisseur: " + e.getMessage(), e);
        }
    }

    public List<Fournisseur> rechercherFournisseurs(String terme) {
        List<Fournisseur> resultats = new ArrayList<>();
        String sql = "SELECT * FROM fournisseurs WHERE supprime = false AND " +
                    "(LOWER(nom) LIKE LOWER(?) OR LOWER(contact) LIKE LOWER(?) OR " +
                    "LOWER(telephone) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?))";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchTerm = "%" + terme.toLowerCase() + "%";
            for (int i = 1; i <= 4; i++) {
                pstmt.setString(i, searchTerm);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Fournisseur fournisseur = new Fournisseur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("contact"),
                        rs.getString("telephone"),
                        rs.getString("email"),
                        rs.getString("adresse")
                    );
                    fournisseur.setStatut(rs.getString("statut"));
                    resultats.add(fournisseur);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche des fournisseurs: " + e.getMessage(), e);
        }
        return resultats;
    }
}