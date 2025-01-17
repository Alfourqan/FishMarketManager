package com.poissonnerie.model;

import java.time.LocalDateTime;

public class MouvementCaisse {
    private int id;
    private LocalDateTime date;
    private TypeMouvement type;
    private double montant;
    private String description;

    public enum TypeMouvement {
        ENTREE, SORTIE
    }

    public MouvementCaisse(int id, LocalDateTime date, TypeMouvement type, double montant, String description) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.montant = montant;
        this.description = description;
    }

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public TypeMouvement getType() { return type; }
    public void setType(TypeMouvement type) { this.type = type; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %.2f € - %s",
            date.toString(),
            type.toString(),
            montant,
            description);
    }
}
