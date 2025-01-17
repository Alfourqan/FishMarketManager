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

    // Getters et setters
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nom)
          .append(" - ")
          .append(String.format("%.2f€", prix))
          .append(" (")
          .append(categorie)
          .append(")");

        // Ajoute l'état du stock
        if (stock <= seuilAlerte) {
            sb.append(" ⚠️ Stock bas: ").append(stock);
        } else {
            sb.append(" - Stock: ").append(stock);
        }

        return sb.toString();
    }
}