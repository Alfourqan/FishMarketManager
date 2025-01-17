package com.poissonnerie.model;

public class Produit {
    private int id;
    private String nom;
    private String categorie;
    private double prix;
    private int stock;
    private int seuilAlerte;

    public Produit(int id, String nom, String categorie, double prix, int stock, int seuilAlerte) {
        this.id = id;
        this.nom = nom;
        this.categorie = categorie;
        this.prix = prix;
        this.stock = stock;
        this.seuilAlerte = seuilAlerte;
    }

    // Getters et setters existants
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(int seuil) { this.seuilAlerte = seuil; }

    // Méthodes améliorées pour la gestion du stock
    public void ajusterStock(int quantite) {
        int nouveauStock = this.stock + quantite;
        if (nouveauStock < 0) {
            throw new IllegalArgumentException(
                String.format("Stock insuffisant. Stock actuel: %d, Quantité demandée: %d",
                    this.stock, Math.abs(quantite)));
        }
        this.stock = nouveauStock;
    }

    public boolean estStockBas() {
        return this.stock > 0 && this.stock <= this.seuilAlerte;
    }

    public String getStatutStock() {
        if (this.stock <= 0) {
            return "RUPTURE";
        } else if (this.stock <= this.seuilAlerte) {
            return "BAS";
        } else {
            return "NORMAL";
        }
    }

    public String getStatutStockFormatted() {
        switch (getStatutStock()) {
            case "RUPTURE":
                return "⛔ RUPTURE DE STOCK";
            case "BAS":
                return String.format("⚠️ Stock critique: %d (seuil: %d)", stock, seuilAlerte);
            default:
                return String.format("✓ Stock normal: %d", stock);
        }
    }

    @Override
    public String toString() {
        return String.format("%s - %.2f€ (%s) %s",
            nom,
            prix,
            categorie,
            getStatutStockFormatted());
    }
}