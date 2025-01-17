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

    public void chargerConfigurations() {
        configurations.clear();
        configCache.clear();
        String sql = "SELECT * FROM configurations ORDER BY cle";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            try {
                ResultSet rs = stmt.executeQuery(sql);
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
                conn.commit();
                System.out.println("Configurations chargées: " + configurations.size() + " entrées");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des configurations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du chargement des configurations", e);
        }
    }

    public void mettreAJourConfiguration(ConfigurationParam config) {
        if (config == null || config.getCle() == null || config.getCle().trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration invalide");
        }

        String sql = "UPDATE configurations SET valeur = ? WHERE cle = ?";
        System.out.println("Mise à jour de la configuration: " + config.getCle() + " = " + config.getValeur());

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, config.getValeur());
                pstmt.setString(2, config.getCle());
                int rowsUpdated = pstmt.executeUpdate();

                if (rowsUpdated > 0) {
                    configCache.put(config.getCle(), config.getValeur());
                    for (ConfigurationParam conf : configurations) {
                        if (conf.getCle().equals(config.getCle())) {
                            conf.setValeur(config.getValeur());
                            break;
                        }
                    }
                    conn.commit();
                    System.out.println("Configuration mise à jour avec succès");
                } else {
                    conn.rollback();
                    System.err.println("Configuration non trouvée: " + config.getCle());
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la configuration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour de la configuration", e);
        }
    }

    public void reinitialiserConfigurations() {
        String sql = "UPDATE configurations SET valeur = CASE " +
                    "WHEN cle = 'TAUX_TVA' THEN '20.0' " +
                    "WHEN cle = 'TVA_ENABLED' THEN 'true' " +
                    "WHEN cle = 'FORMAT_RECU' THEN 'COMPACT' " +
                    "WHEN cle = 'PIED_PAGE_RECU' THEN 'Merci de votre visite !' " +
                    "WHEN cle = 'EN_TETE_RECU' THEN 'Votre Poissonnerie de Confiance' " +
                    "ELSE '' END " +
                    "WHERE cle IN ('TAUX_TVA', 'TVA_ENABLED', 'FORMAT_RECU', 'PIED_PAGE_RECU', " +
                    "'EN_TETE_RECU', 'NOM_ENTREPRISE', 'ADRESSE_ENTREPRISE', 'TELEPHONE_ENTREPRISE', " +
                    "'SIRET_ENTREPRISE', 'LOGO_PATH')";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                int rowsUpdated = stmt.executeUpdate(sql);
                conn.commit();
                System.out.println("Configurations réinitialisées: " + rowsUpdated + " entrées mises à jour");
                chargerConfigurations(); // Recharger les configurations après réinitialisation
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la réinitialisation des configurations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la réinitialisation des configurations", e);
        }
    }

    public String getValeur(String cle) {
        return configCache.getOrDefault(cle, "");
    }

    public List<ConfigurationParam> getConfigurations() {
        return configurations;
    }
    public double getTauxTVA() {
        String taux = getValeur(ConfigurationParam.CLE_TAUX_TVA);
        try {
            return Double.parseDouble(taux);
        } catch (NumberFormatException e) {
            System.err.println("Erreur de conversion du taux TVA: " + e.getMessage());
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
}