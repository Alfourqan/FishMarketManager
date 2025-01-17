package com.poissonnerie.test;

import com.poissonnerie.model.Produit;
import com.poissonnerie.model.InventaireManager;
import com.poissonnerie.model.InventaireManager.InventaireObserver;

public class TestInventaire {
    public static void main(String[] args) {
        // Créer un gestionnaire d'inventaire
        InventaireManager inventaireManager = new InventaireManager();
        
        // Créer un observer pour les alertes
        InventaireObserver observer = new InventaireObserver() {
            @Override
            public void onStockBas(Produit produit) {
                System.out.println("🚨 ALERTE: Stock bas pour " + produit.getNom() + 
                    " (Stock actuel: " + produit.getStock() + 
                    ", Seuil: " + produit.getSeuilAlerte() + ")");
            }

            @Override
            public void onRuptureStock(Produit produit) {
                System.out.println("⛔ ALERTE CRITIQUE: Rupture de stock pour " + produit.getNom());
            }

            @Override
            public void onStockAjuste(Produit produit, int ancienStock, int nouveauStock) {
                System.out.println("ℹ️ Stock ajusté pour " + produit.getNom() + 
                    " : " + ancienStock + " → " + nouveauStock);
            }
        };

        // Ajouter l'observer au gestionnaire
        inventaireManager.ajouterObserver(observer);

        // Créer un produit test
        Produit saumon = new Produit(1, "Saumon frais", "Poisson", 25.99, 10, 5);
        System.out.println("\nÉtat initial du produit:");
        System.out.println(saumon);

        // Test 1: Réduire le stock mais rester au-dessus du seuil
        System.out.println("\nTest 1: Réduction du stock (reste normal)");
        inventaireManager.ajusterStock(saumon, -3);
        System.out.println(saumon);

        // Test 2: Réduire le stock en dessous du seuil
        System.out.println("\nTest 2: Réduction du stock (passe sous le seuil)");
        inventaireManager.ajusterStock(saumon, -3);
        System.out.println(saumon);

        // Test 3: Mettre le stock à zéro
        System.out.println("\nTest 3: Mise en rupture de stock");
        inventaireManager.ajusterStock(saumon, -4);
        System.out.println(saumon);

        // Test 4: Réapprovisionner
        System.out.println("\nTest 4: Réapprovisionnement");
        inventaireManager.ajusterStock(saumon, 8);
        System.out.println(saumon);
    }
}
