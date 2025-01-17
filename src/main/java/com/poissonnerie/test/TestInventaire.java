package com.poissonnerie.test;

import com.poissonnerie.model.Produit;
import com.poissonnerie.model.InventaireManager;
import com.poissonnerie.model.InventaireManager.InventaireObserver;

public class TestInventaire {
    public static void main(String[] args) {
        // Créer un gestionnaire d'inventaire
        InventaireManager inventaireManager = new InventaireManager();

        // Créer un observer pour les alertes avec des messages plus détaillés
        InventaireObserver observer = new InventaireObserver() {
            @Override
            public void onStockBas(Produit produit) {
                System.out.println("\n🚨 ALERTE: Stock bas pour " + produit.getNom());
                System.out.println("  → Stock actuel: " + produit.getStock());
                System.out.println("  → Seuil d'alerte: " + produit.getSeuilAlerte());
                System.out.println("  → Statut: " + produit.getStatutStockFormatted());
            }

            @Override
            public void onRuptureStock(Produit produit) {
                System.out.println("\n⛔ ALERTE CRITIQUE: Rupture de stock");
                System.out.println("  → Produit: " + produit.getNom());
                System.out.println("  → Catégorie: " + produit.getCategorie());
                System.out.println("  → Réapprovisionnement nécessaire!");
            }

            @Override
            public void onStockAjuste(Produit produit, int ancienStock, int nouveauStock) {
                System.out.println("\nℹ️ Modification de stock");
                System.out.println("  → Produit: " + produit.getNom());
                System.out.println("  → Ancien stock: " + ancienStock);
                System.out.println("  → Nouveau stock: " + nouveauStock);
                System.out.println("  → Variation: " + (nouveauStock - ancienStock));
            }
        };

        // Ajouter l'observer au gestionnaire
        inventaireManager.ajouterObserver(observer);

        // Test avec plusieurs produits
        System.out.println("\n=== Test de gestion des stocks ===");

        // Test 1: Produit avec stock normal
        Produit saumon = new Produit(1, "Saumon frais", "Poisson", 20.0, 25.99, 10, 5);
        System.out.println("\n1. État initial du saumon:");
        System.out.println(saumon);

        // Test 2: Réduction du stock mais reste normal
        System.out.println("\n2. Réduction du stock (reste normal)");
        inventaireManager.ajusterStock(saumon, -3);
        System.out.println(saumon);

        // Test 3: Stock passe sous le seuil d'alerte
        System.out.println("\n3. Stock passe sous le seuil");
        inventaireManager.ajusterStock(saumon, -3);
        System.out.println(saumon);

        // Test 4: Tentative de retrait excessif
        System.out.println("\n4. Tentative de retrait excessif");
        try {
            inventaireManager.ajusterStock(saumon, -10);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Erreur attendue: " + e.getMessage());
        }

        // Test 5: Mise en rupture de stock
        System.out.println("\n5. Mise en rupture de stock");
        inventaireManager.ajusterStock(saumon, -4);
        System.out.println(saumon);

        // Test 6: Réapprovisionnement
        System.out.println("\n6. Réapprovisionnement");
        inventaireManager.ajusterStock(saumon, 8);
        System.out.println(saumon);

        System.out.println("\n=== Fin des tests ===");
    }
}