package com.poissonnerie.view;

import com.poissonnerie.controller.ConfigurationController;
import com.poissonnerie.model.ConfigurationParam;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class ConfigurationViewSwing {
    private final JPanel mainPanel;
    private final ConfigurationController controller;
    private final Map<String, JComponent> champsSaisie;
    private final Color couleurPrincipale = new Color(33, 150, 243);
    private final Color couleurFond = new Color(245, 245, 245);
    private final Font titreFont = new Font("Segoe UI", Font.BOLD, 24);
    private final Font sousTitreFont = new Font("Segoe UI", Font.BOLD, 16);
    private final Font texteNormalFont = new Font("Segoe UI", Font.PLAIN, 14);
    private JPanel previewPanel;

    public ConfigurationViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new ConfigurationController();
        champsSaisie = new HashMap<>();

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(couleurFond);

        // Panel principal avec scroll pour les configurations
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setOpaque(false);

        // En-tête avec titre
        JPanel headerPanel = createHeaderPanel();

        // Sections de configuration
        configPanel.add(createTVASection());
        configPanel.add(Box.createVerticalStrut(15));
        configPanel.add(createEntrepriseSection());
        configPanel.add(Box.createVerticalStrut(15));
        configPanel.add(createRecusSection());

        // Panel de prévisualisation
        previewPanel = createPreviewPanel();

        // Split pane pour avoir la configuration à gauche et la prévisualisation à droite
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(configPanel),
            previewPanel);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(null);

        // Panel des boutons
        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        Border titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Prévisualisation du reçu",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            sousTitreFont
        );
        panel.setBorder(titledBorder);
        panel.setBackground(Color.WHITE);

        JTextArea previewArea = new JTextArea();
        previewArea.setFont(new Font("Monospace", Font.PLAIN, 12));
        previewArea.setEditable(false);
        previewArea.setMargin(new Insets(10, 10, 10, 10));

        // Exemple de reçu
        StringBuilder preview = new StringBuilder();
        preview.append("================================\n");
        preview.append("       POISSONNERIE XYZ         \n");
        preview.append("================================\n");
        preview.append("123 Rue de la Mer\n");
        preview.append("75001 Paris\n");
        preview.append("Tel: 01.23.45.67.89\n");
        preview.append("SIRET: 123 456 789 00001\n");
        preview.append("--------------------------------\n");
        preview.append("Article 1            10.00 EUR\n");
        preview.append("Article 2            15.00 EUR\n");
        preview.append("--------------------------------\n");
        preview.append("Sous-total:          25.00 EUR\n");
        preview.append("TVA (20%):            5.00 EUR\n");
        preview.append("TOTAL:               30.00 EUR\n");
        preview.append("================================\n");
        preview.append("Merci de votre visite !\n");
        preview.append("================================\n");

        previewArea.setText(preview.toString());
        panel.add(new JScrollPane(previewArea), BorderLayout.CENTER);

        // Bouton de mise à jour de la prévisualisation
        JButton updatePreviewButton = createStyledButton("Mettre à jour la prévisualisation", 
            MaterialDesign.MDI_REFRESH, new Color(76, 175, 80));
        updatePreviewButton.addActionListener(e -> updatePreview());
        panel.add(updatePreviewButton, BorderLayout.SOUTH);

        return panel;
    }

    private void updatePreview() {
        // Mise à jour de la prévisualisation avec les valeurs actuelles
        JTextArea previewArea = (JTextArea) ((JScrollPane) previewPanel.getComponent(0)).getViewport().getView();
        StringBuilder preview = new StringBuilder();

        // En-tête
        String nomEntreprise = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_NOM_ENTREPRISE)).getText();
        preview.append("================================\n");
        preview.append(String.format("%s%s%s\n",
            " ".repeat(3),
            nomEntreprise,
            " ".repeat(3)));
        preview.append("================================\n");

        // Informations entreprise
        String adresse = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_ADRESSE_ENTREPRISE)).getText();
        String telephone = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE)).getText();
        String siret = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_SIRET_ENTREPRISE)).getText();

        preview.append(adresse + "\n");
        preview.append("Tel: " + telephone + "\n");
        preview.append("SIRET: " + siret + "\n");
        preview.append("--------------------------------\n");

        // Exemple d'articles avec TVA
        boolean tvaEnabled = ((JCheckBox) champsSaisie.get(ConfigurationParam.CLE_TVA_ENABLED)).isSelected();
        double tauxTva = (double) ((JSpinner) champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA)).getValue();

        double sousTotal = 25.00;
        preview.append(String.format("Sous-total:%14.2f EUR\n", sousTotal));

        if (tvaEnabled) {
            double montantTva = sousTotal * (tauxTva / 100);
            preview.append(String.format("TVA (%.1f%%):%13.2f EUR\n", tauxTva, montantTva));
            preview.append(String.format("TOTAL:%18.2f EUR\n", sousTotal + montantTva));
        } else {
            preview.append(String.format("TOTAL:%18.2f EUR\n", sousTotal));
        }

        preview.append("================================\n");

        // Pied de page
        String piedPage = ((JTextArea) champsSaisie.get(ConfigurationParam.CLE_PIED_PAGE_RECU)).getText();
        preview.append(piedPage + "\n");
        preview.append("================================\n");

        previewArea.setText(preview.toString());
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);

        FontIcon settingsIcon = FontIcon.of(MaterialDesign.MDI_SETTINGS);
        settingsIcon.setIconSize(32);
        settingsIcon.setIconColor(couleurPrincipale);
        JLabel iconLabel = new JLabel(settingsIcon);

        JLabel titleLabel = new JLabel("Paramètres de l'Application");
        titleLabel.setFont(titreFont);
        titleLabel.setForeground(new Color(33, 33, 33));

        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        return headerPanel;
    }

    private JPanel createTVASection() {
        JPanel panel = createSectionPanel("Configuration TVA", MaterialDesign.MDI_PERCENT);

        // Activation TVA
        JCheckBox tvaEnabledCheck = new JCheckBox("Activer la TVA");
        tvaEnabledCheck.setFont(texteNormalFont);
        champsSaisie.put(ConfigurationParam.CLE_TVA_ENABLED, tvaEnabledCheck);

        // Taux TVA
        JSpinner tauxTvaSpinner = new JSpinner(new SpinnerNumberModel(20.0, 0.0, 100.0, 0.1));
        tauxTvaSpinner.setFont(texteNormalFont);
        champsSaisie.put(ConfigurationParam.CLE_TAUX_TVA, tauxTvaSpinner);

        // Layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(tvaEnabledCheck, gbc);

        gbc.gridy = 2;
        panel.add(new JLabel("Taux de TVA (%):"), gbc);
        gbc.gridx = 1;
        panel.add(tauxTvaSpinner, gbc);

        return panel;
    }

    private JPanel createEntrepriseSection() {
        JPanel panel = createSectionPanel("Informations de l'entreprise", MaterialDesign.MDI_DOMAIN);

        String[][] champs = {
            {ConfigurationParam.CLE_NOM_ENTREPRISE, "Nom de l'entreprise"},
            {ConfigurationParam.CLE_ADRESSE_ENTREPRISE, "Adresse"},
            {ConfigurationParam.CLE_TELEPHONE_ENTREPRISE, "Téléphone"},
            {ConfigurationParam.CLE_SIRET_ENTREPRISE, "Numéro SIRET"}
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        for (String[] champ : champs) {
            JLabel label = new JLabel(champ[1] + ":");
            label.setFont(texteNormalFont);

            JTextField textField = new JTextField(30);
            textField.setFont(texteNormalFont);
            styleTextField(textField);
            champsSaisie.put(champ[0], textField);

            panel.add(label, gbc);
            gbc.gridx = 1;
            panel.add(textField, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
        }

        return panel;
    }

    private JPanel createRecusSection() {
        JPanel panel = createSectionPanel("Personnalisation des reçus", MaterialDesign.MDI_RECEIPT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Format des reçus
        JLabel formatLabel = new JLabel("Format des reçus:");
        formatLabel.setFont(texteNormalFont);
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"COMPACT", "DETAILLE"});
        formatCombo.setFont(texteNormalFont);
        champsSaisie.put(ConfigurationParam.CLE_FORMAT_RECU, formatCombo);

        panel.add(formatLabel, gbc);
        gbc.gridx = 1;
        panel.add(formatCombo, gbc);

        // Logo
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel logoLabel = new JLabel("Logo de l'entreprise:");
        logoLabel.setFont(texteNormalFont);

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField logoPathField = new JTextField(20);
        logoPathField.setFont(texteNormalFont);
        styleTextField(logoPathField);
        champsSaisie.put(ConfigurationParam.CLE_LOGO_PATH, logoPathField);

        JButton chooseLogoButton = new JButton("Choisir...");
        chooseLogoButton.addActionListener(e -> choisirLogo(logoPathField));

        logoPanel.add(logoPathField);
        logoPanel.add(chooseLogoButton);

        panel.add(logoLabel, gbc);
        gbc.gridx = 1;
        panel.add(logoPanel, gbc);

        // En-tête et pied de page
        String[][] champs = {
            {ConfigurationParam.CLE_EN_TETE_RECU, "Message d'en-tête"},
            {ConfigurationParam.CLE_PIED_PAGE_RECU, "Message de pied de page"}
        };

        for (String[] champ : champs) {
            gbc.gridx = 0;
            gbc.gridy++;
            JLabel label = new JLabel(champ[1] + ":");
            label.setFont(texteNormalFont);

            JTextArea textArea = new JTextArea(2, 30);
            textArea.setFont(texteNormalFont);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(textArea);
            champsSaisie.put(champ[0], textArea);

            panel.add(label, gbc);
            gbc.gridx = 1;
            panel.add(scrollPane, gbc);
        }

        return panel;
    }

    private JPanel createSectionPanel(String titre, MaterialDesign icon) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            )
        ));

        // En-tête de section avec icône
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setBackground(Color.WHITE);

        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(20);
        fontIcon.setIconColor(couleurPrincipale);
        JLabel iconLabel = new JLabel(fontIcon);

        JLabel titleLabel = new JLabel(titre);
        titleLabel.setFont(sousTitreFont);

        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 15, 0);

        panel.add(headerPanel, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH, couleurPrincipale);
        JButton reinitialiserBtn = createStyledButton("Réinitialiser", MaterialDesign.MDI_RESTORE, new Color(244, 67, 54));
        JButton sauvegarderBtn = createStyledButton("Sauvegarder", MaterialDesign.MDI_CONTENT_SAVE, new Color(76, 175, 80));

        actualiserBtn.addActionListener(e -> actualiserConfigurations());
        reinitialiserBtn.addActionListener(e -> reinitialiserConfigurations());
        sauvegarderBtn.addActionListener(e -> sauvegarderConfigurations());

        buttonPanel.add(actualiserBtn);
        buttonPanel.add(reinitialiserBtn);
        buttonPanel.add(sauvegarderBtn);

        return buttonPanel;
    }

    private void choisirLogo(JTextField logoPathField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));

        if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            logoPathField.setText(file.getAbsolutePath());
        }
    }

    private void actualiserConfigurations() {
        try {
            mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            loadData();
            showSuccessMessage("Configurations actualisées avec succès");
            updatePreview(); // Update preview after loading data
        } catch (Exception ex) {
            showErrorMessage("Erreur lors de l'actualisation : " + ex.getMessage());
        } finally {
            mainPanel.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void loadData() {
        try {
            System.out.println("Chargement des configurations...");
            controller.chargerConfigurations();

            for (Map.Entry<String, JComponent> entry : champsSaisie.entrySet()) {
                String valeur = controller.getValeur(entry.getKey());
                JComponent composant = entry.getValue();

                if (composant instanceof JTextField) {
                    ((JTextField) composant).setText(valeur);
                } else if (composant instanceof JTextArea) {
                    ((JTextArea) composant).setText(valeur);
                } else if (composant instanceof JCheckBox) {
                    ((JCheckBox) composant).setSelected(Boolean.parseBoolean(valeur));
                } else if (composant instanceof JSpinner) {
                    try {
                        ((JSpinner) composant).setValue(Double.parseDouble(valeur));
                    } catch (NumberFormatException e) {
                        ((JSpinner) composant).setValue(20.0);
                    }
                } else if (composant instanceof JComboBox) {
                    ((JComboBox<?>) composant).setSelectedItem(valeur);
                }
            }

            System.out.println("Configurations chargées avec succès");
            updatePreview(); // Update preview after loading data
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des configurations: " + e.getMessage());
            showErrorMessage("Erreur lors du chargement des configurations : " + e.getMessage());
        }
    }

    private void sauvegarderConfigurations() {
        try {
            if (!validerChamps()) {
                return;
            }

            System.out.println("Début de la sauvegarde des configurations...");
            boolean hasChanges = false;

            for (Map.Entry<String, JComponent> entry : champsSaisie.entrySet()) {
                String cle = entry.getKey();
                JComponent composant = entry.getValue();
                String nouvelleValeur = "";

                if (composant instanceof JTextField) {
                    nouvelleValeur = ((JTextField) composant).getText().trim();
                } else if (composant instanceof JTextArea) {
                    nouvelleValeur = ((JTextArea) composant).getText().trim();
                } else if (composant instanceof JCheckBox) {
                    nouvelleValeur = String.valueOf(((JCheckBox) composant).isSelected());
                } else if (composant instanceof JSpinner) {
                    nouvelleValeur = String.valueOf(((JSpinner) composant).getValue());
                } else if (composant instanceof JComboBox) {
                    nouvelleValeur = String.valueOf(((JComboBox<?>) composant).getSelectedItem());
                }

                String ancienneValeur = controller.getValeur(cle);
                if (!nouvelleValeur.equals(ancienneValeur)) {
                    System.out.println("Mise à jour de la configuration: " + cle + " = " + nouvelleValeur);
                    ConfigurationParam config = new ConfigurationParam(0, cle, nouvelleValeur, "");
                    controller.mettreAJourConfiguration(config);
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                showSuccessMessage("Les paramètres ont été sauvegardés avec succès");
                loadData();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            showErrorMessage("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    private boolean validerChamps() {
        // Validation du taux de TVA
        JSpinner tauxTVASpinner = (JSpinner) champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA);
        double tauxTVA = (double) tauxTVASpinner.getValue();
        if (tauxTVA < 0 || tauxTVA > 100) {
            showErrorMessage("Le taux de TVA doit être compris entre 0 et 100");
            return false;
        }

        // Validation du numéro de téléphone
        JTextField telephoneField = (JTextField) champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE);
        String telephone = telephoneField.getText().trim();
        if (!telephone.isEmpty() && !telephone.matches("^[0-9+\\-\\s]*$")) {
            showErrorMessage("Le numéro de téléphone contient des caractères invalides");
            return false;
        }

        // Validation du SIRET
        JTextField siretField = (JTextField) champsSaisie.get(ConfigurationParam.CLE_SIRET_ENTREPRISE);
        String siret = siretField.getText().trim();
        if (!siret.isEmpty() && !siret.matches("^[0-9]{14}$")) {
            showErrorMessage("Le numéro SIRET doit contenir exactement 14 chiffres");
            return false;
        }

        // Validation du format des reçus
        JComboBox<?> formatCombo = (JComboBox<?>) champsSaisie.get(ConfigurationParam.CLE_FORMAT_RECU);
        String formatRecu = formatCombo.getSelectedItem().toString();
        if (!formatRecu.equals("COMPACT") && !formatRecu.equals("DETAILLE")) {
            showErrorMessage("Le format des reçus doit être 'COMPACT' ou 'DETAILLE'");
            return false;
        }

        return true;
    }

    private void reinitialiserConfigurations() {
        if (showConfirmDialog("Êtes-vous sûr de vouloir réinitialiser tous les paramètres ?")) {
            try {
                System.out.println("Réinitialisation des configurations...");
                controller.reinitialiserConfigurations();
                loadData();
                showSuccessMessage("Les paramètres ont été réinitialisés avec succès");
            } catch (Exception e) {
                System.err.println("Erreur lors de la réinitialisation: " + e.getMessage());
                showErrorMessage("Erreur lors de la réinitialisation : " + e.getMessage());
            }
        }
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode, Color color) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);

        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void styleTextField(JTextField textField) {
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Succès",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Erreur",
            JOptionPane.ERROR_MESSAGE);
    }

    private boolean showConfirmDialog(String message) {
        return JOptionPane.showConfirmDialog(mainPanel, message, "Confirmation",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}