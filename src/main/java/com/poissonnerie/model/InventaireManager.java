package com.poissonnerie.model;

import java.util.ArrayList;
import java.util.List;

public class InventaireManager {
    private List<InventaireObserver> observers;

    public InventaireManager() {
        this.observers = new ArrayList<>();
    }

    public interface InventaireObserver {
        void onStockBas(Produit produit);
        void onRuptureStock(Produit produit);
        void onStockAjuste(Produit produit, int ancienStock, int nouveauStock);
    }

    public void ajouterObserver(InventaireObserver observer) {
        observers.add(observer);
    }

    public void retirerObserver(InventaireObserver observer) {
        observers.remove(observer);
    }

    public void ajusterStock(Produit produit, int quantite) {
        int ancienStock = produit.getStock();
        try {
            produit.ajusterStock(quantite);
            int nouveauStock = produit.getStock();
            
            // Notifier les observateurs du changement de stock
            for (InventaireObserver observer : observers) {
                observer.onStockAjuste(produit, ancienStock, nouveauStock);
                
                // Vérifier si le nouveau stock est bas ou en rupture
                if (nouveauStock == 0) {
                    observer.onRuptureStock(produit);
                } else if (produit.estStockBas()) {
                    observer.onStockBas(produit);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Impossible d'ajuster le stock: " + e.getMessage());
        }
    }

    public List<Produit> getProduitsBas(List<Produit> produits) {
        List<Produit> produitsBas = new ArrayList<>();
        for (Produit produit : produits) {
            if (produit.estStockBas()) {
                produitsBas.add(produit);
            }
        }
        return produitsBas;
    }

    public List<Produit> getProduitsEnRupture(List<Produit> produits) {
        List<Produit> produitsRupture = new ArrayList<>();
        for (Produit produit : produits) {
            if (produit.getStock() == 0) {
                produitsRupture.add(produit);
            }
        }
        return produitsRupture;
    }
}
