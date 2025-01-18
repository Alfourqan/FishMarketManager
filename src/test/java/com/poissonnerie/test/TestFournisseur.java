package com.poissonnerie.test;

import com.poissonnerie.controller.FournisseurController;
import com.poissonnerie.model.Fournisseur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class TestFournisseur {
    private FournisseurController controller;
    private Fournisseur fournisseurTest;

    @BeforeEach
    void setUp() {
        controller = new FournisseurController();
        fournisseurTest = new Fournisseur(
            1,
            "Fournisseur Test",
            "Contact Test",
            "0123456789",
            "test@example.com",
            "123 rue Test"
        );
    }

    @Test
    @DisplayName("Test validation du fournisseur avec données valides")
    void testValidationFournisseurValide() {
        assertDoesNotThrow(() -> controller.ajouterFournisseur(fournisseurTest),
            "Le fournisseur valide devrait être accepté");
    }

    @Test
    @DisplayName("Test validation du fournisseur null")
    void testValidationFournisseurNull() {
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterFournisseur(null),
            "Un fournisseur null devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation du nom vide")
    void testValidationNomVide() {
        fournisseurTest.setNom("");
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterFournisseur(fournisseurTest),
            "Un nom vide devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation de l'email invalide")
    void testValidationEmailInvalide() {
        fournisseurTest.setEmail("email_invalide");
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterFournisseur(fournisseurTest),
            "Un email invalide devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation du téléphone invalide")
    void testValidationTelephoneInvalide() {
        fournisseurTest.setTelephone("telephone@invalide");
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterFournisseur(fournisseurTest),
            "Un téléphone invalide devrait lever une exception");
    }

    @Test
    @DisplayName("Test recherche avec terme null")
    void testRechercheTermeNull() {
        assertTrue(controller.rechercherFournisseurs(null).isEmpty(),
            "La recherche avec un terme null devrait retourner une liste vide");
    }

    @Test
    @DisplayName("Test recherche avec terme vide")
    void testRechercheTermeVide() {
        assertTrue(controller.rechercherFournisseurs("  ").isEmpty(),
            "La recherche avec un terme vide devrait retourner une liste vide");
    }

    @Test
    @DisplayName("Test suppression fournisseur invalide")
    void testSuppressionFournisseurInvalide() {
        Fournisseur fournisseurInvalide = new Fournisseur(
            -1,
            "Test",
            "Test",
            "0123456789",
            "test@example.com",
            "Test"
        );
        assertThrows(IllegalArgumentException.class,
            () -> controller.supprimerFournisseur(fournisseurInvalide),
            "La suppression d'un fournisseur avec ID invalide devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation des longueurs maximales")
    void testValidationLongueurMaximale() {
        // Création d'une chaîne de 101 caractères
        String longString = "a".repeat(101);
        
        fournisseurTest.setNom(longString);
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterFournisseur(fournisseurTest),
            "Un nom trop long devrait lever une exception");

        // Réinitialisation du nom et test de l'email
        fournisseurTest.setNom("Nom valide");
        fournisseurTest.setEmail(longString + "@example.com");
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterFournisseur(fournisseurTest),
            "Un email trop long devrait lever une exception");
    }

    @Test
    @DisplayName("Test de l'assainissement des entrées")
    void testSanitizationEntrees() {
        fournisseurTest.setNom("Test<script>alert('xss')</script>");
        fournisseurTest.setEmail("test@example.com'; DROP TABLE users;--");
        
        assertDoesNotThrow(() -> controller.ajouterFournisseur(fournisseurTest),
            "Les caractères dangereux devraient être échappés");
    }
}
