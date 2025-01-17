package com.poissonnerie.model;

import java.util.Objects;

/**
 * Classe de gestion des paramètres de configuration de l'application
 */
public class ConfigurationParam {
    private int id;
    private String cle;
    private String valeur;
    private String description;

    /**
     * Constructeur avec validation des paramètres
     * @param id Identifiant unique du paramètre
     * @param cle Clé du paramètre
     * @param valeur Valeur du paramètre
     * @param description Description du paramètre
     * @throws IllegalArgumentException si les paramètres sont invalides
     */
    public ConfigurationParam(int id, String cle, String valeur, String description) {
        this.id = id;
        this.cle = Objects.requireNonNull(cle, "La clé ne peut pas être null");
        this.valeur = Objects.requireNonNull(valeur, "La valeur ne peut pas être null");
        this.description = Objects.requireNonNull(description, "La description ne peut pas être null");

        if (cle.trim().isEmpty()) {
            throw new IllegalArgumentException("La clé ne peut pas être vide");
        }
    }

    // Getters et setters avec validation
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCle() { return cle; }
    public void setCle(String cle) {
        this.cle = Objects.requireNonNull(cle, "La clé ne peut pas être null");
        if (cle.trim().isEmpty()) {
            throw new IllegalArgumentException("La clé ne peut pas être vide");
        }
    }

    public String getValeur() { return valeur; }
    public void setValeur(String valeur) {
        this.valeur = Objects.requireNonNull(valeur, "La valeur ne peut pas être null");
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "La description ne peut pas être null");
    }

    // Constantes pour les clés de configuration avec documentation
    /** Taux de TVA appliqué aux ventes */
    public static final String CLE_TAUX_TVA = "TAUX_TVA";
    /** Activation/désactivation de la TVA */
    public static final String CLE_TVA_ENABLED = "TVA_ENABLED";

    /** Nom officiel de l'entreprise */
    public static final String CLE_NOM_ENTREPRISE = "NOM_ENTREPRISE";
    /** Adresse complète de l'entreprise */
    public static final String CLE_ADRESSE_ENTREPRISE = "ADRESSE_ENTREPRISE";
    /** Numéro de téléphone de contact */
    public static final String CLE_TELEPHONE_ENTREPRISE = "TELEPHONE_ENTREPRISE";
    /** Numéro SIRET de l'entreprise */
    public static final String CLE_SIRET_ENTREPRISE = "SIRET_ENTREPRISE";

    /** Chemin vers le logo de l'entreprise */
    public static final String CLE_LOGO_PATH = "LOGO_PATH";
    /** Format des reçus (A4, A5, etc.) */
    public static final String CLE_FORMAT_RECU = "FORMAT_RECU";
    /** Texte personnalisé en pied de page des reçus */
    public static final String CLE_PIED_PAGE_RECU = "PIED_PAGE_RECU";
    /** En-tête personnalisé des reçus */
    public static final String CLE_EN_TETE_RECU = "EN_TETE_RECU";
}