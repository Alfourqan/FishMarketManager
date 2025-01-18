package com.poissonnerie.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

public class InventaireManager {
    private static final Logger LOGGER = Logger.getLogger(InventaireManager.class.getName());
    private final List<InventaireObserver> observers;

    public InventaireManager() {
        // Utilisation de CopyOnWriteArrayList pour la thread-safety
        this.observers = new CopyOnWriteArrayList<>();
    }

    public interface InventaireObserver {
        void onStockBas(Produit produit);
        void onRuptureStock(Produit produit);
        void onStockAjuste(Produit produit, int ancienStock, int nouveauStock);
    }

    public void ajouterObserver(InventaireObserver observer) {
        if (observer == null) {
            LOGGER.warning("Tentative d'ajout d'un observer null");
            throw new IllegalArgumentException("L'observer ne peut pas être null");
        }
        observers.add(observer);
        LOGGER.info("Observer ajouté. Total observers: " + observers.size());
    }

    public void retirerObserver(InventaireObserver observer) {
        if (observer == null) {
            LOGGER.warning("Tentative de retrait d'un observer null");
            throw new IllegalArgumentException("L'observer ne peut pas être null");
        }
        observers.remove(observer);
        LOGGER.info("Observer retiré. Total observers: " + observers.size());
    }

    public void ajusterStock(Produit produit, int quantite) {
        if (produit == null) {
            LOGGER.severe("Tentative d'ajustement de stock avec un produit null");
            throw new IllegalArgumentException("Le produit ne peut pas être null");
        }

        LOGGER.info(String.format("Ajustement du stock pour %s: %d → %d",
            produit.getNom(), produit.getStock(), produit.getStock() + quantite));

        int ancienStock = produit.getStock();
        try {
            produit.ajusterStock(quantite);
            int nouveauStock = produit.getStock();

            // Notifier les observateurs du changement de stock
            for (InventaireObserver observer : observers) {
                try {
                    observer.onStockAjuste(produit, ancienStock, nouveauStock);

                    // Vérifier si le nouveau stock est bas ou en rupture
                    if (nouveauStock == 0) {
                        LOGGER.warning("Rupture de stock pour " + produit.getNom());
                        observer.onRuptureStock(produit);
                    } else if (produit.estStockBas()) {
                        LOGGER.warning("Stock bas pour " + produit.getNom());
                        observer.onStockBas(produit);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la notification de l'observer", e);
                    // Continue avec les autres observers même si un échoue
                }
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajustement du stock", e);
            throw new IllegalArgumentException("Impossible d'ajuster le stock: " + e.getMessage());
        }
    }

    public List<Produit> getProduitsBas(List<Produit> produits) {
        if (produits == null) {
            LOGGER.warning("Liste de produits null passée à getProduitsBas");
            return Collections.emptyList();
        }

        List<Produit> produitsBas = new ArrayList<>();
        for (Produit produit : produits) {
            if (produit != null && produit.estStockBas()) {
                produitsBas.add(produit);
                LOGGER.info("Produit en stock bas détecté: " + produit.getNom());
            }
        }
        return Collections.unmodifiableList(produitsBas);
    }

    public List<Produit> getProduitsEnRupture(List<Produit> produits) {
        if (produits == null) {
            LOGGER.warning("Liste de produits null passée à getProduitsEnRupture");
            return Collections.emptyList();
        }

        List<Produit> produitsRupture = new ArrayList<>();
        for (Produit produit : produits) {
            if (produit != null && produit.getStock() == 0) {
                produitsRupture.add(produit);
                LOGGER.info("Produit en rupture détecté: " + produit.getNom());
            }
        }
        return Collections.unmodifiableList(produitsRupture);
    }
}