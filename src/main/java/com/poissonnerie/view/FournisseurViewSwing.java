package com.poissonnerie.view;

import com.poissonnerie.controller.FournisseurController;
import com.poissonnerie.model.Fournisseur;
import com.poissonnerie.util.PDFGenerator;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FournisseurViewSwing {
    private final JPanel mainPanel;
    private final FournisseurController controller;
    private final JTable tableFournisseurs;
    private final DefaultTableModel tableModel;
    private JTextField searchField;

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
        setupTableStyle();

        initializeComponents();
        loadData();
    }

    private void setupTableStyle() {
        // Configuration de base du tableau
        tableFournisseurs.setShowGrid(false);
        tableFournisseurs.setGridColor(new Color(230, 230, 230));
        tableFournisseurs.setBackground(Color.WHITE);
        tableFournisseurs.setSelectionBackground(new Color(232, 240, 254));
        tableFournisseurs.setSelectionForeground(new Color(33, 33, 33));
        tableFournisseurs.setRowHeight(35);
        tableFournisseurs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableFournisseurs.setIntercellSpacing(new Dimension(0, 0));

        // Configuration des cellules avec alternance de couleurs
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                // Padding des cellules ajusté
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                ((JLabel) c).setHorizontalAlignment(JLabel.LEFT);
                return c;
            }
        };

        // Appliquer le renderer à toutes les colonnes
        for (int i = 0; i < tableFournisseurs.getColumnCount(); i++) {
            tableFournisseurs.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // Style de l'en-tête simplifié
        JTableHeader header = tableFournisseurs.getTableHeader();
        header.setBackground(new Color(33, 33, 33));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

        // Configuration du rendu de l'en-tête
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                label.setBackground(new Color(33, 33, 33));
                label.setForeground(Color.WHITE);
                label.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                label.setHorizontalAlignment(JLabel.LEFT);
                return label;
            }
        };

        header.setDefaultRenderer(headerRenderer);

        // Ajustement des colonnes
        tableFournisseurs.getColumnModel().getColumn(0).setMaxWidth(60);
        tableFournisseurs.getColumnModel().getColumn(0).setMinWidth(60);
    }

    private void initializeComponents() {
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel d'en-tête avec titre et recherche
        JPanel headerPanel = createHeaderPanel();

        // Panel des boutons d'action
        JPanel actionPanel = createActionPanel();

        // Conteneur principal
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setOpaque(false);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(actionPanel, BorderLayout.CENTER);

        // ScrollPane pour la table
        JScrollPane scrollPane = new JScrollPane(tableFournisseurs);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 225)));

        mainPanel.add(contentPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("Gestion des Fournisseurs");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        searchField = createSearchField();

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(searchField, BorderLayout.EAST);

        return headerPanel;
    }

    private JTextField createSearchField() {
        JTextField field = new JTextField(20);
        field.setPreferredSize(new Dimension(250, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        field.setText("Rechercher un fournisseur...");
        field.setForeground(Color.GRAY);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (field.getText().equals("Rechercher un fournisseur...")) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (field.getText().isEmpty()) {
                    field.setText("Rechercher un fournisseur...");
                    field.setForeground(Color.GRAY);
                }
            }
        });

        field.addActionListener(e -> {
            String searchTerm = field.getText();
            if (!searchTerm.equals("Rechercher un fournisseur...")) {
                updateTableWithSearch(searchTerm);
            }
        });

        return field;
    }

    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JButton ajouterBtn = createStyledButton("Nouveau", new Color(76, 175, 80));
        JButton modifierBtn = createStyledButton("Modifier", new Color(33, 150, 243));
        JButton supprimerBtn = createStyledButton("Supprimer", new Color(244, 67, 54));
        JButton actualiserBtn = createStyledButton("Actualiser", new Color(156, 39, 176));
        JButton rapportBtn = createStyledButton("Rapport", new Color(63, 81, 181));

        ajouterBtn.addActionListener(e -> showFournisseurDialog(null));
        modifierBtn.addActionListener(e -> modifierFournisseurSelectionne());
        supprimerBtn.addActionListener(e -> supprimerFournisseurSelectionne());
        actualiserBtn.addActionListener(e -> loadData());
        rapportBtn.addActionListener(e -> genererRapport());

        actionPanel.add(ajouterBtn);
        actionPanel.add(modifierBtn);
        actionPanel.add(supprimerBtn);
        actionPanel.add(actualiserBtn);
        actionPanel.add(rapportBtn);

        return actionPanel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
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

    private void modifierFournisseurSelectionne() {
        int selectedRow = tableFournisseurs.getSelectedRow();
        if (selectedRow >= 0) {
            showFournisseurDialog(controller.getFournisseurs().get(selectedRow));
        } else {
            showWarningMessage("Veuillez sélectionner un fournisseur à modifier");
        }
    }

    private void supprimerFournisseurSelectionne() {
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

        JTextField nomField = createStyledTextField();
        JTextField contactField = createStyledTextField();
        JTextField telephoneField = createStyledTextField();
        JTextField emailField = createStyledTextField();
        JTextArea adresseArea = createStyledTextArea();
        JScrollPane adresseScroll = new JScrollPane(adresseArea);
        adresseScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        addFormField(panel, gbc, "Nom:", nomField, 0);
        addFormField(panel, gbc, "Contact:", contactField, 1);
        addFormField(panel, gbc, "Téléphone:", telephoneField, 2);
        addFormField(panel, gbc, "Email:", emailField, 3);
        addFormField(panel, gbc, "Adresse:", adresseScroll, 4);

        if (fournisseur != null) {
            nomField.setText(fournisseur.getNom());
            contactField.setText(fournisseur.getContact());
            telephoneField.setText(fournisseur.getTelephone());
            emailField.setText(fournisseur.getEmail());
            adresseArea.setText(fournisseur.getAdresse());
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton okButton = createStyledButton("Enregistrer", new Color(76, 175, 80));
        JButton cancelButton = createStyledButton("Annuler", new Color(158, 158, 158));

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

        if (nom.isEmpty()) throw new IllegalArgumentException("Le nom est obligatoire");
        if (telephone.isEmpty()) throw new IllegalArgumentException("Le téléphone est obligatoire");

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

    private void updateTableWithSearch(String searchTerm) {
        try {
            List<Fournisseur> resultats = controller.rechercherFournisseurs(searchTerm);
            refreshTable(resultats);
        } catch (Exception e) {
            showErrorMessage("Erreur lors de la recherche : " + e.getMessage());
        }
    }

    private void refreshTable(List<Fournisseur> fournisseurs) {
        tableModel.setRowCount(0);
        for (Fournisseur fournisseur : fournisseurs) {
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

    private void genererRapport() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String nomFichier = "rapport_fournisseurs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            PDFGenerator.genererRapportFournisseurs(controller.getFournisseurs(), outputStream);
            PDFGenerator.sauvegarderPDF(outputStream.toByteArray(), nomFichier);
            showSuccessMessage("Rapport généré avec succès :\n" + nomFichier);

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