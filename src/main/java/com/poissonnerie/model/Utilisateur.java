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
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(motDePasse.getBytes());
            StringBuilder hexString = new StringBuilder(64);

            for (byte b : hash) {
                hexString.append(String.format("%02x", b & 0xff));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du hashage du mot de passe", e);
            throw new RuntimeException("Erreur de sécurité", e);
        }
    }

    public void mettreAJourDernierLogin() {
        this.dernierLogin = LocalDateTime.now();
    }
}