package com.poissonnerie.model;

import java.time.LocalDateTime;

public class Fournisseur {
    private int id;
    private String nom;
    private String contact;
    private String telephone;
    private String email;
    private String adresse;
    private String statut;
    private LocalDateTime derniereCommande;

    public Fournisseur(int id, String nom, String contact, String telephone, String email, String adresse) {
        this.id = id;
        this.nom = nom;
        this.contact = contact;
        this.telephone = telephone;
        this.email = email;
        this.adresse = adresse;
        this.statut = "Actif"; // Statut par défaut
        this.derniereCommande = null;
    }

    // Getters
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getContact() { return contact; }
    public String getTelephone() { return telephone; }
    public String getEmail() { return email; }
    public String getAdresse() { return adresse; }
    public String getStatut() { return statut; }
    public LocalDateTime getDerniereCommande() { return derniereCommande; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setContact(String contact) { this.contact = contact; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setEmail(String email) { this.email = email; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDerniereCommande(LocalDateTime derniereCommande) { this.derniereCommande = derniereCommande; }
}