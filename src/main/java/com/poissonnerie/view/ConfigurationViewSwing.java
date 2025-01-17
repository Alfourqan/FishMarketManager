package com.poissonnerie.view;

import com.poissonnerie.controller.ConfigurationController;
import com.poissonnerie.model.ConfigurationParam;
import javax.swing.*;
import java.awt.*;
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
        loadData(); // Chargement initial des données
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
            {ConfigurationParam.CLE_PIED_PAGE_RECU, "Message de pied de page", "Merci de votre visite !"}
        }));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton reinitialiserBtn = new JButton("Réinitialiser");
        JButton sauvegarderBtn = new JButton("Sauvegarder");

        reinitialiserBtn.addActionListener(e -> reinitialiserConfigurations());
        sauvegarderBtn.addActionListener(e -> sauvegarderConfigurations());

        buttonPanel.add(reinitialiserBtn);
        buttonPanel.add(sauvegarderBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String titre, String[][] champs) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(titre),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 5, 2, 5);

        for (String[] champ : champs) {
            JLabel label = new JLabel(champ[1] + ":");
            label.setPreferredSize(new Dimension(150, label.getPreferredSize().height));

            JTextField textField = new JTextField(20);
            textField.setName(champ[0]); // Utiliser le nom pour identifier le champ
            champsSaisie.put(champ[0], textField);

            gbc.gridx = 0;
            panel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(textField, gbc);

            gbc.gridy++;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
        }

        return panel;
    }

    private void loadData() {
        try {
            controller.chargerConfigurations();
            for (Map.Entry<String, JTextField> entry : champsSaisie.entrySet()) {
                String valeur = controller.getValeur(entry.getKey());
                if (valeur != null) {
                    SwingUtilities.invokeLater(() -> entry.getValue().setText(valeur));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors du chargement des configurations : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE)
            );
        }
    }

    private void sauvegarderConfigurations() {
        try {
            boolean hasChanges = false;

            for (Map.Entry<String, JTextField> entry : champsSaisie.entrySet()) {
                String cle = entry.getKey();
                String nouvelleValeur = entry.getValue().getText().trim();
                String ancienneValeur = controller.getValeur(cle);

                if (!nouvelleValeur.equals(ancienneValeur)) {
                    ConfigurationParam config = new ConfigurationParam(0, cle, nouvelleValeur, "");
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
                loadData(); // Recharger les données après la réinitialisation
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