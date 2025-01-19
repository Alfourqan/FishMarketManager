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
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                conn.setAutoCommit(false);
                try {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        try {
                            ConfigurationParam config = new ConfigurationParam(
                                rs.getInt("id"),
                                sanitizeInput(rs.getString("cle")),
                                sanitizeInput(rs.getString("valeur")),
                                sanitizeInput(rs.getString("description"))
                            );
                            configurations.add(config);
                            configCache.put(config.getCle(), config.getValeur());
                        } catch (IllegalArgumentException e) {
                            LOGGER.warning("Configuration invalide ignorée: " + e.getMessage());
                            // Continue avec la prochaine configuration
                        }
                    }
                    conn.commit();
                    LOGGER.info("Configurations chargées: " + configurations.size() + " entrées");
                    break; // Sortie de la boucle si succès
                } catch (SQLException e) {
                    conn.rollback();
                    LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations (tentative " + (retryCount + 1) + ")", e);
                    retryCount++;
                    if (retryCount >= MAX_RETRIES) {
                        throw new RuntimeException("Échec du chargement des configurations après " + MAX_RETRIES + " tentatives", e);
                    }
                    // Attente exponentielle entre les tentatives
                    Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                }
            } catch (SQLException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale lors du chargement des configurations", e);
                throw new RuntimeException("Erreur lors du chargement des configurations", e);
            }
        }
    }

    public void mettreAJourConfiguration(ConfigurationParam config) {
        validateConfiguration(config);

        String sql = "UPDATE configurations SET valeur = ? WHERE cle = ?";
        LOGGER.info("Mise à jour de la configuration: " + config.getCle());

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
                    conn.commit();
                    LOGGER.info("Configuration mise à jour avec succès");
                } else {
                    conn.rollback();
                    LOGGER.warning("Configuration non trouvée: " + config.getCle());
                    throw new IllegalStateException("Configuration non trouvée: " + config.getCle());
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de la configuration", e);
                throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de la configuration", e);
            throw new RuntimeException("Erreur lors de la mise à jour de la configuration", e);
        }
    }

    public void reinitialiserConfigurations() {
        String sql = "UPDATE configurations SET valeur = CASE " +
                    "WHEN cle = 'TAUX_TVA' THEN '20.0' " +
                    "WHEN cle = 'TVA_ENABLED' THEN 'true' " +
                    "WHEN cle = 'FORMAT_RECU' THEN 'COMPACT' " +
                    "WHEN cle = 'PIED_PAGE_RECU' THEN 'Merci de votre visite !' " +
                    "WHEN cle = 'EN_TETE_RECU' THEN '' " +
                    "ELSE '' END " +
                    "WHERE cle IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int paramIndex = 1;
                stmt.setString(paramIndex++, ConfigurationParam.CLE_TAUX_TVA);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_TVA_ENABLED);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_FORMAT_RECU);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_PIED_PAGE_RECU);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_EN_TETE_RECU);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_NOM_ENTREPRISE);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_ADRESSE_ENTREPRISE);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_TELEPHONE_ENTREPRISE);
                stmt.setString(paramIndex++, ConfigurationParam.CLE_SIRET_ENTREPRISE);
                stmt.setString(paramIndex, ConfigurationParam.CLE_LOGO_PATH);

                int rowsUpdated = stmt.executeUpdate();
                conn.commit();
                LOGGER.info("Configurations réinitialisées: " + rowsUpdated + " entrées mises à jour");
                chargerConfigurations(); // Recharger les configurations après réinitialisation
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
                throw new RuntimeException("Erreur lors de la réinitialisation: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
            throw new RuntimeException("Erreur lors de la réinitialisation des configurations", e);
        }
    }

    public String getValeur(String cle) {
        if (!SAFE_KEY_PATTERN.matcher(cle).matches()) {
            LOGGER.warning("Tentative d'accès avec une clé invalide: " + cle);
            return "";
        }
        return sanitizeInput(configCache.getOrDefault(cle, ""));
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

        // Validation spécifique selon le type de configuration
        switch (config.getCle()) {
            case ConfigurationParam.CLE_TAUX_TVA:
                validateTauxTVA(config.getValeur());
                break;
            case ConfigurationParam.CLE_TVA_ENABLED:
                validateBoolean(config.getValeur());
                break;
            case ConfigurationParam.CLE_FORMAT_RECU:
                validateFormatRecu(config.getValeur());
                break;
        }
    }

    private void validateTauxTVA(String valeur) {
        try {
            double taux = Double.parseDouble(valeur);
            if (taux < 0 || taux > 100) {
                throw new IllegalArgumentException("Le taux de TVA doit être compris entre 0 et 100");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Le taux de TVA doit être un nombre valide");
        }
    }

    private void validateBoolean(String valeur) {
        if (!valeur.equalsIgnoreCase("true") && !valeur.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("La valeur doit être 'true' ou 'false'");
        }
    }

    private void validateFormatRecu(String valeur) {
        if (!valeur.equals("COMPACT") && !valeur.equals("DETAILLE")) {
            throw new IllegalArgumentException("Le format du reçu doit être 'COMPACT' ou 'DETAILLE'");
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Échapper les caractères spéciaux HTML et les caractères dangereux
        return input.replaceAll("[<>\"'&/\\\\]", "_")
                   .replaceAll("(?i)javascript:", "")
                   .replaceAll("(?i)data:", "")
                   .replaceAll("(?i)vbscript:", "");
    }
}