package com.poissonnerie.model;

import java.util.HashSet;
import java.util.Set;

public class Role {
    private Integer id;
    private String nom;
    private String description;
    private Set<Permission> permissions;

    public Role() {
        this.permissions = new HashSet<>();
    }

    public Role(String nom, String description) {
        this();
        this.nom = nom;
        this.description = description;
    }

    // Getters
    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public Set<Permission> getPermissions() { return new HashSet<>(permissions); }

    // Setters
    public void setId(Integer id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setDescription(String description) { this.description = description; }
    
    // Gestion des permissions
    public void ajouterPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void retirerPermission(Permission permission) {
        this.permissions.remove(permission);
    }

    public boolean hasPermission(String permissionCode) {
        return permissions.stream()
            .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    @Override
    public String toString() {
        return "Role{" +
            "id=" + id +
            ", nom='" + nom + '\'' +
            ", description='" + description + '\'' +
            ", permissions=" + permissions +
            '}';
    }
}
