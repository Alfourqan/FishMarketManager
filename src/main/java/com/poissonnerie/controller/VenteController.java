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
        return new ArrayList<>(ventes); // Retourne une copie pour éviter les modifications externes
    }

    public void chargerVentes() {
        ventes.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Chargement des ventes en cours...");
            String sql = "SELECT v.*, c.* FROM ventes v LEFT JOIN clients c ON v.client_id = c.id ORDER BY v.date DESC";

            try (Statement stmt = conn.createStatement();
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

                    // Charger les lignes de vente
                    chargerLignesVente(conn, vente);
                    ventes.add(vente);
                }
            }

            System.out.println("Ventes chargées avec succès: " + ventes.size() + " ventes");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement des ventes: " + e.getMessage());
            throw new RuntimeException("Erreur lors du chargement des ventes", e);
        }
    }

    private void chargerLignesVente(Connection conn, Vente vente) throws SQLException {
        String sql = "SELECT l.*, p.* FROM lignes_vente l " +
                    "JOIN produits p ON l.produit_id = p.id " +
                    "WHERE l.vente_id = ?";

        List<Vente.LigneVente> lignes = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vente.getId());

            try (ResultSet rs = pstmt.executeQuery()) {
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
            }

            vente.setLignes(lignes);
            System.out.println("Lignes de vente chargées pour la vente " + vente.getId() + ": " + lignes.size() + " lignes");
        }
    }

    public void enregistrerVente(Vente vente) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Démarrer la transaction explicitement pour SQLite
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("BEGIN TRANSACTION");
            }

            System.out.println("Début de l'enregistrement de la vente...");

            // Insérer la vente
            String sqlVente = "INSERT INTO ventes (date, client_id, credit, total) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlVente)) {
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
                        int venteId = rs.getInt("id");
                        vente.setId(venteId);
                        System.out.println("ID de vente généré: " + venteId);
                    } else {
                        throw new SQLException("Impossible de récupérer l'ID de la vente");
                    }
                }
            }

            // Insérer les lignes de vente et mettre à jour les stocks
            for (Vente.LigneVente ligne : vente.getLignes()) {
                // Insérer la ligne de vente
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO lignes_vente (vente_id, produit_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)")) {
                    pstmt.setInt(1, vente.getId());
                    pstmt.setInt(2, ligne.getProduit().getId());
                    pstmt.setInt(3, ligne.getQuantite());
                    pstmt.setDouble(4, ligne.getPrixUnitaire());
                    pstmt.executeUpdate();
                }

                // Mettre à jour le stock
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE produits SET stock = stock - ? WHERE id = ? AND stock >= ?")) {
                    pstmt.setInt(1, ligne.getQuantite());
                    pstmt.setInt(2, ligne.getProduit().getId());
                    pstmt.setInt(3, ligne.getQuantite());
                    int updated = pstmt.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Stock insuffisant pour le produit: " + ligne.getProduit().getNom());
                    }
                    System.out.println("Stock mis à jour pour le produit " + ligne.getProduit().getId());
                }
            }

            // Mettre à jour le solde client si vente à crédit
            if (vente.isCredit() && vente.getClient() != null) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE clients SET solde = solde + ? WHERE id = ?")) {
                    pstmt.setDouble(1, vente.getTotal());
                    pstmt.setInt(2, vente.getClient().getId());
                    pstmt.executeUpdate();
                    System.out.println("Solde client mis à jour");
                }
            }

            // Valider la transaction
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("COMMIT");
            }

            ventes.add(vente);
            System.out.println("Vente enregistrée avec succès");

        } catch (SQLException e) {
            try (Connection conn = DatabaseManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ROLLBACK");
                System.err.println("Transaction annulée suite à une erreur");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            System.err.println("Erreur lors de l'enregistrement de la vente: " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'enregistrement de la vente", e);
        }
    }
}