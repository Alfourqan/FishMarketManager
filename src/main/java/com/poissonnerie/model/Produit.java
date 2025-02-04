package com.poissonnerie.model;

public class Produit {
    private int id;
    private String nom;
    private String categorie;
    private double prixAchat;
    private double prixVente;
    private int stock;
    private int seuilAlerte;
    private String reference;
    private Fournisseur fournisseur;
    private int fournisseurId; // Ajout du fournisseur_id

    public Produit(int id, String nom, String categorie, double prixAchat, double prixVente, int stock, int seuilAlerte) {
        this.id = id;
        this.nom = nom;
        this.categorie = categorie;
        this.prixAchat = prixAchat;
        this.prixVente = prixVente;
        this.stock = stock;
        this.seuilAlerte = seuilAlerte;
        this.reference = generateReference();
        this.fournisseurId = 1; // Fournisseur par défaut
    }

    // Getters et setters essentiels
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public double getPrixAchat() { return prixAchat; }
    public void setPrixAchat(double prixAchat) { this.prixAchat = prixAchat; }

    public double getPrixVente() { return prixVente; }
    public void setPrixVente(double prixVente) { this.prixVente = prixVente; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(int seuil) { this.seuilAlerte = seuil; }

    public String getReference() { return reference; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public void setFournisseur(Fournisseur fournisseur) { 
        this.fournisseur = fournisseur;
        if (fournisseur != null) {
            this.fournisseurId = fournisseur.getId();
        }
    }

    // Nouveau getter et setter pour fournisseurId
    public int getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(int fournisseurId) { this.fournisseurId = fournisseurId; }

    // Méthodes de calcul optimisées
    public double getMarge() { return prixVente - prixAchat; }
    public double getTauxMarge() { return prixAchat > 0 ? ((prixVente - prixAchat) / prixAchat) * 100 : 0; }

    private String generateReference() {
        return String.format("P%04d-%s", id, 
            categorie.substring(0, Math.min(3, categorie.length())).toUpperCase());
    }

    // Gestion du stock
    public void ajusterStock(int quantite) {
        int nouveauStock = this.stock + quantite;
        if (nouveauStock < 0) {
            throw new IllegalArgumentException(
                String.format("Stock insuffisant. Stock actuel: %d, Quantité demandée: %d",
                    this.stock, Math.abs(quantite)));
        }
        this.stock = nouveauStock;
    }

    // Méthodes de vérification du stock
    public boolean estStockBas() {
        return stock > 0 && stock <= seuilAlerte;
    }

    public boolean estEnRupture() {
        return stock == 0;
    }

    // Statut du stock
    public String getStatutStock() {
        if (stock == 0) return "RUPTURE";
        if (stock <= seuilAlerte) return "BAS";
        if (stock <= seuilAlerte * 2) return "ATTENTION";
        return "NORMAL";
    }

    public String getStatutStockFormatted() {
        switch (getStatutStock()) {
            case "RUPTURE": return "⛔ RUPTURE DE STOCK";
            case "BAS": return String.format("⚠️ Stock critique: %d (seuil: %d)", stock, seuilAlerte);
            case "ATTENTION": return String.format("⚠️ Stock à surveiller: %d (seuil: %d)", stock, seuilAlerte);
            default: return String.format("✓ Stock normal: %d", stock);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (Réf: %s) - Prix: %.0f FCFA %s %s",
            nom,
            reference,
            prixVente,
            fournisseur != null ? "- " + fournisseur.getNom() : "",
            getStatutStockFormatted());
    }
}