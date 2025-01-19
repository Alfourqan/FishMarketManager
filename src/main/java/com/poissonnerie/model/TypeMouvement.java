package com.poissonnerie.model;

/**
 * Énumération des types de mouvements de caisse.
 */
public enum TypeMouvement {
    ENTREE("Entrée", true),
    SORTIE("Sortie", false),
    RETOUR("Retour/Avoir", true),
    ANNULATION("Annulation", false),
    REMBOURSEMENT("Remboursement", false);

    private final String libelle;
    private final boolean positif;

    TypeMouvement(String libelle, boolean positif) {
        this.libelle = libelle;
        this.positif = positif;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean isPositif() {
        return positif;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
