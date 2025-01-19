package com.poissonnerie;

import com.poissonnerie.model.Client;
import com.poissonnerie.controller.ClientController;
import org.junit.jupiter.api.*;
import java.sql.SQLException;

public class TestClient {
    
    private ClientController clientController;
    private static final String NOM_TEST = "Client Test";
    private static final String TELEPHONE_TEST = "0123456789";
    private static final String ADRESSE_TEST = "Adresse Test";
    private static final double SOLDE_INITIAL = 0.0;
    
    @BeforeEach
    public void setUp() {
        clientController = new ClientController();
    }
    
    @Test
    public void testCreationClient() {
        Client client = new Client(1, NOM_TEST, TELEPHONE_TEST, ADRESSE_TEST, SOLDE_INITIAL);
        
        Assertions.assertEquals(NOM_TEST, client.getNom(), "Le nom du client devrait correspondre");
        Assertions.assertEquals(TELEPHONE_TEST, client.getTelephone(), "Le téléphone devrait correspondre");
        Assertions.assertEquals(ADRESSE_TEST, client.getAdresse(), "L'adresse devrait correspondre");
        Assertions.assertEquals(SOLDE_INITIAL, client.getSolde(), "Le solde initial devrait être 0");
    }
    
    @Test
    public void testAjoutClient() {
        Client client = new Client(0, NOM_TEST, TELEPHONE_TEST, ADRESSE_TEST, SOLDE_INITIAL);
        
        Assertions.assertDoesNotThrow(() -> {
            clientController.ajouterClient(client);
        }, "L'ajout d'un client devrait réussir");
        
        Assertions.assertTrue(client.getId() > 0, "L'ID du client devrait être positif après l'ajout");
    }
    
    @Test
    public void testMiseAJourClient() {
        // Créer et ajouter un client
        Client client = new Client(0, NOM_TEST, TELEPHONE_TEST, ADRESSE_TEST, SOLDE_INITIAL);
        clientController.ajouterClient(client);
        
        // Modifier le client
        String nouveauTelephone = "9876543210";
        client.setTelephone(nouveauTelephone);
        
        Assertions.assertDoesNotThrow(() -> {
            clientController.mettreAJourClient(client);
        }, "La mise à jour d'un client devrait réussir");
        
        // Recharger les clients et vérifier la mise à jour
        clientController.chargerClients();
        boolean clientTrouve = clientController.getClients().stream()
                                             .anyMatch(c -> c.getId() == client.getId() && 
                                                          c.getTelephone().equals(nouveauTelephone));
        
        Assertions.assertTrue(clientTrouve, "Le client devrait être mis à jour avec le nouveau téléphone");
    }
    
    @Test
    public void testRechercheClient() {
        // Créer et ajouter un client unique
        String nomUnique = "Client Unique Test";
        Client client = new Client(0, nomUnique, TELEPHONE_TEST, ADRESSE_TEST, SOLDE_INITIAL);
        clientController.ajouterClient(client);
        
        // Rechercher le client
        Client clientTrouve = clientController.rechercherClientParNom(nomUnique);
        
        Assertions.assertNotNull(clientTrouve, "Le client devrait être trouvé");
        Assertions.assertEquals(nomUnique, clientTrouve.getNom(), "Le nom du client trouvé devrait correspondre");
    }
    
    @Test
    public void testValidationClient() {
        // Test avec un nom vide
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Client client = new Client(0, "", TELEPHONE_TEST, ADRESSE_TEST, SOLDE_INITIAL);
            clientController.ajouterClient(client);
        }, "L'ajout d'un client avec un nom vide devrait échouer");
        
        // Test avec un solde négatif
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Client client = new Client(0, NOM_TEST, TELEPHONE_TEST, ADRESSE_TEST, -100.0);
            clientController.ajouterClient(client);
        }, "L'ajout d'un client avec un solde négatif devrait échouer");
    }
    
    @Test
    public void testSuppression() {
        // Créer et ajouter un client
        Client client = new Client(0, "Client à supprimer", TELEPHONE_TEST, ADRESSE_TEST, SOLDE_INITIAL);
        clientController.ajouterClient(client);
        
        Assertions.assertDoesNotThrow(() -> {
            clientController.supprimerClient(client);
        }, "La suppression d'un client devrait réussir");
        
        // Vérifier que le client n'existe plus
        clientController.chargerClients();
        boolean clientExiste = clientController.getClients().stream()
                                             .anyMatch(c -> c.getId() == client.getId());
        
        Assertions.assertFalse(clientExiste, "Le client ne devrait plus exister après suppression");
    }
}
