package com.poissonnerie.controller;

import com.poissonnerie.model.Produit;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitController {
    private final List<Produit> produits = new ArrayList<>();

    public List<Produit> getProduits() {
        return produits;
    }

    // Alias de getProduits() pour maintenir la compatibilité avec ReportController
    public List<Produit> getTousProduits() {
        return getProduits();
    }

    public void chargerProduits() {
        produits.clear();
        String sql = "SELECT * FROM produits";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Produit produit = new Produit(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("categorie"),
                    rs.getDouble("prix_achat"),
                    rs.getDouble("prix_vente"),
                    rs.getInt("stock"),
                    rs.getInt("seuil_alerte")
                );
                produits.add(produit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ajouterProduit(Produit produit) {
        String sql = "INSERT INTO produits (nom, categorie, prix_achat, prix_vente, stock, seuil_alerte) VALUES (?, ?, ?, ?, ?, ?)";
        String getIdSql = "SELECT last_insert_rowid() as id";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, produit.getNom());
                pstmt.setString(2, produit.getCategorie());
                pstmt.setDouble(3, produit.getPrixAchat());
                pstmt.setDouble(4, produit.getPrixVente());
                pstmt.setInt(5, produit.getStock());
                pstmt.setInt(6, produit.getSeuilAlerte());
                pstmt.executeUpdate();

                // Récupérer l'ID généré
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(getIdSql)) {
                    if (rs.next()) {
                        produit.setId(rs.getInt("id"));
                        produits.add(produit);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout du produit", e);
        }
    }

    public void mettreAJourProduit(Produit produit) {
        String sql = "UPDATE produits SET nom = ?, categorie = ?, prix_achat = ?, prix_vente = ?, stock = ?, seuil_alerte = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, produit.getNom());
                pstmt.setString(2, produit.getCategorie());
                pstmt.setDouble(3, produit.getPrixAchat());
                pstmt.setDouble(4, produit.getPrixVente());
                pstmt.setInt(5, produit.getStock());
                pstmt.setInt(6, produit.getSeuilAlerte());
                pstmt.setInt(7, produit.getId());

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    conn.commit();
                } else {
                    conn.rollback();
                    throw new SQLException("Aucun produit trouvé avec l'ID: " + produit.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour du produit", e);
        }
    }

    public boolean produitUtiliseDansVentes(int produitId) {
        String sql = "SELECT COUNT(*) as count FROM lignes_vente WHERE produit_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, produitId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void supprimerProduit(Produit produit) throws SQLException {
        if (produitUtiliseDansVentes(produit.getId())) {
            throw new SQLException("Impossible de supprimer ce produit car il est utilisé dans des ventes existantes.");
        }

        String sql = "DELETE FROM produits WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, produit.getId());
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    produits.remove(produit);
                    conn.commit();
                } else {
                    conn.rollback();
                    throw new SQLException("Aucun produit trouvé avec l'ID: " + produit.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}