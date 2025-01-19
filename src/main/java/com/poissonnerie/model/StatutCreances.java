package com.poissonnerie.model;

/**
 * Énumération des différents statuts de créances possibles.
 */
public enum StatutCreances {
    NORMAL("Normal"),
    EN_RETARD("En retard"),
    CRITIQUE("Critique"),
    EN_CONTENTIEUX("En contentieux");

    private final String libelle;

    StatutCreances(String libelle) {
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
