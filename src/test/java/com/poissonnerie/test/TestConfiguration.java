package com.poissonnerie.test;

import com.poissonnerie.controller.ConfigurationController;
import com.poissonnerie.model.ConfigurationParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

public class TestConfiguration {
    private ConfigurationController controller;

    @BeforeEach
    void setUp() {
        // Désactiver la validation SIRET pendant l'initialisation
        System.setProperty("SKIP_SIRET_VALIDATION", "true");
        controller = new ConfigurationController();
        controller.reinitialiserConfigurations();
    }

    @AfterEach
    void tearDown() {
        // Réactiver la validation SIRET après chaque test
        System.clearProperty("SKIP_SIRET_VALIDATION");
    }

    @Test
    @DisplayName("Test validation des entrées avec caractères spéciaux")
    void testValidationEntreesSpeciales() {
        ConfigurationParam config = new ConfigurationParam(
            1,
            ConfigurationParam.CLE_TAUX_TVA,
            "<script>alert('xss')</script>20.0",
            "Description test"
        );

        assertDoesNotThrow(() -> controller.mettreAJourConfiguration(config),
            "Les caractères spéciaux devraient être échappés");

        String valeur = controller.getValeur(ConfigurationParam.CLE_TAUX_TVA);
        assertFalse(valeur.contains("<script>"),
            "Les balises script devraient être échappées");
    }

    @Test
    @DisplayName("Test validation des clés de configuration")
    void testValidationCles() {
        assertThrows(IllegalArgumentException.class,
            () -> new ConfigurationParam(1, "clé_invalide", "20.0", "Test"),
            "Les clés en minuscules devraient être rejetées");

        assertThrows(IllegalArgumentException.class,
            () -> new ConfigurationParam(1, "CLÉ-INVALIDE", "20.0", "Test"),
            "Les caractères spéciaux dans les clés devraient être rejetés");
    }

    @Test
    @DisplayName("Test des configurations sensibles")
    void testConfigurationsSensibles() {
        ConfigurationParam configSiret = new ConfigurationParam(
            1,
            ConfigurationParam.CLE_SIRET_ENTREPRISE,
            "12345678901234",
            "SIRET test"
        );

        assertDoesNotThrow(() -> controller.mettreAJourConfiguration(configSiret),
            "Un SIRET valide devrait être accepté");

        ConfigurationParam configEmail = new ConfigurationParam(
            2,
            ConfigurationParam.CLE_EMAIL,
            "test@example.com",
            "Email test"
        );

        assertDoesNotThrow(() -> controller.mettreAJourConfiguration(configEmail),
            "Un email valide devrait être accepté");

        ConfigurationParam configEmailInvalide = new ConfigurationParam(
            3,
            ConfigurationParam.CLE_EMAIL,
            "email_invalide",
            "Email test invalide"
        );

        assertThrows(IllegalArgumentException.class,
            () -> controller.mettreAJourConfiguration(configEmailInvalide),
            "Un email invalide devrait être rejeté");
    }

    @Test
    @DisplayName("Test des limites de valeurs")
    void testLimitesValeurs() {
        String longValue = "a".repeat(1001);
        assertThrows(IllegalArgumentException.class,
            () -> new ConfigurationParam(1, ConfigurationParam.CLE_TAUX_TVA, longValue, "Test"),
            "Une valeur trop longue devrait être rejetée");

        assertThrows(IllegalArgumentException.class,
            () -> new ConfigurationParam(1, ConfigurationParam.CLE_TAUX_TVA, "101.0", "Test"),
            "Un taux TVA > 100 devrait être rejeté");
    }

    @Test
    @DisplayName("Test de réinitialisation des configurations")
    void testReinitialisationConfigurations() {
        controller.reinitialiserConfigurations();

        assertEquals("20.0", controller.getValeur(ConfigurationParam.CLE_TAUX_TVA),
            "Le taux TVA devrait être réinitialisé à 20.0");

        assertEquals("true", controller.getValeur(ConfigurationParam.CLE_TVA_ENABLED),
            "TVA_ENABLED devrait être réinitialisé à true");

        assertEquals("COMPACT", controller.getValeur(ConfigurationParam.CLE_FORMAT_RECU),
            "FORMAT_RECU devrait être réinitialisé à COMPACT");
    }

    @Test
    @DisplayName("Test de sécurité des entrées SQL")
    void testSecuriteSQL() {
        assertThrows(IllegalArgumentException.class,
            () -> controller.mettreAJourConfiguration(new ConfigurationParam(
                1,
                ConfigurationParam.CLE_TAUX_TVA,
                "20.0; DROP TABLE configurations;--",
                "Test injection SQL"
            )),
            "Les tentatives d'injection SQL devraient être rejetées");
    }
}