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

    // Statistiques détaillées des stocks avec filtres avancés
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
            .mapToDouble(p -> p.getPrixVente() * p.getStock())
            .sum());

        // Analyse détaillée par statut
        Map<String, List<Produit>> produitsParStatut = produitsFiltered.stream()
            .collect(Collectors.groupingBy(p -> getStatutStock(p)));
        stats.put("produitsParStatut", produitsParStatut);

        // Statistiques par catégorie
        Map<String, Long> statsParCategorie = produitsFiltered.stream()
            .collect(Collectors.groupingBy(
                Produit::getCategorie,
                Collectors.counting()
            ));
        stats.put("statsParCategorie", statsParCategorie);

        // Analyse des seuils d'alerte
        List<Produit> produitsSousAlerte = produitsFiltered.stream()
            .filter(p -> p.getStock() <= p.getSeuilAlerte())
            .sorted(Comparator.comparing(Produit::getStock))
            .collect(Collectors.toList());
        stats.put("produitsSousAlerte", produitsSousAlerte);

        // Analyse des produits en surstock
        List<Produit> produitsEnSurstock = produitsFiltered.stream()
            .filter(p -> p.getStock() >= p.getSeuilAlerte() * 3)
            .sorted(Comparator.comparing(Produit::getStock).reversed())
            .collect(Collectors.toList());
        stats.put("produitsEnSurstock", produitsEnSurstock);

        return stats;
    }

    // Filtrer les stocks
    public static List<Produit> filtrerStocks(List<Produit> produits, 
            String statutFiltre, Double prixMin, Double prixMax) {
        return produits.stream()
            .filter(p -> statutFiltre == null || getStatutStock(p).equals(statutFiltre))
            .filter(p -> prixMin == null || p.getPrixVente() >= prixMin)
            .filter(p -> prixMax == null || p.getPrixVente() <= prixMax)
            .collect(Collectors.toList());
    }

    // Analyse détaillée des ventes avec période et filtres
    public static Map<String, Object> analyserVentes(List<Vente> ventes, LocalDate dateDebut, 
            LocalDate dateFin, ModePaiement modePaiement) {
        Map<String, Object> stats = new HashMap<>();

        // Filtrage des ventes
        List<Vente> ventesFiltrees = ventes.stream()
            .filter(v -> !v.getDate().toLocalDate().isBefore(dateDebut))
            .filter(v -> !v.getDate().toLocalDate().isAfter(dateFin))
            .filter(v -> modePaiement == null || v.getModePaiement() == modePaiement)
            .collect(Collectors.toList());

        // Analyse du chiffre d'affaires
        double caTotal = ventesFiltrees.stream()
            .mapToDouble(Vente::getTotal)
            .sum();
        stats.put("chiffreAffaires", caTotal);

        // Analyse par mode de paiement
        Map<ModePaiement, DoubleSummaryStatistics> statsParMode = ventesFiltrees.stream()
            .collect(Collectors.groupingBy(
                Vente::getModePaiement,
                Collectors.summarizingDouble(Vente::getTotal)
            ));
        stats.put("statsParMode", statsParMode);

        // Analyse des produits vendus
        Map<Produit, VenteStats> statsParProduit = ventesFiltrees.stream()
            .flatMap(v -> v.getLignes().stream())
            .collect(Collectors.groupingBy(
                Vente.LigneVente::getProduit,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    lignes -> new VenteStats(
                        lignes.stream().mapToLong(Vente.LigneVente::getQuantite).sum(),
                        lignes.stream().mapToDouble(l -> l.getPrixUnitaire() * l.getQuantite()).sum()
                    )
                )
            ));
        stats.put("statsParProduit", statsParProduit);

        // Analyse temporelle
        Map<LocalDate, Double> ventesParJour = ventesFiltrees.stream()
            .collect(Collectors.groupingBy(
                v -> v.getDate().toLocalDate(),
                Collectors.summingDouble(Vente::getTotal)
            ));
        stats.put("ventesParJour", ventesParJour);

        return stats;
    }

    // Analyse approfondie des créances
    public static Map<String, Object> analyserCreances(List<Client> clients) {
        Map<String, Object> stats = new HashMap<>();

        // Total des créances
        double totalCreances = clients.stream()
            .mapToDouble(Client::getSolde)
            .sum();
        stats.put("totalCreances", totalCreances);

        // Répartition par statut
        Map<String, List<Client>> clientsParStatut = clients.stream()
            .collect(Collectors.groupingBy(
                c -> String.valueOf(c.getStatutCreances()),
                Collectors.toList()
            ));
        stats.put("clientsParStatut", clientsParStatut);

        // Statistiques par statut
        Map<String, DoubleSummaryStatistics> statsParStatut = clients.stream()
            .collect(Collectors.groupingBy(
                c -> String.valueOf(c.getStatutCreances()),
                Collectors.summarizingDouble(Client::getSolde)
            ));
        stats.put("statsParStatut", statsParStatut);

        // Clients avec retard de paiement
        List<Client> clientsRetard = clients.stream()
            .filter(c -> String.valueOf(c.getStatutCreances()).equals("EN_RETARD"))
            .sorted(Comparator.comparing(Client::getSolde).reversed())
            .collect(Collectors.toList());
        stats.put("clientsRetard", clientsRetard);

        return stats;
    }

    // Analyse financière détaillée
    public static Map<String, Object> analyserFinances(List<Vente> ventes, List<MouvementCaisse> mouvements,
            LocalDate dateDebut, LocalDate dateFin) {
        Map<String, Object> stats = new HashMap<>();

        // Filtrage par période
        List<Vente> ventesPeriode = ventes.stream()
            .filter(v -> !v.getDate().toLocalDate().isBefore(dateDebut))
            .filter(v -> !v.getDate().toLocalDate().isAfter(dateFin))
            .collect(Collectors.toList());

        List<MouvementCaisse> mouvementsPeriode = mouvements.stream()
            .filter(m -> !m.getDate().toLocalDate().isBefore(dateDebut))
            .filter(m -> !m.getDate().toLocalDate().isAfter(dateFin))
            .collect(Collectors.toList());

        // Analyse du chiffre d'affaires
        Map<LocalDate, Double> caParJour = ventesPeriode.stream()
            .collect(Collectors.groupingBy(
                v -> v.getDate().toLocalDate(),
                Collectors.summingDouble(Vente::getTotal)
            ));
        stats.put("chiffreAffairesParJour", caParJour);

        double caTotal = caParJour.values().stream().mapToDouble(Double::doubleValue).sum();
        stats.put("chiffreAffairesTotal", caTotal);

        // Analyse des dépenses
        Map<LocalDate, Double> depensesParJour = mouvementsPeriode.stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.SORTIE)
            .collect(Collectors.groupingBy(
                m -> m.getDate().toLocalDate(),
                Collectors.summingDouble(MouvementCaisse::getMontant)
            ));
        stats.put("depensesParJour", depensesParJour);

        double depensesTotal = depensesParJour.values().stream()
            .mapToDouble(Double::doubleValue).sum();
        stats.put("depensesTotal", depensesTotal);

        // Calcul des bénéfices
        stats.put("beneficeNet", caTotal - depensesTotal);

        // Analyse des marges
        if (caTotal > 0) {
            stats.put("tauxMargeBrute", ((caTotal - depensesTotal) / caTotal) * 100);
        }

        return stats;
    }

    private static String getStatutStock(Produit produit) {
        if (produit.getStock() <= 0) {
            return "RUPTURE";
        } else if (produit.getStock() <= produit.getSeuilAlerte()) {
            return "ALERTE";
        } else if (produit.getStock() >= produit.getSeuilAlerte() * 3) {
            return "SURSTOCK";
        } else {
            return "NORMAL";
        }
    }

    // Classe utilitaire pour les statistiques de vente
    private static class VenteStats {
        private final long quantite;
        private final double montantTotal;

        public VenteStats(long quantite, double montantTotal) {
            this.quantite = quantite;
            this.montantTotal = montantTotal;
        }

        public long getQuantite() { return quantite; }
        public double getMontantTotal() { return montantTotal; }
    }
}