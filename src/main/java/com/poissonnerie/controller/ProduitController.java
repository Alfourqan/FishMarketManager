package com.poissonnerie.controller;

import com.poissonnerie.model.Produit;
import com.poissonnerie.model.UserAction;
import com.poissonnerie.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProduitController {
    private static final Logger LOGGER = Logger.getLogger(ProduitController.class.getName());
    private final List<Produit> produits = new ArrayList<>();
    private static final int BATCH_SIZE = 100;
    private final UserActionController userActionController = UserActionController.getInstance();

    public List<Produit> getProduits() {
        return new ArrayList<>(produits);
    }

    public List<Produit> getTousProduits() {
        return getProduits();
    }

    private void validateProduit(Produit produit) throws IllegalArgumentException {
        if (produit.getNom() == null || produit.getNom().trim().length() < 2) {
            throw new IllegalArgumentException("Le nom du produit doit contenir au moins 2 caractères");
        }

        String categorie = produit.getCategorie();
        if (categorie == null || !List.of("Frais", "Surgelé", "Transformé").contains(categorie.trim())) {
            throw new IllegalArgumentException("La catégorie doit être 'Frais', 'Surgelé' ou 'Transformé'");
        }

        if (produit.getPrixAchat() <= 0) {
            throw new IllegalArgumentException("Le prix d'achat doit être positif");
        }
        if (produit.getPrixVente() <= 0) {
            throw new IllegalArgumentException("Le prix de vente doit être positif");
        }
        if (produit.getPrixVente() <= produit.getPrixAchat()) {
            throw new IllegalArgumentException("Le prix de vente doit être supérieur au prix d'achat");
        }

        if (produit.getStock() < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif");
        }
        if (produit.getSeuilAlerte() < 0) {
            throw new IllegalArgumentException("Le seuil d'alerte ne peut pas être négatif");
        }
    }

    public void ajouterProduit(Produit produit) {
        LOGGER.info("Début de l'ajout du produit: " + produit.getNom());
        Connection conn = null;

        try {
            validateProduit(produit);

            if (produit.getFournisseur() == null) {
                throw new IllegalArgumentException("Un fournisseur doit être sélectionné pour le produit");
            }

            // Obtenir une connexion et désactiver l'auto-commit
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // Vérifier l'existence du fournisseur
            String checkFournisseurSql = "SELECT id FROM fournisseurs WHERE id = ? AND supprime = false";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkFournisseurSql)) {
                checkStmt.setInt(1, produit.getFournisseur().getId());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Le fournisseur sélectionné n'existe pas ou a été supprimé");
                    }
                }
            }

            // Insérer le produit
            String sql = "INSERT INTO produits (nom, categorie, prix_achat, prix_vente, stock, seuil_alerte, fournisseur_id, supprime) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, false)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, produit.getNom());
                pstmt.setString(2, produit.getCategorie());
                pstmt.setDouble(3, produit.getPrixAchat());
                pstmt.setDouble(4, produit.getPrixVente());
                pstmt.setInt(5, produit.getStock());
                pstmt.setInt(6, produit.getSeuilAlerte());
                pstmt.setInt(7, produit.getFournisseur().getId());

                pstmt.executeUpdate();

                // Récupérer l'ID généré
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid() as id")) {
                    if (rs.next()) {
                        produit.setId(rs.getInt("id"));
                        produits.add(produit);

                        UserAction action = new UserAction(
                            UserAction.ActionType.CREATION,
                            "",
                            String.format("Ajout du produit %s (Catégorie: %s, Stock initial: %d, Fournisseur: %s)",
                                produit.getNom(),
                                produit.getCategorie(),
                                produit.getStock(),
                                produit.getFournisseur().getNom()),
                            UserAction.EntityType.PRODUIT,
                            produit.getId()
                        );
                        userActionController.logAction(action);
                    }
                }
            }

            conn.commit();
            LOGGER.info("Produit ajouté avec succès: " + produit.getNom());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur SQL lors de l'ajout du produit", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du rollback", ex);
                }
            }
            throw new RuntimeException("Erreur lors de l'ajout du produit: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du produit", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du rollback", ex);
                }
            }
            throw new RuntimeException(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture de la connexion", e);
                }
            }
        }
    }

    public void chargerProduits() {
        produits.clear();
        String sql = "SELECT * FROM produits WHERE supprime = false ORDER BY id LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, BATCH_SIZE);
            stmt.setFetchSize(BATCH_SIZE);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Produit produit = new Produit(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("categorie"),
                        rs.getDouble("prix_achat"),
                        rs.getDouble("prix_vente"),
                        rs.getInt("stock"),
                        rs.getInt("seuil_alerte")
                    );
                    produits.add(produit);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des produits", e);
            throw new RuntimeException("Erreur lors du chargement des produits", e);
        }
    }

    public void mettreAJourProduit(Produit produit) {
        // Valider le produit avant la mise à jour
        validateProduit(produit);

        String sql = "UPDATE produits SET nom = ?, categorie = ?, prix_achat = ?, prix_vente = ?, stock = ?, seuil_alerte = ? " +
                    "WHERE id = ? AND supprime = false";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, produit.getNom());
                pstmt.setString(2, produit.getCategorie());
                pstmt.setDouble(3, produit.getPrixAchat());
                pstmt.setDouble(4, produit.getPrixVente());
                pstmt.setInt(5, produit.getStock());
                pstmt.setInt(6, produit.getSeuilAlerte());
                pstmt.setInt(7, produit.getId());

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    conn.commit();
                    // Mettre à jour la liste en mémoire
                    for (int i = 0; i < produits.size(); i++) {
                        if (produits.get(i).getId() == produit.getId()) {
                            produits.set(i, produit);

                            UserAction action = new UserAction(
                                UserAction.ActionType.MODIFICATION,
                                "SYSTEM", // À remplacer par l'utilisateur connecté
                                String.format("Mise à jour du produit %s (Catégorie: %s, Nouveau stock: %d)",
                                    produit.getNom(),
                                    produit.getCategorie(),
                                    produit.getStock()),
                                UserAction.EntityType.PRODUIT,
                                produit.getId()
                            );
                            userActionController.logAction(action);
                            break;
                        }
                    }
                } else {
                    conn.rollback();
                    throw new IllegalArgumentException("Aucun produit trouvé avec l'ID: " + produit.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erreur SQL lors de la mise à jour du produit: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du produit", e);
            throw new RuntimeException("Erreur lors de la mise à jour du produit", e);
        }
    }

    public boolean produitUtiliseDansVentes(int produitId) {
        String sql = "SELECT 1 FROM lignes_vente WHERE produit_id = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, produitId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de l'utilisation du produit", e);
            throw new RuntimeException("Erreur lors de la vérification de l'utilisation du produit", e);
        }
    }

    public void supprimerProduit(Produit produit) {
        if (produitUtiliseDansVentes(produit.getId())) {
            throw new IllegalStateException("Impossible de supprimer ce produit car il est utilisé dans des ventes existantes.");
        }

        String sql = "UPDATE produits SET supprime = true WHERE id = ? AND supprime = false";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, produit.getId());
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    conn.commit();
                    produits.removeIf(p -> p.getId() == produit.getId());

                    UserAction action = new UserAction(
                        UserAction.ActionType.SUPPRESSION,
                        "SYSTEM", // À remplacer par l'utilisateur connecté
                        String.format("Suppression du produit %s (Catégorie: %s)",
                            produit.getNom(),
                            produit.getCategorie()),
                        UserAction.EntityType.PRODUIT,
                        produit.getId()
                    );
                    userActionController.logAction(action);
                } else {
                    conn.rollback();
                    throw new IllegalArgumentException("Aucun produit trouvé avec l'ID: " + produit.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du produit", e);
                throw new RuntimeException("Erreur SQL lors de la suppression du produit: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors de la suppression du produit", e);
            throw new RuntimeException("Erreur lors de la suppression du produit", e);
        }
    }
}