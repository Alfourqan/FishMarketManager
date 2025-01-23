package com.poissonnerie.view;

import com.poissonnerie.controller.ConfigurationController;
import com.poissonnerie.model.ConfigurationParam;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.swing.text.JTextComponent;

public class ConfigurationViewSwing {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationViewSwing.class.getName());
    private final JPanel mainPanel;
    private final ConfigurationController controller;
    private final Map<String, JComponent> champsSaisie;
    private final Color couleurPrincipale = new Color(33, 150, 243);
    private final Color couleurFond = new Color(245, 245, 245);
    private final Font titreFont = new Font("Segoe UI", Font.BOLD, 24);
    private final Font sousTitreFont = new Font("Segoe UI", Font.BOLD, 16);
    private final Font texteNormalFont = new Font("Segoe UI", Font.PLAIN, 14);
    private JPanel previewPanel;
    private static final Set<String> FORMATS_IMAGE_AUTORISES = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
    private static final long TAILLE_MAX_LOGO = 1024 * 1024; // 1MB
    private static final ExecutorService previewExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PreviewUpdater");
        t.setDaemon(true);
        return t;
    });
    private Timer previewUpdateTimer;

    public ConfigurationViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new ConfigurationController();
        champsSaisie = new HashMap<>();
        SwingUtilities.invokeLater(this::initializeComponents);
        Runtime.getRuntime().addShutdownHook(new Thread(previewExecutor::shutdown));
    }

    private void styleTextField(JTextField textField) {
        textField.setFont(texteNormalFont);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private void styleTextArea(JTextArea textArea) {
        textArea.setFont(texteNormalFont);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private JButton createStyledButton(String text, MaterialDesign icon, Color color) {
        JButton button = new JButton(text);
        button.setFont(texteNormalFont);

        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(16);
        fontIcon.setIconColor(color);
        button.setIcon(fontIcon);

        button.setBackground(Color.WHITE);
        button.setForeground(color);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                button.setForeground(Color.WHITE);
                fontIcon.setIconColor(Color.WHITE);
                button.setIcon(fontIcon);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setForeground(color);
                fontIcon.setIconColor(color);
                button.setIcon(fontIcon);
            }
        });

        return button;
    }

    private void showErrorMessage(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(mainPanel, message, "Erreur", JOptionPane.ERROR_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(mainPanel, message, "Erreur", JOptionPane.ERROR_MESSAGE)
            );
        }
    }

    private void showSuccessMessage(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            JOptionPane.showMessageDialog(mainPanel, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(mainPanel, message, "Succès", JOptionPane.INFORMATION_MESSAGE)
            );
        }
    }

    private void setCursor(Cursor cursor) {
        if (SwingUtilities.isEventDispatchThread()) {
            mainPanel.setCursor(cursor);
            if (previewPanel != null) {
                previewPanel.setCursor(cursor);
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                mainPanel.setCursor(cursor);
                if (previewPanel != null) {
                    previewPanel.setCursor(cursor);
                }
            });
        }
    }
    private boolean validerChamps() {
        List<String> erreurs = new ArrayList<>();

        // Validation du SIRET si non désactivée
        if (System.getProperty("SKIP_SIRET_VALIDATION") == null) {
            String siret = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_SIRET_ENTREPRISE)).getText().trim();
            if (!siret.isEmpty() && !siret.matches("\\d{14}")) {
                erreurs.add("Le numéro SIRET doit contenir exactement 14 chiffres");
            }
        }

        // Validation du téléphone
        String telephone = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE)).getText().trim();
        if (!telephone.isEmpty() && !telephone.matches("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,4}$")) {
            erreurs.add("Format de téléphone invalide");
        }

        // Validation de la TVA
        if (((JCheckBox) champsSaisie.get(ConfigurationParam.CLE_TVA_ENABLED)).isSelected()) {
            double tva = (double) ((JSpinner) champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA)).getValue();
            if (tva < 0 || tva > 100) {
                erreurs.add("Le taux de TVA doit être compris entre 0 et 100");
            }
        }

        // Validation des tailles de police
        int policeTitre = (int) ((JSpinner) champsSaisie.get(ConfigurationParam.CLE_POLICE_TITRE_RECU)).getValue();
        int policeTexte = (int) ((JSpinner) champsSaisie.get(ConfigurationParam.CLE_POLICE_TEXTE_RECU)).getValue();

        if (policeTitre < 8 || policeTitre > 24) {
            erreurs.add("La taille de la police du titre doit être entre 8 et 24");
        }
        if (policeTexte < 8 || policeTexte > 20) {
            erreurs.add("La taille de la police du texte doit être entre 8 et 20");
        }

        // Validation des caractères spéciaux
        for (Map.Entry<String, JComponent> entry : champsSaisie.entrySet()) {
            JComponent composant = entry.getValue();
            if (composant instanceof JTextComponent) {
                String texte = ((JTextComponent) composant).getText();
                if (contientCaracteresSpeciaux(texte)) {
                    erreurs.add("Les caractères spéciaux dangereux ne sont pas autorisés dans les champs de texte");
                    break;
                }
            }
        }

        if (!erreurs.isEmpty()) {
            showErrorMessage("Erreurs de validation :\n- " + String.join("\n- ", erreurs));
            return false;
        }

        return true;
    }

    private boolean contientCaracteresSpeciaux(String input) {
        if (input == null) return false;

        String[] motifsSuspects = {
            "<script>", "javascript:", "vbscript:",
            "onload=", "onerror=", "onclick=",
            "data:", "file:", "ftp:", "http:", "https:",
            "%00", "\\x00", "\\u0000"
        };

        String inputLower = input.toLowerCase();
        for (String motif : motifsSuspects) {
            if (inputLower.contains(motif.toLowerCase())) {
                return true;
            }
        }

        // Validation supplémentaire pour les caractères de contrôle
        return input.chars().anyMatch(ch -> Character.isISOControl(ch) && !Character.isWhitespace(ch));
    }
    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(couleurFond);

        // Initialisation des composants en arrière-plan
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                initializeFields();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    completeInitialization();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation", e);
                    showErrorMessage("Erreur d'initialisation : " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // Méthodes d'initialisation des composants ComboBox
    private JComboBox<String> createFormatRecuComboBox() {
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"COMPACT", "DETAILLE"});
        champsSaisie.put(ConfigurationParam.CLE_FORMAT_RECU, comboBox);
        return comboBox;
    }

    private JComboBox<String> createBordureRecuComboBox() {
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"SIMPLE", "DOUBLE", "POINTILLES"});
        champsSaisie.put(ConfigurationParam.CLE_STYLE_BORDURE_RECU, comboBox);
        return comboBox;
    }

    private JComboBox<String> createAlignementComboBox(String key) {
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"GAUCHE", "CENTRE", "DROITE"});
        champsSaisie.put(key, comboBox);
        return comboBox;
    }

    private void initializeFields() {
        // Initialisation des champs de base
        champsSaisie.put(ConfigurationParam.CLE_NOM_ENTREPRISE, new JTextField());
        champsSaisie.put(ConfigurationParam.CLE_ADRESSE_ENTREPRISE, new JTextField());
        champsSaisie.put(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE, new JTextField());
        champsSaisie.put(ConfigurationParam.CLE_SIRET_ENTREPRISE, new JTextField());
        champsSaisie.put(ConfigurationParam.CLE_LOGO_PATH, new JTextField());
        champsSaisie.put(ConfigurationParam.CLE_MESSAGE_COMMERCIAL_RECU, new JTextArea());
        champsSaisie.put(ConfigurationParam.CLE_EN_TETE_RECU, new JTextArea());
        champsSaisie.put(ConfigurationParam.CLE_PIED_PAGE_RECU, new JTextArea());
        champsSaisie.put(ConfigurationParam.CLE_INFO_SUPPLEMENTAIRE_RECU, new JTextArea());

        // Configuration TVA
        JCheckBox tvaEnabledCheck = new JCheckBox("Activer la TVA");
        champsSaisie.put(ConfigurationParam.CLE_TVA_ENABLED, tvaEnabledCheck);
        JSpinner tauxTvaSpinner = new JSpinner(new SpinnerNumberModel(20.0, 0.0, 100.0, 0.1));
        champsSaisie.put(ConfigurationParam.CLE_TAUX_TVA, tauxTvaSpinner);

        // Options de mise en page
        createFormatRecuComboBox();
        createBordureRecuComboBox();
        createAlignementComboBox(ConfigurationParam.CLE_ALIGNEMENT_TITRE_RECU);
        createAlignementComboBox(ConfigurationParam.CLE_ALIGNEMENT_TEXTE_RECU);

        champsSaisie.put(ConfigurationParam.CLE_POLICE_TITRE_RECU, new JSpinner(new SpinnerNumberModel(14, 8, 24, 1)));
        champsSaisie.put(ConfigurationParam.CLE_POLICE_TEXTE_RECU, new JSpinner(new SpinnerNumberModel(12, 8, 20, 1)));
        champsSaisie.put(ConfigurationParam.CLE_AFFICHER_TVA_DETAILS, new JCheckBox("Afficher les détails de TVA"));
    }

    private void completeInitialization() {
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setOpaque(false);

        JPanel headerPanel = createHeaderPanel();

        configPanel.add(createTVASection());
        configPanel.add(Box.createVerticalStrut(15));
        configPanel.add(createEntrepriseSection());
        configPanel.add(Box.createVerticalStrut(15));
        configPanel.add(createRecusSection());

        previewPanel = createPreviewPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(configPanel),
            previewPanel);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(null);

        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Chargement initial des données
        SwingWorker<Void, Void> dataLoader = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                loadData();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    updatePreview();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du chargement des données", e);
                    showErrorMessage("Erreur de chargement : " + e.getMessage());
                }
            }
        };
        dataLoader.execute();
    }


    private void updatePreview() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updatePreview);
            return;
        }

        if (previewUpdateTimer != null && previewUpdateTimer.isRunning()) {
            previewUpdateTimer.restart();
            return;
        }

        if (previewUpdateTimer == null) {
            previewUpdateTimer = new Timer(250, e -> {
                previewExecutor.submit(this::updatePreviewContent);
                previewUpdateTimer.stop();
            });
            previewUpdateTimer.setRepeats(false);
        }

        previewUpdateTimer.start();
    }

    private void updatePreviewContent() {
        try {
            JTextArea previewArea = (JTextArea) ((JScrollPane) previewPanel.getComponent(0)).getViewport().getView();
            StringBuilder preview = new StringBuilder(1024); // Pré-allocation pour éviter les réallocations

            // Récupération des paramètres de style
            String styleBordure = ((JComboBox<?>) champsSaisie.get(ConfigurationParam.CLE_STYLE_BORDURE_RECU)).getSelectedItem().toString();
            String alignementTitre = ((JComboBox<?>) champsSaisie.get(ConfigurationParam.CLE_ALIGNEMENT_TITRE_RECU)).getSelectedItem().toString();
            boolean afficherTVADetails = ((JCheckBox) champsSaisie.get(ConfigurationParam.CLE_AFFICHER_TVA_DETAILS)).isSelected();

            // Définition des caractères de bordure selon le style
            String ligneBordure;
            switch (styleBordure) {
                case "DOUBLE":
                    ligneBordure = "================================";
                    break;
                case "POINTILLES":
                    ligneBordure = "--------------------------------";
                    break;
                default:
                    ligneBordure = "--------------------------------";
                    break;
            }

            // Logo
            String logoPath = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_LOGO_PATH)).getText();
            if (!logoPath.isEmpty()) {
                preview.append("[LOGO]\n");
            }

            // En-tête
            preview.append(ligneBordure).append("\n");
            String nomEntreprise = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_NOM_ENTREPRISE)).getText();
            String enTete = ((JTextArea) champsSaisie.get(ConfigurationParam.CLE_EN_TETE_RECU)).getText();

            // Alignement du titre
            if (!nomEntreprise.isEmpty()) {
                String titreAligne = alignerTexte(nomEntreprise, alignementTitre, 32);
                preview.append(titreAligne).append("\n");
            }

            if (!enTete.isEmpty()) {
                preview.append(enTete).append("\n");
            }

            preview.append(ligneBordure).append("\n");

            // Informations de l'entreprise
            String adresse = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_ADRESSE_ENTREPRISE)).getText();
            String telephone = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE)).getText();
            String siret = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_SIRET_ENTREPRISE)).getText();

            if (!adresse.isEmpty()) preview.append(adresse).append("\n");
            if (!telephone.isEmpty()) preview.append("Tel: ").append(telephone).append("\n");
            if (!siret.isEmpty()) preview.append("SIRET: ").append(siret).append("\n");

            preview.append(ligneBordure).append("\n");

            // Message commercial
            String msgCommercial = ((JTextArea) champsSaisie.get(ConfigurationParam.CLE_MESSAGE_COMMERCIAL_RECU)).getText();
            if (!msgCommercial.isEmpty()) {
                preview.append(msgCommercial).append("\n");
                preview.append(ligneBordure).append("\n");
            }

            // Articles exemple
            double sousTotal = 25.00;
            preview.append(String.format("Article 1%14.2f EUR\n", 10.00));
            preview.append(String.format("Article 2%14.2f EUR\n", 15.00));
            preview.append(ligneBordure).append("\n");
            preview.append(String.format("Sous-total:%14.2f EUR\n", sousTotal));

            // TVA
            boolean tvaEnabled = ((JCheckBox) champsSaisie.get(ConfigurationParam.CLE_TVA_ENABLED)).isSelected();
            if (tvaEnabled) {
                double tauxTva = (double) ((JSpinner) champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA)).getValue();
                double montantTva = sousTotal * (tauxTva / 100);

                if (afficherTVADetails) {
                    preview.append(String.format("Base HT:%16.2f EUR\n", sousTotal));
                    preview.append(String.format("TVA (%.1f%%):%13.2f EUR\n", tauxTva, montantTva));
                } else {
                    preview.append(String.format("TVA (%.1f%%):%13.2f EUR\n", tauxTva, montantTva));
                }
                preview.append(String.format("TOTAL:%18.2f EUR\n", sousTotal + montantTva));
            } else {
                preview.append(String.format("TOTAL:%18.2f EUR\n", sousTotal));
            }

            preview.append(ligneBordure).append("\n");

            // Pied de page
            String piedPage = ((JTextArea) champsSaisie.get(ConfigurationParam.CLE_PIED_PAGE_RECU)).getText();
            if (!piedPage.isEmpty()) {
                preview.append(piedPage).append("\n");
            } else {
                preview.append("Merci de votre visite !\n");
            }

            // Informations supplémentaires
            String infosSup = ((JTextArea) champsSaisie.get(ConfigurationParam.CLE_INFO_SUPPLEMENTAIRE_RECU)).getText();
            if (!infosSup.isEmpty()) {
                preview.append(ligneBordure).append("\n");
                preview.append(infosSup).append("\n");
            }

            preview.append(ligneBordure).append("\n");

            // Format détaillé
            String format = ((JComboBox<?>) champsSaisie.get(ConfigurationParam.CLE_FORMAT_RECU)).getSelectedItem().toString();
            if (format.equals("DETAILLE")) {
                preview.append("\nMode de paiement: ESPECES\n");
                preview.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
                preview.append("N° Ticket: #").append(String.format("%05d", 12345)).append("\n");
            }

            // Mise à jour efficace du texte
            String newPreview = preview.toString();
            if (!newPreview.equals(previewArea.getText())) {
                previewArea.setText(newPreview);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de l'aperçu", e);
            SwingUtilities.invokeLater(() ->
                showErrorMessage("Erreur lors de la mise à jour de l'aperçu : " + e.getMessage()));
        }
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

        panel.add(new JScrollPane(previewArea), BorderLayout.CENTER);

        JButton updatePreviewButton = createStyledButton("Mettre à jour la prévisualisation",
            MaterialDesign.MDI_REFRESH, new Color(76, 175, 80));
        updatePreviewButton.addActionListener(e -> updatePreview());
        panel.add(updatePreviewButton, BorderLayout.SOUTH);

        return panel;
    }

    private String repeat(String str, int count) {
        if (str == null) {
            return null;
        }
        if (count <= 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(str);
        }
        return result.toString();
    }

    private String alignerTexte(String texte, String alignement, int largeur) {
        if (texte.length() > largeur) {
            return texte.substring(0, largeur);
        }

        int espaces = largeur - texte.length();
        String resultat;

        switch (alignement) {
            case "DROITE":
                resultat = repeat(" ", espaces) + texte;
                break;
            case "CENTRE":
                int espacesAvant = espaces / 2;
                int espacesApres = espaces - espacesAvant;
                resultat = repeat(" ", espacesAvant) + texte + repeat(" ", espacesApres);
                break;
            default: // GAUCHE
                resultat = texte + repeat(" ", espaces);
                break;
        }
        return resultat;
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

    private JPanel createSectionPanel(String title, MaterialDesign icon) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                sousTitreFont,
                new Color(33, 33, 33)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Ajout de l'icône
        if (icon != null) {
            FontIcon fontIcon = FontIcon.of(icon);
            fontIcon.setIconSize(24);
            fontIcon.setIconColor(couleurPrincipale);
            JLabel iconLabel = new JLabel(fontIcon);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(0, 0, 10, 0);
            panel.add(iconLabel, gbc);
        }

        return panel;
    }

    private JPanel createTVASection() {
        JPanel panel = createSectionPanel("Configuration TVA", MaterialDesign.MDI_PERCENT);

        JCheckBox tvaEnabledCheck = (JCheckBox) champsSaisie.get(ConfigurationParam.CLE_TVA_ENABLED);
        tvaEnabledCheck.setFont(texteNormalFont);
        tvaEnabledCheck.setToolTipText("Active ou désactive le calcul de la TVA sur les ventes");

        JPanel tauxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner tauxTvaSpinner = (JSpinner) champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA);
        JSlider tauxSlider = new JSlider(0, 100, 20);

        tauxTvaSpinner.addChangeListener(e -> {
            double value = (double) tauxTvaSpinner.getValue();
            tauxSlider.setValue((int) value);
            updatePreview();
        });

        tauxSlider.addChangeListener(e -> {
            tauxTvaSpinner.setValue((double) tauxSlider.getValue());
            updatePreview();
        });

        tauxTvaSpinner.setFont(texteNormalFont);

        tvaEnabledCheck.addActionListener(e -> {
            boolean enabled = tvaEnabledCheck.isSelected();
            tauxTvaSpinner.setEnabled(enabled);
            tauxSlider.setEnabled(enabled);
            updatePreview();
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(tvaEnabledCheck, gbc);

        gbc.gridy = 2;
        panel.add(new JLabel("Taux de TVA (%):"), gbc);

        tauxPanel.add(tauxTvaSpinner);
        tauxPanel.add(tauxSlider);
        gbc.gridx = 1;
        panel.add(tauxPanel, gbc);

        JPanel exemplePanel = new JPanel();
        exemplePanel.setBorder(BorderFactory.createTitledBorder("Exemple de calcul"));
        JLabel exempleLabel = new JLabel("Prix HT: 100€");
        JLabel resultatLabel = new JLabel("Prix TTC: 120€");
        exemplePanel.add(exempleLabel);
        exemplePanel.add(resultatLabel);

        ChangeListener updateExemple = e -> {
            if (tvaEnabledCheck.isSelected()) {
                double taux = (double) tauxTvaSpinner.getValue();
                double prixHT = 100.0;
                double prixTTC = prixHT * (1 + taux / 100);
                resultatLabel.setText(String.format("Prix TTC: %.2f€", prixTTC));
            } else {
                resultatLabel.setText("TVA désactivée");
            }
        };
        tauxTvaSpinner.addChangeListener(updateExemple);
        tvaEnabledCheck.addActionListener(e -> updateExemple.stateChanged(null));

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(exemplePanel, gbc);

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

            JTextField textField = (JTextField) champsSaisie.get(champ[0]);
            textField.setFont(texteNormalFont);
            styleTextField(textField);

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
        panel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Format des reçus
        JLabel formatLabel = new JLabel("Format des reçus:");
        formatLabel.setFont(texteNormalFont);
        JComboBox<String> formatCombo = (JComboBox<String>) champsSaisie.get(ConfigurationParam.CLE_FORMAT_RECU);
        formatCombo.setFont(texteNormalFont);
        formatCombo.setToolTipText("COMPACT: Version simplifiée du reçu\nDETAILLE: Version complète avec informations additionnelles");

        panel.add(formatLabel, gbc);
        gbc.gridx = 1;
        panel.add(formatCombo, gbc);

        // Style de bordure
        gbc.gridx = 0;
        gbc.gridy++;
        JLabel bordureLabel = new JLabel("Style de bordure:");
        bordureLabel.setFont(texteNormalFont);
        JComboBox<String> bordureCombo = (JComboBox<String>) champsSaisie.get(ConfigurationParam.CLE_STYLE_BORDURE_RECU);
        bordureCombo.setFont(texteNormalFont);

        panel.add(bordureLabel, gbc);
        gbc.gridx = 1;
        panel.add(bordureCombo, gbc);

        // Polices
        gbc.gridx = 0;
        gbc.gridy++;
        JPanel policeTitrePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel policeTitreLabel = new JLabel("Taille police titre:");
        policeTitreLabel.setFont(texteNormalFont);
        JSpinner policeTitreSpinner = (JSpinner) champsSaisie.get(ConfigurationParam.CLE_POLICE_TITRE_RECU);
        policeTitreSpinner.setFont(texteNormalFont);
        policeTitrePanel.add(policeTitreLabel);
        policeTitrePanel.add(policeTitreSpinner);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(policeTitrePanel, gbc);

        // Police texte
        gbc.gridy++;
        JPanel policeTextePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel policeTexteLabel = new JLabel("Taille police texte:");
        policeTexteLabel.setFont(texteNormalFont);
        JSpinner policeTexteSpinner = (JSpinner) champsSaisie.get(ConfigurationParam.CLE_POLICE_TEXTE_RECU);
        policeTexteSpinner.setFont(texteNormalFont);
        policeTextePanel.add(policeTexteLabel);
        policeTextePanel.add(policeTexteSpinner);

        panel.add(policeTextePanel, gbc);

        // Alignements
        gbc.gridy++;
        JPanel alignementPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        alignementPanel.setBorder(BorderFactory.createTitledBorder("Alignements"));

        String[] alignements = {"GAUCHE", "CENTRE", "DROITE"};

        JLabel alignementTitreLabel = new JLabel("Alignement titre:");
        JComboBox<String> alignementTitreCombo = (JComboBox<String>) champsSaisie.get(ConfigurationParam.CLE_ALIGNEMENT_TITRE_RECU);

        JLabel alignementTexteLabel = new JLabel("Alignement texte:");
        JComboBox<String> alignementTexteCombo = (JComboBox<String>) champsSaisie.get(ConfigurationParam.CLE_ALIGNEMENT_TEXTE_RECU);

        alignementPanel.add(alignementTitreLabel);
        alignementPanel.add(alignementTitreCombo);
        alignementPanel.add(alignementTexteLabel);
        alignementPanel.add(alignementTexteCombo);

        gbc.gridwidth = 2;
        panel.add(alignementPanel, gbc);

        // Messages personnalisés
        gbc.gridy++;
        JPanel messagesPanel = new JPanel(new GridBagLayout());
        messagesPanel.setBorder(BorderFactory.createTitledBorder("Messages personnalisés"));
        GridBagConstraints gbcMessages = new GridBagConstraints();
        gbcMessages.gridx = 0;
        gbcMessages.gridy = 0;
        gbcMessages.fill = GridBagConstraints.HORIZONTAL;
        gbcMessages.insets = new Insets(5, 5, 5, 5);

        // En-tête
        JLabel enTeteLabel = new JLabel("Message d'en-tête:");
        JTextArea enTeteArea = (JTextArea) champsSaisie.get(ConfigurationParam.CLE_EN_TETE_RECU);
        styleTextArea(enTeteArea);
        messagesPanel.add(enTeteLabel, gbcMessages);
        gbcMessages.gridy++;
        messagesPanel.add(new JScrollPane(enTeteArea), gbcMessages);

        // Message commercial
        gbcMessages.gridy++;
        JLabel msgCommercialLabel = new JLabel("Message commercial:");
        JTextArea msgCommercialArea = (JTextArea) champsSaisie.get(ConfigurationParam.CLE_MESSAGE_COMMERCIAL_RECU);
        styleTextArea(msgCommercialArea);
        messagesPanel.add(msgCommercialLabel, gbcMessages);
        gbcMessages.gridy++;
        messagesPanel.add(new JScrollPane(msgCommercialArea), gbcMessages);

        // Pied de page
        gbcMessages.gridy++;
        JLabel piedPageLabel = new JLabel("Message de pied de page:");
        JTextArea piedPageArea = (JTextArea) champsSaisie.get(ConfigurationParam.CLE_PIED_PAGE_RECU);
        styleTextArea(piedPageArea);
        messagesPanel.add(piedPageLabel, gbcMessages);
        gbcMessages.gridy++;
        messagesPanel.add(new JScrollPane(piedPageArea), gbcMessages);

        // Infos supplémentaires
        gbcMessages.gridy++;
        JLabel infoSupLabel = new JLabel("Informations supplémentaires:");
        JTextArea infoSupArea = (JTextArea) champsSaisie.get(ConfigurationParam.CLE_INFO_SUPPLEMENTAIRE_RECU);
        styleTextArea(infoSupArea);
        messagesPanel.add(infoSupLabel, gbcMessages);
        gbcMessages.gridy++;
        messagesPanel.add(new JScrollPane(infoSupArea), gbcMessages);

        gbc.gridy++;
        panel.add(messagesPanel, gbc);

        // Options supplémentaires
        gbc.gridy++;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options supplémentaires"));

        JCheckBox afficherTVACheck = (JCheckBox) champsSaisie.get(ConfigurationParam.CLE_AFFICHER_TVA_DETAILS);
        afficherTVACheck.setFont(texteNormalFont);
        optionsPanel.add(afficherTVACheck);

        panel.add(optionsPanel, gbc);

        // Prévisualisation
        gbc.gridy++;
        JLabel previewLabel = new JLabel("Les modifications sont visibles dans l'aperçu à droite");
        previewLabel.setFont(new Font(texteNormalFont.getName(), Font.ITALIC, texteNormalFont.getSize()));
        previewLabel.setForeground(new Color(128, 128, 128));
        panel.add(previewLabel, gbc);

        // Ajout des listeners pour la mise à jour de la prévisualisation
        JComponent[] composantsAvecUpdate = {
            formatCombo, bordureCombo, policeTitreSpinner, policeTexteSpinner,
            alignementTitreCombo, alignementTexteCombo, afficherTVACheck
        };

        for(JComponent composant : composantsAvecUpdate) {
            if (composant instanceof JSpinner) {
                ((JSpinner) composant).addChangeListener(e -> updatePreview());
            } else if (composant instanceof JComboBox) {
                ((JComboBox<?>) composant).addActionListener(e -> updatePreview());
            } else if (composant instanceof JCheckBox) {
                ((JCheckBox) composant).addActionListener(e -> updatePreview());
            }
        }

        // Listeners pour les champs de texte
        JTextComponent[] textComponents = {
            (JTextField) champsSaisie.get(ConfigurationParam.CLE_NOM_ENTREPRISE),
            (JTextField) champsSaisie.get(ConfigurationParam.CLE_ADRESSE_ENTREPRISE),
            (JTextField) champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE),
            (JTextField) champsSaisie.get(ConfigurationParam.CLE_SIRET_ENTREPRISE),
            (JTextArea) champsSaisie.get(ConfigurationParam.CLE_MESSAGE_COMMERCIAL_RECU),
            (JTextArea) champsSaisie.get(ConfigurationParam.CLE_EN_TETE_RECU),
            (JTextArea) champsSaisie.get(ConfigurationParam.CLE_PIED_PAGE_RECU),
            (JTextArea) champsSaisie.get(ConfigurationParam.CLE_INFO_SUPPLEMENTAIRE_RECU)
        };

        for (JTextComponent comp : textComponents) {
            comp.getDocument().addDocumentListener(new DocumentListener() {
                private void handleChange() {
                    updatePreview();
                }
                @Override
                public void insertUpdate(DocumentEvent e) { handleChange(); }
                @Override
                public void removeUpdate(DocumentEvent e) { handleChange(); }
                @Override
                public void changedUpdate(DocumentEvent e) { handleChange(); }
            });
        }

        return panel;
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; 
        }
        return name.substring(lastIndexOf + 1).toLowerCase();
    }

    private String calculateFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] hash = digest.digest(fileBytes);
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du calcul du hash du fichier", e);
            return "";
        }
    }

    private boolean validateLogo(File file) {
        if (!file.exists() || !file.isFile()) {
            showErrorMessage("Le fichier n'existe pas ou n'est pas accessible");
            return false;
        }

        String extension = getFileExtension(file);
        if (!FORMATS_IMAGE_AUTORISES.contains(extension)) {
            showErrorMessage("Format de fichier non autorisé. Formats acceptés: " + 
                           String.join(", ", FORMATS_IMAGE_AUTORISES));
            return false;
        }

        if (file.length() > TAILLE_MAX_LOGO) {
            showErrorMessage("Le fichier est trop volumineux. Taille maximum: " + 
                           (TAILLE_MAX_LOGO / 1024) + "KB");
            return false;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                showErrorMessage("Le fichier n'est pas une image valide");
                return false;
            }

            int maxDimension = 1024;
            if (image.getWidth() > maxDimension || image.getHeight() > maxDimension) {
                showErrorMessage("Les dimensions de l'image sont trop grandes. " +
                               "Maximum: " + maxDimension + "x" + maxDimension + " pixels");
                return false;
            }

            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la validation de l'image", e);
            showErrorMessage("Erreur lors de la lecture de l'image");
            return false;
        }
    }

    private void loadData() {
        try {
            Map<String, String> configurations = controller.getConfigurations();
            for (Map.Entry<String, String> entry : configurations.entrySet()) {
                JComponent composant = champsSaisie.get(entry.getKey());
                if (composant != null) {
                    if (composant instanceof JTextField) {
                        ((JTextField) composant).setText(entry.getValue());
                    } else if (composant instanceof JTextArea) {
                        ((JTextArea) composant).setText(entry.getValue());
                    } else if (composant instanceof JSpinner) {
                        try {
                            ((JSpinner) composant).setValue(Double.parseDouble(entry.getValue()));
                        } catch (NumberFormatException e) {
                            LOGGER.warning("Valeur invalide pour le spinner: " + entry.getValue());
                        }
                    } else if (composant instanceof JCheckBox) {
                        ((JCheckBox) composant).setSelected(Boolean.parseBoolean(entry.getValue()));
                    } else if (composant instanceof JComboBox) {
                        ((JComboBox<?>) composant).setSelectedItem(entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations", e);
            showErrorMessage("Erreur lors du chargement des configurations: " + e.getMessage());
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);

        // Bouton Exporter
        JButton exportButton = createStyledButton("Exporter", MaterialDesign.MDI_EXPORT,
            new Color(33, 150, 243));
        exportButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers JSON", "json"));
            fileChooser.setSelectedFile(new File("configurations.json"));

            if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
                        selectedFile = new File(selectedFile.getParentFile(), 
                                             selectedFile.getName() + ".json");
                    }
                    controller.exporterConfigurations(selectedFile);
                    showSuccessMessage("Configurations exportées avec succès");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'exportation", ex);
                    showErrorMessage("Erreur lors de l'exportation: " + ex.getMessage());
                }
            }
        });

        // Bouton Importer
        JButton importButton = createStyledButton("Importer", MaterialDesign.MDI_IMPORT,
            new Color(76, 175, 80));
        importButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers JSON", "json"));

            if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                try {
                    controller.importerConfigurations(fileChooser.getSelectedFile());
                    loadData();
                    updatePreview();
                    showSuccessMessage("Configurations importées avec succès");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'importation", ex);
                    showErrorMessage("Erreur lors de l'importation: " + ex.getMessage());
                }
            }
        });

        // Bouton Réinitialiser
        JButton resetButton = createStyledButton("Réinitialiser", MaterialDesign.MDI_RESTORE,
            new Color(244, 67, 54));
        resetButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(mainPanel,
                "Voulez-vous vraiment réinitialiser toutes les configurations ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    controller.reinitialiserConfigurations();
                    loadData();
                    updatePreview();
                    showSuccessMessage("Configurations réinitialisées avec succès");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation", ex);
                    showErrorMessage("Erreur lors de la réinitialisation: " + ex.getMessage());
                }
            }
        });

        // Bouton Sauvegarder
        JButton saveButton = createStyledButton("Sauvegarder", MaterialDesign.MDI_CONTENT_SAVE,
            new Color(33, 150, 243));
        saveButton.addActionListener(e -> {
            try {
                List<ConfigurationParam> configsToSave = new ArrayList<>();
                for (Map.Entry<String, JComponent> entry : champsSaisie.entrySet()) {
                    String cle = entry.getKey();
                    JComponent composant = entry.getValue();
                    String valeur = "";

                    if (composant instanceof JTextField) {
                        valeur = ((JTextField) composant).getText();
                    } else if (composant instanceof JTextArea) {
                        valeur = ((JTextArea) composant).getText();
                    } else if (composant instanceof JSpinner) {
                        valeur = String.valueOf(((JSpinner) composant).getValue());
                    } else if (composant instanceof JCheckBox) {
                        valeur = String.valueOf(((JCheckBox) composant).isSelected());
                    } else if (composant instanceof JComboBox) {
                        Object selectedItem = ((JComboBox<?>) composant).getSelectedItem();
                        valeur = selectedItem != null ? selectedItem.toString() : "";
                    }

                    configsToSave.add(new ConfigurationParam(0, cle, valeur, ""));
                }

                controller.sauvegarderConfigurations(configsToSave);
                showSuccessMessage("Configurations sauvegardées avec succès");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde", ex);
                showErrorMessage("Erreur lors de la sauvegarde: " + ex.getMessage());
            }
        });

        buttonPanel.add(importButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}