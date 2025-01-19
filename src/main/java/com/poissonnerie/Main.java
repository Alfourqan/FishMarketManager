package com.poissonnerie;

import com.poissonnerie.model.*;
import com.poissonnerie.util.ReportBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            LOGGER.info("Démarrage du générateur de rapports...");

            // Création des données de test
            List<Produit> produits = creerDonneesTest();

            // Test de génération de rapport PDF
            ReportBuilder reportBuilder = new ReportBuilder();
            String cheminRapportPDF = "rapport_stocks_" + LocalDate.now() + ".pdf";
            reportBuilder.genererRapportStocks(cheminRapportPDF, produits, "Poissons", true);
            LOGGER.info("Rapport PDF généré avec succès: " + cheminRapportPDF);

            // Test de génération de rapport Excel
            String cheminRapportExcel = "rapport_stocks_" + LocalDate.now() + ".xlsx";
            reportBuilder.genererRapportStocks(cheminRapportExcel, produits, "Poissons", false);
            LOGGER.info("Rapport Excel généré avec succès: " + cheminRapportExcel);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération des rapports", e);
            e.printStackTrace();
        }
    }

    private static List<Produit> creerDonneesTest() {
        List<Produit> produits = new ArrayList<>();

        // Ajout de quelques produits de test
        Produit p1 = new Produit();
        p1.setId(1L);
        p1.setNom("Saumon frais");
        p1.setPrixVente(25.99);
        p1.setStock(15);
        p1.setCategorie("Poissons");
        produits.add(p1);

        Produit p2 = new Produit();
        p2.setId(2L);
        p2.setNom("Thon rouge");
        p2.setPrixVente(32.99);
        p2.setStock(8);
        p2.setCategorie("Poissons");
        produits.add(p2);

        Produit p3 = new Produit();
        p3.setId(3L);
        p3.setNom("Dorade royale");
        p3.setPrixVente(18.99);
        p3.setStock(20);
        p3.setCategorie("Poissons");
        produits.add(p3);

        return produits;
    }
}