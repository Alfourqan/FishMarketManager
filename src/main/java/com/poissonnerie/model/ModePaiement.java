package com.poissonnerie.model;

/**
 * Énumération des différents modes de paiement supportés par l'application.
 */
public enum ModePaiement {
    ESPECES("Espèces"),
    CARTE("Carte bancaire"),
    CHEQUE("Chèque"),
    VIREMENT("Virement bancaire"),
    CREDIT("Crédit");

    private final String libelle;

    ModePaiement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
