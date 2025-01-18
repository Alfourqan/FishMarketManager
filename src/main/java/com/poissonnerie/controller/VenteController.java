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
    private static final int TRANSACTION_TIMEOUT_SECONDS = 30;

    public VenteController() {
        this.ventes = new ArrayList<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        LOGGER.info("Initialisation des tables de la base de données...");
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Activer les contraintes de clé étrangère
            stmt.execute("PRAGMA foreign_keys = ON");

            // Mettre à jour la table ventes si nécessaire
            stmt.execute("CREATE TABLE IF NOT EXISTS ventes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "date BIGINT NOT NULL, " +
                        "client_id INTEGER, " +
                        "credit BOOLEAN NOT NULL, " +
                        "total DOUBLE NOT NULL, " +
                        "supprime BOOLEAN DEFAULT false, " +
                        "FOREIGN KEY (client_id) REFERENCES clients(id))");

            // Mettre à jour la table lignes_vente si nécessaire
            stmt.execute("CREATE TABLE IF NOT EXISTS lignes_vente (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "vente_id INTEGER NOT NULL, " +
                        "produit_id INTEGER NOT NULL, " +
                        "quantite INTEGER NOT NULL, " +
                        "prix_unitaire DOUBLE NOT NULL, " +
                        "supprime BOOLEAN DEFAULT false, " +
                        "FOREIGN KEY (vente_id) REFERENCES ventes(id), " +
                        "FOREIGN KEY (produit_id) REFERENCES produits(id))");

            // Créer les index si nécessaire
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventes_client ON ventes(client_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lignes_vente_vente ON lignes_vente(vente_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lignes_vente_produit ON lignes_vente(produit_id)");

            LOGGER.info("Tables mises à jour avec succès");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de l'initialisation des tables", e);
            // Continue même si l'initialisation échoue car les tables peuvent déjà exister
        }
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
                // Définir un timeout de transaction
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA busy_timeout = " + (TRANSACTION_TIMEOUT_SECONDS * 1000));
                }

                try {
                    String sql = "SELECT v.*, c.* FROM ventes v LEFT JOIN clients c ON v.client_id = c.id " +
                               "WHERE v.supprime = false ORDER BY v.date DESC";

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
                    "WHERE l.vente_id = ? AND l.supprime = false";

        List<Vente.LigneVente> lignes = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vente.getId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Produit produit = creerProduitDepuisResultSet(rs);
                    validateProduit(produit);

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

    private void validateProduit(Produit produit) {
        if (produit == null) {
            throw new IllegalStateException("Produit invalide dans la ligne de vente");
        }
        if (produit.getPrixVente() <= 0) {
            throw new IllegalStateException("Prix de vente invalide pour le produit: " + produit.getId());
        }
        if (produit.getPrixAchat() < 0) {
            throw new IllegalStateException("Prix d'achat invalide pour le produit: " + produit.getId());
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

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Échappement des caractères spéciaux HTML et SQL
        return input.replaceAll("[<>\"'%;)(&+\\[\\]{}]", "")
                   .trim()
                   .replaceAll("\\s+", " ");
    }

    private void validateStock(Connection conn, Vente.LigneVente ligne) throws SQLException {
        String sql = "SELECT stock FROM produits WHERE id = ? AND supprime = false AND stock >= ?";
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

        boolean lockAcquired = false;
        try {
            // Tentative d'acquisition du verrou avec timeout
            if (!stockLock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("Impossible d'acquérir le verrou pour la mise à jour du stock");
            }
            lockAcquired = true;

            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
                try (Connection conn = DatabaseManager.getConnection()) {
                    conn.setAutoCommit(false);

                    // Définir un timeout de transaction
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("PRAGMA busy_timeout = " + (TRANSACTION_TIMEOUT_SECONDS * 1000));
                    }

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
                        LOGGER.log(Level.SEVERE, "Erreur SQL lors de l'enregistrement de la vente", e);
                        if (attempt == MAX_RETRY_ATTEMPTS) {
                            throw new RuntimeException("Erreur lors de l'enregistrement de la vente: " + e.getMessage(), e);
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
                        throw new RuntimeException("Erreur lors de l'enregistrement de la vente: " + e.getMessage(), e);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interruption pendant l'attente du verrou", e);
        } finally {
            if (lockAcquired) {
                stockLock.unlock();
            }
        }
    }

    private int insererVente(Connection conn, Vente vente) throws SQLException {
        String sql = "INSERT INTO ventes (date, client_id, credit, total, supprime) VALUES (?, ?, ?, ?, false)";

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
        String sql = "INSERT INTO lignes_vente (vente_id, produit_id, quantite, prix_unitaire, supprime) " +
                    "VALUES (?, ?, ?, ?, false)";

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
        String sql = "UPDATE produits SET stock = stock - ? " +
                    "WHERE id = ? AND supprime = false AND stock >= ?";

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
        String sql = "UPDATE clients SET solde = solde + ? " +
                    "WHERE id = ? AND supprime = false AND (solde + ?) <= ?";

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

    private void validateVente(Vente vente) {
        LOGGER.info("Début de la validation de la vente...");
        List<String> erreurs = new ArrayList<>();

        if (vente == null) {
            LOGGER.severe("Tentative de validation d'une vente null");
            throw new IllegalArgumentException("La vente ne peut pas être null");
        }

        LOGGER.fine("Validation des lignes de vente...");
        if (vente.getLignes() == null || vente.getLignes().isEmpty()) {
            LOGGER.warning("Tentative de validation d'une vente sans lignes");
            erreurs.add("Une vente doit contenir au moins une ligne");
        }

        LOGGER.fine("Validation de la date...");
        if (vente.getDate() == null) {
            LOGGER.warning("Tentative de validation d'une vente sans date");
            erreurs.add("La date de vente est obligatoire");
        } else if (vente.getDate().isAfter(LocalDateTime.now())) {
            LOGGER.warning("Tentative de validation d'une vente avec une date future: " + vente.getDate());
            erreurs.add("La date de vente ne peut pas être dans le futur");
        }

        LOGGER.fine("Validation du mode de paiement...");
        if (vente.isCredit()) {
            if (vente.getClient() == null) {
                LOGGER.warning("Tentative de vente à crédit sans client");
                erreurs.add("Un client est requis pour une vente à crédit");
            } else {
                LOGGER.fine("Vérification de la limite de crédit pour le client " + vente.getClient().getId());
                double nouveauSolde = vente.getClient().getSolde() + vente.getTotal();
                if (nouveauSolde > LIMITE_CREDIT_MAX) {
                    LOGGER.warning(String.format(
                        "Dépassement de la limite de crédit pour le client %d. Nouveau solde: %.2f, Limite: %.2f",
                        vente.getClient().getId(), nouveauSolde, LIMITE_CREDIT_MAX));
                    erreurs.add(String.format("Le crédit maximum autorisé (%.2f€) serait dépassé", LIMITE_CREDIT_MAX));
                }
            }
        }

        LOGGER.fine("Validation du montant total...");
        if (vente.getTotal() < 0) {
            LOGGER.warning("Tentative de validation d'une vente avec un total négatif: " + vente.getTotal());
            erreurs.add("Le total de la vente ne peut pas être négatif");
        }

        if (vente.getLignes() != null) {
            LOGGER.fine("Validation détaillée des lignes de vente...");
            for (Vente.LigneVente ligne : vente.getLignes()) {
                if (ligne == null || ligne.getProduit() == null) {
                    LOGGER.warning("Ligne de vente ou produit null détecté");
                    erreurs.add("Les lignes de vente et leurs produits ne peuvent pas être null");
                    continue;
                }

                String produitNom = sanitizeInput(ligne.getProduit().getNom());
                if (ligne.getQuantite() <= 0) {
                    LOGGER.warning(String.format(
                        "Quantité invalide (%d) détectée pour le produit %s",
                        ligne.getQuantite(), produitNom));
                    erreurs.add(String.format("La quantité doit être positive pour l'article %s",
                        produitNom));
                }
                if (ligne.getPrixUnitaire() <= 0) {
                    LOGGER.warning(String.format(
                        "Prix unitaire invalide (%.2f) détecté pour le produit %s",
                        ligne.getPrixUnitaire(), produitNom));
                    erreurs.add(String.format("Le prix unitaire doit être positif pour l'article %s",
                        produitNom));
                }

                // Vérification supplémentaire de la cohérence des prix
                double ecartPrixAutorise = 0.01; // 1% d'écart maximum autorisé
                double ecartPrix = Math.abs(ligne.getPrixUnitaire() - ligne.getProduit().getPrixVente())
                                 / ligne.getProduit().getPrixVente();
                if (ecartPrix > ecartPrixAutorise) {
                    LOGGER.warning(String.format(
                        "Écart de prix suspect détecté pour %s: prix unitaire=%.2f, prix catalogue=%.2f",
                        produitNom, ligne.getPrixUnitaire(), ligne.getProduit().getPrixVente()));
                    erreurs.add(String.format(
                        "Le prix unitaire pour l'article %s diffère significativement du prix catalogue",
                        produitNom));
                }
            }
        }

        // Vérification du total calculé
        LOGGER.fine("Vérification de la cohérence du total...");
        double calculatedTotal = vente.getMontantTotal();
        if (Math.abs(calculatedTotal - vente.getTotal()) > 0.01) {
            String message = String.format(
                "Le total de la vente (%.2f) ne correspond pas à la somme des lignes (%.2f)",
                vente.getTotal(), calculatedTotal);
            LOGGER.warning(message);
            erreurs.add(message);
        }

        if (!erreurs.isEmpty()) {
            String erreursConcatenees = String.join("\n", erreurs);
            LOGGER.severe("Validation échouée avec les erreurs suivantes:\n" + erreursConcatenees);
            throw new IllegalArgumentException(erreursConcatenees);
        }

        LOGGER.info("Validation de la vente réussie");
    }
}