package com.poissonnerie.test;

import com.poissonnerie.controller.VenteController;
import com.poissonnerie.model.*;
import com.poissonnerie.util.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.sql.Connection;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

public class TestVente {
    private VenteController controller;
    private Vente venteTest;
    private Client clientTest;
    private Produit produitTest;

    @BeforeEach
    void setUp() {
        // Initialiser la base de données de test
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Activer les contraintes de clé étrangère
            stmt.execute("PRAGMA foreign_keys = ON");

            // Supprimer les tables existantes dans l'ordre inverse des dépendances
            stmt.execute("DROP TABLE IF EXISTS lignes_vente");
            stmt.execute("DROP TABLE IF EXISTS ventes");
            stmt.execute("DROP TABLE IF EXISTS produits");
            stmt.execute("DROP TABLE IF EXISTS clients");

            // Créer les tables dans l'ordre des dépendances
            // 1. Clients
            stmt.execute("""
                CREATE TABLE clients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nom TEXT NOT NULL,
                    telephone TEXT,
                    adresse TEXT,
                    solde DOUBLE DEFAULT 0.0,
                    supprime BOOLEAN DEFAULT false
                )
            """);

            // 2. Produits
            stmt.execute("""
                CREATE TABLE produits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nom TEXT NOT NULL,
                    categorie TEXT,
                    prix_achat DOUBLE NOT NULL,
                    prix_vente DOUBLE NOT NULL,
                    stock INTEGER NOT NULL,
                    seuil_alerte INTEGER NOT NULL,
                    supprime BOOLEAN DEFAULT false
                )
            """);

            // 3. Ventes
            stmt.execute("""
                CREATE TABLE ventes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date BIGINT NOT NULL,
                    client_id INTEGER,
                    credit BOOLEAN NOT NULL,
                    total DOUBLE NOT NULL,
                    supprime BOOLEAN DEFAULT false,
                    FOREIGN KEY (client_id) REFERENCES clients(id)
                )
            """);

            // 4. Lignes de vente
            stmt.execute("""
                CREATE TABLE lignes_vente (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    vente_id INTEGER NOT NULL,
                    produit_id INTEGER NOT NULL,
                    quantite INTEGER NOT NULL,
                    prix_unitaire DOUBLE NOT NULL,
                    supprime BOOLEAN DEFAULT false,
                    FOREIGN KEY (vente_id) REFERENCES ventes(id),
                    FOREIGN KEY (produit_id) REFERENCES produits(id)
                )
            """);

            // Insérer les données de test
            stmt.execute("""
                INSERT INTO clients (id, nom, telephone, adresse, solde)
                VALUES (1, 'Client Test', '0123456789', '123 rue Test', 0.0)
            """);

            stmt.execute("""
                INSERT INTO produits (id, nom, categorie, prix_achat, prix_vente, stock, seuil_alerte)
                VALUES (1, 'Produit Test', 'Catégorie Test', 10.0, 15.0, 100, 10)
            """);

        } catch (Exception e) {
            fail("Échec de l'initialisation de la base de données de test: " + e.getMessage());
        }

        controller = new VenteController();
        clientTest = new Client(1, "Client Test", "0123456789", "123 rue Test", 0.0);
        produitTest = new Produit(1, "Produit Test", "Catégorie Test", 10.0, 15.0, 100, 10);

        // Créer une vente de test valide
        List<Vente.LigneVente> lignes = new ArrayList<>();
        lignes.add(new Vente.LigneVente(produitTest, 2, 15.0));

        venteTest = new Vente(1, LocalDateTime.now(), clientTest, false, 30.0);
        venteTest.setLignes(lignes);
    }

    @Test
    @DisplayName("Test validation d'une vente valide")
    void testValidationVenteValide() {
        assertDoesNotThrow(() -> {
            controller.enregistrerVente(venteTest);
        }, "Une vente valide devrait être acceptée");
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
        assertThrows(IllegalArgumentException.class,
            () -> {
                Vente venteInvalide = new Vente(1, LocalDateTime.now(), null, true, 30.0);
                venteInvalide.setLignes(Arrays.asList(new Vente.LigneVente(produitTest, 2, 15.0)));
                controller.enregistrerVente(venteInvalide);
            },
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
        // Ajouter une vente valide
        controller.enregistrerVente(venteTest);

        // Vérifier que la modification de la copie n'affecte pas l'original
        List<Vente> ventes = controller.getVentes();
        int tailleDepartVentes = ventes.size();
        ventes.clear();

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
}