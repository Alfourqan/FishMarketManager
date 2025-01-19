package com.poissonnerie.model;

import java.time.LocalDateTime;

public class MouvementCaisse {
    private int id;
    private LocalDateTime date;
    private TypeMouvement type;
    private double montant;
    private String description;

    public enum TypeMouvement {
        ENTREE("ENTREE"),
        SORTIE("SORTIE"),
        OUVERTURE("OUVERTURE"),
        CLOTURE("CLOTURE");

        private final String value;

        TypeMouvement(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static TypeMouvement fromString(String text) {
            for (TypeMouvement type : TypeMouvement.values()) {
                if (type.value.equalsIgnoreCase(text)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Type de mouvement invalide: " + text);
        }
    }

    public MouvementCaisse(int id, LocalDateTime date, TypeMouvement type, double montant, String description) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
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
    public void setMontant(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        this.montant = montant;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return String.format("%s - %s: %.2f € - %s",
            date.toString(),
            type.getValue(),
            montant,
            description);
    }
}