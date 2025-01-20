package com.poissonnerie.controller;

import com.poissonnerie.model.ConfigurationParam;
import com.poissonnerie.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public class ConfigurationController {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationController.class.getName());
    private final List<ConfigurationParam> configurations = new ArrayList<>();
    private final Map<String, String> configCache = new HashMap<>();
    private static final int MAX_RETRIES = 3;
    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[A-Z_]{1,50}$");
    private static final SecureRandom secureRandom = new SecureRandom();

    public void chargerConfigurations() {
        configurations.clear();
        configCache.clear();
        String sql = "SELECT * FROM configurations ORDER BY cle";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    String cle = sanitizeInput(rs.getString("cle"));
                    String valeur = sanitizeInput(rs.getString("valeur"));
                    String description = sanitizeInput(rs.getString("description"));

                    // Désactiver temporairement la validation SIRET pendant l'initialisation
                    if (cle.equals(ConfigurationParam.CLE_SIRET_ENTREPRISE)) {
                        System.setProperty("SKIP_SIRET_VALIDATION", "true");
                    }

                    ConfigurationParam config = new ConfigurationParam(
                        rs.getInt("id"),
                        cle,
                        valeur,
                        description
                    );

                    if (cle.equals(ConfigurationParam.CLE_SIRET_ENTREPRISE)) {
                        System.clearProperty("SKIP_SIRET_VALIDATION");
                    }

                    configurations.add(config);
                    configCache.put(config.getCle(), config.getValeur());
                } catch (Exception e) {
                    LOGGER.warning("Configuration invalide ignorée: " + e.getMessage());
                    // Continue avec la prochaine configuration
                }
            }
            LOGGER.info("Configurations chargées: " + configurations.size() + " entrées");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations", e);
            throw new RuntimeException("Erreur lors du chargement des configurations", e);
        }
    }

    public void mettreAJourConfiguration(ConfigurationParam config) {
        validateConfiguration(config);

        String sql = "UPDATE configurations SET valeur = ? WHERE cle = ?";
        LOGGER.info("Mise à jour de la configuration: " + config.getCle());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sanitizeInput(config.getValeur()));
            pstmt.setString(2, sanitizeInput(config.getCle()));
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                configCache.put(config.getCle(), config.getValeur());
                for (ConfigurationParam conf : configurations) {
                    if (conf.getCle().equals(config.getCle())) {
                        conf.setValeur(config.getValeur());
                        break;
                    }
                }
                LOGGER.info("Configuration mise à jour avec succès");
            } else {
                LOGGER.warning("Configuration non trouvée: " + config.getCle());
                throw new IllegalStateException("Configuration non trouvée: " + config.getCle());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de la configuration", e);
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
        }
    }

    public void reinitialiserConfigurations() {
        // Désactiver temporairement la validation SIRET
        System.setProperty("SKIP_SIRET_VALIDATION", "true");

        try {
            String sql = "UPDATE configurations SET valeur = CASE " +
                        "WHEN cle = 'TAUX_TVA' THEN '20.0' " +
                        "WHEN cle = 'TVA_ENABLED' THEN 'true' " +
                        "WHEN cle = 'FORMAT_RECU' THEN 'COMPACT' " +
                        "WHEN cle = 'PIED_PAGE_RECU' THEN 'Merci de votre visite !' " +
                        "WHEN cle = 'SIRET_ENTREPRISE' THEN '12345678901234' " +
                        "ELSE valeur END " +
                        "WHERE cle IN (?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, ConfigurationParam.CLE_TAUX_TVA);
                stmt.setString(2, ConfigurationParam.CLE_TVA_ENABLED);
                stmt.setString(3, ConfigurationParam.CLE_FORMAT_RECU);
                stmt.setString(4, ConfigurationParam.CLE_PIED_PAGE_RECU);
                stmt.setString(5, ConfigurationParam.CLE_SIRET_ENTREPRISE);

                stmt.executeUpdate();
                LOGGER.info("Configurations réinitialisées avec succès");

                // Recharger les configurations après réinitialisation
                chargerConfigurations();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
            throw new RuntimeException("Erreur lors de la réinitialisation des configurations", e);
        } finally {
            // Réactiver la validation SIRET
            System.clearProperty("SKIP_SIRET_VALIDATION");
        }
    }

    public String getValeur(String cle) {
        if (!SAFE_KEY_PATTERN.matcher(cle).matches()) {
            LOGGER.warning("Tentative d'accès avec une clé invalide: " + cle);
            return "";
        }
        return sanitizeInput(configCache.getOrDefault(cle, ""));
    }

    public String getConfiguration(String cle, String defaultValue) {
        String valeur = getValeur(cle);
        return valeur.isEmpty() ? defaultValue : valeur;
    }

    public List<ConfigurationParam> getConfigurations() {
        return new ArrayList<>(configurations);
    }

    public double getTauxTVA() {
        String taux = getValeur(ConfigurationParam.CLE_TAUX_TVA);
        try {
            double tauxTVA = Double.parseDouble(taux);
            if (tauxTVA < 0 || tauxTVA > 100) {
                LOGGER.warning("Taux TVA invalide: " + tauxTVA);
                return 20.0; // Valeur par défaut standard
            }
            return tauxTVA;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Erreur de conversion du taux TVA", e);
            return 20.0; // Valeur par défaut standard
        }
    }

    public Map<String, String> getInfosEntreprise() {
        Map<String, String> infos = new HashMap<>();
        String[] cles = {
            ConfigurationParam.CLE_NOM_ENTREPRISE,
            ConfigurationParam.CLE_ADRESSE_ENTREPRISE,
            ConfigurationParam.CLE_TELEPHONE_ENTREPRISE,
            ConfigurationParam.CLE_PIED_PAGE_RECU
        };

        for (String cle : cles) {
            infos.put(cle.toLowerCase().replace("_entreprise", "")
                               .replace("_recu", ""), getValeur(cle));
        }
        return infos;
    }

    private void validateConfiguration(ConfigurationParam config) {
        if (config == null) {
            throw new IllegalArgumentException("La configuration ne peut pas être null");
        }
        if (config.getCle() == null || !SAFE_KEY_PATTERN.matcher(config.getCle()).matches()) {
            throw new IllegalArgumentException("Format de clé de configuration invalide");
        }
        if (config.getValeur() == null) {
            throw new IllegalArgumentException("La valeur de configuration ne peut pas être null");
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[<>\"'&/\\\\]", "_")
                   .replaceAll("(?i)javascript:", "")
                   .replaceAll("(?i)data:", "")
                   .replaceAll("(?i)vbscript:", "");
    }
}