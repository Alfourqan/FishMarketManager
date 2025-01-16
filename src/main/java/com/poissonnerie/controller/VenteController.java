package com.poissonnerie.controller;

import com.poissonnerie.model.Vente;
import com.poissonnerie.model.Produit;
import com.poissonnerie.model.Client;
import com.poissonnerie.util.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VenteController {
    private final ObservableList<Vente> ventes = FXCollections.observableArrayList();

    public ObservableList<Vente> getVentes() {
        return ventes;
    }

    public void chargerVentes() {
        ventes.clear();
        String sql = "SELECT v.*, c.* FROM ventes v LEFT JOIN clients c ON v.client_id = c.id ORDER BY v.date DESC";
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Client client = null;
                if (rs.getObject("client_id") != null) {
                    client = new Client(
                        rs.getInt("client_id"),
                        rs.getString("nom"),
                        rs.getString("telephone"),
                        rs.getString("adresse"),
                        rs.getDouble("solde")
                    );
                }
                
                Vente vente = new Vente(
                    rs.getInt("id"),
                    rs.getTimestamp("date").toLocalDateTime(),
                    client,
                    rs.getBoolean("credit"),
                    rs.getDouble("total")
                );
                
                chargerLignesVente(vente);
                ventes.add(vente);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void chargerLignesVente(Vente vente) {
        String sql = "SELECT l.*, p.* FROM lignes_vente l " +
                    "JOIN produits p ON l.produit_id = p.id " +
                    "WHERE l.vente_id = ?";
        
        List<Vente.LigneVente> lignes = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vente.getId());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Produit produit = new Produit(
                    rs.getInt("produit_id"),
                    rs.getString("nom"),
                    rs.getString("categorie"),
                    rs.getDouble("prix"),
                    rs.getInt("stock"),
                    rs.getInt("seuil_alerte")
                );
                
                Vente.LigneVente ligne = new Vente.LigneVente(
                    produit,
                    rs.getInt("quantite"),
                    rs.getDouble("prix_unitaire")
                );
                
                lignes.add(ligne);
            }
            
            vente.setLignes(lignes);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void enregistrerVente(Vente vente) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            // Insertion de la vente
            String sqlVente = "INSERT INTO ventes (date, client_id, credit, total) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlVente, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                if (vente.getClient() != null) {
                    pstmt.setInt(2, vente.getClient().getId());
                } else {
                    pstmt.setNull(2, Types.INTEGER);
                }
                pstmt.setBoolean(3, vente.isCredit());
                pstmt.setDouble(4, vente.getTotal());
                
                pstmt.executeUpdate();
                
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        vente.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            // Insertion des lignes de vente
            String sqlLigne = "INSERT INTO lignes_vente (vente_id, produit_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlLigne)) {
                for (Vente.LigneVente ligne : vente.getLignes()) {
                    pstmt.setInt(1, vente.getId());
                    pstmt.setInt(2, ligne.getProduit().getId());
                    pstmt.setInt(3, ligne.getQuantite());
                    pstmt.setDouble(4, ligne.getPrixUnitaire());
                    pstmt.executeUpdate();
                    
                    // Mise à jour du stock
                    String sqlStock = "UPDATE produits SET stock = stock - ? WHERE id = ?";
                    try (PreparedStatement pstmtStock = conn.prepareStatement(sqlStock)) {
                        pstmtStock.setInt(1, ligne.getQuantite());
                        pstmtStock.setInt(2, ligne.getProduit().getId());
                        pstmtStock.executeUpdate();
                    }
                }
            }
            
            // Mise à jour du solde client si vente à crédit
            if (vente.isCredit() && vente.getClient() != null) {
                String sqlSolde = "UPDATE clients SET solde = solde + ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlSolde)) {
                    pstmt.setDouble(1, vente.getTotal());
                    pstmt.setInt(2, vente.getClient().getId());
                    pstmt.executeUpdate();
                }
            }
            
            conn.commit();
            ventes.add(vente);
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
