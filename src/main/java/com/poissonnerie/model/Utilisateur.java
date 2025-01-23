package com.poissonnerie.model;

import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Utilisateur {
    private static final Logger LOGGER = Logger.getLogger(Utilisateur.class.getName());
    
    private int id;
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

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { 
        this.motDePasse = hashMotDePasse(motDePasse);
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getDernierLogin() { return dernierLogin; }
    public void setDernierLogin(LocalDateTime dernierLogin) { 
        this.dernierLogin = dernierLogin;
    }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public boolean verifierMotDePasse(String motDePasse) {
        String hashedInput = hashMotDePasse(motDePasse);
        return this.motDePasse.equals(hashedInput);
    }

    private String hashMotDePasse(String motDePasse) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(motDePasse.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du hashage du mot de passe", e);
            throw new RuntimeException("Erreur de sécurité", e);
        }
    }

    public void mettreAJourDernierLogin() {
        this.dernierLogin = LocalDateTime.now();
        LOGGER.info("Dernière connexion mise à jour pour l'utilisateur: " + this.nom);
    }
}
