package com.poissonnerie.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Vente {
    private int id;
    private final LocalDateTime date;
    private final Client client;
    private final boolean credit;
    private double total;
    private List<LigneVente> lignes;

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
    }

    // Classe interne pour les lignes de vente
    public static class LigneVente {
        private final Produit produit;
        private final int quantite;
        private final double prixUnitaire;

        public LigneVente(Produit produit, int quantite, double prixUnitaire) {
            if (produit == null) {
                throw new IllegalArgumentException("Le produit ne peut pas être null");
            }
            if (quantite <= 0) {
                throw new IllegalArgumentException("La quantité doit être supérieure à 0");
            }
            if (prixUnitaire <= 0) {
                throw new IllegalArgumentException("Le prix unitaire doit être supérieur à 0");
            }

            this.produit = produit;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
        }

        // Getters - Pas de setters pour garantir l'immutabilité
        public Produit getProduit() { return produit; }
        public int getQuantite() { return quantite; }
        public double getPrixUnitaire() { return prixUnitaire; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LigneVente that = (LigneVente) o;
            return quantite == that.quantite &&
                   Double.compare(that.prixUnitaire, prixUnitaire) == 0 &&
                   Objects.equals(produit, that.produit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(produit, quantite, prixUnitaire);
        }
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
        this.total = total;
    }

    public List<LigneVente> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    public void setLignes(List<LigneVente> lignes) {
        if (lignes == null) {
            throw new IllegalArgumentException("La liste des lignes ne peut pas être null");
        }
        if (lignes.isEmpty()) {
            throw new IllegalArgumentException("Une vente doit avoir au moins une ligne");
        }
        // Vérifier chaque ligne
        for (LigneVente ligne : lignes) {
            if (ligne == null) {
                throw new IllegalArgumentException("Les lignes de vente ne peuvent pas être null");
            }
        }

        this.lignes = new ArrayList<>(lignes);
        validateTotal();
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

    // Valider que le total correspond à la somme des lignes
    private void validateTotal() {
        double calculatedTotal = getMontantTotal();
        if (Math.abs(calculatedTotal - total) > 0.01) { // Tolérance pour les erreurs d'arrondi
            throw new IllegalStateException(
                String.format("Le total de la vente (%.2f) ne correspond pas à la somme des lignes (%.2f)",
                    total, calculatedTotal)
            );
        }
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
               Objects.equals(lignes, vente.lignes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, client, credit, total, lignes);
    }
}