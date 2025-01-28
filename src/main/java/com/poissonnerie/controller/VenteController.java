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
    private final CaisseController caisseController;

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
        this.caisseController = new CaisseController();
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
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            try {
                // Vérification du stock avec retries
                int retryCount = 0;
                boolean success = false;
                while (!success && retryCount < 3) {
                    try {
                        verifierStockSuffisant(conn, vente.getLignes());
                        success = true;
                    } catch (SQLException e) {
                        if (e.getMessage().contains("database is locked") && retryCount < 2) {
                            retryCount++;
                            Thread.sleep(1000);
                        } else {
                            throw e;
                        }
                    }
                }

                // Insertion de la vente avec generated keys
                int venteId = insererVente(conn, vente);
                vente.setId(venteId);

                // Insertion des lignes et mise à jour des stocks
                for (Vente.LigneVente ligne : vente.getLignes()) {
                    insererLigneVente(conn, venteId, ligne);
                    mettreAJourStock(conn, ligne);
                }

                // Mise à jour du solde client si nécessaire
                if (vente.isCredit() && vente.getClient() != null) {
                    mettreAJourSoldeClient(conn, vente);
                }

                conn.commit();
                ventes.add(vente);
                LOGGER.info("Vente enregistrée avec succès, ID: " + vente.getId());

            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException re) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                }
                LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de la vente", e);
                throw new RuntimeException("Erreur lors de l'enregistrement: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'enregistrement de la vente", e);
            throw new RuntimeException("Erreur d'enregistrement: " + e.getMessage(), e);
        }
    }

    private void validateVente(Vente vente) {
        LOGGER.info("Validation de la vente...");
        List<String> erreurs = new ArrayList<>();

        if (vente == null) {
            throw new IllegalArgumentException("La vente ne peut pas être null");
        }

        if (vente.getLignes() == null || vente.getLignes().isEmpty()) {
            throw new IllegalArgumentException("Une vente doit avoir au moins une ligne");
        }

        if (vente.getLignes().size() > MAX_LIGNES_VENTE) {
            erreurs.add("Nombre maximum de lignes dépassé: " + vente.getLignes().size());
        }

        // Vérification de la date
        if (vente.getDate() == null) {
            LOGGER.warning("Date de vente manquante, une nouvelle vente sera créée");
            Vente nouvelleVente = new Vente(
                vente.getId(),
                LocalDateTime.now(),
                vente.getClient(),
                vente.isCredit(),
                vente.getTotal(),
                vente.getModePaiement()
            );
            // Copier les lignes de vente
            nouvelleVente.setLignes(vente.getLignes());
            vente = nouvelleVente;
        }

        // Vérification du crédit
        if (vente.isCredit() && vente.getClient() == null) {
            erreurs.add("Un client est requis pour une vente à crédit");
        }

        // Vérification des lignes
        for (Vente.LigneVente ligne : vente.getLignes()) {
            if (ligne == null || ligne.getProduit() == null) {
                erreurs.add("Une ligne de vente est invalide");
                continue;
            }

            if (ligne.getQuantite() <= 0) {
                erreurs.add("La quantité doit être positive pour " + ligne.getProduit().getNom());
            }

            if (ligne.getPrixUnitaire() <= 0) {
                erreurs.add("Le prix unitaire doit être positif pour " + ligne.getProduit().getNom());
            }
        }

        if (!erreurs.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", erreurs));
        }
    }

    private void verifierStockSuffisant(Connection conn, List<Vente.LigneVente> lignes) throws SQLException {
        String sql = "SELECT p.id, p.stock FROM produits p WHERE p.id = ? FOR UPDATE NOWAIT";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Vente.LigneVente ligne : lignes) {
                pstmt.setInt(1, ligne.getProduit().getId());

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int stockActuel = rs.getInt("stock");
                        if (stockActuel < ligne.getQuantite()) {
                            throw new SQLException(
                                String.format("Stock insuffisant pour %s. Stock: %d, Demandé: %d",
                                    ligne.getProduit().getNom(), stockActuel, ligne.getQuantite())
                            );
                        }
                    } else {
                        throw new SQLException("Produit non trouvé: " + ligne.getProduit().getId());
                    }
                }
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

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Impossible de récupérer l'ID de la vente");
                }
            }
        }
    }
    private void insererLigneVente(Connection conn, int venteId, Vente.LigneVente ligne) throws SQLException {
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
}