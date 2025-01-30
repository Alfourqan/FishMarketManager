package com.poissonnerie.model;

import java.time.LocalDateTime;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.mindrot.jbcrypt.BCrypt;

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

    private static String hashMotDePasse(String motDePasse) {
        try {
            return BCrypt.hashpw(motDePasse, BCrypt.gensalt(12));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du hashage du mot de passe", e);
            throw new RuntimeException("Erreur de sécurité: impossible de hasher le mot de passe", e);
        }
    }

    public boolean verifierMotDePasse(String motDePasse) {
        try {
            return BCrypt.checkpw(motDePasse, this.motDePasse);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification du mot de passe", e);
            return false;
        }
    }

    public void mettreAJourDernierLogin() {
        this.dernierLogin = LocalDateTime.now();
    }
}