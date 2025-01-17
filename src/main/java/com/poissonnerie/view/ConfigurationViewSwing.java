package com.poissonnerie.view;

import com.poissonnerie.controller.ConfigurationController;
import com.poissonnerie.model.ConfigurationParam;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.swing.FontIcon;

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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(245, 245, 245));

        // En-tête avec titre
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("Paramètres de l'Application");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(33, 33, 33));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Panel principal avec scroll
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Section TVA avec style moderne
        contentPanel.add(createSectionPanel("Configuration TVA", new String[][]{
            {ConfigurationParam.CLE_TAUX_TVA, "Taux de TVA (%)", "20.0"}
        }, MaterialDesignI.PERCENT));

        // Section Informations Entreprise
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(createSectionPanel("Informations de l'entreprise", new String[][]{
            {ConfigurationParam.CLE_NOM_ENTREPRISE, "Nom de l'entreprise", ""},
            {ConfigurationParam.CLE_ADRESSE_ENTREPRISE, "Adresse", ""},
            {ConfigurationParam.CLE_TELEPHONE_ENTREPRISE, "Téléphone", ""}
        }, MaterialDesignI.DOMAIN));

        // Section Personnalisation Reçus
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(createSectionPanel("Personnalisation des reçus", new String[][]{
            {ConfigurationParam.CLE_PIED_PAGE_RECU, "Message de pied de page", "Merci de votre visite !"}
        }, MaterialDesignI.RECEIPT));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(245, 245, 245));
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel des boutons avec style moderne
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesignI.REFRESH, new Color(33, 150, 243));
        JButton reinitialiserBtn = createStyledButton("Réinitialiser", MaterialDesignI.RESTORE, new Color(244, 67, 54));
        JButton sauvegarderBtn = createStyledButton("Sauvegarder", MaterialDesignI.CONTENT_SAVE, new Color(76, 175, 80));

        actualiserBtn.addActionListener(e -> {
            try {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                loadData();
                showSuccessMessage("Configurations actualisées avec succès");
            } catch (Exception ex) {
                showErrorMessage("Erreur lors de l'actualisation : " + ex.getMessage());
            } finally {
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }
        });

        reinitialiserBtn.addActionListener(e -> reinitialiserConfigurations());
        sauvegarderBtn.addActionListener(e -> sauvegarderConfigurations());

        buttonPanel.add(actualiserBtn);
        buttonPanel.add(reinitialiserBtn);
        buttonPanel.add(sauvegarderBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String titre, String[][] champs, MaterialDesignI icon) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
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
        fontIcon.setIconColor(new Color(33, 150, 243));
        JLabel iconLabel = new JLabel(fontIcon);
        JLabel titleLabel = new JLabel(titre);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
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

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        for (String[] champ : champs) {
            JLabel label = new JLabel(champ[1] + ":");
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            label.setPreferredSize(new Dimension(150, label.getPreferredSize().height));

            JTextField textField = new JTextField(30);
            textField.setName(champ[0]);
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            champsSaisie.put(champ[0], textField);

            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(textField, gbc);

            gbc.gridy++;
        }

        return panel;
    }

    private JButton createStyledButton(String text, MaterialDesignI iconCode, Color color) {
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

        // Effet de survol
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

    private void loadData() {
        try {
            System.out.println("Chargement des configurations...");
            controller.chargerConfigurations();
            for (Map.Entry<String, JTextField> entry : champsSaisie.entrySet()) {
                String valeur = controller.getValeur(entry.getKey());
                System.out.println("Configuration chargée: " + entry.getKey() + " = " + valeur);
                entry.getValue().setText(valeur);
            }
            System.out.println("Configurations chargées avec succès");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement des configurations: " + e.getMessage());
            showErrorMessage("Erreur lors du chargement des configurations : " + e.getMessage());
        }
    }

    private boolean validerChamps() {
        // Validation du taux de TVA
        String tauxTVA = champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA).getText().trim();
        try {
            double tva = Double.parseDouble(tauxTVA);
            if (tva < 0 || tva > 100) {
                showErrorMessage("Le taux de TVA doit être un nombre entre 0 et 100");
                return false;
            }
        } catch (NumberFormatException e) {
            showErrorMessage("Le taux de TVA doit être un nombre valide");
            return false;
        }

        // Validation du numéro de téléphone
        String telephone = champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE).getText().trim();
        if (!telephone.isEmpty() && !telephone.matches("^[0-9+\\-\\s]*$")) {
            showErrorMessage("Le numéro de téléphone contient des caractères invalides");
            return false;
        }

        return true;
    }

    private void sauvegarderConfigurations() {
        try {
            if (!validerChamps()) {
                return;
            }

            System.out.println("Début de la sauvegarde des configurations...");
            boolean hasChanges = false;
            for (Map.Entry<String, JTextField> entry : champsSaisie.entrySet()) {
                String cle = entry.getKey();
                String nouvelleValeur = entry.getValue().getText().trim();
                String ancienneValeur = controller.getValeur(cle);

                if (!nouvelleValeur.equals(ancienneValeur)) {
                    System.out.println("Mise à jour de la configuration: " + cle + " = " + nouvelleValeur);
                    ConfigurationParam config = new ConfigurationParam(0, cle, nouvelleValeur, "");
                    controller.mettreAJourConfiguration(config);
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                System.out.println("Configurations sauvegardées avec succès");
                showSuccessMessage("Les paramètres ont été sauvegardés avec succès");
                loadData(); // Recharger les données pour refléter les changements
            } else {
                System.out.println("Aucun changement à sauvegarder");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            showErrorMessage("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    private void reinitialiserConfigurations() {
        if (showConfirmDialog("Êtes-vous sûr de vouloir réinitialiser tous les paramètres ?")) {
            try {
                System.out.println("Réinitialisation des configurations...");
                controller.reinitialiserConfigurations();
                loadData(); // Recharger les données après la réinitialisation
                System.out.println("Configurations réinitialisées avec succès");
                showSuccessMessage("Les paramètres ont été réinitialisés avec succès");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Erreur lors de la réinitialisation: " + e.getMessage());
                showErrorMessage("Erreur lors de la réinitialisation : " + e.getMessage());
            }
        }
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