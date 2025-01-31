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
import javax.swing.SwingUtilities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ConfigurationController {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationController.class.getName());
    private final List<ConfigurationParam> configurations = new ArrayList<>();
    private final Map<String, String> configCache = new HashMap<>();
    private static final int MAX_RETRIES = 3;
    private static final Pattern SAFE_KEY_PATTERN = Pattern.compile("^[A-Z_]{1,50}$");
    private static final SecureRandom secureRandom = new SecureRandom();
    private boolean isLoading = false;
    private final Gson gson;

    public ConfigurationController() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    public Map<String, String> getConfigurations() {
        if (configCache.isEmpty()) {
            chargerConfigurations();
        }
        return new HashMap<>(configCache);
    }

    public void chargerConfigurations() {
        if (isLoading) {
            LOGGER.info("Chargement déjà en cours, ignoré");
            return;
        }

        isLoading = true;
        configurations.clear();
        configCache.clear();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM configurations ORDER BY cle")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    String cle = sanitizeInput(rs.getString("cle"));
                    String valeur = sanitizeInput(rs.getString("valeur"));
                    String description = sanitizeInput(rs.getString("description"));

                    ConfigurationParam config = new ConfigurationParam(
                        rs.getInt("id"),
                        cle,
                        valeur,
                        description
                    );

                    configurations.add(config);
                    configCache.put(config.getCle(), config.getValeur());
                } catch (Exception e) {
                    LOGGER.warning("Configuration invalide ignorée: " + e.getMessage());
                }
            }
            LOGGER.info("Configurations chargées: " + configurations.size() + " entrées");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations", e);
            throw new RuntimeException("Erreur lors du chargement des configurations", e);
        } finally {
            isLoading = false;
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
        try {
            String sql = "UPDATE configurations SET valeur = CASE " +
                        "WHEN cle = ? THEN '20.0' " +
                        "WHEN cle = ? THEN 'true' " +
                        "WHEN cle = ? THEN 'COMPACT' " +
                        "WHEN cle = ? THEN 'Merci de votre visite !' " +
                        "WHEN cle = ? THEN '12345678901234' " +
                        "ELSE valeur END " +
                        "WHERE cle IN (?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                String[] params = {
                    ConfigurationParam.CLE_TAUX_TVA,
                    ConfigurationParam.CLE_TVA_ENABLED,
                    ConfigurationParam.CLE_FORMAT_RECU,
                    ConfigurationParam.CLE_PIED_PAGE_RECU,
                    ConfigurationParam.CLE_SIRET_ENTREPRISE
                };

                for (int i = 0; i < params.length; i++) {
                    stmt.setString(i + 1, params[i]);
                    stmt.setString(i + 6, params[i]);
                }

                stmt.executeUpdate();
                LOGGER.info("Configurations réinitialisées avec succès");

                chargerConfigurations();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
            throw new RuntimeException("Erreur lors de la réinitialisation des configurations", e);
        }
    }

    public void sauvegarderConfigurations(List<ConfigurationParam> configs) {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("La liste des configurations ne peut pas être vide");
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String sql = "UPDATE configurations SET valeur = ? WHERE cle = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (ConfigurationParam config : configs) {
                    validateConfiguration(config);
                    pstmt.setString(1, sanitizeInput(config.getValeur()));
                    pstmt.setString(2, sanitizeInput(config.getCle()));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
            chargerConfigurations();
            LOGGER.info("Configurations sauvegardées avec succès");
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du rollback", ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde des configurations", e);
            throw new RuntimeException("Erreur lors de la sauvegarde des configurations", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture de la connexion", e);
                }
            }
        }
    }

    public void exporterConfigurations(File file) throws IOException {
        if (configCache.isEmpty()) {
            chargerConfigurations();
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            Map<String, String> configMap = configurations.stream()
                .collect(Collectors.toMap(
                    ConfigurationParam::getCle,
                    ConfigurationParam::getValeur
                ));
            gson.toJson(configMap, writer);
            LOGGER.info("Configurations exportées vers: " + file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'exportation des configurations", e);
            throw new IOException("Erreur lors de l'exportation des configurations : " + e.getMessage(), e);
        }
    }

    public void importerConfigurations(File file) throws IOException {
        try (Reader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            Map<String, String> importedConfigs = gson.fromJson(reader,
                new TypeToken<Map<String, String>>(){}.getType());

            if (importedConfigs == null || importedConfigs.isEmpty()) {
                throw new IllegalArgumentException("Le fichier d'import est vide ou mal formaté");
            }

            List<ConfigurationParam> configsToUpdate = new ArrayList<>();
            for (Map.Entry<String, String> entry : importedConfigs.entrySet()) {
                String cle = entry.getKey();
                String valeur = entry.getValue();

                if (!SAFE_KEY_PATTERN.matcher(cle).matches()) {
                    LOGGER.warning("Clé invalide ignorée: " + cle);
                    continue;
                }

                try {
                    valeur = ConfigurationParam.validateValeur(valeur, cle);
                    configsToUpdate.add(new ConfigurationParam(0, cle, valeur, ""));
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Valeur invalide pour la clé " + cle + ": " + e.getMessage());
                }
            }

            if (!configsToUpdate.isEmpty()) {
                sauvegarderConfigurations(configsToUpdate);
                LOGGER.info("Configurations importées avec succès depuis: " + file.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("Aucune configuration valide trouvée dans le fichier");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'importation des configurations", e);
            throw new IOException("Erreur lors de l'importation des configurations : " + e.getMessage(), e);
        }
    }

    public String getValeur(String cle) {
        if (!SAFE_KEY_PATTERN.matcher(cle).matches()) {
            LOGGER.warning("Tentative d'accès avec une clé invalide: " + cle);
            return "";
        }
        return configCache.getOrDefault(cle, "");
    }

    public List<ConfigurationParam> getConfigurationsList() {
        return new ArrayList<>(configurations);
    }

    public double getTauxTVA() {
        String taux = getValeur(ConfigurationParam.CLE_TAUX_TVA);
        try {
            double tauxTVA = Double.parseDouble(taux);
            if (tauxTVA < 0 || tauxTVA > 100) {
                LOGGER.warning("Taux TVA invalide: " + tauxTVA);
                return 20.0;
            }
            return tauxTVA;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Erreur de conversion du taux TVA", e);
            return 20.0;
        }
    }

    private void validateConfiguration(ConfigurationParam config) {
        if (config == null) {
            throw new IllegalArgumentException("La configuration ne peut pas être null");
        }
        if (!SAFE_KEY_PATTERN.matcher(config.getCle()).matches() && !config.getCle().equals("TELEPHONE")) {
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
        return input.replaceAll("[<>\"'%;)(&+\\[\\]{}]", "_")
                   .replaceAll("(?i)javascript:", "")
                   .replaceAll("(?i)data:", "")
                   .replaceAll("(?i)vbscript:", "");
    }
}