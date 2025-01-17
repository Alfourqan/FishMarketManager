package com.poissonnerie.view;

import com.poissonnerie.controller.ConfigurationController;
import com.poissonnerie.model.ConfigurationParam;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.HashMap;

public class ConfigurationViewSwing {
    private final JPanel mainPanel;
    private final ConfigurationController controller;
    private final Map<String, JTextField> champsSaisie;

    public ConfigurationViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new ConfigurationController();
        champsSaisie = new HashMap<>();

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel principal avec scroll
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Section TVA
        contentPanel.add(createSectionPanel("Configuration TVA", new String[][]{
            {ConfigurationParam.CLE_TAUX_TVA, "Taux de TVA (%)", "20.0"}
        }));

        // Section Informations Entreprise
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createSectionPanel("Informations de l'entreprise", new String[][]{
            {ConfigurationParam.CLE_NOM_ENTREPRISE, "Nom de l'entreprise", ""},
            {ConfigurationParam.CLE_ADRESSE_ENTREPRISE, "Adresse", ""},
            {ConfigurationParam.CLE_TELEPHONE_ENTREPRISE, "Téléphone", ""}
        }));

        // Section Personnalisation Reçus
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createSectionPanel("Personnalisation des reçus", new String[][]{
            {ConfigurationParam.CLE_PIED_PAGE_RECU, "Message de pied de page", ""}
        }));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reinitialiserBtn = new JButton("Réinitialiser");
        JButton sauvegarderBtn = new JButton("Sauvegarder");

        reinitialiserBtn.addActionListener(e -> reinitialiserConfigurations());
        sauvegarderBtn.addActionListener(e -> sauvegarderConfigurations());

        buttonPanel.add(reinitialiserBtn);
        buttonPanel.add(sauvegarderBtn);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String titre, String[][] champs) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(titre),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        for (String[] champ : champs) {
            JPanel rowPanel = new JPanel(new BorderLayout(10, 0));

            JLabel label = new JLabel(champ[1] + ":");
            label.setPreferredSize(new Dimension(150, label.getPreferredSize().height));
            rowPanel.add(label, BorderLayout.WEST);

            JTextField textField = new JTextField(20);
            textField.putClientProperty("cle", champ[0]);
            champsSaisie.put(champ[0], textField);
            rowPanel.add(textField, BorderLayout.CENTER);

            panel.add(rowPanel);
            panel.add(Box.createVerticalStrut(5));
        }

        return panel;
    }

    private void loadData() {
        try {
            controller.chargerConfigurations();
            for (Map.Entry<String, JTextField> entry : champsSaisie.entrySet()) {
                String valeur = controller.getValeur(entry.getKey());
                entry.getValue().setText(valeur);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des configurations : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sauvegarderConfigurations() {
        try {
            boolean hasChanges = false;

            for (ConfigurationParam config : controller.getConfigurations()) {
                JTextField field = champsSaisie.get(config.getCle());
                if (field != null && !config.getValeur().equals(field.getText().trim())) {
                    config.setValeur(field.getText().trim());
                    controller.mettreAJourConfiguration(config);
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Les paramètres ont été sauvegardés avec succès",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur de validation : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors de la sauvegarde : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reinitialiserConfigurations() {
        if (JOptionPane.showConfirmDialog(mainPanel,
            "Êtes-vous sûr de vouloir réinitialiser tous les paramètres ?",
            "Confirmation",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                controller.reinitialiserConfigurations();
                loadData();
                JOptionPane.showMessageDialog(mainPanel,
                    "Les paramètres ont été réinitialisés avec succès",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de la réinitialisation : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}