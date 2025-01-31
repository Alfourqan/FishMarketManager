package com.poissonnerie.controller;

import com.poissonnerie.model.Produit;
import com.poissonnerie.model.UserAction;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitController {
    private final List<Produit> produits = new ArrayList<>();
    private static final int BATCH_SIZE = 100;
    private final UserActionController userActionController = UserActionController.getInstance();

    public List<Produit> getProduits() {
        return new ArrayList<>(produits);
    }

    // Alias de getProduits() pour maintenir la compatibilité avec ReportController
    public List<Produit> getTousProduits() {
        return getProduits();
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
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du chargement des produits", e);
        }
    }

    public void ajouterProduit(Produit produit) {
        String sql = "INSERT INTO produits (nom, categorie, prix_achat, prix_vente, stock, seuil_alerte) VALUES (?, ?, ?, ?, ?, ?)";
        String getIdSql = "SELECT last_insert_rowid() as id";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, produit.getNom());
                pstmt.setString(2, produit.getCategorie());
                pstmt.setDouble(3, produit.getPrixAchat());
                pstmt.setDouble(4, produit.getPrixVente());
                pstmt.setInt(5, produit.getStock());
                pstmt.setInt(6, produit.getSeuilAlerte());
                pstmt.executeUpdate();

                // Récupérer l'ID généré
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(getIdSql)) {
                    if (rs.next()) {
                        produit.setId(rs.getInt("id"));
                        produits.add(produit);

                        // Log de l'action d'ajout
                        UserAction action = new UserAction(
                            UserAction.ActionType.CREATION,
                            "SYSTEM", // À remplacer par l'utilisateur connecté
                            String.format("Ajout du produit %s (Catégorie: %s, Stock initial: %d)",
                                produit.getNom(),
                                produit.getCategorie(),
                                produit.getStock()),
                            UserAction.EntityType.PRODUIT,
                            produit.getId()
                        );
                        userActionController.logAction(action);
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout du produit", e);
        }
    }

    public void mettreAJourProduit(Produit produit) {
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

                            // Log de l'action de mise à jour
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
                    throw new SQLException("Aucun produit trouvé avec l'ID: " + produit.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour du produit", e);
        }
    }

    public boolean produitUtiliseDansVentes(int produitId) {
        String sql = "SELECT 1 FROM lignes_vente WHERE produit_id = ? AND supprime = false LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, produitId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

                    // Log de l'action de suppression
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
                    throw new SQLException("Aucun produit trouvé avec l'ID: " + produit.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du produit", e);
        }
    }
}