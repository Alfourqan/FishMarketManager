package com.poissonnerie.util;

import com.poissonnerie.model.Role;
import com.poissonnerie.model.Permission;
import com.poissonnerie.controller.RoleController;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.SQLException;

public class InitialisationRoles {
    private static final Logger LOGGER = Logger.getLogger(InitialisationRoles.class.getName());

    // Codes des permissions pour la vente
    public static final String PERM_VENTE_CREER = "VENTE_CREER";
    public static final String PERM_VENTE_MODIFIER = "VENTE_MODIFIER";
    public static final String PERM_VENTE_ANNULER = "VENTE_ANNULER";
    public static final String PERM_VENTE_CONSULTER = "VENTE_CONSULTER";

    // Codes des permissions pour la gestion des stocks
    public static final String PERM_STOCK_AJOUTER = "STOCK_AJOUTER";
    public static final String PERM_STOCK_MODIFIER = "STOCK_MODIFIER";
    public static final String PERM_STOCK_SUPPRIMER = "STOCK_SUPPRIMER";
    public static final String PERM_STOCK_CONSULTER = "STOCK_CONSULTER";

    // Codes des permissions pour la gestion des clients
    public static final String PERM_CLIENT_CREER = "CLIENT_CREER";
    public static final String PERM_CLIENT_MODIFIER = "CLIENT_MODIFIER";
    public static final String PERM_CLIENT_SUPPRIMER = "CLIENT_SUPPRIMER";
    public static final String PERM_CLIENT_CONSULTER = "CLIENT_CONSULTER";

    // Codes des permissions pour la gestion financière
    public static final String PERM_FINANCE_CONSULTER = "FINANCE_CONSULTER";
    public static final String PERM_FINANCE_MODIFIER = "FINANCE_MODIFIER";
    public static final String PERM_CAISSE_GERER = "CAISSE_GERER";

    // Codes des permissions pour la configuration système
    public static final String PERM_CONFIG_SYSTEME = "CONFIG_SYSTEME";
    public static final String PERM_UTILISATEUR_GERER = "UTILISATEUR_GERER";

    private static final RoleController roleController = RoleController.getInstance();

    public static void initialiserRoles() {
        try {
            // Rôle Vendeur
            Role vendeur = new Role("VENDEUR", "Employé chargé des ventes");
            ajouterPermissionsVendeur(vendeur);
            roleController.creerRole(vendeur);
            LOGGER.info("Rôle VENDEUR créé avec succès");

            // Rôle Comptable
            Role comptable = new Role("COMPTABLE", "Responsable de la comptabilité");
            ajouterPermissionsComptable(comptable);
            roleController.creerRole(comptable);
            LOGGER.info("Rôle COMPTABLE créé avec succès");

            // Rôle Manager
            Role manager = new Role("MANAGER", "Gestionnaire du magasin");
            ajouterPermissionsManager(manager);
            roleController.creerRole(manager);
            LOGGER.info("Rôle MANAGER créé avec succès");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation des rôles", e);
            throw new RuntimeException("Erreur lors de l'initialisation des rôles", e);
        }
    }

    private static void ajouterPermissionsVendeur(Role vendeur) {
        // Permissions de vente
        vendeur.ajouterPermission(new Permission(PERM_VENTE_CREER, "Créer une vente", "Ventes"));
        vendeur.ajouterPermission(new Permission(PERM_VENTE_MODIFIER, "Modifier une vente", "Ventes"));
        vendeur.ajouterPermission(new Permission(PERM_VENTE_CONSULTER, "Consulter les ventes", "Ventes"));

        // Permissions de stock (limitées)
        vendeur.ajouterPermission(new Permission(PERM_STOCK_CONSULTER, "Consulter les stocks", "Stocks"));

        // Permissions clients
        vendeur.ajouterPermission(new Permission(PERM_CLIENT_CREER, "Créer un client", "Clients"));
        vendeur.ajouterPermission(new Permission(PERM_CLIENT_CONSULTER, "Consulter les clients", "Clients"));
        vendeur.ajouterPermission(new Permission(PERM_CLIENT_MODIFIER, "Modifier un client", "Clients"));

        // Permission caisse
        vendeur.ajouterPermission(new Permission(PERM_CAISSE_GERER, "Gérer la caisse", "Finance"));
    }

