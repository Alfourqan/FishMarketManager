package com.poissonnerie.controller;

import com.poissonnerie.model.*;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public class VenteController {
    private static final Logger LOGGER = Logger.getLogger(VenteController.class.getName());
    private final List<Vente> ventes;
    private static final double LIMITE_CREDIT_MAX = 5000.0;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final ReentrantLock stockLock = new ReentrantLock();
    private static final long LOCK_TIMEOUT_SECONDS = 10;

    public VenteController() {
        this.ventes = new ArrayList<>();
    }

    public List<Vente> getVentes() {
        return new ArrayList<>(ventes);
    }

    public void chargerVentes() {
        LOGGER.info("Chargement des ventes en cours...");
        ventes.clear();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
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

                            chargerLignesVente(conn, vente);
                            ventes.add(vente);
                        }

                        conn.commit();
                        LOGGER.info("Ventes chargées avec succès: " + ventes.size() + " ventes");
                        return;
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        LOGGER.log(Level.SEVERE, "Échec définitif du chargement des ventes après " + MAX_RETRY_ATTEMPTS + " tentatives", e);
                        throw new RuntimeException("Erreur lors du chargement des ventes", e);
                    }
                    LOGGER.warning("Tentative " + attempt + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (SQLException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Opération interrompue", e);
                }
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Erreur fatale lors du chargement des ventes", e);
                    throw new RuntimeException("Erreur lors du chargement des ventes", e);
                }
            }
        }
    }

    private Client creerClientDepuisResultSet(ResultSet rs) throws SQLException {
        try {
            return new Client(
                rs.getInt("client_id"),
                sanitizeInput(rs.getString("nom")),
                sanitizeInput(rs.getString("telephone")),
                sanitizeInput(rs.getString("adresse")),
                rs.getDouble("solde")
            );
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création d'un client depuis le ResultSet", e);
            throw e;
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
        try {
            return new Produit(
                rs.getInt("produit_id"),
                sanitizeInput(rs.getString("nom")),
                sanitizeInput(rs.getString("categorie")),
                rs.getDouble("prix_achat"),
                rs.getDouble("prix_vente"),
                rs.getInt("stock"),
                rs.getInt("seuil_alerte")
            );
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création d'un produit depuis le ResultSet", e);
            throw e;
        }
    }

    private void validateVente(Vente vente) {
        List<String> erreurs = new ArrayList<>();

        if (vente == null) {
            throw new IllegalArgumentException("La vente ne peut pas être null");
        }
        if (vente.getLignes() == null || vente.getLignes().isEmpty()) {
            erreurs.add("Une vente doit contenir au moins une ligne");
        }
        if (vente.getDate() == null) {
            erreurs.add("La date de vente est obligatoire");
        } else if (vente.getDate().isAfter(LocalDateTime.now())) {
            erreurs.add("La date de vente ne peut pas être dans le futur");
        }
        if (vente.isCredit() && vente.getClient() == null) {
            erreurs.add("Un client est requis pour une vente à crédit");
        }
        if (vente.getTotal() < 0) {
            erreurs.add("Le total de la vente ne peut pas être négatif");
        }

        // Validation du crédit client
        if (vente.isCredit() && vente.getClient() != null) {
            double nouveauSolde = vente.getClient().getSolde() + vente.getTotal();
            if (nouveauSolde > LIMITE_CREDIT_MAX) {
                erreurs.add(String.format("Le crédit maximum autorisé (%.2f€) serait dépassé", LIMITE_CREDIT_MAX));
            }
        }

        if (vente.getLignes() != null) {
            for (Vente.LigneVente ligne : vente.getLignes()) {
                if (ligne == null || ligne.getProduit() == null) {
                    erreurs.add("Les lignes de vente et leurs produits ne peuvent pas être null");
                    continue;
                }
                if (ligne.getQuantite() <= 0) {
                    erreurs.add(String.format("La quantité doit être positive pour l'article %s",
                        sanitizeInput(ligne.getProduit().getNom())));
                }
                if (ligne.getPrixUnitaire() <= 0) {
                    erreurs.add(String.format("Le prix unitaire doit être positif pour l'article %s",
                        sanitizeInput(ligne.getProduit().getNom())));
                }
            }
        }

        // Vérifier que le total correspond à la somme des lignes
        double calculatedTotal = vente.getMontantTotal();
        if (Math.abs(calculatedTotal - vente.getTotal()) > 0.01) {
            erreurs.add(String.format("Le total de la vente (%.2f) ne correspond pas à la somme des lignes (%.2f)",
                vente.getTotal(), calculatedTotal));
        }

        if (!erreurs.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", erreurs));
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[<>\"'%;)(&+]", "")
                   .trim()
                   .replaceAll("\\s+", " ");
    }

    private void validateStock(Connection conn, Vente.LigneVente ligne) throws SQLException {
        String sql = "SELECT stock FROM produits WHERE id = ? AND stock >= ? FOR UPDATE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ligne.getProduit().getId());
            pstmt.setInt(2, ligne.getQuantite());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException(
                        String.format("Stock insuffisant pour le produit %s (demandé: %d)",
                            sanitizeInput(ligne.getProduit().getNom()),
                            ligne.getQuantite())
                    );
                }
            }
        }
    }

    public void enregistrerVente(Vente vente) {
        LOGGER.info("Début de l'enregistrement de la vente...");
        validateVente(vente);

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            if (!stockLock.tryLock()) {
                try {
                    if (!stockLock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new RuntimeException("Impossible d'acquérir le verrou pour la mise à jour du stock");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interruption pendant l'attente du verrou", e);
                }
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Vérifier le stock pour chaque ligne avant de commencer la transaction
                    for (Vente.LigneVente ligne : vente.getLignes()) {
                        validateStock(conn, ligne);
                    }

                    // Insérer la vente
                    int venteId = insererVente(conn, vente);
                    vente.setId(venteId);

                    // Insérer les lignes de vente et mettre à jour les stocks
                    for (Vente.LigneVente ligne : vente.getLignes()) {
                        enregistrerLigneVente(conn, venteId, ligne);
                        mettreAJourStock(conn, ligne);
                    }

                    // Mettre à jour le solde client si vente à crédit
                    if (vente.isCredit() && vente.getClient() != null) {
                        mettreAJourSoldeClient(conn, vente);
                    }

                    conn.commit();
                    ventes.add(vente);
                    LOGGER.info("Vente enregistrée avec succès, ID: " + vente.getId());
                    return;

                } catch (SQLException e) {
                    conn.rollback();
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        LOGGER.log(Level.SEVERE, "Échec définitif de l'enregistrement de la vente après " + MAX_RETRY_ATTEMPTS + " tentatives", e);
                        throw new RuntimeException("Erreur lors de l'enregistrement de la vente", e);
                    }
                    LOGGER.warning("Tentative " + attempt + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (SQLException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Opération interrompue", e);
                }
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'enregistrement de la vente", e);
                    throw new RuntimeException("Erreur lors de l'enregistrement de la vente", e);
                }
            } finally {
                stockLock.unlock();
            }
        }
    }

    private int insererVente(Connection conn, Vente vente) throws SQLException {
        String sql = "INSERT INTO ventes (date, client_id, credit, total) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("L'insertion de la vente a échoué, aucune ligne affectée.");
            }

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("Impossible de récupérer l'ID de la vente");
            }
        }
    }

    private void enregistrerLigneVente(Connection conn, int venteId, Vente.LigneVente ligne) throws SQLException {
        String sql = "INSERT INTO lignes_vente (vente_id, produit_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, venteId);
            pstmt.setInt(2, ligne.getProduit().getId());
            pstmt.setInt(3, ligne.getQuantite());
            pstmt.setDouble(4, ligne.getPrixUnitaire());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("L'insertion de la ligne de vente a échoué");
            }
            LOGGER.info("Ligne de vente enregistrée pour le produit: " + ligne.getProduit().getId());
        }
    }

    private void mettreAJourStock(Connection conn, Vente.LigneVente ligne) throws SQLException {
        String sql = "UPDATE produits SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ligne.getQuantite());
            pstmt.setInt(2, ligne.getProduit().getId());
            pstmt.setInt(3, ligne.getQuantite());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Stock insuffisant pour le produit: " + sanitizeInput(ligne.getProduit().getNom()));
            }
            LOGGER.info("Stock mis à jour pour le produit " + ligne.getProduit().getId());
        }
    }

    private void mettreAJourSoldeClient(Connection conn, Vente vente) throws SQLException {
        String sql = "UPDATE clients SET solde = solde + ? WHERE id = ? AND (solde + ?) <= ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, vente.getTotal());
            pstmt.setInt(2, vente.getClient().getId());
            pstmt.setDouble(3, vente.getTotal());
            pstmt.setDouble(4, LIMITE_CREDIT_MAX);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Impossible de mettre à jour le solde client: limite de crédit dépassée");
            }
            LOGGER.info("Solde client mis à jour pour le client " + vente.getClient().getId());
        }
    }
}