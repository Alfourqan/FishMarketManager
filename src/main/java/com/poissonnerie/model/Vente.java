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

    public enum ModePaiement {
        ESPECES("Espèces"),
        CARTE("Carte bancaire"),
        CREDIT("Crédit");

        private final String libelle;

        ModePaiement(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }

        public static ModePaiement fromString(String text) {
            for (ModePaiement mode : ModePaiement.values()) {
                if (mode.libelle.equalsIgnoreCase(text)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Mode de paiement invalide: " + text);
        }
    }

    private int id;
    private LocalDateTime date;
    private final Client client;
    private final boolean credit;
    private double total;
    private List<LigneVente> lignes;
    private final ModePaiement modePaiement;
    private static final double TAUX_TVA_DEFAULT = 20.0;
    private double montantRecu;
    private double montantRendu;

    public Vente(int id, LocalDateTime date, Client client, boolean credit, double total, ModePaiement modePaiement) {
        validateConstructorParams(date, client, credit, total, modePaiement);

        this.id = id;
        this.date = date;
        this.client = client;
        this.credit = credit;
        this.total = total;
        this.modePaiement = modePaiement;
        this.lignes = new ArrayList<>();
    }

    private void validateConstructorParams(LocalDateTime date, Client client, boolean credit, double total, ModePaiement modePaiement) {
        if (date == null || date.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Date de vente invalide");
        }
        if (credit && client == null) {
            throw new IllegalArgumentException("Une vente à crédit doit avoir un client associé");
        }
        if (total < 0 || modePaiement == null) {
            throw new IllegalArgumentException("Paramètres de vente invalides");
        }
        if (credit && modePaiement != ModePaiement.CREDIT) {
            throw new IllegalArgumentException("Une vente à crédit doit avoir le mode de paiement CREDIT");
        }
    }

    // Getters essentiels
    public int getId() { return id; }
    public void setId(int id) {
        if (id <= 0) throw new IllegalArgumentException("L'ID doit être positif");
        this.id = id;
    }
    public LocalDateTime getDate() { return date; }
    public Client getClient() { return client; }
    public boolean isCredit() { return credit; }
    public ModePaiement getModePaiement() { return modePaiement; }
    public double getTotal() { return total; }
    public List<LigneVente> getLignes() { return Collections.unmodifiableList(lignes); }

    public void setTotal(double total) {
        if (total < 0) throw new IllegalArgumentException("Le total ne peut pas être négatif");
        if (!lignes.isEmpty() && Math.abs(total - getMontantTotal()) > 0.01) {
            throw new IllegalStateException("Le total ne correspond pas à la somme des lignes");
        }
        this.total = total;
        LOGGER.log(Level.INFO, "Total de la vente {0} mis à jour: {1}", new Object[]{id, total});
    }

    public void setLignes(List<LigneVente> lignes) {
        if (lignes == null) throw new IllegalArgumentException("Liste des lignes invalide");
        lignes.forEach(this::validateLigne);
        this.lignes = new ArrayList<>(lignes);
        LOGGER.log(Level.INFO, "Lignes de la vente {0} mises à jour: {1} lignes", new Object[]{id, lignes.size()});
    }

    public static class LigneVente {
        private final Produit produit;
        private int quantite;
        private double prixUnitaire;
        private final LocalDateTime dateModification;

        public LigneVente(Produit produit, int quantite, double prixUnitaire) {
            if (produit == null) throw new IllegalArgumentException("Produit invalide");
            validateQuantite(quantite);
            validatePrixUnitaire(prixUnitaire);

            this.produit = produit;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
            this.dateModification = LocalDateTime.now();
        }

        private void validateQuantite(int quantite) {
            if (quantite <= 0) throw new IllegalArgumentException("Quantité invalide");
            if (produit != null && quantite > produit.getStock()) {
                throw new IllegalArgumentException("Stock insuffisant");
            }
        }

        private void validatePrixUnitaire(double prixUnitaire) {
            if (prixUnitaire <= 0) throw new IllegalArgumentException("Prix unitaire invalide");
        }

        // Getters et setters essentiels
        public Produit getProduit() { return produit; }
        public int getQuantite() { return quantite; }
        public double getPrixUnitaire() { return prixUnitaire; }
        public LocalDateTime getDateModification() { return dateModification; }

        public void setQuantite(int quantite) {
            validateQuantite(quantite);
            this.quantite = quantite;
            LOGGER.log(Level.INFO, "Quantité pour {0} mise à jour: {1}", new Object[]{produit.getNom(), quantite});
        }

        public void setPrixUnitaire(double prixUnitaire) {
            validatePrixUnitaire(prixUnitaire);
            this.prixUnitaire = prixUnitaire;
            LOGGER.log(Level.INFO, "Prix unitaire pour {0} mis à jour: {1}", new Object[]{produit.getNom(), prixUnitaire});
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LigneVente)) return false;
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

    public double getMontantTotal() {
        return lignes.stream()
            .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
            .sum();
    }

    private void validateLigne(LigneVente ligne) {
        if (ligne == null || ligne.getProduit() == null) 
            throw new IllegalArgumentException("Ligne de vente invalide");
        if (ligne.getQuantite() <= 0 || ligne.getPrixUnitaire() <= 0)
            throw new IllegalArgumentException("Quantité ou prix unitaire invalide");
    }

    public double getTotalHT() {
        return Math.round(getMontantTotal() / (1 + (TAUX_TVA_DEFAULT / 100)) * 100.0) / 100.0;
    }

    public double getMontantTVA() {
        return Math.round((total - getTotalHT()) * 100.0) / 100.0;
    }
    public double getMontantRecu() {
        return montantRecu;
    }

    public void setMontantRecu(double montantRecu) {
        if (montantRecu < 0) {
            throw new IllegalArgumentException("Le montant reçu ne peut pas être négatif");
        }
        if (montantRecu < this.total && this.modePaiement == ModePaiement.ESPECES) {
            throw new IllegalArgumentException("Le montant reçu doit être supérieur ou égal au total pour un paiement en espèces");
        }
        LOGGER.log(Level.INFO,
            String.format("Enregistrement du montant reçu pour la vente %d: %.2f €",
                id, montantRecu));
        this.montantRecu = montantRecu;
        this.montantRendu = montantRecu - this.total > 0 ? montantRecu - this.total : 0;
    }

    public double getMontantRendu() {
        return montantRendu;
    }

    // Add back getTauxTVA method
    public double getTauxTVA() {
        return TAUX_TVA_DEFAULT;
    }

    public void setDateVente(LocalDateTime dateVente) {
        if (dateVente == null || dateVente.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Date de vente invalide");
        }
        this.date = dateVente;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vente)) return false;
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