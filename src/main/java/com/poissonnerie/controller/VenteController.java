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
import java.util.regex.Pattern;

public class VenteController {
    private static final Logger LOGGER = Logger.getLogger(VenteController.class.getName());
    private final List<Vente> ventes;
    private static final double LIMITE_CREDIT_MAX = 5000.0;
    private static final int TRANSACTION_TIMEOUT_SECONDS = 30;

    // Motifs de validation
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final int MAX_LIGNES_VENTE = 100;

    public VenteController() {
        this.ventes = new ArrayList<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        LOGGER.info("Initialisation des tables de la base de données...");
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Activer les contraintes de clé étrangère et configurer le timeout
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = " + (TRANSACTION_TIMEOUT_SECONDS * 1000));

            // Mettre à jour la table ventes si nécessaire
            stmt.execute("CREATE TABLE IF NOT EXISTS ventes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "date BIGINT NOT NULL, " +
                        "client_id INTEGER, " +
                        "credit BOOLEAN NOT NULL, " +
                        "total DOUBLE NOT NULL CHECK (total >= 0), " +
                        "mode_paiement VARCHAR(50) NOT NULL DEFAULT 'ESPECES', " +
                        "supprime BOOLEAN DEFAULT false, " +
                        "FOREIGN KEY (client_id) REFERENCES clients(id))");

            // Ajouter la colonne mode_paiement si elle n'existe pas déjà
            try {
                stmt.execute("ALTER TABLE ventes ADD COLUMN mode_paiement VARCHAR(50) NOT NULL DEFAULT 'ESPECES'");
                LOGGER.info("Colonne mode_paiement ajoutée à la table ventes");
            } catch (SQLException e) {
                // La colonne existe déjà, on ignore l'erreur
                LOGGER.fine("La colonne mode_paiement existe déjà dans la table ventes");
            }

            // Mettre à jour la table lignes_vente si nécessaire
            stmt.execute("CREATE TABLE IF NOT EXISTS lignes_vente (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "vente_id INTEGER NOT NULL, " +
                        "produit_id INTEGER NOT NULL, " +
                        "quantite INTEGER NOT NULL CHECK (quantite > 0), " +
                        "prix_unitaire DOUBLE NOT NULL CHECK (prix_unitaire > 0), " +
                        "supprime BOOLEAN DEFAULT false, " +
                        "FOREIGN KEY (vente_id) REFERENCES ventes(id), " +
                        "FOREIGN KEY (produit_id) REFERENCES produits(id))");

            // Créer les index si nécessaire
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventes_client ON ventes(client_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lignes_vente_vente ON lignes_vente(vente_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lignes_vente_produit ON lignes_vente(produit_id)");

            LOGGER.info("Tables mises à jour avec succès");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'initialisation des tables", e);
            throw new RuntimeException("Erreur lors de l'initialisation de la base de données", e);
        }
    }

    public List<Vente> getVentes() {
        return new ArrayList<>(ventes);
    }

    public void chargerVentes() {
        LOGGER.info("Chargement des ventes en cours...");
        ventes.clear();

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT v.*, c.* FROM ventes v LEFT JOIN clients c ON v.client_id = c.id " +
                        "WHERE v.supprime = false ORDER BY v.date DESC LIMIT 1000";

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

                LOGGER.info("Ventes chargées avec succès: " + ventes.size() + " ventes");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Échec du chargement des ventes", e);
                throw new RuntimeException("Erreur lors du chargement des ventes", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors du chargement des ventes", e);
            throw new RuntimeException("Erreur lors du chargement des ventes", e);
        }
    }

    private Client creerClientDepuisResultSet(ResultSet rs) throws SQLException {
        try {
            int clientId = rs.getInt("client_id");
            String nom = sanitizeInput(rs.getString("nom"));
            String telephone = sanitizeInput(rs.getString("telephone"));
            String adresse = sanitizeInput(rs.getString("adresse"));
            double solde = rs.getDouble("solde");

            // Validation supplémentaire des données
            if (clientId <= 0) {
                throw new SQLException("ID client invalide: " + clientId);
            }
            if (nom == null || nom.trim().isEmpty()) {
                throw new SQLException("Nom client invalide");
            }
            if (solde < 0 || solde > LIMITE_CREDIT_MAX) {
                throw new SQLException("Solde client invalide: " + solde);
            }

            return new Client(clientId, nom, telephone, adresse, solde);
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

                    int quantite = rs.getInt("quantite");
                    double prixUnitaire = rs.getDouble("prix_unitaire");

                    // Validation supplémentaire
                    if (quantite <= 0) {
                        throw new SQLException("Quantité invalide: " + quantite);
                    }
                    if (prixUnitaire <= 0) {
                        throw new SQLException("Prix unitaire invalide: " + prixUnitaire);
                    }

                    Vente.LigneVente ligne = new Vente.LigneVente(produit, quantite, prixUnitaire);
                    lignes.add(ligne);
                }
            }

            if (lignes.size() > MAX_LIGNES_VENTE) {
                throw new SQLException("Nombre maximum de lignes dépassé: " + lignes.size());
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
        if (produit.getStock() < 0) {
            throw new IllegalStateException("Stock invalide pour le produit: " + produit.getId());
        }
    }

    private Produit creerProduitDepuisResultSet(ResultSet rs) throws SQLException {
        try {
            int produitId = rs.getInt("produit_id");
            String nom = sanitizeInput(rs.getString("nom"));
            String categorie = sanitizeInput(rs.getString("categorie"));
            double prixAchat = rs.getDouble("prix_achat");
            double prixVente = rs.getDouble("prix_vente");
            int stock = rs.getInt("stock");
            int seuilAlerte = rs.getInt("seuil_alerte");

            // Validation supplémentaire
            if (produitId <= 0) {
                throw new SQLException("ID produit invalide: " + produitId);
            }
            if (nom == null || nom.trim().isEmpty()) {
                throw new SQLException("Nom produit invalide");
            }
            if (prixAchat < 0 || prixVente <= 0 || prixVente < prixAchat) {
                throw new SQLException("Prix invalides: achat=" + prixAchat + ", vente=" + prixVente);
            }
            if (stock < 0) {
                throw new SQLException("Stock invalide: " + stock);
            }
            if (seuilAlerte < 0) {
                throw new SQLException("Seuil d'alerte invalide: " + seuilAlerte);
            }

            return new Produit(produitId, nom, categorie, prixAchat, prixVente, stock, seuilAlerte);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création d'un produit depuis le ResultSet", e);
            throw e;
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Nettoyer les caractères spéciaux et les caractères de contrôle
        String cleaned = input.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", "")
                            .replaceAll("[<>\"'%;)(&+\\[\\]{}]", "")
                            .trim()
                            .replaceAll("\\s+", " ");

        // Limiter la longueur
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
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

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Définir un timeout de transaction
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA busy_timeout = " + (TRANSACTION_TIMEOUT_SECONDS * 1000));
                }

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

            } catch (SQLException | IllegalStateException e) {
                try {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Transaction annulée", e);
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du rollback", ex);
                }
                LOGGER.log(Level.SEVERE, "Erreur SQL lors de l'enregistrement de la vente", e);
                throw new RuntimeException("Erreur lors de l'enregistrement de la vente: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'enregistrement de la vente", e);
            throw new RuntimeException("Erreur lors de l'enregistrement de la vente: " + e.getMessage(), e);
        }
    }

