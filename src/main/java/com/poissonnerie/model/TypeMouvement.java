package com.poissonnerie.model;

/**
 * @deprecated Utilisez MouvementCaisse.TypeMouvement à la place. Cette énumération sera supprimée dans une version future.
 */
@Deprecated
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