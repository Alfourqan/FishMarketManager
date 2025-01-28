package com.poissonnerie.model;

import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Utilisateur {
    private static final Logger LOGGER = Logger.getLogger(Utilisateur.class.getName());

    private final int id;
    private String nom;
    private String motDePasse;
    private String role;
    private LocalDateTime dernierLogin;
    private boolean actif;

    public Utilisateur(int id, String nom, String motDePasse, String role) {
        this.id = id;
        this.nom = nom;
        this.motDePasse = hashMotDePasse(motDePasse);
        this.role = role;
        this.actif = true;
    }

    public int getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getDernierLogin() { return dernierLogin; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = hashMotDePasse(motDePasse);
    }

    public boolean verifierMotDePasse(String motDePasse) {
        return this.motDePasse.equals(hashMotDePasse(motDePasse));
    }

    private static String hashMotDePasse(String motDePasse) {
        return BCrypt.hashpw(motDePasse, BCrypt.gensalt(12));
    }

    public boolean verifierMotDePasse(String motDePasse) {
        return BCrypt.checkpw(motDePasse, this.motDePasse);
    }

    public void mettreAJourDernierLogin() {
        this.dernierLogin = LocalDateTime.now();
    }
}