package com.poissonnerie;

import com.poissonnerie.model.Vente;
import com.poissonnerie.model.Produit;
import com.poissonnerie.model.Client;
import com.poissonnerie.controller.VenteController;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestVente {
    
    private VenteController venteController;
    private Produit produitTest;
    private Client clientTest;
    
    @BeforeEach
    public void setUp() {
        venteController = new VenteController();
        
        // Créer un produit de test
        produitTest = new Produit(1, "Poisson Test", "Poisson Frais", 10.0, 15.0, 100, 10);
        
        // Créer un client de test
        clientTest = new Client(1, "Client Test", "0123456789", "Adresse Test", 0.0);
    }
    
    @Test
    public void testCreationVente() {
        LocalDateTime dateVente = LocalDateTime.now();
        Vente vente = new Vente(1, dateVente, clientTest, false, 0.0);
        
        Assertions.assertEquals(dateVente, vente.getDate(), "La date de vente devrait correspondre");
        Assertions.assertEquals(clientTest, vente.getClient(), "Le client devrait correspondre");
        Assertions.assertFalse(vente.isCredit(), "La vente ne devrait pas être à crédit");
        Assertions.assertEquals(0.0, vente.getTotal(), "Le total initial devrait être 0");
    }
    
    @Test
    public void testAjoutLigneVente() {
        Vente vente = new Vente(1, LocalDateTime.now(), clientTest, false, 0.0);
        Vente.LigneVente ligne = new Vente.LigneVente(produitTest, 2, produitTest.getPrixVente());
        
        List<Vente.LigneVente> lignes = new ArrayList<>();
        lignes.add(ligne);
        vente.setLignes(lignes);
        
        Assertions.assertEquals(1, vente.getLignes().size(), "La vente devrait avoir une ligne");
        Assertions.assertEquals(2 * produitTest.getPrixVente(), vente.getTotal(), 
                              "Le total devrait correspondre à la quantité * prix");
    }
    
    @Test
    public void testEnregistrementVente() {
        // Créer une nouvelle vente
        Vente vente = new Vente(0, LocalDateTime.now(), clientTest, false, 0.0);
        Vente.LigneVente ligne = new Vente.LigneVente(produitTest, 2, produitTest.getPrixVente());
        
        List<Vente.LigneVente> lignes = new ArrayList<>();
        lignes.add(ligne);
        vente.setLignes(lignes);
        
        Assertions.assertDoesNotThrow(() -> {
            venteController.enregistrerVente(vente);
        }, "L'enregistrement de la vente devrait réussir");
        
        Assertions.assertTrue(vente.getId() > 0, "L'ID de la vente devrait être positif après l'enregistrement");
    }
    
    @Test
    public void testVenteACredit() {
        // Créer une vente à crédit
        Vente vente = new Vente(0, LocalDateTime.now(), clientTest, true, 0.0);
        Vente.LigneVente ligne = new Vente.LigneVente(produitTest, 1, produitTest.getPrixVente());
        
        List<Vente.LigneVente> lignes = new ArrayList<>();
        lignes.add(ligne);
        vente.setLignes(lignes);
        
        double soldeInitial = clientTest.getSolde();
        
        Assertions.assertDoesNotThrow(() -> {
            venteController.enregistrerVente(vente);
        }, "L'enregistrement de la vente à crédit devrait réussir");
        
        // Vérifier que le solde du client a été mis à jour
        Assertions.assertTrue(clientTest.getSolde() > soldeInitial, 
                            "Le solde du client devrait augmenter après une vente à crédit");
    }
    
    @Test
    public void testChargementVentes() {
        venteController.chargerVentes();
        List<Vente> ventes = venteController.getVentes();
        
        Assertions.assertNotNull(ventes, "La liste des ventes ne devrait pas être null");
        Assertions.assertFalse(ventes.isEmpty(), "Il devrait y avoir au moins une vente");
    }
}
