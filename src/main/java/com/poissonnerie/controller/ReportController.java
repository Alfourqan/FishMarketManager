package com.poissonnerie.controller;

import com.poissonnerie.model.*;
import com.poissonnerie.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.ByteArrayOutputStream;

public class ReportController {
    private static final Logger LOGGER = Logger.getLogger(ReportController.class.getName());
    private final VenteController venteController;
    private final ProduitController produitController;
    private final ClientController clientController;
    private final CaisseController caisseController;
    private final FournisseurController fournisseurController;

    public ReportController() {
        this.venteController = new VenteController();
        this.produitController = new ProduitController();
        this.clientController = new ClientController();
        this.caisseController = new CaisseController();
        this.fournisseurController = new FournisseurController();
    }

    public void genererRapportStocksExcel(String cheminFichier) {
        try {
            List<Produit> produits = produitController.getProduits();
            Map<String, Double> statistiques = calculerStatistiquesStocks(produits);
            ExcelGenerator.genererRapportStocks(produits, statistiques, cheminFichier);
            LOGGER.info("Rapport des stocks généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des stocks", e);
        }
    }

    public void genererRapportVentesExcel(LocalDateTime debut, LocalDateTime fin, String cheminFichier) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());

            Map<String, Double> analyses = analyserVentesPourRapport(ventes);
            ExcelGenerator.genererRapportVentes(ventes, analyses, cheminFichier);

            LOGGER.info("Rapport des ventes Excel généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel des ventes", e);
        }
    }

    private Map<String, Double> analyserVentesPourRapport(List<Vente> ventes) {
        Map<String, Double> analyses = new HashMap<>();

        // Chiffre d'affaires total
        double caTotal = ventes.stream().mapToDouble(Vente::getTotal).sum();
        analyses.put("Chiffre d'affaires total", caTotal);

        // Moyenne des ventes
        OptionalDouble moyenneVentes = ventes.stream()
            .mapToDouble(Vente::getTotal)
            .average();
        analyses.put("Moyenne des ventes", moyenneVentes.orElse(0.0));

        // Nombre de ventes
        analyses.put("Nombre total de ventes", (double) ventes.size());

        // Panier moyen
        double panierMoyen = ventes.isEmpty() ? 0.0 : caTotal / ventes.size();
        analyses.put("Panier moyen", panierMoyen);

        // Top produits vendus par chiffre d'affaires
        Map<String, Double> ventesParProduit = ventes.stream()
            .flatMap(v -> v.getLignes().stream())
            .collect(Collectors.groupingBy(
                ligne -> ligne.getProduit().getNom(),
                Collectors.summingDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
            ));

        // Ajout des top produits individuellement dans les analyses
        ventesParProduit.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> analyses.put("Top produit - " + entry.getKey(), entry.getValue()));

        return analyses;
    }

    public void genererRapportFinancierExcel(LocalDateTime debut, LocalDateTime fin, String cheminFichier) {
        try {
            // Préparation des données financières
            Map<String, Double> chiffreAffaires = calculerChiffreAffaires(debut, fin);
            Map<String, Double> couts = calculerCouts(debut, fin);
            Map<String, Double> benefices = calculerBenefices(chiffreAffaires, couts);
            Map<String, Double> marges = calculerMarges(chiffreAffaires, couts);

            // Génération du rapport avec les 4 maps requises
            ExcelGenerator.genererRapportFinancier(
                chiffreAffaires,
                couts,
                benefices,
                marges,
                cheminFichier
            );

            LOGGER.info("Rapport financier généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier", e);
            throw new RuntimeException("Erreur lors de la génération du rapport financier", e);
        }
    }

    public Map<String, Double> calculerChiffreAffaires(LocalDateTime debut, LocalDateTime fin) {
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

        // Calcul des variations mensuelles
        List<String> mois = new ArrayList<>(caParMois.keySet());
        Collections.sort(mois);
        for (int i = 1; i < mois.size(); i++) {
            double moisPrecedent = caParMois.get(mois.get(i-1));
            double moisActuel = caParMois.get(mois.get(i));
            if (moisPrecedent > 0) {
                double variation = ((moisActuel - moisPrecedent) / moisPrecedent) * 100;
                caParPeriode.put("Variation " + mois.get(i), variation);
            }
        }

        // Ajout des données mensuelles
        caParPeriode.putAll(caParMois);

        // Total sur la période
        double total = caParMois.values().stream().mapToDouble(Double::doubleValue).sum();
        caParPeriode.put("Total période", total);

        // Moyenne mensuelle
        double moyenneMensuelle = caParMois.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        caParPeriode.put("Moyenne mensuelle", moyenneMensuelle);

        return caParPeriode;
    }

    private Map<String, Double> calculerCouts(LocalDateTime debut, LocalDateTime fin) {
        Map<String, Double> couts = new HashMap<>();

        // Calcul des coûts d'achats pour la période
        double totalAchats = caisseController.getMouvements().stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.SORTIE)
            .filter(m -> !m.getDate().isBefore(debut) && !m.getDate().isAfter(fin))
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
        couts.put("Total achats", totalAchats);

        return couts;
    }

    private Map<String, Double> calculerBenefices(Map<String, Double> chiffreAffaires, Map<String, Double> couts) {
        Map<String, Double> benefices = new HashMap<>();

        double totalCA = chiffreAffaires.getOrDefault("Total période", 0.0);
        double totalCouts = couts.getOrDefault("Total achats", 0.0);
        double beneficeNet = totalCA - totalCouts;

        benefices.put("Chiffre d'affaires", totalCA);
        benefices.put("Coûts totaux", totalCouts);
        benefices.put("Bénéfice net", beneficeNet);

        return benefices;
    }

    private Map<String, Double> calculerMarges(Map<String, Double> chiffreAffaires, Map<String, Double> couts) {
        Map<String, Double> marges = new HashMap<>();

        double totalCA = chiffreAffaires.getOrDefault("Total période", 0.0);
        double totalCouts = couts.getOrDefault("Total achats", 0.0);

        if (totalCA > 0) {
            double margeBrute = ((totalCA - totalCouts) / totalCA) * 100;
            marges.put("Marge brute (%)", margeBrute);
        }

        return marges;
    }

    public Map<String, Double> calculerStatistiquesStocks(List<Produit> produits) {
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

    public void genererRapportStocksPDF(List<Produit> produits, Map<String, Double> statistiques, ByteArrayOutputStream outputStream) {
        try {
            PDFGenerator.genererRapportStocks(produits, statistiques, outputStream);
            LOGGER.info("Rapport des stocks PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des stocks", e);
        }
    }

    public void genererRapportFournisseursPDF(ByteArrayOutputStream outputStream) {
        try {
            List<Fournisseur> fournisseurs = getFournisseursAvecStats();
            PDFGenerator.genererRapportFournisseurs(fournisseurs, outputStream);
            LOGGER.info("Rapport des fournisseurs PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des fournisseurs", e);
        }
    }

    public void genererRapportCreancesPDF(ByteArrayOutputStream outputStream) {
        try {
            List<Client> clients = clientController.getClients().stream()
                .filter(c -> c.getSolde() > 0)
                .sorted((c1, c2) -> Double.compare(c2.getSolde(), c1.getSolde()))
                .collect(Collectors.toList());

            if (clients.isEmpty()) {
                LOGGER.warning("Aucun client avec des créances n'a été trouvé");
                return;
            }

            PDFGenerator.genererRapportCreances(clients, outputStream);
            LOGGER.info("Rapport des créances PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des créances: " + e.getMessage(), e);
        }
    }

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

    public void genererRapportVentesPDF(LocalDateTime debut, LocalDateTime fin, ByteArrayOutputStream outputStream) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());
            PDFGenerator.genererRapportVentes(ventes, outputStream);
            LOGGER.info("Rapport des ventes PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF des ventes", e);
        }
    }

    public void genererRapportFinancierPDF(LocalDateTime debut, LocalDateTime fin, ByteArrayOutputStream outputStream) {
        try {
            Map<String, Double> chiffreAffaires = calculerChiffreAffaires(debut, fin);
            Map<String, Double> couts = calculerCouts(debut, fin);
            Map<String, Double> benefices = calculerBenefices(chiffreAffaires, couts);
            Map<String, Double> marges = calculerMarges(chiffreAffaires, couts);

            PDFGenerator.genererRapportFinancier(chiffreAffaires, couts, benefices, marges, outputStream);
            LOGGER.info("Rapport financier PDF généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier PDF", e);
            throw new RuntimeException("Erreur lors de la génération du rapport financier PDF", e);
        }
    }

    private List<Fournisseur> getFournisseursAvecStats() {
        return new ArrayList<>();
    }
    public Map<String, Double> analyserPerformanceStock() {
        try {
            List<Produit> produits = produitController.getProduits();
            Map<String, Double> analyses = new HashMap<>();

            // Taux de rotation du stock
            double tauxRotation = calculerTauxRotationStock(produits);
            analyses.put("Taux de rotation du stock", tauxRotation);

            // Produits en alerte
            long nbProduitsAlerte = produits.stream()
                .filter(p -> p.getQuantite() <= p.getSeuilAlerte())
                .count();
            analyses.put("Nombre de produits en alerte", (double) nbProduitsAlerte);

            // Valeur moyenne du stock
            double valeurMoyenne = produits.stream()
                .mapToDouble(p -> p.getPrixAchat() * p.getQuantite())
                .average()
                .orElse(0.0);
            analyses.put("Valeur moyenne du stock", valeurMoyenne);

            return analyses;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse de la performance du stock", e);
            return new HashMap<>();
        }
    }

    private double calculerTauxRotationStock(List<Produit> produits) {
        if (produits.isEmpty()) return 0.0;

        double valeurStockMoyen = produits.stream()
            .mapToDouble(p -> p.getPrixAchat() * p.getQuantite())
            .average()
            .orElse(0.0);

        double coutVentesPeriode = venteController.getVentes().stream()
            .flatMap(v -> v.getLignes().stream())
            .mapToDouble(ligne -> ligne.getProduit().getPrixAchat() * ligne.getQuantite())
            .sum();

        return valeurStockMoyen > 0 ? (coutVentesPeriode / valeurStockMoyen) * 365 : 0.0;
    }

    public Map<String, List<Double>> analyserEvolutionVentes(LocalDateTime debut, LocalDateTime fin) {
        try {
            List<Vente> ventes = venteController.getVentes().stream()
                .filter(v -> !v.getDate().isBefore(debut) && !v.getDate().isAfter(fin))
                .collect(Collectors.toList());

            Map<String, List<Double>> evolution = new HashMap<>();

            // Évolution du chiffre d'affaires
            List<Double> caJournalier = ventes.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getDate().toLocalDate(),
                    Collectors.summingDouble(Vente::getTotal)
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
            evolution.put("Chiffre d'affaires journalier", caJournalier);

            // Évolution du panier moyen
            List<Double> panierMoyen = ventes.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getDate().toLocalDate(),
                    Collectors.averagingDouble(Vente::getTotal)
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
            evolution.put("Panier moyen journalier", panierMoyen);

            return evolution;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse de l'évolution des ventes", e);
            return new HashMap<>();
        }
    }
    public Map<String, Double> analyserAchatsFournisseurs(LocalDateTime debut, LocalDateTime fin) {
        try {
            // Pour l'instant, retourne des données fictives
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

    public void genererRapportCreancesExcel(String cheminFichier) {
        try {
            List<Client> clients = clientController.getClients().stream()
                .filter(c -> c.getSolde() > 0)
                .sorted((c1, c2) -> Double.compare(c2.getSolde(), c1.getSolde()))
                .collect(Collectors.toList());

            if (clients.isEmpty()) {
                LOGGER.warning("Aucun client avec des créances n'a été trouvé");
                return;
            }

            ExcelGenerator.genererRapportCreances(clients, cheminFichier);
            LOGGER.info("Rapport des créances Excel généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel des créances", e);
        }
    }

    public void genererRapportFournisseursExcel(String cheminFichier) {
        try {
            List<Fournisseur> fournisseurs = fournisseurController.getFournisseurs();
            ExcelGenerator.genererRapportFournisseurs(fournisseurs, cheminFichier);
            LOGGER.info("Rapport des fournisseurs Excel généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel des fournisseurs", e);
        }
    }
}