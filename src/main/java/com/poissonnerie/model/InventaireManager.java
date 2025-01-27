package com.poissonnerie.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.LocalDateTime;

public class InventaireManager {
    private static final Logger LOGGER = Logger.getLogger(InventaireManager.class.getName());
    private final List<InventaireObserver> observers;
    private final List<AjustementStock> historique;

    public InventaireManager() {
        this.observers = new CopyOnWriteArrayList<>();
        this.historique = new CopyOnWriteArrayList<>();
    }

    public static class AjustementStock {
        private final Produit produit;
        private final int ancienStock;
        private final int nouveauStock;
        private final LocalDateTime date;
        private final String raison;

        public AjustementStock(Produit produit, int ancienStock, int nouveauStock, String raison) {
            this.produit = produit;
            this.ancienStock = ancienStock;
            this.nouveauStock = nouveauStock;
            this.date = LocalDateTime.now();
            this.raison = raison;
        }

        public Produit getProduit() { return produit; }
        public int getAncienStock() { return ancienStock; }
        public int getNouveauStock() { return nouveauStock; }
        public LocalDateTime getDate() { return date; }
        public String getRaison() { return raison; }
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

    public void ajusterStock(Produit produit, int quantite, String raison) {
        if (produit == null) {
            LOGGER.severe("Tentative d'ajustement de stock avec un produit null");
            throw new IllegalArgumentException("Le produit ne peut pas être null");
        }

        if (raison == null || raison.trim().isEmpty()) {
            LOGGER.warning("Raison non spécifiée pour l'ajustement de stock");
            raison = "Ajustement manuel";
        }

        LOGGER.info(String.format("Ajustement du stock pour %s: %d → %d, Raison: %s",
            produit.getNom(), produit.getStock(), produit.getStock() + quantite, raison));

        int ancienStock = produit.getStock();
        try {
            produit.ajusterStock(quantite);
            int nouveauStock = produit.getStock();

            // Enregistrer l'ajustement dans l'historique
            historique.add(new AjustementStock(produit, ancienStock, nouveauStock, raison));

            // Notifier les observateurs du changement de stock
            for (InventaireObserver observer : observers) {
                try {
                    observer.onStockAjuste(produit, ancienStock, nouveauStock);

                    // Vérifier si le nouveau stock est bas ou en rupture
                    if (nouveauStock == 0) {
                        LOGGER.warning("Rupture de stock pour " + produit.getNom());
                        observer.onRuptureStock(produit);
                    } else if (nouveauStock <= produit.getSeuilAlerte()) {
                        LOGGER.warning("Stock bas pour " + produit.getNom());
                        observer.onStockBas(produit);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la notification de l'observer", e);
                }
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajustement du stock", e);
            throw new IllegalArgumentException("Impossible d'ajuster le stock: " + e.getMessage());
        }
    }

    public void ajusterStock(Produit produit, int quantite) {
        ajusterStock(produit, quantite, "Ajustement manuel");
    }

    public List<AjustementStock> getHistorique() {
        return Collections.unmodifiableList(historique);
    }

    public List<AjustementStock> getHistoriqueProduit(Produit produit) {
        if (produit == null) return Collections.emptyList();

        return historique.stream()
            .filter(ajustement -> ajustement.getProduit().equals(produit))
            .toList();
    }

    public List<Produit> getProduitsBas(List<Produit> produits) {
        if (produits == null) {
            LOGGER.warning("Liste de produits null passée à getProduitsBas");
            return Collections.emptyList();
        }

        List<Produit> produitsBas = new ArrayList<>();
        for (Produit produit : produits) {
            if (produit != null && (produit.getStock() <= produit.getSeuilAlerte())) {
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

    public Map<String, Double> calculerStatistiquesInventaire(List<Produit> produits) {
        if (produits == null || produits.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> stats = new HashMap<>();

        // Valeur totale du stock
        double valeurTotale = produits.stream()
            .mapToDouble(p -> p.getPrixVente() * p.getStock())
            .sum();
        stats.put("valeur_totale", valeurTotale);

        // Taux de rotation moyen
        double tauxRotationMoyen = historique.stream()
            .mapToDouble(a -> Math.abs(a.getNouveauStock() - a.getAncienStock()))
            .average()
            .orElse(0.0);
        stats.put("taux_rotation_moyen", tauxRotationMoyen);

        // Pourcentage de produits en alerte
        long produitsEnAlerte = produits.stream()
            .filter(p -> p.getStock() <= p.getSeuilAlerte())
            .count();
        double pourcentageAlerte = (double) produitsEnAlerte / produits.size() * 100;
        stats.put("pourcentage_alerte", pourcentageAlerte);

        return stats;
    }
}