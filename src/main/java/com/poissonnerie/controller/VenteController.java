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
import java.util.logging.Logger;
import java.util.logging.Level;

public class VenteController {
    private static final Logger LOGGER = Logger.getLogger(VenteController.class.getName());
    private final List<Vente> ventes;

    public VenteController() {
        this.ventes = new ArrayList<>();
    }

    public List<Vente> getVentes() {
        return new ArrayList<>(ventes); // Retourne une copie pour éviter les modifications externes
    }

    public void chargerVentes() {
        LOGGER.info("Chargement des ventes en cours...");
        ventes.clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "SELECT v.*, c.* FROM ventes v LEFT JOIN clients c ON v.client_id = c.id ORDER BY v.date DESC";

                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    while (rs.next()) {
                        Client client = null;
                        if (rs.getObject("client_id") != null) {
                            client = creerClientDepuisResultSet(rs);
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

                    conn.commit();
                    LOGGER.info("Ventes chargées avec succès: " + ventes.size() + " ventes");
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors du chargement des ventes", e);
                throw new RuntimeException("Erreur lors du chargement des ventes", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion à la base de données", e);
            throw new RuntimeException("Erreur de connexion à la base de données", e);
        }
    }

    private Client creerClientDepuisResultSet(ResultSet rs) throws SQLException {
        return new Client(
            rs.getInt("client_id"),
            rs.getString("nom"),
            rs.getString("telephone"),
            rs.getString("adresse"),
            rs.getDouble("solde")
        );
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
                    Produit produit = creerProduitDepuisResultSet(rs);
                    Vente.LigneVente ligne = new Vente.LigneVente(
                        produit,
                        rs.getInt("quantite"),
                        rs.getDouble("prix_unitaire")
                    );
                    lignes.add(ligne);
                }
            }

            vente.setLignes(lignes);
            LOGGER.info("Lignes de vente chargées pour la vente " + vente.getId() + ": " + lignes.size() + " lignes");
        }
    }

    private Produit creerProduitDepuisResultSet(ResultSet rs) throws SQLException {
        return new Produit(
            rs.getInt("produit_id"),
            rs.getString("nom"),
            rs.getString("categorie"),
            rs.getDouble("prix_achat"),
            rs.getDouble("prix_vente"),
            rs.getInt("stock"),
            rs.getInt("seuil_alerte")
        );
    }

    private void validateVente(Vente vente) {
        if (vente == null) {
            throw new IllegalArgumentException("La vente ne peut pas être null");
        }
        if (vente.getLignes() == null || vente.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Une vente doit contenir au moins une ligne");
        }
        if (vente.getDate() == null) {
            throw new IllegalArgumentException("La date de vente est obligatoire");
        }
        if (vente.isCredit() && vente.getClient() == null) {
            throw new IllegalArgumentException("Un client est requis pour une vente à crédit");
        }
        for (Vente.LigneVente ligne : vente.getLignes()) {
            if (ligne.getQuantite() <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive pour tous les articles");
            }
            if (ligne.getPrixUnitaire() <= 0) {
                throw new IllegalArgumentException("Le prix unitaire doit être positif pour tous les articles");
            }
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Échapper les caractères spéciaux HTML et SQL
        return input.replaceAll("[<>\"'%;)(&+]", "");
    }

    public void enregistrerVente(Vente vente) {
        LOGGER.info("Début de l'enregistrement de la vente...");
        validateVente(vente);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insérer la vente
                String sqlVente = "INSERT INTO ventes (date, client_id, credit, total) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlVente, Statement.RETURN_GENERATED_KEYS)) {
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
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            vente.setId(rs.getInt(1));
                            LOGGER.info("ID de vente généré: " + vente.getId());
                        } else {
                            throw new SQLException("Impossible de récupérer l'ID de la vente");
                        }
                    }
                }

                // Insérer les lignes de vente et mettre à jour les stocks
                for (Vente.LigneVente ligne : vente.getLignes()) {
                    enregistrerLigneVente(conn, vente.getId(), ligne);
                    mettreAJourStock(conn, ligne);
                }

                // Mettre à jour le solde client si vente à crédit
                if (vente.isCredit() && vente.getClient() != null) {
                    mettreAJourSoldeClient(conn, vente);
                }

                conn.commit();
                ventes.add(vente);
                LOGGER.info("Vente enregistrée avec succès");

            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de la vente", e);
                throw new RuntimeException("Erreur lors de l'enregistrement de la vente", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de l'enregistrement de la vente", e);
            throw new RuntimeException("Erreur de connexion lors de l'enregistrement de la vente", e);
        }
    }

    private void enregistrerLigneVente(Connection conn, int venteId, Vente.LigneVente ligne) throws SQLException {
        String sql = "INSERT INTO lignes_vente (vente_id, produit_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, venteId);
            pstmt.setInt(2, ligne.getProduit().getId());
            pstmt.setInt(3, ligne.getQuantite());
            pstmt.setDouble(4, ligne.getPrixUnitaire());
            pstmt.executeUpdate();
            LOGGER.info("Ligne de vente enregistrée pour le produit: " + ligne.getProduit().getId());
        }
    }

    private void mettreAJourStock(Connection conn, Vente.LigneVente ligne) throws SQLException {
        String sql = "UPDATE produits SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ligne.getQuantite());
            pstmt.setInt(2, ligne.getProduit().getId());
            pstmt.setInt(3, ligne.getQuantite());
            int updated = pstmt.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Stock insuffisant pour le produit: " + sanitizeInput(ligne.getProduit().getNom()));
            }
            LOGGER.info("Stock mis à jour pour le produit " + ligne.getProduit().getId());
        }
    }

    private void mettreAJourSoldeClient(Connection conn, Vente vente) throws SQLException {
        String sql = "UPDATE clients SET solde = solde + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, vente.getTotal());
            pstmt.setInt(2, vente.getClient().getId());
            pstmt.executeUpdate();
            LOGGER.info("Solde client mis à jour");
        }
    }
}