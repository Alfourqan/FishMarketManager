package com.poissonnerie.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Vente {
    private static final Logger LOGGER = Logger.getLogger(Vente.class.getName());
    private int id;
    private final LocalDateTime date;
    private final Client client;
    private final boolean credit;
    private double total;
    private List<LigneVente> lignes;
    private ModePaiement modePaiement;
    private static final double TAUX_TVA_DEFAULT = 20.0; // 20% par défaut

    public Vente(int id, LocalDateTime date, Client client, boolean credit, double total) {
        if (date == null) {
            throw new IllegalArgumentException("La date de vente ne peut pas être null");
        }
        if (date.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date de vente ne peut pas être dans le futur");
        }
        if (credit && client == null) {
            throw new IllegalArgumentException("Une vente à crédit doit avoir un client associé");
        }
        if (total < 0) {
            throw new IllegalArgumentException("Le total de la vente ne peut pas être négatif");
        }

        this.id = id;
        this.date = date;
        this.client = client;
        this.credit = credit;
        this.total = total;
        this.lignes = new ArrayList<>();
        // Par défaut, si c'est une vente à crédit, mode crédit, sinon comptant
        this.modePaiement = credit ? ModePaiement.CREDIT : ModePaiement.ESPECES;
    }

    // Getters et setters avec validations
    public int getId() { return id; }
    public void setId(int id) { 
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID doit être positif");
        }
        this.id = id; 
    }

    public LocalDateTime getDate() { return date; }
    public Client getClient() { return client; }
    public boolean isCredit() { return credit; }

    public double getTotal() { return total; }
    public void setTotal(double total) {
        if (total < 0) {
            throw new IllegalArgumentException("Le total ne peut pas être négatif");
        }

        // Si nous avons des lignes, vérifier que le total correspond
        if (!lignes.isEmpty() && Math.abs(total - getMontantTotal()) > 0.01) {
            throw new IllegalStateException(
                String.format("Le total de la vente (%.2f) ne correspond pas à la somme des lignes (%.2f)",
                    total, getMontantTotal())
            );
        }

        LOGGER.log(Level.INFO, 
            String.format("Modification du total de la vente %d: %.2f → %.2f", 
                id, this.total, total));
        this.total = total;
    }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) {
        if (modePaiement == null) {
            throw new IllegalArgumentException("Le mode de paiement ne peut pas être null");
        }
        if (credit && modePaiement != ModePaiement.CREDIT) {
            throw new IllegalArgumentException("Une vente à crédit doit avoir le mode de paiement CREDIT");
        }
        this.modePaiement = modePaiement;
    }

    public List<LigneVente> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    public void setLignes(List<LigneVente> lignes) {
        if (lignes == null) {
            throw new IllegalArgumentException("La liste des lignes ne peut pas être null");
        }

        // Vérifier chaque ligne si la liste n'est pas vide
        if (!lignes.isEmpty()) {
            for (LigneVente ligne : lignes) {
                if (ligne == null) {
                    throw new IllegalArgumentException("Les lignes de vente ne peuvent pas être null");
                }
                validateLigne(ligne);
            }
        }

        LOGGER.log(Level.INFO, 
            String.format("Mise à jour des lignes de la vente %d: %d lignes", 
                id, lignes.size()));
        this.lignes = new ArrayList<>(lignes);
    }

    // Méthode pour calculer le montant total de la vente
    public double getMontantTotal() {
        if (lignes == null || lignes.isEmpty()) {
            return 0.0;
        }
        return lignes.stream()
            .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
            .sum();
    }

    private void validateLigne(LigneVente ligne) {
        if (ligne.getProduit() == null) {
            throw new IllegalArgumentException("Le produit ne peut pas être null");
        }
        if (ligne.getQuantite() <= 0) {
            throw new IllegalArgumentException(
                String.format("La quantité doit être positive pour l'article %s",
                    ligne.getProduit().getNom()));
        }
        if (ligne.getPrixUnitaire() <= 0) {
            throw new IllegalArgumentException(
                String.format("Le prix unitaire doit être positif pour l'article %s",
                    ligne.getProduit().getNom()));
        }
    }

    // Classe interne pour les lignes de vente
    public static class LigneVente {
        private final Produit produit;
        private int quantite;
        private double prixUnitaire;
        private final LocalDateTime dateModification;

        public LigneVente(Produit produit, int quantite, double prixUnitaire) {
            if (produit == null) {
                throw new IllegalArgumentException("Le produit ne peut pas être null");
            }
            validateQuantite(quantite);
            validatePrixUnitaire(prixUnitaire);

            this.produit = produit;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
            this.dateModification = LocalDateTime.now();
        }

        private void validateQuantite(int quantite) {
            if (quantite <= 0) {
                throw new IllegalArgumentException("La quantité doit être supérieure à 0");
            }
            if (produit != null && quantite > produit.getStock()) {
                throw new IllegalArgumentException(
                    String.format("La quantité demandée (%d) dépasse le stock disponible (%d) pour %s",
                        quantite, produit.getStock(), produit.getNom())
                );
            }
        }

        private void validatePrixUnitaire(double prixUnitaire) {
            if (prixUnitaire <= 0) {
                throw new IllegalArgumentException("Le prix unitaire doit être supérieur à 0");
            }
        }

        // Getters et setters avec validation
        public Produit getProduit() { return produit; }

        public int getQuantite() { return quantite; }
        public void setQuantite(int quantite) {
            validateQuantite(quantite);
            LOGGER.log(Level.INFO, 
                String.format("Modification de la quantité pour %s: %d → %d", 
                    produit.getNom(), this.quantite, quantite));
            this.quantite = quantite;
        }

        public double getPrixUnitaire() { return prixUnitaire; }
        public void setPrixUnitaire(double prixUnitaire) {
            validatePrixUnitaire(prixUnitaire);
            LOGGER.log(Level.INFO, 
                String.format("Modification du prix unitaire pour %s: %.2f → %.2f", 
                    produit.getNom(), this.prixUnitaire, prixUnitaire));
            this.prixUnitaire = prixUnitaire;
        }

        public LocalDateTime getDateModification() { return dateModification; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LigneVente that = (LigneVente) o;
            return quantite == that.quantite &&
                   Double.compare(that.prixUnitaire, prixUnitaire) == 0 &&
                   Objects.equals(produit, that.produit) &&
                   Objects.equals(dateModification, that.dateModification);
        }

        @Override
        public int hashCode() {
            return Objects.hash(produit, quantite, prixUnitaire, dateModification);
        }
    }

    // Nouvelles méthodes pour PDFGenerator
    public List<Produit> getProduits() {
        List<Produit> produits = new ArrayList<>();
        for (LigneVente ligne : getLignes()) {
            produits.add(ligne.getProduit());
        }
        return produits;
    }

    public String getStatut() {
        if (credit && client != null) {
            return client.getStatutCreances().toString();
        }
        return "COMPTANT";
    }

    public double getTotalHT() {
        double totalHT = 0.0;
        for (LigneVente ligne : getLignes()) {
            totalHT += ligne.getQuantite() * ligne.getPrixUnitaire() / (1 + (getTauxTVA() / 100));
        }
        return Math.round(totalHT * 100.0) / 100.0;
    }

    public double getTauxTVA() {
        // On pourrait récupérer le taux de TVA depuis la configuration
        return TAUX_TVA_DEFAULT;
    }

    public double getMontantTVA() {
        double totalHT = getTotalHT();
        return Math.round((total - totalHT) * 100.0) / 100.0;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vente vente = (Vente) o;
        return id == vente.id &&
               credit == vente.credit &&
               Double.compare(vente.total, total) == 0 &&
               Objects.equals(date, vente.date) &&
               Objects.equals(client, vente.client) &&
               Objects.equals(lignes, vente.lignes) &&
               modePaiement == vente.modePaiement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, client, credit, total, lignes, modePaiement);
    }
}