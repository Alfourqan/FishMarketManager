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

    // Statistiques des stocks
    public static Map<String, Object> analyserStocks(List<Produit> produits) {
        Map<String, Object> stats = new HashMap<>();
        
        // Statistiques globales
        stats.put("totalProduits", produits.size());
        stats.put("valeurTotaleStock", produits.stream()
            .mapToDouble(p -> p.getPrix() * p.getQuantite())
            .sum());
        
        // Analyse par statut
        Map<String, Long> statutsCount = produits.stream()
            .collect(Collectors.groupingBy(
                p -> getStatutStock(p),
                Collectors.counting()
            ));
        stats.put("statutsCount", statutsCount);
        
        // Produits critiques
        List<Produit> produitsCritiques = produits.stream()
            .filter(p -> p.getQuantite() <= p.getSeuilAlerte())
            .collect(Collectors.toList());
        stats.put("produitsCritiques", produitsCritiques);
        
        return stats;
    }

    // Filtrer les stocks
    public static List<Produit> filtrerStocks(List<Produit> produits, 
            String statutFiltre, Double prixMin, Double prixMax) {
        return produits.stream()
            .filter(p -> statutFiltre == null || getStatutStock(p).equals(statutFiltre))
            .filter(p -> prixMin == null || p.getPrix() >= prixMin)
            .filter(p -> prixMax == null || p.getPrix() <= prixMax)
            .collect(Collectors.toList());
    }

    // Statistiques des ventes
    public static Map<String, Object> analyserVentes(List<Vente> ventes, LocalDate dateDebut, LocalDate dateFin) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Vente> ventesFiltrees = ventes.stream()
            .filter(v -> !v.getDate().toLocalDate().isBefore(dateDebut))
            .filter(v -> !v.getDate().toLocalDate().isAfter(dateFin))
            .collect(Collectors.toList());
        
        // Chiffre d'affaires total
        double caTotal = ventesFiltrees.stream()
            .mapToDouble(Vente::getTotal)
            .sum();
        stats.put("chiffreAffaires", caTotal);
        
        // Nombre de ventes par mode de paiement
        Map<ModePaiement, Long> ventesParMode = ventesFiltrees.stream()
            .collect(Collectors.groupingBy(
                Vente::getModePaiement,
                Collectors.counting()
            ));
        stats.put("ventesParMode", ventesParMode);
        
        // Top produits vendus
        Map<Produit, Long> produitsVendus = ventesFiltrees.stream()
            .flatMap(v -> v.getLignes().stream())
            .collect(Collectors.groupingBy(
                Vente.LigneVente::getProduit,
                Collectors.summingLong(Vente.LigneVente::getQuantite)
            ));
        stats.put("topProduits", produitsVendus);
        
        return stats;
    }

    // Statistiques des créances
    public static Map<String, Object> analyserCreances(List<Client> clients) {
        Map<String, Object> stats = new HashMap<>();
        
        // Total des créances
        double totalCreances = clients.stream()
            .mapToDouble(Client::getTotalCreances)
            .sum();
        stats.put("totalCreances", totalCreances);
        
        // Répartition par statut
        Map<StatutCreances, Long> creancesParStatut = clients.stream()
            .collect(Collectors.groupingBy(
                Client::getStatutCreances,
                Collectors.counting()
            ));
        stats.put("creancesParStatut", creancesParStatut);
        
        // Clients avec retard de paiement
        List<Client> clientsRetard = clients.stream()
            .filter(c -> c.getStatutCreances() == StatutCreances.EN_RETARD)
            .collect(Collectors.toList());
        stats.put("clientsRetard", clientsRetard);
        
        return stats;
    }

    // Analyse financière
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
        
        // Chiffre d'affaires
        double ca = ventesPeriode.stream()
            .mapToDouble(Vente::getTotal)
            .sum();
        stats.put("chiffreAffaires", ca);
        
        // Total des dépenses
        double depenses = mouvementsPeriode.stream()
            .filter(m -> m.getType() == TypeMouvement.SORTIE)
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
        stats.put("depenses", depenses);
        
        // Bénéfice net
        stats.put("beneficeNet", ca - depenses);
        
        // Analyse par jour
        Map<LocalDate, Double> caParJour = ventesPeriode.stream()
            .collect(Collectors.groupingBy(
                v -> v.getDate().toLocalDate(),
                Collectors.summingDouble(Vente::getTotal)
            ));
        stats.put("chiffreAffairesParJour", caParJour);
        
        return stats;
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
