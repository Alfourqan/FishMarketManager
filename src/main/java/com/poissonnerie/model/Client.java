package com.poissonnerie.model;

import javafx.beans.property.*;

public class Client {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty telephone = new SimpleStringProperty();
    private final StringProperty adresse = new SimpleStringProperty();
    private final DoubleProperty solde = new SimpleDoubleProperty();

    public Client(int id, String nom, String telephone, String adresse, double solde) {
        this.id.set(id);
        this.nom.set(nom);
        this.telephone.set(telephone);
        this.adresse.set(adresse);
        this.solde.set(solde);
    }

    // Getters et setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getNom() { return nom.get(); }
    public void setNom(String nom) { this.nom.set(nom); }
    public StringProperty nomProperty() { return nom; }

    public String getTelephone() { return telephone.get(); }
    public void setTelephone(String telephone) { this.telephone.set(telephone); }
    public StringProperty telephoneProperty() { return telephone; }

    public String getAdresse() { return adresse.get(); }
    public void setAdresse(String adresse) { this.adresse.set(adresse); }
    public StringProperty adresseProperty() { return adresse; }

    public double getSolde() { return solde.get(); }
    public void setSolde(double solde) { this.solde.set(solde); }
    public DoubleProperty soldeProperty() { return solde; }
}
