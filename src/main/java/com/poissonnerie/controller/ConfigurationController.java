package com.poissonnerie.controller;

import com.poissonnerie.model.ConfigurationParam;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationController {
    private final List<ConfigurationParam> configurations = new ArrayList<>();
    private final Map<String, String> configCache = new HashMap<>();

    public List<ConfigurationParam> getConfigurations() {
        return configurations;
    }

    public void chargerConfigurations() {
        configurations.clear();
        configCache.clear();
        String sql = "SELECT * FROM configurations ORDER BY cle";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ConfigurationParam config = new ConfigurationParam(
                    rs.getInt("id"),
                    rs.getString("cle"),
                    rs.getString("valeur"),
                    rs.getString("description")
                );
                configurations.add(config);
                configCache.put(config.getCle(), config.getValeur());
            }
            System.out.println("Configurations chargées: " + configurations.size() + " entrées");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du chargement des configurations", e);
        }
    }

    public void mettreAJourConfiguration(ConfigurationParam config) {
        if (config == null || config.getCle() == null || config.getCle().trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration invalide");
        }

        // Validation spécifique pour certains paramètres
        switch (config.getCle()) {
            case ConfigurationParam.CLE_TAUX_TVA:
                try {
                    double tva = Double.parseDouble(config.getValeur());
                    if (tva < 0 || tva > 100) {
                        throw new IllegalArgumentException("Le taux de TVA doit être entre 0 et 100");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Le taux de TVA doit être un nombre valide");
                }
                break;
            case ConfigurationParam.CLE_TELEPHONE_ENTREPRISE:
                if (!config.getValeur().matches("^[0-9+\\-\\s]*$")) {
                    throw new IllegalArgumentException("Format de numéro de téléphone invalide");
                }
                break;
        }

        String sql = "UPDATE configurations SET valeur = ? WHERE cle = ?";
        System.out.println("Mise à jour de la configuration: " + config.getCle() + " = " + config.getValeur());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, config.getValeur());
            pstmt.setString(2, config.getCle());
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                // Mise à jour du cache et de la liste
                configCache.put(config.getCle(), config.getValeur());
                for (ConfigurationParam conf : configurations) {
                    if (conf.getCle().equals(config.getCle())) {
                        conf.setValeur(config.getValeur());
                        break;
                    }
                }
                System.out.println("Configuration mise à jour avec succès");
            } else {
                System.out.println("Aucune configuration mise à jour");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour de la configuration: " + e.getMessage(), e);
        }
    }

    public String getValeur(String cle) {
        return configCache.getOrDefault(cle, "");
    }

    public double getTauxTVA() {
        String taux = getValeur(ConfigurationParam.CLE_TAUX_TVA);
        try {
            return Double.parseDouble(taux);
        } catch (NumberFormatException e) {
            return 0.0; // Valeur par défaut si non configurée
        }
    }

    public Map<String, String> getInfosEntreprise() {
        Map<String, String> infos = new HashMap<>();
        infos.put("nom", getValeur(ConfigurationParam.CLE_NOM_ENTREPRISE));
        infos.put("adresse", getValeur(ConfigurationParam.CLE_ADRESSE_ENTREPRISE));
        infos.put("telephone", getValeur(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE));
        infos.put("pied_page", getValeur(ConfigurationParam.CLE_PIED_PAGE_RECU));
        return infos;
    }

    public void reinitialiserConfigurations() {
        String sql = "UPDATE configurations SET valeur = CASE " +
                    "WHEN cle = 'TAUX_TVA' THEN '20.0' " +
                    "WHEN cle = 'PIED_PAGE_RECU' THEN 'Merci de votre visite !' " +
                    "ELSE '' END";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            int rowsUpdated = stmt.executeUpdate(sql);
            System.out.println("Configurations réinitialisées: " + rowsUpdated + " entrées mises à jour");
            chargerConfigurations(); // Recharger les configurations
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la réinitialisation des configurations", e);
        }
    }
}