    private int insererVente(Connection conn, Vente vente) throws SQLException {
        String sql = "INSERT INTO ventes (date, client_id, credit, total, mode_paiement) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
            pstmt.setString(5, vente.getModePaiement().name());

            pstmt.executeUpdate();

            // Utiliser last_insert_rowid() de SQLite pour obtenir l'ID généré
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
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

            pstmt.executeUpdate();
            LOGGER.info("Ligne de vente enregistrée pour le produit: " + ligne.getProduit().getId());
        }
    }

    private void mettreAJourStock(Connection conn, Vente.LigneVente ligne) throws SQLException {
        String sql = "UPDATE produits SET stock = stock - ? WHERE id = ? AND supprime = false AND stock >= ?";

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
        // Vérifier d'abord si le nouveau solde ne dépassera pas la limite
        String checkSql = "SELECT solde FROM clients WHERE id = ? AND supprime = false";
        double nouveauSolde;

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, vente.getClient().getId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Client introuvable");
                }
                nouveauSolde = rs.getDouble("solde") + vente.getTotal();
                if (nouveauSolde > LIMITE_CREDIT_MAX) {
                    throw new SQLException(String.format(
                        "Limite de crédit dépassée. Solde actuel: %.2f, Montant vente: %.2f, Limite: %.2f",
                        rs.getDouble("solde"), vente.getTotal(), LIMITE_CREDIT_MAX));
                }
            }
        }

        // Mettre à jour le solde
        String updateSql = "UPDATE clients SET solde = ? WHERE id = ? AND supprime = false";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setDouble(1, nouveauSolde);
            pstmt.setInt(2, vente.getClient().getId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Impossible de mettre à jour le solde client");
            }
            LOGGER.info(String.format(
                "Solde client mis à jour pour le client %d: %.2f → %.2f",
                vente.getClient().getId(), nouveauSolde - vente.getTotal(), nouveauSolde));
        }
    }

    private void validateVente(Vente vente) {
        LOGGER.info("Début de la validation de la vente...");
        List<String> erreurs = new ArrayList<>();

        if (vente == null) {
            LOGGER.severe("Tentative de validation d'une vente null");
            throw new IllegalArgumentException("La vente ne peut pas être null");
        }

        if (vente.getLignes() == null || vente.getLignes().isEmpty()) {
            LOGGER.warning("Tentative de validation d'une vente sans lignes");
            throw new IllegalArgumentException("Une vente doit avoir au moins une ligne");
        }

        if (vente.getLignes().size() > MAX_LIGNES_VENTE) {
            erreurs.add("Nombre maximum de lignes dépassé: " + vente.getLignes().size());
        }

        if (vente.getDate() == null) {
            LOGGER.warning("Tentative de validation d'une vente sans date");
            erreurs.add("La date de vente est obligatoire");
        } else if (vente.getDate().isAfter(LocalDateTime.now())) {
            LOGGER.warning("Tentative de validation d'une vente avec une date future: " + vente.getDate());
            erreurs.add("La date de vente ne peut pas être dans le futur");
        }

        if (vente.isCredit() && vente.getClient() == null) {
            LOGGER.warning("Tentative de vente à crédit sans client");
            erreurs.add("Un client est requis pour une vente à crédit");
        }

        if (vente.getTotal() < 0) {
            LOGGER.warning("Tentative de validation d'une vente avec un total négatif: " + vente.getTotal());
            erreurs.add("Le total de la vente ne peut pas être négatif");
        }

        // Vérifier chaque ligne de vente
        for (Vente.LigneVente ligne : vente.getLignes()) {
            if (ligne == null) {
                erreurs.add("Une ligne de vente ne peut pas être null");
                continue;
            }

            if (ligne.getProduit() == null) {
                erreurs.add("Le produit ne peut pas être null");
                continue;
            }

            if (ligne.getQuantite() <= 0) {
                erreurs.add(String.format(
                    "La quantité doit être positive pour l'article %s (quantité: %d)",
                    sanitizeInput(ligne.getProduit().getNom()), ligne.getQuantite()));
            }

            if (ligne.getPrixUnitaire() <= 0) {
                erreurs.add(String.format(
                    "Le prix unitaire doit être positif pour l'article %s (prix: %.2f)",
                    sanitizeInput(ligne.getProduit().getNom()), ligne.getPrixUnitaire()));
            }
        }

        if (!erreurs.isEmpty()) {
            String erreursConcatenees = String.join("\n", erreurs);
            LOGGER.severe("Validation échouée avec les erreurs suivantes:\n" + erreursConcatenees);
            throw new IllegalArgumentException(erreursConcatenees);
        }

        LOGGER.info("Validation de la vente réussie");
    }
}