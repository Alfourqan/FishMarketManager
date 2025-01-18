package com.poissonnerie.test;

import com.poissonnerie.controller.VenteController;
import com.poissonnerie.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        List<Vente.LigneVente> lignesVides = new ArrayList<>();
        assertThrows(IllegalArgumentException.class,
            () -> venteTest.setLignes(lignesVides),
            "Une vente sans lignes devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation d'une vente à crédit sans client")
    void testValidationVenteCreditSansClient() {
        assertThrows(IllegalArgumentException.class,
            () -> new Vente(1, LocalDateTime.now(), null, true, 30.0),
            "Une vente à crédit sans client devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation des quantités négatives")
    void testValidationQuantitesNegatives() {
        assertThrows(IllegalArgumentException.class,
            () -> new Vente.LigneVente(produitTest, -1, 15.0),
            "Une quantité négative devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation des prix unitaires négatifs")
    void testValidationPrixUnitairesNegatifs() {
        assertThrows(IllegalArgumentException.class,
            () -> new Vente.LigneVente(produitTest, 1, -15.0),
            "Un prix unitaire négatif devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation de la date dans le futur")
    void testValidationDateFutur() {
        LocalDateTime dateFuture = LocalDateTime.now().plusDays(1);
        assertThrows(IllegalArgumentException.class,
            () -> new Vente(1, dateFuture, clientTest, false, 30.0),
            "Une date dans le futur devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation de la date null")
    void testValidationDateNull() {
        assertThrows(IllegalArgumentException.class,
            () -> new Vente(1, null, clientTest, false, 30.0),
            "Une date null devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation du total négatif")
    void testValidationTotalNegatif() {
        assertThrows(IllegalArgumentException.class,
            () -> new Vente(1, LocalDateTime.now(), clientTest, false, -30.0),
            "Un total négatif devrait lever une exception");
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

    @Test
    @DisplayName("Test de la copie défensive de la liste des lignes")
    void testCopieDefensiveListeLignes() {
        List<Vente.LigneVente> lignes = venteTest.getLignes();
        assertThrows(UnsupportedOperationException.class,
            () -> lignes.clear(),
            "La liste des lignes ne devrait pas être modifiable");
    }

    @Test
    @DisplayName("Test de la validation du total calculé")
    void testValidationTotalCalcule() {
        Vente.LigneVente ligne = new Vente.LigneVente(produitTest, 2, 15.0);
        Vente vente = new Vente(1, LocalDateTime.now(), clientTest, false, 40.0); // Total incorrect
        assertThrows(IllegalStateException.class,
            () -> vente.setLignes(Arrays.asList(ligne)),
            "Un total incorrect devrait lever une exception");
    }

    @Test
    @DisplayName("Test de l'égalité des ventes")
    void testEgaliteVentes() {
        Vente.LigneVente ligne = new Vente.LigneVente(produitTest, 2, 15.0);
        Vente vente1 = new Vente(1, LocalDateTime.now(), clientTest, false, 30.0);
        vente1.setLignes(Arrays.asList(ligne));

        Vente vente2 = new Vente(1, vente1.getDate(), clientTest, false, 30.0);
        vente2.setLignes(Arrays.asList(ligne));

        assertEquals(vente1, vente2, "Deux ventes avec les mêmes attributs devraient être égales");
        assertEquals(vente1.hashCode(), vente2.hashCode(),
            "Les hashCodes de deux ventes égales devraient être identiques");
    }
}