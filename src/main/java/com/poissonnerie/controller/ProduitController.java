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
                    rs.getDouble("prix"),
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
        String sql = "INSERT INTO produits (nom, categorie, prix, stock, seuil_alerte) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, produit.getNom());
            pstmt.setString(2, produit.getCategorie());
            pstmt.setDouble(3, produit.getPrix());
            pstmt.setInt(4, produit.getStock());
            pstmt.setInt(5, produit.getSeuilAlerte());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    produit.setId(generatedKeys.getInt(1));
                }
            }

            produits.add(produit);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void mettreAJourProduit(Produit produit) {
        String sql = "UPDATE produits SET nom = ?, categorie = ?, prix = ?, stock = ?, seuil_alerte = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, produit.getNom());
            pstmt.setString(2, produit.getCategorie());
            pstmt.setDouble(3, produit.getPrix());
            pstmt.setInt(4, produit.getStock());
            pstmt.setInt(5, produit.getSeuilAlerte());
            pstmt.setInt(6, produit.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void supprimerProduit(Produit produit) {
        String sql = "DELETE FROM produits WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, produit.getId());
            pstmt.executeUpdate();
            produits.remove(produit);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}