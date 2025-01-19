package com.poissonnerie.model;

public class LigneVente {
    private Produit produit;
    private int quantite;
    private double prixUnitaire;

    public LigneVente(Produit produit, int quantite, double prixUnitaire) {
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    // Getters
    public Produit getProduit() { return produit; }
    public int getQuantite() { return quantite; }
    public double getPrixUnitaire() { return prixUnitaire; }

    // Setters
    public void setProduit(Produit produit) { this.produit = produit; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    // Méthodes métier
    public double getTotal() {
        return quantite * prixUnitaire;
    }
}
