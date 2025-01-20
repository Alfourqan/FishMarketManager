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

    public Map<String, String> chargerConfigurations() {
        configurations.clear();
        configCache.clear();
        String sql = "SELECT * FROM configurations ORDER BY cle";
        Map<String, String> config = new HashMap<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    String cle = sanitizeInput(rs.getString("cle"));
                    String valeur = sanitizeInput(rs.getString("valeur"));
                    String description = sanitizeInput(rs.getString("description"));

                    if (cle.equals(ConfigurationParam.CLE_SIRET_ENTREPRISE)) {
                        System.setProperty("SKIP_SIRET_VALIDATION", "true");
                    }

                    ConfigurationParam configParam = new ConfigurationParam(
                        rs.getInt("id"),
                        cle,
                        valeur,
                        description
                    );

                    if (cle.equals(ConfigurationParam.CLE_SIRET_ENTREPRISE)) {
                        System.clearProperty("SKIP_SIRET_VALIDATION");
                    }

                    configurations.add(configParam);
                    configCache.put(configParam.getCle(), configParam.getValeur());
                    config.put(configParam.getCle(), configParam.getValeur());
                } catch (Exception e) {
                    LOGGER.warning("Configuration invalide ignorée: " + e.getMessage());
                }
            }
            LOGGER.info("Configurations chargées: " + configurations.size() + " entrées");
            return config;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations", e);
            throw new RuntimeException("Erreur lors du chargement des configurations", e);
        }
    }

    public void sauvegarderConfigurations(Map<String, String> configurations) {
        if (configurations == null || configurations.isEmpty()) {
            throw new IllegalArgumentException("Les configurations ne peuvent pas être nulles ou vides");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            String sql = "UPDATE configurations SET valeur = ? WHERE cle = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Map.Entry<String, String> entry : configurations.entrySet()) {
                    String cle = sanitizeInput(entry.getKey());
                    String valeur = sanitizeInput(entry.getValue());

                    if (!SAFE_KEY_PATTERN.matcher(cle).matches()) {
                        throw new IllegalArgumentException("Format de clé de configuration invalide: " + cle);
                    }

                    pstmt.setString(1, valeur);
                    pstmt.setString(2, cle);
                    pstmt.addBatch();
                }

                int[] results = pstmt.executeBatch();
                conn.commit();

                // Mise à jour du cache
                configCache.clear();
                configCache.putAll(configurations);

                LOGGER.info("Configurations sauvegardées avec succès");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde des configurations", e);
            throw new RuntimeException("Erreur lors de la sauvegarde des configurations", e);
        }
    }

    public void reinitialiserConfiguration() {
        System.setProperty("SKIP_SIRET_VALIDATION", "true");

        try {
            StringBuilder sql = new StringBuilder("UPDATE configurations SET valeur = CASE cle ");
            List<String> params = new ArrayList<>();

            // Configuration des valeurs par défaut
            Map<String, String> defaultConfigs = new HashMap<>();
            defaultConfigs.put("TAUX_TVA", "20.0");
            defaultConfigs.put("TVA_ENABLED", "true");
            defaultConfigs.put("FORMAT_RECU", "COMPACT");
            defaultConfigs.put("PIED_PAGE_RECU", "Merci de votre visite !");
            defaultConfigs.put("POLICE_TITRE_RECU", "14");
            defaultConfigs.put("POLICE_TEXTE_RECU", "12");
            defaultConfigs.put("ALIGNEMENT_TITRE_RECU", "CENTRE");
            defaultConfigs.put("ALIGNEMENT_TEXTE_RECU", "GAUCHE");
            defaultConfigs.put("STYLE_BORDURE_RECU", "SIMPLE");
            defaultConfigs.put("MESSAGE_COMMERCIAL_RECU", "");
            defaultConfigs.put("AFFICHER_TVA_DETAILS", "true");
            defaultConfigs.put("COULEUR_TITRE_RECU", "#000000");
            defaultConfigs.put("COULEUR_TEXTE_RECU", "#000000");
            defaultConfigs.put("AFFICHER_CODE_BARRES", "true");
            defaultConfigs.put("POSITION_CODE_BARRES", "BOTTOM");
            defaultConfigs.put("AFFICHER_QR_CODE", "false");
            defaultConfigs.put("CONTENU_QR_CODE", "NUMERO_TICKET");
            defaultConfigs.put("AFFICHER_COORDONNEES_CLIENT", "true");
            defaultConfigs.put("STYLE_TABLEAU_PRODUITS", "GRILLE");
            defaultConfigs.put("AFFICHER_SIGNATURE", "false");
            defaultConfigs.put("POSITION_SIGNATURE", "BOTTOM");
            defaultConfigs.put("AFFICHER_CONDITIONS", "true");
            defaultConfigs.put("TEXTE_CONDITIONS", "Merci de votre confiance !");
            defaultConfigs.put("AFFICHER_POINTS_FIDELITE", "false");
            defaultConfigs.put("FORMAT_IMPRESSION", "A4");
            defaultConfigs.put("ORIENTATION_IMPRESSION", "PORTRAIT");
            defaultConfigs.put("LANGUE_TICKET", "FR");

            // Construction de la requête SQL
            for (Map.Entry<String, String> entry : defaultConfigs.entrySet()) {
                sql.append("WHEN ? THEN ? ");
                params.add(entry.getKey());
                params.add(entry.getValue());
            }
            sql.append("ELSE valeur END WHERE cle IN (");
            sql.append("?,".repeat(defaultConfigs.size() - 1)).append("?)");

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                int paramIndex = 1;
                // Ajout des paramètres pour les valeurs
                for (Map.Entry<String, String> entry : defaultConfigs.entrySet()) {
                    stmt.setString(paramIndex++, entry.getKey());
                    stmt.setString(paramIndex++, entry.getValue());
                }
                // Ajout des paramètres pour la clause IN
                for (String key : defaultConfigs.keySet()) {
                    stmt.setString(paramIndex++, key);
                }

                stmt.executeUpdate();
                LOGGER.info("Configurations réinitialisées avec succès");

                // Recharger les configurations
                chargerConfigurations();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
            throw new RuntimeException("Erreur lors de la réinitialisation des configurations", e);
        } finally {
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

    public Map<String, Object> getOptionsPersonnalisationTicket() {
        Map<String, Object> options = new HashMap<>();

        // Options d'affichage
        options.put("afficherCodeBarres", Boolean.parseBoolean(getValeur(ConfigurationParam.CLE_AFFICHER_CODE_BARRES)));
        options.put("positionCodeBarres", getValeur(ConfigurationParam.CLE_POSITION_CODE_BARRES));
        options.put("afficherQRCode", Boolean.parseBoolean(getValeur(ConfigurationParam.CLE_AFFICHER_QR_CODE)));
        options.put("contenuQRCode", getValeur(ConfigurationParam.CLE_CONTENU_QR_CODE));
        options.put("afficherCoordonneesClient", Boolean.parseBoolean(getValeur(ConfigurationParam.CLE_AFFICHER_COORDONNEES_CLIENT)));
        options.put("styleTableauProduits", getValeur(ConfigurationParam.CLE_STYLE_TABLEAU_PRODUITS));
        options.put("afficherSignature", Boolean.parseBoolean(getValeur(ConfigurationParam.CLE_AFFICHER_SIGNATURE)));
        options.put("positionSignature", getValeur(ConfigurationParam.CLE_POSITION_SIGNATURE));
        options.put("afficherConditions", Boolean.parseBoolean(getValeur(ConfigurationParam.CLE_AFFICHER_CONDITIONS)));
        options.put("texteConditions", getValeur(ConfigurationParam.CLE_TEXTE_CONDITIONS));
        options.put("afficherPointsFidelite", Boolean.parseBoolean(getValeur(ConfigurationParam.CLE_AFFICHER_POINTS_FIDELITE)));

        // Options d'impression
        options.put("formatImpression", getValeur(ConfigurationParam.CLE_FORMAT_IMPRESSION));
        options.put("orientationImpression", getValeur(ConfigurationParam.CLE_ORIENTATION_IMPRESSION));
        options.put("langueTicket", getValeur(ConfigurationParam.CLE_LANGUE_TICKET));

        return options;
    }

    public void sauvegarderOptionsPersonnalisationTicket(Map<String, String> options) {
        if (options == null) {
            throw new IllegalArgumentException("Les options ne peuvent pas être null");
        }

        options.forEach((cle, valeur) -> {
            switch (cle) {
                case "afficherCodeBarres":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_AFFICHER_CODE_BARRES, valeur, ""));
                    break;
                case "positionCodeBarres":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_POSITION_CODE_BARRES, valeur, ""));
                    break;
                case "afficherQRCode":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_AFFICHER_QR_CODE, valeur, ""));
                    break;
                case "contenuQRCode":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_CONTENU_QR_CODE, valeur, ""));
                    break;
                case "afficherCoordonneesClient":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_AFFICHER_COORDONNEES_CLIENT, valeur, ""));
                    break;
                case "styleTableauProduits":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_STYLE_TABLEAU_PRODUITS, valeur, ""));
                    break;
                case "afficherSignature":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_AFFICHER_SIGNATURE, valeur, ""));
                    break;
                case "positionSignature":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_POSITION_SIGNATURE, valeur, ""));
                    break;
                case "afficherConditions":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_AFFICHER_CONDITIONS, valeur, ""));
                    break;
                case "texteConditions":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_TEXTE_CONDITIONS, valeur, ""));
                    break;
                case "afficherPointsFidelite":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_AFFICHER_POINTS_FIDELITE, valeur, ""));
                    break;
                case "formatImpression":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_FORMAT_IMPRESSION, valeur, ""));
                    break;
                case "orientationImpression":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_ORIENTATION_IMPRESSION, valeur, ""));
                    break;
                case "langueTicket":
                    mettreAJourConfiguration(new ConfigurationParam(-1, ConfigurationParam.CLE_LANGUE_TICKET, valeur, ""));
                    break;
                default:
                    LOGGER.warning("Option de personnalisation inconnue ignorée: " + cle);
            }
        });

        LOGGER.info("Options de personnalisation des tickets mises à jour avec succès");
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