package com.poissonnerie.controller;

import com.poissonnerie.model.*;
import com.poissonnerie.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ReportController {
    private static final Logger LOGGER = Logger.getLogger(ReportController.class.getName());
    private final VenteController venteController;
    private final ProduitController produitController;
    private final ClientController clientController;
    private final CaisseController caisseController;

    public ReportController() {
        this.venteController = new VenteController();
        this.produitController = new ProduitController();
        this.clientController = new ClientController();
        this.caisseController = new CaisseController();
    }

    public void genererRapportStocksExcel(String cheminFichier) {
        try {
            List<Produit> produits = produitController.getTousProduits();
            Map<String, Double> statistiques = calculerStatistiquesStocks(produits);
            ExcelGenerator.genererRapportStocks(produits, statistiques, cheminFichier);
            LOGGER.info("Rapport des stocks généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des stocks", e);
        }
    }

    private Map<String, Double> calculerStatistiquesStocks(List<Produit> produits) {
        Map<String, Double> stats = new HashMap<>();
        
        // Valeur totale du stock
        double valeurTotale = produits.stream()
            .mapToDouble(p -> p.getPrix() * p.getQuantite())
            .sum();
        stats.put("Valeur totale du stock", valeurTotale);

        // Nombre de produits en rupture
        long produitsEnRupture = produits.stream()
            .filter(p -> p.getQuantite() <= p.getSeuilAlerte())
            .count();
        stats.put("Produits en alerte stock", (double) produitsEnRupture);

        // Moyenne des quantités
        double moyenneQuantites = produits.stream()
            .mapToDouble(Produit::getQuantite)
            .average()
            .orElse(0.0);
        stats.put("Moyenne des quantités", moyenneQuantites);

        return stats;
    }

    public void genererRapportVentesExcel(LocalDateTime debut, LocalDateTime fin, String cheminFichier) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());
            Map<String, Double> analyses = analyserVentes(ventes);
            ExcelGenerator.genererRapportVentes(ventes, analyses, cheminFichier);
            LOGGER.info("Rapport des ventes généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des ventes", e);
        }
    }

    private Map<String, Double> analyserVentes(List<Vente> ventes) {
        Map<String, Double> analyses = new HashMap<>();
        
        // Chiffre d'affaires total
        double caTotal = ventes.stream().mapToDouble(Vente::getTotal).sum();
        analyses.put("Chiffre d'affaires total", caTotal);

        // Moyenne des ventes
        double moyenneVentes = ventes.stream()
            .mapToDouble(Vente::getTotal)
            .average()
            .orElse(0.0);
        analyses.put("Moyenne des ventes", moyenneVentes);

        // Répartition par mode de paiement
        Map<Vente.ModePaiement, Double> parModePaiement = ventes.stream()
            .collect(Collectors.groupingBy(
                Vente::getModePaiement,
                Collectors.summingDouble(Vente::getTotal)
            ));
        
        parModePaiement.forEach((mode, total) -> 
            analyses.put("Total " + mode.toString(), total));

        return analyses;
    }

    public void genererRapportFinancierExcel(LocalDateTime debut, LocalDateTime fin, String cheminFichier) {
        try {
            Map<String, Double> chiffreAffaires = calculerChiffreAffaires(debut, fin);
            Map<String, Double> couts = calculerCouts(debut, fin);
            Map<String, Double> benefices = calculerBenefices(chiffreAffaires, couts);
            Map<String, Double> marges = calculerMarges(chiffreAffaires, couts);
            
            ExcelGenerator.genererRapportFinancier(
                chiffreAffaires, couts, benefices, marges, cheminFichier);
            LOGGER.info("Rapport financier généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier", e);
            throw new RuntimeException("Erreur lors de la génération du rapport financier", e);
        }
    }

    private Map<String, Double> calculerChiffreAffaires(LocalDateTime debut, LocalDateTime fin) {
        List<Vente> ventes = venteController.getVentes().stream()
            .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
            .collect(Collectors.toList());
        Map<String, Double> caParPeriode = new HashMap<>();

        // Calcul par mois
        Map<String, Double> caParMois = ventes.stream()
            .collect(Collectors.groupingBy(
                v -> v.getDate().format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")),
                Collectors.summingDouble(Vente::getTotal)
            ));

        caParPeriode.putAll(caParMois);

        // Total sur la période
        double total = caParMois.values().stream().mapToDouble(Double::doubleValue).sum();
        caParPeriode.put("Total période", total);

        return caParPeriode;
    }

    private Map<String, Double> calculerCouts(LocalDateTime debut, LocalDateTime fin) {
        Map<String, Double> couts = new HashMap<>();
        
        // Achats (mouvements de caisse sortants)
        double totalAchats = caisseController.getMouvements().stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.SORTIE)
            .filter(m -> m.getDate().isAfter(debut) && m.getDate().isBefore(fin))
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
        couts.put("Total achats", totalAchats);
        
        // Autres coûts peuvent être ajoutés ici
        
        return couts;
    }

    private Map<String, Double> calculerBenefices(
            Map<String, Double> chiffreAffaires,
            Map<String, Double> couts) {
        Map<String, Double> benefices = new HashMap<>();
        
        double totalCA = chiffreAffaires.get("Total période");
        double totalCouts = couts.get("Total achats");
        double beneficeNet = totalCA - totalCouts;
        
        benefices.put("Chiffre d'affaires", totalCA);
        benefices.put("Coûts totaux", totalCouts);
        benefices.put("Bénéfice net", beneficeNet);
        
        return benefices;
    }

    private Map<String, Double> calculerMarges(
            Map<String, Double> chiffreAffaires,
            Map<String, Double> couts) {
        Map<String, Double> marges = new HashMap<>();
        
        double totalCA = chiffreAffaires.get("Total période");
        double totalCouts = couts.get("Total achats");
        
        if (totalCA > 0) {
            double margeBrute = ((totalCA - totalCouts) / totalCA) * 100;
            marges.put("Marge brute (%)", margeBrute);
        }
        
        return marges;
    }

    // Méthodes de génération de rapports PDF
    public void genererRapportVentesPDF(LocalDateTime debut, LocalDateTime fin, String cheminFichier) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());
            Map<String, Double> analyses = analyserVentes(ventes);
            PDFGenerator.genererRapportVentes(ventes, analyses, cheminFichier);
            LOGGER.info("Rapport des ventes PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des ventes", e);
        }
    }

    public void genererRapportStocksPDF(String cheminFichier) {
        try {
            List<Produit> produits = produitController.getProduits();
            Map<String, Double> statistiques = calculerStatistiquesStocks(produits);
            PDFGenerator.genererRapportStocks(produits, statistiques, cheminFichier);
            LOGGER.info("Rapport des stocks PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des stocks", e);
        }
    }

    public void genererRapportFournisseursPDF(String cheminFichier) {
        try {
            List<Fournisseur> fournisseurs = getFournisseursAvecStats();
            PDFGenerator.genererRapportFournisseurs(fournisseurs, cheminFichier);
            LOGGER.info("Rapport des fournisseurs PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des fournisseurs", e);
        }
    }

    public void genererRapportCreancesPDF(String cheminFichier) {
        try {
            List<Client> clients = clientController.getClientsAvecCreances();
            PDFGenerator.genererRapportCreances(clients, cheminFichier);
            LOGGER.info("Rapport des créances PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des créances", e);
        }
    }

    public void genererRapportFinancierPDF(LocalDateTime debut, LocalDateTime fin, String cheminFichier) {
        try {
            Map<String, Double> chiffreAffaires = calculerChiffreAffaires(debut, fin);
            Map<String, Double> couts = calculerCouts(debut, fin);
            Map<String, Double> benefices = calculerBenefices(chiffreAffaires, couts);
            Map<String, Double> marges = calculerMarges(chiffreAffaires, couts);

            PDFGenerator.genererRapportFinancier(chiffreAffaires, couts, benefices, marges, cheminFichier);
            LOGGER.info("Rapport financier PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier PDF", e);
            throw new RuntimeException("Erreur lors de la génération du rapport financier PDF", e);
        }
    }

    // Méthodes de génération de rapports Excel
    public void genererRapportVentesExcel(LocalDateTime debut, LocalDateTime fin, String cheminFichier) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());
            Map<String, Double> analyses = analyserVentes(ventes);
            ExcelGenerator.genererRapportVentes(ventes, analyses, cheminFichier);
            LOGGER.info("Rapport des ventes Excel généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel des ventes", e);
        }
    }

    public void genererRapportStocksExcel(String cheminFichier) {
        try {
            List<Produit> produits = produitController.getProduits();
            Map<String, Double> statistiques = calculerStatistiquesStocks(produits);
            ExcelGenerator.genererRapportStocks(produits, statistiques, cheminFichier);
            LOGGER.info("Rapport des stocks Excel généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel des stocks", e);
        }
    }

    // Méthodes d'analyse pour les graphiques
    public Map<String, Double> analyserVentesParPeriode(LocalDateTime debut, LocalDateTime fin) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());

            return ventes.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getDate().format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")),
                    Collectors.summingDouble(Vente::getTotal)
                ));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse des ventes par période", e);
            return new HashMap<>();
        }
    }

    public Map<String, Integer> analyserStocksParCategorie() {
        try {
            List<Produit> produits = produitController.getProduits();
            return produits.stream()
                .collect(Collectors.groupingBy(
                    Produit::getCategorie,
                    Collectors.summingInt(Produit::getQuantite)
                ));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse des stocks par catégorie", e);
            return new HashMap<>();
        }
    }

    public Map<String, Double> analyserModePaiement(LocalDateTime debut, LocalDateTime fin) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());

            return ventes.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getModePaiement().toString(),
                    Collectors.summingDouble(Vente::getTotal)
                ));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse des modes de paiement", e);
            return new HashMap<>();
        }
    }

    public Map<String, Double> analyserAchatsFournisseurs(LocalDateTime debut, LocalDateTime fin) {
        try {
            // TODO: Implémenter la logique pour récupérer les achats par fournisseur
            // Pour l'instant, on retourne des données fictives
            Map<String, Double> donnees = new HashMap<>();
            donnees.put("Fournisseur 1", 1500.0);
            donnees.put("Fournisseur 2", 2300.0);
            donnees.put("Fournisseur 3", 1800.0);
            return donnees;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse des achats par fournisseur", e);
            return new HashMap<>();
        }
    }

    // Méthodes utilitaires
    private List<Fournisseur> getFournisseursAvecStats() {
        // TODO: Implémenter la récupération des statistiques des fournisseurs
        return new ArrayList<>();
    }

}