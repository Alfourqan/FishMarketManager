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
    private static final int BATCH_SIZE = 100;

    // Motifs de validation
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    private static final int MAX_LIGNES_VENTE = 100;

    private static final String SQL_INSERT_VENTE = 
        "INSERT INTO ventes (date, client_id, credit, total) VALUES (?, ?, ?, ?)";
    private static final String SQL_INSERT_LIGNE_VENTE = 
        "INSERT INTO lignes_vente (vente_id, produit_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE_STOCK = 
        "UPDATE produits SET stock = stock - ? WHERE id = ? AND supprime = false AND stock >= ?";
    private static final String SQL_UPDATE_SOLDE_CLIENT = 
        "UPDATE clients SET solde = solde + ? WHERE id = ? AND supprime = false AND " +
        "(solde + ?) <= ?";

    public VenteController() {
        this.ventes = new ArrayList<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        LOGGER.info("Initialisation des tables de la base de données...");
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Optimisation des paramètres SQLite
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = " + (TRANSACTION_TIMEOUT_SECONDS * 1000));
            stmt.execute("PRAGMA cache_size = 2000");
            stmt.execute("PRAGMA page_size = 4096");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");

            // Index optimisés pour les requêtes fréquentes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventes_date ON ventes(date DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventes_client_date ON ventes(client_id, date DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lignes_vente_produit_date ON lignes_vente(produit_id, vente_id)");

            LOGGER.info("Base de données optimisée avec succès");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'optimisation de la base de données", e);
            throw new RuntimeException("Erreur d'initialisation de la base de données", e);
        }
    }

    public List<Vente> getVentes() {
        return new ArrayList<>(ventes);
    }

    public void chargerVentes() {
        LOGGER.info("Chargement des ventes en cours...");
        ventes.clear();

        String sql = "SELECT v.*, c.* FROM ventes v " +
                    "LEFT JOIN clients c ON v.client_id = c.id " +
                    "WHERE v.supprime = false " +
                    "ORDER BY v.date DESC LIMIT " + BATCH_SIZE;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Client client = null;
                if (rs.getObject("client_id") != null) {
                    client = creerClientDepuisResultSet(rs);
                }

                Vente vente = new Vente(
                    rs.getInt("id"),
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(rs.getLong("date")),
                        java.time.ZoneId.systemDefault()
                    ),
                    client,
                    rs.getBoolean("credit"),
                    rs.getDouble("total"),
                    rs.getBoolean("credit") ? Vente.ModePaiement.CREDIT : Vente.ModePaiement.ESPECES
                );

                chargerLignesVente(conn, vente);
                ventes.add(vente);
            }

            LOGGER.info("Ventes chargées avec succès: " + ventes.size() + " ventes");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des ventes", e);
            throw new RuntimeException("Erreur lors du chargement des ventes", e);
        }
    }

    private void chargerLignesVente(Connection conn, Vente vente) throws SQLException {
        String sql = "SELECT l.*, p.* FROM lignes_vente l " +
                    "JOIN produits p ON l.produit_id = p.id " +
                    "WHERE l.vente_id = ? AND l.supprime = false";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vente.getId());
            pstmt.setFetchSize(BATCH_SIZE);

            List<Vente.LigneVente> lignes = new ArrayList<>();
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Produit produit = creerProduitDepuisResultSet(rs);
                validateProduit(produit);

                int quantite = rs.getInt("quantite");
                double prixUnitaire = rs.getDouble("prix_unitaire");

                if (quantite <= 0 || prixUnitaire <= 0) {
                    LOGGER.warning("Ligne de vente invalide ignorée: quantité=" + quantite + ", prix=" + prixUnitaire);
                    continue;
                }

                lignes.add(new Vente.LigneVente(produit, quantite, prixUnitaire));
            }

            if (lignes.size() > MAX_LIGNES_VENTE) {
                throw new SQLException("Nombre maximum de lignes dépassé: " + lignes.size());
            }

            vente.setLignes(lignes);
        }
    }

    public void enregistrerVente(Vente vente) {
        LOGGER.info("Début de l'enregistrement de la vente...");
        validateVente(vente);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Vérification du stock en une seule requête
                verifierStockSuffisant(conn, vente.getLignes());

                // Insertion de la vente
                int venteId = insererVente(conn, vente);
                vente.setId(venteId);

                // Insertion des lignes et mise à jour des stocks en batch
                insererLignesEtMettreAJourStocks(conn, venteId, vente.getLignes());

                // Mise à jour du solde client si nécessaire
                if (vente.isCredit() && vente.getClient() != null) {
                    mettreAJourSoldeClient(conn, vente);
                }

                conn.commit();
                ventes.add(vente);
                LOGGER.info("Vente enregistrée avec succès, ID: " + vente.getId());

            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de la vente", e);
                throw new RuntimeException("Erreur lors de l'enregistrement: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'enregistrement de la vente", e);
            throw new RuntimeException("Erreur d'enregistrement: " + e.getMessage(), e);
        }
    }

    private void verifierStockSuffisant(Connection conn, List<Vente.LigneVente> lignes) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT p.id, p.stock FROM produits p WHERE p.id IN (");

        for (int i = 0; i < lignes.size(); i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(") AND (");

        for (int i = 0; i < lignes.size(); i++) {
            if (i > 0) sql.append(" OR ");
            sql.append("(p.id = ? AND p.stock < ?)");
        }
        sql.append(") OR p.supprime = true");

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            // Première série de paramètres pour IN clause
            for (Vente.LigneVente ligne : lignes) {
                pstmt.setInt(paramIndex++, ligne.getProduit().getId());
            }

            // Deuxième série de paramètres pour les conditions de stock
            for (Vente.LigneVente ligne : lignes) {
                pstmt.setInt(paramIndex++, ligne.getProduit().getId());
                pstmt.setInt(paramIndex++, ligne.getQuantite());
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int produitId = rs.getInt("id");
                int stockActuel = rs.getInt("stock");
                Vente.LigneVente ligneProblematique = lignes.stream()
                    .filter(l -> l.getProduit().getId() == produitId)
                    .findFirst()
                    .orElse(null);

                if (ligneProblematique != null) {
                    throw new SQLException(String.format(
                        "Stock insuffisant pour le produit %s (ID: %d). Stock actuel: %d, Quantité demandée: %d",
                        ligneProblematique.getProduit().getNom(),
                        produitId,
                        stockActuel,
                        ligneProblematique.getQuantite()
                    ));
                } else {
                    throw new SQLException("Produit supprimé ou introuvable");
                }
            }
        }
    }

    private void insererLignesEtMettreAJourStocks(Connection conn, int venteId, List<Vente.LigneVente> lignes) 
            throws SQLException {
        try (PreparedStatement pstmtLigne = conn.prepareStatement(SQL_INSERT_LIGNE_VENTE);
             PreparedStatement pstmtStock = conn.prepareStatement(SQL_UPDATE_STOCK)) {

            for (Vente.LigneVente ligne : lignes) {
                // Insertion ligne de vente
                pstmtLigne.setInt(1, venteId);
                pstmtLigne.setInt(2, ligne.getProduit().getId());
                pstmtLigne.setInt(3, ligne.getQuantite());
                pstmtLigne.setDouble(4, ligne.getPrixUnitaire());
                pstmtLigne.addBatch();

                // Mise à jour stock
                pstmtStock.setInt(1, ligne.getQuantite());
                pstmtStock.setInt(2, ligne.getProduit().getId());
                pstmtStock.setInt(3, ligne.getQuantite());
                pstmtStock.addBatch();
            }

            pstmtLigne.executeBatch();
            pstmtStock.executeBatch();
        }
    }

    private void mettreAJourSoldeClient(Connection conn, Vente vente) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_SOLDE_CLIENT)) {
            pstmt.setDouble(1, vente.getTotal());
            pstmt.setInt(2, vente.getClient().getId());
            pstmt.setDouble(3, vente.getTotal());
            pstmt.setDouble(4, LIMITE_CREDIT_MAX);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Impossible de mettre à jour le solde client (limite dépassée ou client introuvable)");
            }
        }
    }

    private int insererVente(Connection conn, Vente vente) throws SQLException {
        String sql = "INSERT INTO ventes (date, client_id, credit, total) VALUES (?, ?, ?, ?)";

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



    private Client creerClientDepuisResultSet(ResultSet rs) throws SQLException {
        try {
            int clientId = rs.getInt("client_id");
            String nom = rs.getString("nom");
            String telephone = rs.getString("telephone");
            String adresse = rs.getString("adresse");
            double solde = rs.getDouble("solde");

            // Validation de l'ID
            if (clientId <= 0) {
                LOGGER.warning("ID client invalide trouvé: " + clientId);
                throw new SQLException("ID client invalide: " + clientId);
            }

            // Nettoyage et validation du nom
            nom = (nom != null) ? sanitizeInput(nom).trim() : "";
            if (nom.isEmpty()) {
                nom = "Client " + clientId;
                LOGGER.info("Nom par défaut généré pour le client ID " + clientId + ": " + nom);
            }

            // Validation du solde
            if (solde < 0) {
                LOGGER.warning("Solde négatif détecté pour le client " + clientId + ": " + solde);
                solde = 0.0;
            } else if (solde > LIMITE_CREDIT_MAX) {
                LOGGER.warning("Solde supérieur à la limite pour le client " + clientId + ": " + solde);
                solde = LIMITE_CREDIT_MAX;
            }

            // Nettoyage des autres champs
            telephone = sanitizeInput(telephone);
            adresse = sanitizeInput(adresse);

            Client client = new Client(clientId, nom, telephone, adresse, solde);
            LOGGER.info("Client créé avec succès - ID: " + clientId + ", Nom: " + nom);
            return client;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du client depuis ResultSet - " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inattendue lors de la création du client - " + e.getMessage(), e);
            throw new SQLException("Erreur lors de la création du client: " + e.getMessage(), e);
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        // Nettoyage des caractères spéciaux et normalisation
        String cleaned = input
            .replaceAll("[\\p{Cntrl}]", "") // Supprime les caractères de contrôle
            .replaceAll("[<>\"';)(&+\\[\\]]", "") // Supprime les caractères potentiellement dangereux
            .trim()
            .replaceAll("\\s+", " "); // Normalise les espaces

        // Limite la longueur en gardant le début
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
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