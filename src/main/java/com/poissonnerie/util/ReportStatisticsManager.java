package com.poissonnerie.util;

import com.poissonnerie.model.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ReportStatisticsManager {
    private static final Logger LOGGER = Logger.getLogger(ReportStatisticsManager.class.getName());
    private static final double SEUIL_ALERTE_STOCK = 10.0;
    private static final double SEUIL_ALERTE_CA = 1000.0;

    public static Map<String, Object> analyserStocks(List<Produit> produits, String categorie) {
        Map<String, Object> stats = new HashMap<>();
        List<Produit> produitsFiltered = produits;

        if (categorie != null && !categorie.isEmpty()) {
            produitsFiltered = produits.stream()
                .filter(p -> p.getCategorie().equals(categorie))
                .collect(Collectors.toList());
        }

        // Statistiques globales
        stats.put("totalProduits", produitsFiltered.size());
        stats.put("valeurTotaleStock", produitsFiltered.stream()
            .mapToDouble(p -> p.getPrixVente() * p.getQuantite())
            .sum());

        // Analyse par statut
        Map<String, List<Produit>> produitsParStatut = produitsFiltered.stream()
            .collect(Collectors.groupingBy(p -> getStatutStock(p)));
        stats.put("produitsParStatut", produitsParStatut);

        // Produits sous alerte
        List<Produit> produitsSousAlerte = produitsFiltered.stream()
            .filter(p -> p.getQuantite() <= p.getSeuilAlerte())
            .collect(Collectors.toList());
        stats.put("produitsSousAlerte", produitsSousAlerte);

        return stats;
    }

    public static Map<String, Object> analyserVentes(List<Vente> ventes, LocalDate debut, 
            LocalDate fin, ModePaiement modePaiement) {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Vente> ventesFiltrees = ventes.stream()
                .filter(v -> !v.getDate().toLocalDate().isBefore(debut))
                .filter(v -> !v.getDate().toLocalDate().isAfter(fin))
                .filter(v -> modePaiement == null || v.getModePaiement() == modePaiement)
                .collect(Collectors.toList());

            // Statistiques de base
            double chiffreAffaires = ventesFiltrees.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum();
            stats.put("chiffreAffaires", chiffreAffaires);
            stats.put("nombreVentes", ventesFiltrees.size());
            stats.put("panierMoyen", ventesFiltrees.isEmpty() ? 0.0 : 
                chiffreAffaires / ventesFiltrees.size());

            // Statistiques par mode de paiement
            Map<ModePaiement, DoubleSummaryStatistics> statsParMode = ventesFiltrees.stream()
                .collect(Collectors.groupingBy(
                    Vente::getModePaiement,
                    Collectors.summarizingDouble(Vente::getMontantTotal)
                ));
            stats.put("statsParMode", statsParMode);

            // Répartition des ventes
            Map<String, Double> repartition = calculerRepartitionVentes(ventesFiltrees);
            stats.put("repartitionVentes", repartition);

            // Analyse des tendances
            Map<LocalDate, Double> ventesParJour = ventesFiltrees.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getDate().toLocalDate(),
                    Collectors.summingDouble(Vente::getMontantTotal)
                ));
            stats.put("ventesParJour", ventesParJour);

            // Génération des alertes
            List<String> alertes = genererAlertes(ventesFiltrees, chiffreAffaires);
            stats.put("alertes", alertes);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse des ventes", e);
            stats.put("erreur", e.getMessage());
        }

        return stats;
    }

    private static Map<String, Double> calculerRepartitionVentes(List<Vente> ventes) {
        if (ventes.isEmpty()) {
            return new HashMap<>();
        }

        double totalVentes = ventes.stream()
            .mapToDouble(Vente::getMontantTotal)
            .sum();

        Map<String, Double> repartition = new HashMap<>();

        // Par mode de paiement
        Map<ModePaiement, Double> parMode = ventes.stream()
            .collect(Collectors.groupingBy(
                Vente::getModePaiement,
                Collectors.summingDouble(Vente::getMontantTotal)
            ));

        parMode.forEach((mode, montant) -> {
            repartition.put(mode.toString(), (montant / totalVentes) * 100);
        });

        return repartition;
    }

    private static List<String> genererAlertes(List<Vente> ventes, double chiffreAffaires) {
        List<String> alertes = new ArrayList<>();

        // Alerte sur le chiffre d'affaires
        if (chiffreAffaires < SEUIL_ALERTE_CA) {
            alertes.add(String.format("Attention: Chiffre d'affaires bas (%.2f €)", chiffreAffaires));
        }

        // Analyse des produits en alerte de stock
        Set<Produit> produitsVendus = ventes.stream()
            .flatMap(v -> v.getLignes().stream())
            .map(Vente.LigneVente::getProduit)
            .collect(Collectors.toSet());

        produitsVendus.stream()
            .filter(p -> p.getQuantite() <= p.getSeuilAlerte())
            .forEach(p -> alertes.add(String.format(
                "Stock faible pour %s: %d unités restantes (seuil: %d)",
                p.getNom(), p.getQuantite(), p.getSeuilAlerte()
            )));

        return alertes;
    }

    public static Map<String, Object> analyserCreances(List<Client> clients) {
        Map<String, Object> stats = new HashMap<>();
        try {
            double totalCreances = clients.stream()
                .mapToDouble(Client::getSolde)
                .sum();
            stats.put("totalCreances", totalCreances);

            Map<String, DoubleSummaryStatistics> statsParStatut = clients.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getStatutCreances().toString(),
                    Collectors.summarizingDouble(Client::getSolde)
                ));
            stats.put("statsParStatut", statsParStatut);

            List<Client> clientsRetard = clients.stream()
                .filter(c -> c.getStatutCreances() == StatutCreances.EN_RETARD)
                .sorted(Comparator.comparing(Client::getSolde).reversed())
                .collect(Collectors.toList());
            stats.put("clientsRetard", clientsRetard);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse des créances", e);
            stats.put("erreur", e.getMessage());
        }

        return stats;
    }

    public static Map<String, Object> analyserFinances(List<Vente> ventes, List<MouvementCaisse> mouvements,
            LocalDate debut, LocalDate fin) {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Vente> ventesPeriode = ventes.stream()
                .filter(v -> !v.getDate().toLocalDate().isBefore(debut))
                .filter(v -> !v.getDate().toLocalDate().isAfter(fin))
                .collect(Collectors.toList());

            List<MouvementCaisse> mouvementsPeriode = mouvements.stream()
                .filter(m -> !m.getDate().toLocalDate().isBefore(debut))
                .filter(m -> !m.getDate().toLocalDate().isAfter(fin))
                .collect(Collectors.toList());

            Map<LocalDate, Double> caParJour = ventesPeriode.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getDate().toLocalDate(),
                    Collectors.summingDouble(Vente::getMontantTotal)
                ));
            stats.put("chiffreAffairesParJour", caParJour);

            double caTotal = caParJour.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
            stats.put("chiffreAffairesTotal", caTotal);

            Map<LocalDate, Double> depensesParJour = mouvementsPeriode.stream()
                .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.SORTIE)
                .collect(Collectors.groupingBy(
                    m -> m.getDate().toLocalDate(),
                    Collectors.summingDouble(MouvementCaisse::getMontant)
                ));
            stats.put("depensesParJour", depensesParJour);

            double depensesTotal = depensesParJour.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
            stats.put("depensesTotal", depensesTotal);

            stats.put("beneficeNet", caTotal - depensesTotal);

            if (caTotal > 0) {
                stats.put("tauxMargeBrute", ((caTotal - depensesTotal) / caTotal) * 100);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse financière", e);
            stats.put("erreur", e.getMessage());
        }

        return stats;
    }

    public static List<Produit> filtrerStocks(List<Produit> produits, 
            String statutFiltre, Double prixMin, Double prixMax) {
        return produits.stream()
            .filter(p -> statutFiltre == null || getStatutStock(p).equals(statutFiltre))
            .filter(p -> prixMin == null || p.getPrixVente() >= prixMin)
            .filter(p -> prixMax == null || p.getPrixVente() <= prixMax)
            .collect(Collectors.toList());
    }

    private static String getStatutStock(Produit produit) {
        if (produit.getQuantite() <= 0) {
            return "RUPTURE";
        } else if (produit.getQuantite() <= produit.getSeuilAlerte()) {
            return "ALERTE";
        } else if (produit.getQuantite() >= produit.getSeuilAlerte() * 3) {
            return "SURSTOCK";
        } else {
            return "NORMAL";
        }
    }
}