package com.poissonnerie.view;

import com.poissonnerie.controller.FournisseurController;
import com.poissonnerie.model.Fournisseur;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

public class FournisseurViewSwing {
    private final JPanel mainPanel;
    private final FournisseurController controller;
    private final JTable tableFournisseurs;
    private final DefaultTableModel tableModel;

    public FournisseurViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new FournisseurController();

        // Création du modèle de table
        String[] columnNames = {"ID", "Nom", "Contact", "Téléphone", "Email", "Adresse"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableFournisseurs = new JTable(tableModel);
        tableFournisseurs.getColumnModel().getColumn(0).setMaxWidth(50);
        tableFournisseurs.setRowHeight(30);
        tableFournisseurs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel des boutons avec style moderne
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 15, 5)
        ));
        buttonPanel.setBackground(new Color(236, 239, 241));

        // Création des boutons avec icônes
        JButton ajouterBtn = createStyledButton("Ajouter", MaterialDesign.MDI_PLUS_BOX);
        JButton modifierBtn = createStyledButton("Modifier", MaterialDesign.MDI_PENCIL_BOX);
        JButton supprimerBtn = createStyledButton("Supprimer", MaterialDesign.MDI_MINUS_BOX);
        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH);

        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);
        buttonPanel.add(actualiserBtn);

        // Style moderne pour la table
        JScrollPane scrollPane = new JScrollPane(tableFournisseurs);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(236, 239, 241));
        scrollPane.getViewport().setBackground(new Color(236, 239, 241));

        tableFournisseurs.setShowGrid(true);
        tableFournisseurs.setGridColor(new Color(200, 200, 200));
        tableFournisseurs.setBackground(new Color(245, 246, 247));
        tableFournisseurs.setSelectionBackground(new Color(197, 202, 233));
        tableFournisseurs.setSelectionForeground(new Color(33, 33, 33));
        tableFournisseurs.setIntercellSpacing(new Dimension(0, 0));
        tableFournisseurs.getTableHeader().setBackground(new Color(220, 224, 228));
        tableFournisseurs.getTableHeader().setFont(tableFournisseurs.getTableHeader().getFont().deriveFont(Font.BOLD));

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

        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        JButton button = new JButton(text, icon);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setFocusPainted(false);
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

        // Champs du formulaire
        JTextField nomField = createStyledTextField();
        JTextField contactField = createStyledTextField();
        JTextField telephoneField = createStyledTextField();
        JTextField emailField = createStyledTextField();
        JTextArea adresseArea = new JTextArea(3, 20);
        adresseArea.setLineWrap(true);
        JScrollPane adresseScroll = new JScrollPane(adresseArea);

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
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annuler");

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
        field.setPreferredSize(new Dimension(200, 30));
        return field;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String labelText, 
                            JComponent field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel label = new JLabel(labelText);
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
                fournisseur.getAdresse()
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

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
