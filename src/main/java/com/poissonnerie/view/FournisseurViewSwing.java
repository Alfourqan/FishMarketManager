package com.poissonnerie.view;

import com.poissonnerie.controller.FournisseurController;
import com.poissonnerie.model.Fournisseur;
import com.poissonnerie.util.PDFGenerator;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.Desktop;


public class FournisseurViewSwing {
    private final JPanel mainPanel;
    private final FournisseurController controller;
    private final JTable tableFournisseurs;
    private final DefaultTableModel tableModel;

    public FournisseurViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new FournisseurController();

        // Création du modèle de table
        String[] columnNames = {"ID", "Nom", "Contact", "Téléphone", "Email", "Adresse", "Statut"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableFournisseurs = new JTable(tableModel);
        tableFournisseurs.getColumnModel().getColumn(0).setMaxWidth(50);
        tableFournisseurs.setRowHeight(35);
        tableFournisseurs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableFournisseurs.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // En-tête avec titre
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 255, 255));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel titleLabel = new JLabel("Gestion des Fournisseurs");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(33, 33, 33));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Panel des boutons avec style moderne
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setOpaque(false);

        // Création des boutons avec icônes
        JButton ajouterBtn = createStyledButton("Ajouter", MaterialDesign.MDI_PLUS_BOX, new Color(76, 175, 80));
        JButton modifierBtn = createStyledButton("Modifier", MaterialDesign.MDI_PENCIL_BOX, new Color(33, 150, 243));
        JButton supprimerBtn = createStyledButton("Supprimer", MaterialDesign.MDI_MINUS_BOX, new Color(244, 67, 54));
        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH, new Color(156, 39, 176));
        JButton rapportBtn = createReportButton();

        headerPanel.add(buttonPanel, BorderLayout.EAST);
        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);
        buttonPanel.add(actualiserBtn);
        buttonPanel.add(rapportBtn);

        // Style moderne pour la table
        JScrollPane scrollPane = new JScrollPane(tableFournisseurs);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(Color.WHITE);

        // Style du tableau
        tableFournisseurs.setShowGrid(true);
        tableFournisseurs.setGridColor(new Color(240, 240, 240));
        tableFournisseurs.setBackground(Color.WHITE);
        tableFournisseurs.setSelectionBackground(new Color(232, 240, 254));
        tableFournisseurs.setSelectionForeground(new Color(33, 33, 33));
        tableFournisseurs.getTableHeader().setBackground(new Color(245, 246, 247));
        tableFournisseurs.getTableHeader().setForeground(new Color(66, 66, 66));
        tableFournisseurs.setIntercellSpacing(new Dimension(0, 0));
        tableFournisseurs.setRowMargin(0);

        // Conteneur principal avec espacement
        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setOpaque(false);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Gestionnaires d'événements
        ajouterBtn.addActionListener(e -> showFournisseurDialog(null));
        modifierBtn.addActionListener(e -> {
            int selectedRow = tableFournisseurs.getSelectedRow();
            if (selectedRow >= 0) {
                showFournisseurDialog(controller.getFournisseurs().get(selectedRow));
            } else {
                showWarningMessage("Veuillez sélectionner un fournisseur à modifier");
            }
        });

        supprimerBtn.addActionListener(e -> {
            int selectedRow = tableFournisseurs.getSelectedRow();
            if (selectedRow >= 0) {
                if (showConfirmDialog("Êtes-vous sûr de vouloir supprimer ce fournisseur ?")) {
                    try {
                        controller.supprimerFournisseur(controller.getFournisseurs().get(selectedRow));
                        refreshTable();
                        showSuccessMessage("Fournisseur supprimé avec succès");
                    } catch (Exception ex) {
                        showErrorMessage(ex.getMessage());
                    }
                }
            } else {
                showWarningMessage("Veuillez sélectionner un fournisseur à supprimer");
            }
        });

        actualiserBtn.addActionListener(e -> loadData());

        mainPanel.add(contentPanel, BorderLayout.CENTER);
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

    private void showFournisseurDialog(Fournisseur fournisseur) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
                                  fournisseur == null ? "Nouveau fournisseur" : "Modifier fournisseur",
                                  true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Champs du formulaire avec style moderne
        JTextField nomField = createStyledTextField();
        JTextField contactField = createStyledTextField();
        JTextField telephoneField = createStyledTextField();
        JTextField emailField = createStyledTextField();
        JTextArea adresseArea = createStyledTextArea();
        JScrollPane adresseScroll = new JScrollPane(adresseArea);
        adresseScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // Layout
        addFormField(panel, gbc, "Nom:", nomField, 0);
        addFormField(panel, gbc, "Contact:", contactField, 1);
        addFormField(panel, gbc, "Téléphone:", telephoneField, 2);
        addFormField(panel, gbc, "Email:", emailField, 3);
        addFormField(panel, gbc, "Adresse:", adresseScroll, 4);

        // Pré-remplissage si modification
        if (fournisseur != null) {
            nomField.setText(fournisseur.getNom());
            contactField.setText(fournisseur.getContact());
            telephoneField.setText(fournisseur.getTelephone());
            emailField.setText(fournisseur.getEmail());
            adresseArea.setText(fournisseur.getAdresse());
        }

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton okButton = new JButton("Enregistrer");
        JButton cancelButton = new JButton("Annuler");

        // Style des boutons
        okButton.setBackground(new Color(76, 175, 80));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);

        cancelButton.setBackground(new Color(158, 158, 158));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);

        okButton.addActionListener(evt -> {
            try {
                validateAndSaveFournisseur(fournisseur, nomField, contactField, telephoneField,
                                         emailField, adresseArea);
                dialog.dispose();
            } catch (Exception e) {
                showErrorMessage(e.getMessage());
            }
        });

        cancelButton.addActionListener(evt -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Finalisation du dialog
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField(20);
        field.setPreferredSize(new Dimension(250, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return field;
    }

    private JTextArea createStyledTextArea() {
        JTextArea area = new JTextArea(3, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        return area;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String labelText,
                            JComponent field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void validateAndSaveFournisseur(Fournisseur fournisseur, JTextField nomField,
                                          JTextField contactField, JTextField telephoneField,
                                          JTextField emailField, JTextArea adresseArea) {
        String nom = nomField.getText().trim();
        String contact = contactField.getText().trim();
        String telephone = telephoneField.getText().trim();
        String email = emailField.getText().trim();
        String adresse = adresseArea.getText().trim();

        // Validation
        if (nom.isEmpty()) throw new IllegalArgumentException("Le nom est obligatoire");
        if (telephone.isEmpty()) throw new IllegalArgumentException("Le téléphone est obligatoire");

        // Sauvegarde
        if (fournisseur == null) {
            controller.ajouterFournisseur(new Fournisseur(0, nom, contact, telephone, email, adresse));
        } else {
            fournisseur.setNom(nom);
            fournisseur.setContact(contact);
            fournisseur.setTelephone(telephone);
            fournisseur.setEmail(email);
            fournisseur.setAdresse(adresse);
            controller.mettreAJourFournisseur(fournisseur);
        }
        refreshTable();
    }

    private void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Attention", JOptionPane.WARNING_MESSAGE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean showConfirmDialog(String message) {
        return JOptionPane.showConfirmDialog(mainPanel, message, "Confirmation",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Fournisseur fournisseur : controller.getFournisseurs()) {
            tableModel.addRow(new Object[]{
                fournisseur.getId(),
                fournisseur.getNom(),
                fournisseur.getContact(),
                fournisseur.getTelephone(),
                fournisseur.getEmail(),
                fournisseur.getAdresse(),
                fournisseur.getStatut()
            });
        }
    }

    private void loadData() {
        try {
            controller.chargerFournisseurs();
            refreshTable();
        } catch (Exception e) {
            showErrorMessage("Erreur lors du chargement des fournisseurs : " + e.getMessage());
        }
    }

    // Ajout du bouton de rapport dans le panneau des boutons
    private JButton createReportButton() {
        JButton rapportBtn = createStyledButton("Générer Rapport", MaterialDesign.MDI_FILE_PDF, new Color(63, 81, 181));
        rapportBtn.addActionListener(e -> genererRapport());
        return rapportBtn;
    }

    // Méthode pour générer le rapport
    private void genererRapport() {
        try {
            String nomFichier = "rapport_fournisseurs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            PDFGenerator.genererRapportFournisseurs(controller.getFournisseurs(), nomFichier);

            showSuccessMessage("Rapport généré avec succès :\n" + nomFichier);

            // Ouvrir le fichier PDF
            try {
                File file = new File(nomFichier);
                if (file.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception ex) {
                System.err.println("Impossible d'ouvrir le fichier : " + ex.getMessage());
            }
        } catch (Exception e) {
            showErrorMessage("Erreur lors de la génération du rapport : " + e.getMessage());
        }
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }
}