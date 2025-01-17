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
        String sql = "SELECT * FROM configurations";

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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void mettreAJourConfiguration(ConfigurationParam config) {
        String sql = "UPDATE configurations SET valeur = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, config.getValeur());
            pstmt.setInt(2, config.getId());
            pstmt.executeUpdate();
            
            // Mise à jour du cache
            configCache.put(config.getCle(), config.getValeur());
        } catch (SQLException e) {
            e.printStackTrace();
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
        return infos;
    }
}
