package com.poissonnerie.test;

import com.poissonnerie.controller.VenteController;
import com.poissonnerie.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class TestVente {
    private VenteController controller;
    private Vente venteTest;
    private Client clientTest;
    private Produit produitTest;

    @BeforeEach
    void setUp() {
        controller = new VenteController();
        clientTest = new Client(1, "Client Test", "0123456789", "123 rue Test", 0.0);
        produitTest = new Produit(1, "Produit Test", "Catégorie Test", 10.0, 15.0, 100, 10);
        
        Vente.LigneVente ligneVente = new Vente.LigneVente(produitTest, 2, 15.0);
        venteTest = new Vente(
            1,
            LocalDateTime.now(),
            clientTest,
            false,
            30.0
        );
        venteTest.setLignes(Arrays.asList(ligneVente));
    }

    @Test
    @DisplayName("Test validation d'une vente valide")
    void testValidationVenteValide() {
        assertDoesNotThrow(() -> controller.enregistrerVente(venteTest),
            "Une vente valide devrait être acceptée");
    }

    @Test
    @DisplayName("Test validation d'une vente null")
    void testValidationVenteNull() {
        assertThrows(IllegalArgumentException.class,
            () -> controller.enregistrerVente(null),
            "Une vente null devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation d'une vente sans lignes")
    void testValidationVenteSansLignes() {
        venteTest.setLignes(new ArrayList<>());
        assertThrows(IllegalArgumentException.class,
            () -> controller.enregistrerVente(venteTest),
            "Une vente sans lignes devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation d'une vente à crédit sans client")
    void testValidationVenteCreditSansClient() {
        venteTest.setCredit(true);
        venteTest.setClient(null);
        assertThrows(IllegalArgumentException.class,
            () -> controller.enregistrerVente(venteTest),
            "Une vente à crédit sans client devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation des quantités négatives")
    void testValidationQuantitesNegatives() {
        Vente.LigneVente ligneInvalide = new Vente.LigneVente(produitTest, -1, 15.0);
        venteTest.setLignes(Arrays.asList(ligneInvalide));
        assertThrows(IllegalArgumentException.class,
            () -> controller.enregistrerVente(venteTest),
            "Une quantité négative devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation des prix unitaires négatifs")
    void testValidationPrixUnitairesNegatifs() {
        Vente.LigneVente ligneInvalide = new Vente.LigneVente(produitTest, 1, -15.0);
        venteTest.setLignes(Arrays.asList(ligneInvalide));
        assertThrows(IllegalArgumentException.class,
            () -> controller.enregistrerVente(venteTest),
            "Un prix unitaire négatif devrait lever une exception");
    }

    @Test
    @DisplayName("Test de la copie défensive de la liste des ventes")
    void testCopieDefensiveListeVentes() {
        controller.enregistrerVente(venteTest);
        List<Vente> ventes = controller.getVentes();
        int tailleDepartVentes = ventes.size();
        
        ventes.clear(); // Modification de la copie
        assertEquals(tailleDepartVentes, controller.getVentes().size(),
            "La modification de la copie ne devrait pas affecter la liste originale");
    }
}
