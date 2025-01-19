package com.poissonnerie;

import com.poissonnerie.model.Produit;
import com.poissonnerie.controller.ProduitController;
import org.junit.jupiter.api.*;
import java.sql.SQLException;

public class TestProduit {
    
    private ProduitController produitController;
    private static final String NOM_TEST = "Poisson Test";
    private static final String CATEGORIE_TEST = "Poisson Frais";
    private static final double PRIX_ACHAT_TEST = 10.0;
    private static final double PRIX_VENTE_TEST = 15.0;
    private static final int STOCK_TEST = 50;
    private static final int SEUIL_ALERTE_TEST = 10;
    
    @BeforeEach
    public void setUp() {
        produitController = new ProduitController();
    }
    
    @Test
    public void testCreationProduit() {
        Produit produit = new Produit(1, NOM_TEST, CATEGORIE_TEST, PRIX_ACHAT_TEST, 
                                    PRIX_VENTE_TEST, STOCK_TEST, SEUIL_ALERTE_TEST);
        
        Assertions.assertEquals(NOM_TEST, produit.getNom(), "Le nom du produit devrait correspondre");
        Assertions.assertEquals(CATEGORIE_TEST, produit.getCategorie(), "La catégorie devrait correspondre");
        Assertions.assertEquals(PRIX_ACHAT_TEST, produit.getPrixAchat(), "Le prix d'achat devrait correspondre");
        Assertions.assertEquals(PRIX_VENTE_TEST, produit.getPrixVente(), "Le prix de vente devrait correspondre");
        Assertions.assertEquals(STOCK_TEST, produit.getStock(), "Le stock devrait correspondre");
        Assertions.assertEquals(SEUIL_ALERTE_TEST, produit.getSeuilAlerte(), "Le seuil d'alerte devrait correspondre");
    }
    
    @Test
    public void testAjoutProduit() {
        Produit produit = new Produit(0, NOM_TEST, CATEGORIE_TEST, PRIX_ACHAT_TEST, 
                                    PRIX_VENTE_TEST, STOCK_TEST, SEUIL_ALERTE_TEST);
        
        Assertions.assertDoesNotThrow(() -> {
            produitController.ajouterProduit(produit);
        }, "L'ajout d'un produit devrait réussir");
        
        Assertions.assertTrue(produit.getId() > 0, "L'ID du produit devrait être positif après l'ajout");
    }
    
    @Test
    public void testMiseAJourProduit() {
        // Créer et ajouter un produit
        Produit produit = new Produit(0, NOM_TEST, CATEGORIE_TEST, PRIX_ACHAT_TEST, 
                                    PRIX_VENTE_TEST, STOCK_TEST, SEUIL_ALERTE_TEST);
        produitController.ajouterProduit(produit);
        
        // Modifier le produit
        String nouveauNom = "Nouveau Nom";
        produit.setNom(nouveauNom);
        
        Assertions.assertDoesNotThrow(() -> {
            produitController.mettreAJourProduit(produit);
        }, "La mise à jour d'un produit devrait réussir");
        
        // Recharger les produits et vérifier la mise à jour
        produitController.chargerProduits();
        boolean produitTrouve = produitController.getProduits().stream()
                                               .anyMatch(p -> p.getId() == produit.getId() && 
                                                            p.getNom().equals(nouveauNom));
        
        Assertions.assertTrue(produitTrouve, "Le produit devrait être mis à jour avec le nouveau nom");
    }
    
    @Test
    public void testSuppressionProduit() {
        // Créer et ajouter un produit
        Produit produit = new Produit(0, "Produit à supprimer", CATEGORIE_TEST, 
                                    PRIX_ACHAT_TEST, PRIX_VENTE_TEST, STOCK_TEST, SEUIL_ALERTE_TEST);
        produitController.ajouterProduit(produit);
        
        Assertions.assertDoesNotThrow(() -> {
            produitController.supprimerProduit(produit);
        }, "La suppression d'un produit devrait réussir");
        
        // Vérifier que le produit n'existe plus
        produitController.chargerProduits();
        boolean produitExiste = produitController.getProduits().stream()
                                               .anyMatch(p -> p.getId() == produit.getId());
        
        Assertions.assertFalse(produitExiste, "Le produit ne devrait plus exister après suppression");
    }
}
