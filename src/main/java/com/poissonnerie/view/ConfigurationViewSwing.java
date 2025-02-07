
package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.controller.ConfigurationController;
import java.util.Map;

public class ConfigurationViewSwing {
    private final JPanel mainPanel;
    private final ConfigurationController configController;

    public ConfigurationViewSwing() {
        this.mainPanel = new JPanel(new BorderLayout());
        this.configController = new ConfigurationController();
        initializeComponents();
    }

    private void initializeComponents() {
        mainPanel.add(createConfigPanel(), BorderLayout.CENTER);
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // TVA Configuration
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("TVA Activée:"), gbc);

        JCheckBox tvaEnabled = new JCheckBox();
        gbc.gridx = 1;
        panel.add(tvaEnabled, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Taux TVA (%):"), gbc);

        JTextField tauxTva = new JTextField(10);
        gbc.gridx = 1;
        panel.add(tauxTva, gbc);

        // Bouton de sauvegarde
        JButton saveButton = new JButton("Sauvegarder");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        // Chargement des configurations existantes
        loadConfigurations(tvaEnabled, tauxTva);

        // Action du bouton de sauvegarde
        saveButton.addActionListener(e -> saveConfigurations(tvaEnabled, tauxTva));

        return panel;
    }

    private void loadConfigurations(JCheckBox tvaEnabled, JTextField tauxTva) {
        try {
            Map<String, String> configs = configController.getConfigurations();
            tvaEnabled.setSelected(Boolean.parseBoolean(configs.getOrDefault("TVA_ENABLED", "false")));
            tauxTva.setText(configs.getOrDefault("TAUX_TVA", "20.0"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des configurations: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveConfigurations(JCheckBox tvaEnabled, JTextField tauxTva) {
        try {
            ConfigurationParam configTVA = new ConfigurationParam(0, "TVA_ENABLED", String.valueOf(tvaEnabled.isSelected()), "");
            ConfigurationParam configTaux = new ConfigurationParam(0, "TAUX_TVA", tauxTva.getText(), "");
            configController.mettreAJourConfiguration(configTVA);
            configController.mettreAJourConfiguration(configTaux);
            
            JOptionPane.showMessageDialog(mainPanel,
                "Configurations sauvegardées avec succès",
                "Succès", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors de la sauvegarde des configurations: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
