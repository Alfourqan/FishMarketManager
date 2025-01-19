package com.poissonnerie.model;

public class Produit {
    private Long id;
    private String nom;
    private String categorie;
    private double prixAchat;
    private double prixVente;
    private int stock;
    private int seuilAlerte;

    public Produit() {
        // Constructeur par défaut
    }

    public Produit(Long id, String nom, String categorie, double prixAchat, double prixVente, int stock, int seuilAlerte) {
        this.id = id;
        this.nom = nom;
        this.categorie = categorie;
        this.prixAchat = prixAchat;
        this.prixVente = prixVente;
        this.stock = stock;
        this.seuilAlerte = seuilAlerte;
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public double getPrixAchat() { return prixAchat; }
    public void setPrixAchat(double prixAchat) { this.prixAchat = prixAchat; }

    public double getPrixVente() { return prixVente; }
    public void setPrixVente(double prixVente) { this.prixVente = prixVente; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(int seuilAlerte) { this.seuilAlerte = seuilAlerte; }

    // Méthodes utilitaires
    public double getMarge() {
        return prixVente - prixAchat;
    }

    public double getTauxMarge() {
        return prixAchat > 0 ? ((prixVente - prixAchat) / prixAchat) * 100 : 0;
    }

    // Nouvelles méthodes pour la gestion du stock
    public void ajusterStock(int quantite) {
        this.stock += quantite;
        if (this.stock < 0) {
            this.stock = 0;
        }
    }

    public boolean estStockBas() {
        return this.stock <= this.seuilAlerte;
    }

    public boolean estEnRupture() {
        return this.stock <= 0;
    }

    // Méthodes supplémentaires pour les rapports
    public String getReference() {
        return String.format("PR%06d", id);
    }

    public int getQuantite() {
        return this.stock;
    }

    public double getPrix() {
        return this.prixVente;
    }

    @Override
    public String toString() {
        return String.format("%s (ID: %d) - Prix vente: %.2f€, Stock: %d",
            nom, id, prixVente, stock);
    }
}