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
    private Fournisseur fournisseur;  // Nouveau champ

    public Produit(int id, String nom, String categorie, double prixAchat, double prixVente, int stock, int seuilAlerte) {
        this.id = id;
        this.nom = nom;
        this.categorie = categorie;
        this.prixAchat = prixAchat;
        this.prixVente = prixVente;
        this.stock = stock;
        this.seuilAlerte = seuilAlerte;
        this.reference = generateReference();
    }

    // Nouveau getter et setter pour fournisseur
    public Fournisseur getFournisseur() { return fournisseur; }
    public void setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; }

    // Getters et setters existants
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

    public double getPrix() { return prixVente; }

    public double getMarge() { return prixVente - prixAchat; }
    public double getTauxMarge() { return prixAchat > 0 ? ((prixVente - prixAchat) / prixAchat) * 100 : 0; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(int seuil) { this.seuilAlerte = seuil; }

    public String getReference() { return reference; }

    private String generateReference() {
        return String.format("P%04d-%s", id, 
            categorie.substring(0, Math.min(3, categorie.length())).toUpperCase());
    }

    public int getQuantite() { 
        return getStock(); 
    }

    public void ajusterStock(int quantite) {
        int nouveauStock = this.stock + quantite;
        if (nouveauStock < 0) {
            throw new IllegalArgumentException(
                String.format("Stock insuffisant. Stock actuel: %d, Quantité demandée: %d",
                    this.stock, Math.abs(quantite)));
        }
        this.stock = nouveauStock;
    }

    public boolean estStockBas() {
        return this.stock > 0 && this.stock <= this.seuilAlerte;
    }

    public boolean estEnRupture() {
        return this.stock == 0;
    }

    public String getStatutStock() {
        if (this.stock == 0) {
            return "RUPTURE";
        } else if (this.stock <= this.seuilAlerte) {
            return "BAS";
        } else if (this.stock <= this.seuilAlerte * 2) {
            return "ATTENTION";
        } else {
            return "NORMAL";
        }
    }

    public String getStatutStockFormatted() {
        switch (getStatutStock()) {
            case "RUPTURE":
                return "⛔ RUPTURE DE STOCK";
            case "BAS":
                return String.format("⚠️ Stock critique: %d (seuil: %d)", stock, seuilAlerte);
            case "ATTENTION":
                return String.format("⚠️ Stock à surveiller: %d (seuil: %d)", stock, seuilAlerte);
            default:
                return String.format("✓ Stock normal: %d", stock);
        }
    }

    @Override
    public String toString() {
        String fournisseurInfo = fournisseur != null ? ", Fournisseur: " + fournisseur.getNom() : "";
        return String.format("%s (Réf: %s) - Achat: %.0f FCFA, Vente: %.0f FCFA (%s)%s %s",
            nom,
            reference,
            prixAchat,
            prixVente,
            categorie,
            fournisseurInfo,
            getStatutStockFormatted());
    }
}