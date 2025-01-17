package com.poissonnerie.model;

public class Client {
    private int id;
    private String nom;
    private String telephone;
    private String adresse;
    private double solde;

    public Client(int id, String nom, String telephone, String adresse, double solde) {
        this.id = id;
        this.nom = nom;
        this.telephone = telephone;
        this.adresse = adresse;
        this.solde = solde;
    }

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public double getSolde() { return solde; }
    public void setSolde(double solde) { this.solde = solde; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nom);
        if (telephone != null && !telephone.isEmpty()) {
            sb.append(" - Tél: ").append(telephone);
        }
        if (solde > 0) {
            sb.append(" (Crédit: ").append(String.format("%.2f€)", solde));
        }
        return sb.toString();
    }
}