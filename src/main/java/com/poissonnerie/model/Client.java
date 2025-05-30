package com.poissonnerie.model;

import java.time.LocalDateTime;

public class Client {
    private int id;
    private String nom;
    private String telephone;
    private String adresse;
    private double solde;
    private LocalDateTime derniereVente;
    private double totalCreances;
    private StatutCreances statutCreances;

    public enum StatutCreances {
        A_JOUR("À jour"),
        EN_RETARD("En retard"),
        CRITIQUE("Critique");

        private final String libelle;

        StatutCreances(String libelle) {
            this.libelle = libelle;
        }

        @Override
        public String toString() {
            return libelle;
        }
    }

    public Client(int id, String nom, String telephone, String adresse, double solde) {
        this.id = id;
        this.nom = nom;
        this.telephone = telephone;
        this.adresse = adresse;
        this.solde = solde;
        this.totalCreances = solde;
        updateStatutCreances();
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
    public void setSolde(double solde) { 
        this.solde = solde;
        this.totalCreances = solde;
        updateStatutCreances();
    }

    public LocalDateTime getDerniereVente() {
        return derniereVente;
    }

    public void setDerniereVente(LocalDateTime derniereVente) {
        this.derniereVente = derniereVente;
    }

    public double getTotalCreances() {
        return totalCreances;
    }

    public StatutCreances getStatutCreances() {
        return statutCreances;
    }

    private void updateStatutCreances() {
        if (solde <= 0) {
            statutCreances = StatutCreances.A_JOUR;
        } else if (solde <= 500) {
            statutCreances = StatutCreances.EN_RETARD;
        } else {
            statutCreances = StatutCreances.CRITIQUE;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nom);
        if (telephone != null && !telephone.isEmpty()) {
            sb.append(" (Tél: ").append(telephone).append(")");
        }
        if (solde > 0) {
            sb.append(" - Crédit: ").append(String.format("%.2f€", solde));
        }
        return sb.toString();
    }
}