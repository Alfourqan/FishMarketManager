package com.poissonnerie;

import com.poissonnerie.controller.ConfigurationController;
import org.junit.jupiter.api.*;
import java.sql.SQLException;

public class TestConfiguration {
    
    private ConfigurationController configController;
    private static final String CLE_TEST = "TEST_CONFIG";
    private static final String VALEUR_TEST = "Valeur de test";
    private static final String DESCRIPTION_TEST = "Configuration de test";
    
    @BeforeEach
    public void setUp() {
        configController = new ConfigurationController();
    }
    
    @Test
    public void testChargementConfigurations() {
        Assertions.assertDoesNotThrow(() -> {
            configController.chargerConfigurations();
        }, "Le chargement des configurations devrait réussir");
    }
    
    @Test
    public void testAjoutConfiguration() {
        Assertions.assertDoesNotThrow(() -> {
            configController.ajouterConfiguration(CLE_TEST, VALEUR_TEST, DESCRIPTION_TEST);
        }, "L'ajout d'une configuration devrait réussir");
        
        String valeur = configController.getConfiguration(CLE_TEST);
        Assertions.assertEquals(VALEUR_TEST, valeur, 
                              "La valeur récupérée devrait correspondre à celle ajoutée");
    }
    
    @Test
    public void testMiseAJourConfiguration() {
        String nouvelleValeur = "Nouvelle valeur";
        
        Assertions.assertDoesNotThrow(() -> {
            // Ajouter d'abord la configuration
            configController.ajouterConfiguration(CLE_TEST, VALEUR_TEST, DESCRIPTION_TEST);
            
            // Mettre à jour la configuration
            configController.mettreAJourConfiguration(CLE_TEST, nouvelleValeur);
        }, "La mise à jour de la configuration devrait réussir");
        
        String valeur = configController.getConfiguration(CLE_TEST);
        Assertions.assertEquals(nouvelleValeur, valeur, 
                              "La valeur devrait être mise à jour");
    }
    
    @Test
    public void testConfigurationParDefaut() {
        String tauxTVA = configController.getConfiguration("TAUX_TVA");
        String tvaEnabled = configController.getConfiguration("TVA_ENABLED");
        
        Assertions.assertNotNull(tauxTVA, "Le taux de TVA ne devrait pas être null");
        Assertions.assertNotNull(tvaEnabled, "L'état de la TVA ne devrait pas être null");
    }
    
    @Test
    public void testConfigurationInexistante() {
        String valeur = configController.getConfiguration("CLE_INEXISTANTE");
        Assertions.assertNull(valeur, "La valeur d'une configuration inexistante devrait être null");
    }
    
    @Test
    public void testValidationCleConfiguration() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            configController.ajouterConfiguration("", VALEUR_TEST, DESCRIPTION_TEST);
        }, "L'ajout d'une configuration avec une clé vide devrait échouer");
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            configController.ajouterConfiguration(null, VALEUR_TEST, DESCRIPTION_TEST);
        }, "L'ajout d'une configuration avec une clé null devrait échouer");
    }
}
