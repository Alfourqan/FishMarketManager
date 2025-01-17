package com.poissonnerie.controller;

import com.poissonnerie.model.Vente;
import com.poissonnerie.model.Produit;
import com.poissonnerie.model.Client;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class VenteController {
    private final List<Vente> ventes = new ArrayList<>();

    public List<Vente> getVentes() {
        return ventes;
    }

    public void chargerVentes() {
        ventes.clear();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT v.*, c.* FROM ventes v LEFT JOIN clients c ON v.client_id = c.id ORDER BY v.date DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs != null && rs.next()) {
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

                // Conversion du timestamp Unix en LocalDateTime
                long timestamp = rs.getLong("date");
                LocalDateTime date = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    java.time.ZoneId.systemDefault()
                );

                Vente vente = new Vente(
                    rs.getInt("id"),
                    date,
                    client,
                    rs.getBoolean("credit"),
                    rs.getDouble("total")
                );

                chargerLignesVente(vente);
                ventes.add(vente);
            }
            System.out.println("Ventes chargées avec succès: " + ventes.size() + " ventes");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement des ventes: " + e.getMessage());
            throw new RuntimeException("Erreur lors du chargement des ventes", e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture des ressources: " + e.getMessage());
            }
        }
    }

    private void chargerLignesVente(Vente vente) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT l.*, p.* FROM lignes_vente l " +
                        "JOIN produits p ON l.produit_id = p.id " +
                        "WHERE l.vente_id = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, vente.getId());
            rs = pstmt.executeQuery();

            List<Vente.LigneVente> lignes = new ArrayList<>();
            while (rs != null && rs.next()) {
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
            System.out.println("Lignes de vente chargées pour la vente " + vente.getId() + ": " + lignes.size() + " lignes");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement des lignes de vente: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture des ressources: " + e.getMessage());
            }
        }
    }

    public void enregistrerVente(Vente vente) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            System.out.println("Début de l'enregistrement de la vente...");

            // Insertion de la vente avec timestamp Unix
            String sqlVente = "INSERT INTO ventes (date, client_id, credit, total) VALUES (?, ?, ?, ?)";
            int venteId;

            try (PreparedStatement pstmt = conn.prepareStatement(sqlVente)) {
                // Conversion de LocalDateTime en timestamp Unix
                long timestamp = vente.getDate().atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();
                pstmt.setLong(1, timestamp);

                if (vente.getClient() != null) {
                    pstmt.setInt(2, vente.getClient().getId());
                } else {
                    pstmt.setNull(2, Types.INTEGER);
                }
                pstmt.setBoolean(3, vente.isCredit());
                pstmt.setDouble(4, vente.getTotal());

                pstmt.executeUpdate();

                // Récupérer l'ID généré
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid() as id")) {
                    if (rs.next()) {
                        venteId = rs.getInt("id");
                        vente.setId(venteId);
                        System.out.println("ID de vente généré: " + venteId);
                    } else {
                        throw new SQLException("Impossible de récupérer l'ID de la vente");
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
                        System.out.println("Stock mis à jour pour le produit " + ligne.getProduit().getId());
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
                    System.out.println("Solde client mis à jour");
                }
            }

            conn.commit();
            ventes.add(vente);
            System.out.println("Vente enregistrée avec succès");

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Transaction annulée suite à une erreur");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    System.err.println("Erreur lors du rollback: " + ex.getMessage());
                }
            }
            e.printStackTrace();
            System.err.println("Erreur lors de l'enregistrement de la vente: " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'enregistrement de la vente", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
                }
            }
        }
    }
}