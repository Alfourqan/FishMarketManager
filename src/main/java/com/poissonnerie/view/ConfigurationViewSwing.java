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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


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

    public static final String CLE_TAUX_TVA = "TAUX_TVA";
    public static final String CLE_TVA_ENABLED = "TVA_ENABLED";
    public static final String CLE_TVA_CODE = "TVA_CODE";
    public static final String CLE_TVA_TYPE = "TVA_TYPE";

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

    private void updatePreview() {
        try {
            JTextArea previewArea = (JTextArea) ((JScrollPane) previewPanel.getComponent(0)).getViewport().getView();
            StringBuilder preview = new StringBuilder();

            String logoPath = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_LOGO_PATH)).getText();
            if (!logoPath.isEmpty()) {
                preview.append("[LOGO]\n");
            }

            preview.append("================================\n");
            String nomEntreprise = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_NOM_ENTREPRISE)).getText();
            if (!nomEntreprise.isEmpty()) {
                preview.append(String.format("%s%s%s\n",
                    " ".repeat(3),
                    nomEntreprise,
                    " ".repeat(3)));
            } else {
                preview.append("       VOTRE ENTREPRISE         \n");
            }
            preview.append("================================\n");

            String adresse = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_ADRESSE_ENTREPRISE)).getText();
            String telephone = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE)).getText();
            String siret = ((JTextField) champsSaisie.get(ConfigurationParam.CLE_SIRET_ENTREPRISE)).getText();

            if (!adresse.isEmpty()) preview.append(adresse + "\n");
            if (!telephone.isEmpty()) preview.append("Tel: " + telephone + "\n");
            if (!siret.isEmpty()) preview.append("SIRET: " + siret + "\n");

            preview.append("--------------------------------\n");

            double sousTotal = 25.00;
            preview.append(String.format("Article 1%14.2f EUR\n", 10.00));
            preview.append(String.format("Article 2%14.2f EUR\n", 15.00));
            preview.append("--------------------------------\n");
            preview.append(String.format("Sous-total:%14.2f EUR\n", sousTotal));

            boolean tvaEnabled = ((JCheckBox) champsSaisie.get(ConfigurationParam.CLE_TVA_ENABLED)).isSelected();
            if (tvaEnabled) {
                double tauxTva = (double) ((JSpinner) champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA)).getValue();
                double montantTva = sousTotal * (tauxTva / 100);
                preview.append(String.format("TVA (%.1f%%):%13.2f EUR\n", tauxTva, montantTva));
                preview.append(String.format("TOTAL:%18.2f EUR\n", sousTotal + montantTva));
            } else {
                preview.append(String.format("TOTAL:%18.2f EUR\n", sousTotal));
            }

            preview.append("================================\n");

            String piedPage = ((JTextArea) champsSaisie.get(ConfigurationParam.CLE_PIED_PAGE_RECU)).getText();
            if (!piedPage.isEmpty()) {
                preview.append(piedPage + "\n");
            } else {
                preview.append("Merci de votre visite !\n");
            }
            preview.append("================================\n");

            String format = ((JComboBox<?>) champsSaisie.get(ConfigurationParam.CLE_FORMAT_RECU)).getSelectedItem().toString();
            if (format.equals("DETAILLE")) {
                preview.append("\nMode de paiement: ESPECES\n");
                preview.append("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n");
                preview.append("N° Ticket: #12345\n");
            }

            previewArea.setText(preview.toString());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de l'aperçu", e);
            e.printStackTrace();
        }
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

        JCheckBox tvaEnabledCheck = new JCheckBox("Activer la TVA");
        tvaEnabledCheck.setFont(texteNormalFont);
        tvaEnabledCheck.setToolTipText("Active ou désactive le calcul de la TVA sur les ventes");
        champsSaisie.put(ConfigurationParam.CLE_TVA_ENABLED, tvaEnabledCheck);

        JPanel tauxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JSpinner tauxTvaSpinner = new JSpinner(new SpinnerNumberModel(20.0, 0.0, 100.0, 0.1));
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
        champsSaisie.put(ConfigurationParam.CLE_TAUX_TVA, tauxTvaSpinner);

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

        JLabel formatLabel = new JLabel("Format des reçus:");
        formatLabel.setFont(texteNormalFont);
        JComboBox<String> formatCombo = new JComboBox<>(new String[]{"COMPACT", "DETAILLE"});
        formatCombo.setFont(texteNormalFont);
        formatCombo.setToolTipText("COMPACT: Version simplifiée du reçu\nDETAILLE: Version complète avec informations additionnelles");
        champsSaisie.put(ConfigurationParam.CLE_FORMAT_RECU, formatCombo);

        formatCombo.addActionListener(e -> updatePreview());

        panel.add(formatLabel, gbc);
        gbc.gridx = 1;
        panel.add(formatCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel logoLabel = new JLabel("Logo de l'entreprise:");
        logoLabel.setFont(texteNormalFont);

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField logoPathField = new JTextField(20);
        logoPathField.setFont(texteNormalFont);
        logoPathField.setToolTipText("Chemin vers le fichier image du logo (formats supportés: JPG, PNG, GIF)");
        styleTextField(logoPathField);
        champsSaisie.put(ConfigurationParam.CLE_LOGO_PATH, logoPathField);

        JButton chooseLogoButton = createStyledButton("Choisir...", MaterialDesign.MDI_FILE_IMAGE, new Color(33, 150, 243));
        chooseLogoButton.addActionListener(e -> choisirLogo(logoPathField));

        JButton clearLogoButton = createStyledButton("", MaterialDesign.MDI_CLOSE_CIRCLE, new Color(244, 67, 54));
        clearLogoButton.setToolTipText("Effacer le logo");
        clearLogoButton.addActionListener(e -> {
            logoPathField.setText("");
            updatePreview();
        });

        logoPanel.add(logoPathField);
        logoPanel.add(chooseLogoButton);
        logoPanel.add(clearLogoButton);

        panel.add(logoLabel, gbc);
        gbc.gridx = 1;
        panel.add(logoPanel, gbc);

        String[][] champsTextAreas = {
            {ConfigurationParam.CLE_EN_TETE_RECU, "Message d'en-tête", "Message affiché en haut du reçu"},
            {ConfigurationParam.CLE_PIED_PAGE_RECU, "Message de pied de page", "Message affiché en bas du reçu"}
        };

        for (String[] champ : champsTextAreas) {
            gbc.gridx = 0;
            gbc.gridy++;
            JLabel label = new JLabel(champ[1] + ":");
            label.setFont(texteNormalFont);

            JTextArea textArea = new JTextArea(2, 30);
            textArea.setFont(texteNormalFont);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setToolTipText(champ[2]);

            textArea.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) { updatePreview(); }
                public void removeUpdate(DocumentEvent e) { updatePreview(); }
                public void insertUpdate(DocumentEvent e) { updatePreview(); }
            });

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

            champsSaisie.put(champ[0], textArea);

            panel.add(label, gbc);
            gbc.gridx = 1;
            panel.add(scrollPane, gbc);
        }

        JLabel previewLabel = new JLabel("Les modifications sont visibles dans l'aperçu à droite");
        previewLabel.setFont(new Font(texteNormalFont.getName(), Font.ITALIC, texteNormalFont.getSize()));
        previewLabel.setForeground(new Color(128, 128, 128));
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(previewLabel, gbc);

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
        LOGGER.info("Ouverture du sélecteur de logo");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Sélectionner un logo");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Images (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif"));

        if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                validateImageFile(file);
                String pathSecurise = securiserCheminFichier(file.getAbsolutePath());
                logoPathField.setText(pathSecurise);
                LOGGER.info("Logo sélectionné et validé: " + pathSecurise);
                updatePreview();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la validation du logo", e);
                showErrorMessage(e.getMessage());
            }
        }
    }

    private String securiserCheminFichier(String chemin) {
        try {
            Path path = Paths.get(chemin).normalize();
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Le fichier n'existe pas");
            }
            // Vérifier qu'on ne sort pas du répertoire de l'application
            Path appDir = Paths.get(System.getProperty("user.dir")).normalize();
            if (!path.startsWith(appDir)) {
                throw new IllegalArgumentException("Accès non autorisé à ce répertoire");
            }
            return path.toString();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la sécurisation du chemin de fichier", e);
            throw new IllegalArgumentException("Chemin de fichier invalide");
        }
    }

    private void validateImageFile(File file) throws Exception {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Le fichier spécifié n'existe pas");
        }

        if (file.length() > TAILLE_MAX_LOGO) {
            throw new IllegalArgumentException("Le fichier est trop volumineux. Taille maximum : 1MB");
        }

        String extension = getFileExtension(file.getName()).toLowerCase();
        if (!FORMATS_IMAGE_AUTORISES.contains(extension)) {
            throw new IllegalArgumentException("Format de fichier non autorisé. Formats acceptés : JPG, PNG, GIF");
        }

        // Vérification du contenu du fichier image
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IllegalArgumentException("Le fichier n'est pas une image valide");
            }
            // Vérification des dimensions
            if (image.getWidth() > 1000 || image.getHeight() > 1000) {
                throw new IllegalArgumentException("Les dimensions de l'image sont trop grandes (max 1000x1000)");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Erreur lors de la lecture de l'image: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        return Optional.ofNullable(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(filename.lastIndexOf(".") + 1))
            .orElse("");
    }

    private void actualiserConfigurations() {
        try {
            mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            LOGGER.info("Actualisation des configurations...");
            loadData();
            showSuccessMessage("Configurations actualisées avec succès");
            updatePreview();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'actualisation des configurations", ex);
            showErrorMessage("Erreur lors de l'actualisation : " + ex.getMessage());
        } finally {
            mainPanel.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void loadData() {
        try {
            LOGGER.info("Chargement des configurations...");
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

            LOGGER.info("Configurations chargées avec succès");
            updatePreview();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des configurations", e);
            System.err.println("Erreur lors du chargement des configurations: " + e.getMessage());
            showErrorMessage("Erreur lors du chargement des configurations : " + e.getMessage());
        }
    }

    private void sauvegarderConfigurations() {
        try {
            if (!validerChamps()) {
                return;
            }

            LOGGER.info("Début de la sauvegarde des configurations...");
            boolean hasChanges = false;

            for (Map.Entry<String, JComponent> entry : champsSaisie.entrySet()) {
                String cle = entry.getKey();
                JComponent composant = entry.getValue();
                String nouvelleValeur = "";

                if (composant instanceof JTextField) {
                    nouvelleValeur = sanitizeInput(((JTextField) composant).getText().trim());
                } else if (composant instanceof JTextArea) {
                    nouvelleValeur = sanitizeInput(((JTextArea) composant).getText().trim());
                } else if (composant instanceof JCheckBox) {
                    nouvelleValeur = String.valueOf(((JCheckBox) composant).isSelected());
                } else if (composant instanceof JSpinner) {
                    nouvelleValeur = String.valueOf(((JSpinner) composant).getValue());
                } else if (composant instanceof JComboBox) {
                    nouvelleValeur = String.valueOf(((JComboBox<?>) composant).getSelectedItem());
                }

                String ancienneValeur = controller.getValeur(cle);
                if (!nouvelleValeur.equals(ancienneValeur)) {
                    LOGGER.log(Level.INFO, "Mise à jour de la configuration: {0} = {1}", new Object[]{cle, nouvelleValeur});
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
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde des configurations", e);
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            showErrorMessage("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    private boolean validerChamps() {
        List<String> erreurs = new ArrayList<>();

        // Validation du taux de TVA
        JSpinner tauxTVASpinner = (JSpinner) champsSaisie.get(ConfigurationParam.CLE_TAUX_TVA);
        double tauxTVA = (double) tauxTVASpinner.getValue();
        if (tauxTVA < 0 || tauxTVA > 100) {
            erreurs.add("Le taux de TVA doit être compris entre 0 et 100");
        }

        // Validation du téléphone avec regex plus stricte
        JTextField telephoneField = (JTextField) champsSaisie.get(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE);
        String telephone = telephoneField.getText().trim();
        if (!telephone.isEmpty() && !telephone.matches("^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$")) {
            erreurs.add("Le numéro de téléphone contient des caractères invalides");
        }

        // Validation du SIRET avec vérification de la clé
        JTextField siretField = (JTextField) champsSaisie.get(ConfigurationParam.CLE_SIRET_ENTREPRISE);
        String siret = siretField.getText().trim();
        if (!siret.isEmpty()) {
            if (!siret.matches("^[0-9]{14}$")) {
                erreurs.add("Le numéro SIRET doit contenir exactement 14 chiffres");
            } else if (!validerCleSIRET(siret)) {
                erreurs.add("Le numéro SIRET est invalide (erreur de clé de contrôle)");
            }
        }

        // Validation du logo
        JTextField logoPathField = (JTextField) champsSaisie.get(ConfigurationParam.CLE_LOGO_PATH);
        String logoPath = logoPathField.getText().trim();
        if (!logoPath.isEmpty()) {
            try {
                validateImageFile(new File(logoPath));
            } catch (Exception e) {
                erreurs.add(e.getMessage());
            }
        }

        // Validation du format des reçus
        JComboBox<?> formatCombo = (JComboBox<?>) champsSaisie.get(ConfigurationParam.CLE_FORMAT_RECU);
        String formatRecu = formatCombo.getSelectedItem().toString();
        if (!formatRecu.equals("COMPACT") && !formatRecu.equals("DETAILLE")) {
            erreurs.add("Le format des reçus doit être 'COMPACT' ou 'DETAILLE'");
        }

        // Validation des champs de texte pour XSS
        for (Map.Entry<String, JComponent> entry : champsSaisie.entrySet()) {
            JComponent composant = entry.getValue();
            if (composant instanceof JTextField || composant instanceof JTextArea) {
                String texte = composant instanceof JTextField ?
                    ((JTextField) composant).getText() :
                    ((JTextArea) composant).getText();

                if (contientCodeMalveillant(texte)) {
                    erreurs.add("Le champ " + entry.getKey() + " contient des caractères non autorisés");
                }
            }
        }

        if (!erreurs.isEmpty()) {
            showErrorMessage(String.join("\n", erreurs));
            return false;
        }

        return true;
    }

    private boolean validerCleSIRET(String siret) {
        try {
            int[] chiffres = siret.chars()
                .map(Character::getNumericValue)
                .toArray();

            int somme = 0;
            for (int i = 0; i < 14; i++) {
                int chiffre = chiffres[i];
                if (i % 2 == 0) {
                    chiffre *= 2;
                    if (chiffre > 9) {
                        chiffre -= 9;
                    }
                }
                somme += chiffre;
            }

            return somme % 10 == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean contientCodeMalveillant(String input) {
        if (input == null) return false;

        // Liste de motifs suspects
        String[] motifsSuspects = {
            "<script", "javascript:", "onerror=", "onload=", "onclick=",
            "data:text/html", "data:text/javascript", "&#", "\\x", "\\u",
            "expression(", "document.cookie", "eval(", "fromCharCode",
            "parseInt", "String.fromCharCode"
        };

        String inputLower = input.toLowerCase();
        return Arrays.stream(motifsSuspects)
            .anyMatch(motif -> inputLower.contains(motif.toLowerCase()));
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";

        return input.replaceAll("[<>\"'%;)(&+\\x\\u]", "")
            .replaceAll("(?i)javascript:", "")
            .replaceAll("(?i)data:", "")
            .replaceAll("&#", "&amp;#")
            .trim()
            .replaceAll("\\s+", " ");
    }

    private void reinitialiserConfigurations() {
        if (showConfirmDialog("Êtes-vous sûr de vouloir réinitialiser tous les paramètres ?")) {
            try {
                LOGGER.info("Réinitialisation des configurations...");
                controller.reinitialiserConfigurations();
                loadData();
                showSuccessMessage("Les paramètres ont été réinitialisés avec succès");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la réinitialisation des configurations", e);
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