package com.poissonnerie.controller;

import com.poissonnerie.model.Fournisseur;
import java.util.ArrayList;
import java.util.List;

public class FournisseurController {
    private List<Fournisseur> fournisseurs;

    public FournisseurController() {
        this.fournisseurs = new ArrayList<>();
    }

    public void chargerFournisseurs() {
        // À implémenter: Charger depuis la base de données
        fournisseurs.clear();
        // Exemple de données de test
        fournisseurs.add(new Fournisseur(1, "Poisson Frais SARL", "Jean Dupont", "0123456789", "jean@poissonfrais.com", "123 Rue de la Mer"));
        fournisseurs.add(new Fournisseur(2, "Marée Bretagne", "Marie Martin", "0234567890", "marie@maree.fr", "456 Avenue de l'Océan"));
    }

    public List<Fournisseur> getFournisseurs() {
        return fournisseurs;
    }

    public void ajouterFournisseur(Fournisseur fournisseur) {
        // À implémenter: Sauvegarder dans la base de données
        fournisseurs.add(fournisseur);
    }

    public void mettreAJourFournisseur(Fournisseur fournisseur) {
        // À implémenter: Mettre à jour dans la base de données
        int index = fournisseurs.indexOf(fournisseur);
        if (index != -1) {
            fournisseurs.set(index, fournisseur);
        }
    }

    public void supprimerFournisseur(Fournisseur fournisseur) {
        // À implémenter: Supprimer de la base de données
        fournisseurs.remove(fournisseur);
    }
}
