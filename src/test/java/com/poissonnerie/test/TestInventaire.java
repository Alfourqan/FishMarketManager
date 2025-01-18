package com.poissonnerie.test;

import com.poissonnerie.model.Produit;
import com.poissonnerie.model.InventaireManager;
import com.poissonnerie.model.InventaireManager.InventaireObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

public class TestInventaire {
    private InventaireManager inventaireManager;
    private Produit saumon;
    private boolean stockBasAppele;
    private boolean ruptureStockAppelee;
    private boolean stockAjusteAppele;

    @BeforeEach
    public void setUp() {
        inventaireManager = new InventaireManager();
        // Initialiser avec un stock normal pour les tests généraux
        saumon = new Produit(1, "Saumon frais", "Poisson", 20.0, 25.99, 10, 5);
        stockBasAppele = false;
        ruptureStockAppelee = false;
        stockAjusteAppele = false;

        inventaireManager.ajouterObserver(new InventaireObserver() {
            @Override
            public void onStockBas(Produit produit) {
                stockBasAppele = true;
                System.out.println("🚨 ALERTE: Stock bas pour " + produit.getNom());
            }

            @Override
            public void onRuptureStock(Produit produit) {
                ruptureStockAppelee = true;
                System.out.println("⛔ ALERTE CRITIQUE: Rupture de stock pour " + produit.getNom());
            }

            @Override
            public void onStockAjuste(Produit produit, int ancienStock, int nouveauStock) {
                stockAjusteAppele = true;
                System.out.println("ℹ️ Stock ajusté: " + produit.getNom() + " " + ancienStock + " → " + nouveauStock);
            }
        });
    }

    @AfterEach
    public void tearDown() {
        inventaireManager = null;
        saumon = null;
    }

    @Test
    public void testAjustementStockNormal() {
        int stockInitial = saumon.getStock();
        inventaireManager.ajusterStock(saumon, -3);

        assertEquals(stockInitial - 3, saumon.getStock(), "Le stock doit être diminué de 3");
        assertTrue(stockAjusteAppele, "L'événement d'ajustement doit être déclenché");
        assertFalse(stockBasAppele, "L'alerte de stock bas ne doit pas être déclenchée");
    }

    @Test
    public void testStockBas() {
        // Ajuster le stock pour qu'il soit bas (mais pas en rupture)
        inventaireManager.ajusterStock(saumon, -6);
        assertEquals(4, saumon.getStock(), "Le stock doit être à 4");
        assertTrue(stockBasAppele, "L'alerte de stock bas doit être déclenchée");
        assertFalse(ruptureStockAppelee, "L'alerte de rupture ne doit pas être déclenchée");
    }

    @Test
    public void testRuptureStock() {
        // Mettre en rupture de stock
        inventaireManager.ajusterStock(saumon, -10);
        assertTrue(ruptureStockAppelee, "L'alerte de rupture doit être déclenchée");
        assertEquals(0, saumon.getStock(), "Le stock doit être à zéro");
    }

    @Test
    public void testAjustementInvalide() {
        assertThrows(IllegalArgumentException.class, () -> {
            inventaireManager.ajusterStock(saumon, -11);
        }, "Doit lever une exception pour un retrait supérieur au stock");
    }

    @Test
    public void testReapprovisionnement() {
        // Commencer avec un stock bas
        saumon.setStock(3);
        assertTrue(saumon.estStockBas(), "Le stock initial doit être bas");

        inventaireManager.ajusterStock(saumon, 7);
        assertFalse(saumon.estStockBas(), "Ne doit plus être en alerte après réapprovisionnement");
        assertEquals(10, saumon.getStock(), "Le stock doit être revenu à 10");
    }

    @Test
    public void testAjusterStockProduitNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            inventaireManager.ajusterStock(null, 1);
        }, "Doit lever une exception pour un produit null");
    }

    @Test
    public void testGetProduitsBas() {
        // Configurer les produits avec des stocks bas
        saumon.setStock(4); // En dessous du seuil (5)
        Produit thon = new Produit(2, "Thon", "Poisson", 15.0, 20.99, 3, 5); // En dessous du seuil (5)
        List<Produit> produits = Arrays.asList(saumon, null, thon);
        List<Produit> produitsBas = inventaireManager.getProduitsBas(produits);

        assertEquals(2, produitsBas.size(), "Doit retourner le bon nombre de produits en stock bas");
        assertTrue(produitsBas.contains(saumon), "Le saumon doit être dans la liste des produits en stock bas");
        assertTrue(produitsBas.contains(thon), "Le thon doit être dans la liste des produits en stock bas");
    }

    @Test
    public void testGetProduitsEnRupture() {
        assertTrue(inventaireManager.getProduitsEnRupture(null).isEmpty(),
            "La liste doit être vide pour une entrée null");

        Produit thonRupture = new Produit(2, "Thon", "Poisson", 15.0, 20.99, 0, 5);
        List<Produit> produits = Arrays.asList(saumon, null, thonRupture);
        List<Produit> produitsRupture = inventaireManager.getProduitsEnRupture(produits);

        assertEquals(1, produitsRupture.size(),
            "Doit retourner le bon nombre de produits en rupture");
        assertTrue(produitsRupture.contains(thonRupture),
            "Le thon doit être dans la liste des produits en rupture");
    }
}