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

/**
 * Tests unitaires pour la gestion de l'inventaire
 */
public class TestInventaire {
    private InventaireManager inventaireManager;
    private Produit saumon;
    private boolean stockBasAppele;
    private boolean ruptureStockAppelee;
    private boolean stockAjusteAppele;

    @BeforeEach
    public void setUp() {
        inventaireManager = new InventaireManager();
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
        inventaireManager.ajusterStock(saumon, -6);
        assertTrue(stockBasAppele, "L'alerte de stock bas doit être déclenchée");
        assertFalse(ruptureStockAppelee, "L'alerte de rupture ne doit pas être déclenchée");
    }

    @Test
    public void testRuptureStock() {
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
        inventaireManager.ajusterStock(saumon, -8);
        assertTrue(stockBasAppele, "Doit déclencher l'alerte de stock bas");

        stockBasAppele = false; // Reset du flag
        inventaireManager.ajusterStock(saumon, 8);
        assertFalse(stockBasAppele, "Ne doit plus être en alerte après réapprovisionnement");
        assertEquals(10, saumon.getStock(), "Le stock doit être revenu à 10");
    }

    // Tests de validation des entrées
    @Test
    public void testAjusterStockProduitNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            inventaireManager.ajusterStock(null, 1);
        }, "Doit lever une exception pour un produit null");
    }

    @Test
    public void testAjouterObserverNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            inventaireManager.ajouterObserver(null);
        }, "Doit lever une exception pour un observer null");
    }

    @Test
    public void testRetirerObserverNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            inventaireManager.retirerObserver(null);
        }, "Doit lever une exception pour un observer null");
    }

    @Test
    public void testGetProduitsBas() {
        assertTrue(inventaireManager.getProduitsBas(null).isEmpty(),
            "La liste doit être vide pour une entrée null");

        List<Produit> produits = Arrays.asList(saumon, null, 
            new Produit(2, "Thon", "Poisson", 15.0, 20.99, 3, 5));
        assertEquals(2, inventaireManager.getProduitsBas(produits).size(),
            "Doit retourner le bon nombre de produits en stock bas");
    }

    @Test
    public void testGetProduitsEnRupture() {
        assertTrue(inventaireManager.getProduitsEnRupture(null).isEmpty(),
            "La liste doit être vide pour une entrée null");

        Produit thonRupture = new Produit(2, "Thon", "Poisson", 15.0, 20.99, 0, 5);
        List<Produit> produits = Arrays.asList(saumon, null, thonRupture);
        assertEquals(1, inventaireManager.getProduitsEnRupture(produits).size(),
            "Doit retourner le bon nombre de produits en rupture");
    }
}