    private static void ajouterPermissionsComptable(Role comptable) {
        // Permissions de consultation des ventes
        comptable.ajouterPermission(new Permission(PERM_VENTE_CONSULTER, "Consulter les ventes", "Ventes"));

        // Permissions financières
        comptable.ajouterPermission(new Permission(PERM_FINANCE_CONSULTER, "Consulter les données financières", "Finance"));
        comptable.ajouterPermission(new Permission(PERM_FINANCE_MODIFIER, "Modifier les données financières", "Finance"));

        // Permissions de rapports
        comptable.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_STOCKS, "Générer rapport des stocks", "Rapports"));
        comptable.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_VENTES, "Générer rapport des ventes", "Rapports"));
        comptable.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_CREANCES, "Générer rapport des créances", "Rapports"));
        comptable.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_FINANCIER, "Générer rapport financier", "Rapports"));
        comptable.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_ACHATS, "Générer rapport des achats", "Rapports"));

        // Permissions de consultation
        comptable.ajouterPermission(new Permission(PERM_STOCK_CONSULTER, "Consulter les stocks", "Stocks"));
        comptable.ajouterPermission(new Permission(PERM_CLIENT_CONSULTER, "Consulter les clients", "Clients"));
    }

    private static void ajouterPermissionsManager(Role manager) {
        // Toutes les permissions de vente
        manager.ajouterPermission(new Permission(PERM_VENTE_CREER, "Créer une vente", "Ventes"));
        manager.ajouterPermission(new Permission(PERM_VENTE_MODIFIER, "Modifier une vente", "Ventes"));
        manager.ajouterPermission(new Permission(PERM_VENTE_ANNULER, "Annuler une vente", "Ventes"));
        manager.ajouterPermission(new Permission(PERM_VENTE_CONSULTER, "Consulter les ventes", "Ventes"));

        // Toutes les permissions de stock
        manager.ajouterPermission(new Permission(PERM_STOCK_AJOUTER, "Ajouter au stock", "Stocks"));
        manager.ajouterPermission(new Permission(PERM_STOCK_MODIFIER, "Modifier le stock", "Stocks"));
        manager.ajouterPermission(new Permission(PERM_STOCK_SUPPRIMER, "Supprimer du stock", "Stocks"));
        manager.ajouterPermission(new Permission(PERM_STOCK_CONSULTER, "Consulter les stocks", "Stocks"));

        // Toutes les permissions clients
        manager.ajouterPermission(new Permission(PERM_CLIENT_CREER, "Créer un client", "Clients"));
        manager.ajouterPermission(new Permission(PERM_CLIENT_MODIFIER, "Modifier un client", "Clients"));
        manager.ajouterPermission(new Permission(PERM_CLIENT_SUPPRIMER, "Supprimer un client", "Clients"));
        manager.ajouterPermission(new Permission(PERM_CLIENT_CONSULTER, "Consulter les clients", "Clients"));

        // Toutes les permissions financières
        manager.ajouterPermission(new Permission(PERM_FINANCE_CONSULTER, "Consulter les données financières", "Finance"));
        manager.ajouterPermission(new Permission(PERM_FINANCE_MODIFIER, "Modifier les données financières", "Finance"));
        manager.ajouterPermission(new Permission(PERM_CAISSE_GERER, "Gérer la caisse", "Finance"));

        // Permissions de configuration
        manager.ajouterPermission(new Permission(PERM_CONFIG_SYSTEME, "Configurer le système", "System"));
        manager.ajouterPermission(new Permission(PERM_UTILISATEUR_GERER, "Gérer les utilisateurs", "System"));

        // Tous les rapports
        manager.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_STOCKS, "Générer rapport des stocks", "Rapports"));
        manager.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_VENTES, "Générer rapport des ventes", "Rapports"));
        manager.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_CREANCES, "Générer rapport des créances", "Rapports"));
        manager.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_FOURNISSEURS, "Générer rapport des fournisseurs", "Rapports"));
        manager.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_FINANCIER, "Générer rapport financier", "Rapports"));
        manager.ajouterPermission(new Permission(ExcelGenerator.PERM_RAPPORT_ACHATS, "Générer rapport des achats", "Rapports"));
    }
}