package com.poissonnerie.test;

import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class TestClient {
    private ClientController controller;
    private Client clientTest;

    @BeforeEach
    void setUp() {
        controller = new ClientController();
        clientTest = new Client(
            1,
            "Client Test",
            "0123456789",
            "123 rue Test",
            0.0
        );
    }

    @Test
    @DisplayName("Test validation du client avec données valides")
    void testValidationClientValide() {
        assertDoesNotThrow(() -> controller.ajouterClient(clientTest),
            "Le client valide devrait être accepté");
    }

    @Test
    @DisplayName("Test validation du client null")
    void testValidationClientNull() {
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterClient(null),
            "Un client null devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation du nom vide")
    void testValidationNomVide() {
        clientTest.setNom("");
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterClient(clientTest),
            "Un nom vide devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation du téléphone invalide")
    void testValidationTelephoneInvalide() {
        clientTest.setTelephone("telephone@invalide");
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterClient(clientTest),
            "Un téléphone invalide devrait lever une exception");
    }

    @Test
    @DisplayName("Test validation des longueurs maximales")
    void testValidationLongueurMaximale() {
        String longString = "a".repeat(101);
        
        clientTest.setNom(longString);
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterClient(clientTest),
            "Un nom trop long devrait lever une exception");

        clientTest.setNom("Nom valide");
        clientTest.setAdresse("a".repeat(256));
        assertThrows(IllegalArgumentException.class,
            () -> controller.ajouterClient(clientTest),
            "Une adresse trop longue devrait lever une exception");
    }

    @Test
    @DisplayName("Test de l'assainissement des entrées")
    void testSanitizationEntrees() {
        clientTest.setNom("Test<script>alert('xss')</script>");
        clientTest.setAdresse("Test'; DROP TABLE clients;--");
        
        assertDoesNotThrow(() -> controller.ajouterClient(clientTest),
            "Les caractères dangereux devraient être échappés");
    }

    @Test
    @DisplayName("Test règlement créance avec montant invalide")
    void testReglementCreanceMontantInvalide() {
        clientTest.setSolde(100.0);
        
        assertThrows(IllegalArgumentException.class,
            () -> controller.reglerCreance(clientTest, -50.0),
            "Un montant négatif devrait lever une exception");

        assertThrows(IllegalArgumentException.class,
            () -> controller.reglerCreance(clientTest, 150.0),
            "Un montant supérieur au solde devrait lever une exception");
    }

    @Test
    @DisplayName("Test règlement créance avec client null")
    void testReglementCreanceClientNull() {
        assertThrows(IllegalArgumentException.class,
            () -> controller.reglerCreance(null, 50.0),
            "Un client null devrait lever une exception");
    }
}
