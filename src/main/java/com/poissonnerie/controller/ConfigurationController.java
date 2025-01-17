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

public class ConfigurationController {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationController.class.getName());
    private final List<ConfigurationParam> configurations = new ArrayList<>();
    private final Map<String, String> configCache = new HashMap<>();

    public void chargerConfigurations() {
        configurations.clear();
        configCache.clear();
        String sql = "SELECT * FROM configurations ORDER BY cle";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    ConfigurationParam config = new ConfigurationParam(
                        rs.getInt("id"),
                        sanitizeInput(rs.getString("cle")),
                        sanitizeInput(rs.getString("valeur")),
                        sanitizeInput(rs.getString("description"))
                    );
                    configurations.add(config);
                    configCache.put(config.getCle(), config.getValeur());
                }
                conn.commit();
                LOGGER.info("Configurations chargées: " + configurations.size() + " entrées");
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations", e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations", e);
            throw new RuntimeException("Erreur lors du chargement des configurations", e);
        }
    }

    public void mettreAJourConfiguration(ConfigurationParam config) {
        validateConfiguration(config);

        String sql = "UPDATE configurations SET valeur = ? WHERE cle = ?";
        LOGGER.info("Mise à jour de la configuration: " + config.getCle() + " = " + config.getValeur());

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
                throw e;
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
                    "WHEN cle = 'EN_TETE_RECU' THEN 'Votre Poissonnerie de Confiance' " +
                    "ELSE '' END " +
                    "WHERE cle IN ('TAUX_TVA', 'TVA_ENABLED', 'FORMAT_RECU', 'PIED_PAGE_RECU', " +
                    "'EN_TETE_RECU', 'NOM_ENTREPRISE', 'ADRESSE_ENTREPRISE', 'TELEPHONE_ENTREPRISE', " +
                    "'SIRET_ENTREPRISE', 'LOGO_PATH')";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int rowsUpdated = stmt.executeUpdate();
                conn.commit();
                LOGGER.info("Configurations réinitialisées: " + rowsUpdated + " entrées mises à jour");
                chargerConfigurations(); // Recharger les configurations après réinitialisation
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
            throw new RuntimeException("Erreur lors de la réinitialisation des configurations", e);
        }
    }

    public String getValeur(String cle) {
        return sanitizeInput(configCache.getOrDefault(cle, ""));
    }

    public List<ConfigurationParam> getConfigurations() {
        return new ArrayList<>(configurations); // Retourne une copie de la liste pour éviter les modifications externes
    }

    public double getTauxTVA() {
        String taux = getValeur(ConfigurationParam.CLE_TAUX_TVA);
        try {
            double tauxTVA = Double.parseDouble(taux);
            if (tauxTVA < 0 || tauxTVA > 100) {
                LOGGER.warning("Taux TVA invalide: " + tauxTVA);
                return 0.0;
            }
            return tauxTVA;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Erreur de conversion du taux TVA", e);
            return 0.0;
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

    private void validateConfiguration(ConfigurationParam config) {
        if (config == null) {
            throw new IllegalArgumentException("La configuration ne peut pas être null");
        }
        if (config.getCle() == null || config.getCle().trim().isEmpty()) {
            throw new IllegalArgumentException("La clé de configuration ne peut pas être vide");
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
            // Ajoutez d'autres validations spécifiques ici
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
        // Échapper les caractères spéciaux HTML
        return input.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("/", "&#x2F;");
    }
}