package com.poissonnerie.model;

import java.time.LocalDateTime;
import java.util.List;

public class Vente {
    private int id;
    private LocalDateTime date;
    private Client client;
    private boolean credit;
    private double total;
    private List<LigneVente> lignes;

    public Vente(int id, LocalDateTime date, Client client, boolean credit, double total) {
        this.id = id;
        this.date = date;
        this.client = client;
        this.credit = credit;
        this.total = total;
    }

    // Classe interne pour les lignes de vente
    public static class LigneVente {
        private Produit produit;
        private int quantite;
        private double prixUnitaire;

        public LigneVente(Produit produit, int quantite, double prixUnitaire) {
            this.produit = produit;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
        }

        // Getters et setters
        public Produit getProduit() { return produit; }
        public void setProduit(Produit produit) { this.produit = produit; }
        public int getQuantite() { return quantite; }
        public void setQuantite(int quantite) { this.quantite = quantite; }
        public double getPrixUnitaire() { return prixUnitaire; }
        public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    }

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public boolean isCredit() { return credit; }
    public void setCredit(boolean credit) { this.credit = credit; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public List<LigneVente> getLignes() { return lignes; }
    public void setLignes(List<LigneVente> lignes) { this.lignes = lignes; }
}