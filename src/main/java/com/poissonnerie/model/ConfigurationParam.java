package com.poissonnerie.model;

public class ConfigurationParam {
    private int id;
    private String cle;
    private String valeur;
    private String description;

    public ConfigurationParam(int id, String cle, String valeur, String description) {
        this.id = id;
        this.cle = cle;
        this.valeur = valeur;
        this.description = description;
    }

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCle() { return cle; }
    public void setCle(String cle) { this.cle = cle; }

    public String getValeur() { return valeur; }
    public void setValeur(String valeur) { this.valeur = valeur; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Constantes pour les clés de configuration
    // Configuration TVA
    public static final String CLE_TAUX_TVA = "TAUX_TVA";
    public static final String CLE_TVA_ENABLED = "TVA_ENABLED";

    // Informations entreprise
    public static final String CLE_NOM_ENTREPRISE = "NOM_ENTREPRISE";
    public static final String CLE_ADRESSE_ENTREPRISE = "ADRESSE_ENTREPRISE";
    public static final String CLE_TELEPHONE_ENTREPRISE = "TELEPHONE_ENTREPRISE";
    public static final String CLE_SIRET_ENTREPRISE = "SIRET_ENTREPRISE";

    // Configuration des reçus
    public static final String CLE_LOGO_PATH = "LOGO_PATH";
    public static final String CLE_FORMAT_RECU = "FORMAT_RECU";
    public static final String CLE_PIED_PAGE_RECU = "PIED_PAGE_RECU";
    public static final String CLE_EN_TETE_RECU = "EN_TETE_RECU";
}