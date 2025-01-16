package com.poissonnerie.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.List;

public class Vente {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final ObjectProperty<LocalDateTime> date = new SimpleObjectProperty<>();
    private final ObjectProperty<Client> client = new SimpleObjectProperty<>();
    private final BooleanProperty credit = new SimpleBooleanProperty();
    private final DoubleProperty total = new SimpleDoubleProperty();
    private List<LigneVente> lignes;

    public Vente(int id, LocalDateTime date, Client client, boolean credit, double total) {
        this.id.set(id);
        this.date.set(date);
        this.client.set(client);
        this.credit.set(credit);
        this.total.set(total);
    }

    // Classe interne pour les lignes de vente
    public static class LigneVente {
        private Produit produit;
        private int quantite;
        private double prixUnitaire;

        public LigneVente(Produit produit, int quantite, double prixUnitaire) {
            this.produit = produit;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
        }

        // Getters et setters
        public Produit getProduit() { return produit; }
        public void setProduit(Produit produit) { this.produit = produit; }
        public int getQuantite() { return quantite; }
        public void setQuantite(int quantite) { this.quantite = quantite; }
        public double getPrixUnitaire() { return prixUnitaire; }
        public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    }

    // Getters et setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public LocalDateTime getDate() { return date.get(); }
    public void setDate(LocalDateTime date) { this.date.set(date); }
    public ObjectProperty<LocalDateTime> dateProperty() { return date; }

    public Client getClient() { return client.get(); }
    public void setClient(Client client) { this.client.set(client); }
    public ObjectProperty<Client> clientProperty() { return client; }

    public boolean isCredit() { return credit.get(); }
    public void setCredit(boolean credit) { this.credit.set(credit); }
    public BooleanProperty creditProperty() { return credit; }

    public double getTotal() { return total.get(); }
    public void setTotal(double total) { this.total.set(total); }
    public DoubleProperty totalProperty() { return total; }

    public List<LigneVente> getLignes() { return lignes; }
    public void setLignes(List<LigneVente> lignes) { this.lignes = lignes; }
}
