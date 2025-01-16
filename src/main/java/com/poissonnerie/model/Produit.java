package com.poissonnerie.model;

import javafx.beans.property.*;

public class Produit {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty categorie = new SimpleStringProperty();
    private final DoubleProperty prix = new SimpleDoubleProperty();
    private final IntegerProperty stock = new SimpleIntegerProperty();
    private final IntegerProperty seuilAlerte = new SimpleIntegerProperty();

    public Produit(int id, String nom, String categorie, double prix, int stock, int seuilAlerte) {
        this.id.set(id);
        this.nom.set(nom);
        this.categorie.set(categorie);
        this.prix.set(prix);
        this.stock.set(stock);
        this.seuilAlerte.set(seuilAlerte);
    }

    // Getters et setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getNom() { return nom.get(); }
    public void setNom(String nom) { this.nom.set(nom); }
    public StringProperty nomProperty() { return nom; }

    public String getCategorie() { return categorie.get(); }
    public void setCategorie(String categorie) { this.categorie.set(categorie); }
    public StringProperty categorieProperty() { return categorie; }

    public double getPrix() { return prix.get(); }
    public void setPrix(double prix) { this.prix.set(prix); }
    public DoubleProperty prixProperty() { return prix; }

    public int getStock() { return stock.get(); }
    public void setStock(int stock) { this.stock.set(stock); }
    public IntegerProperty stockProperty() { return stock; }

    public int getSeuilAlerte() { return seuilAlerte.get(); }
    public void setSeuilAlerte(int seuil) { this.seuilAlerte.set(seuil); }
    public IntegerProperty seuilAlerteProperty() { return seuilAlerte; }
}
