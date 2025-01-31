package com.poissonnerie.model;

import java.time.LocalDateTime;

public class UserAction {
    private int id;
    private ActionType type;
    private String username;
    private LocalDateTime dateTime;
    private String description;
    private EntityType entityType;
    private int entityId;
    private Integer userId;  // Changed to Integer to allow null values

    public enum ActionType {
        CREATION("Création"),
        MODIFICATION("Modification"),
        SUPPRESSION("Suppression"),
        CONSULTATION("Consultation"),
        VALIDATION("Validation");

        private final String value;

        ActionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum EntityType {
        PRODUIT("Produit"),
        CLIENT("Client"),
        VENTE("Vente"),
        FOURNISSEUR("Fournisseur"),
        CAISSE("Caisse"),
        UTILISATEUR("Utilisateur");

        private final String value;

        EntityType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public UserAction(ActionType type, String username, String description, EntityType entityType, int entityId) {
        this.type = type;
        this.username = username;
        this.dateTime = LocalDateTime.now();
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userId = null;  // Initialize as null by default
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s (ID: %d) - %s User: %s",
            dateTime.toString(),
            username,
            type.getValue(),
            entityType.getValue(),
            entityId,
            description,
            userId != null ? userId : "N/A");
    }
}