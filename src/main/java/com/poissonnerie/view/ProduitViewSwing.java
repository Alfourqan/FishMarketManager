package com.poissonnerie.view;

import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.model.Produit;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

public class ProduitViewSwing {
    private final JPanel mainPanel;
    private final ProduitController controller;
    private final JTable tableProduits;
    private final DefaultTableModel tableModel;
    private JTextField searchField;

    public ProduitViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new ProduitController();

        // Création du modèle de table avec icône de statut
        String[] columnNames = {"", "Nom", "Catégorie", "Prix Achat (€)", "Prix Vente (€)", "Marge (%)", "Stock", "Seuil d'alerte"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Icon.class : Object.class;
            }
        };
        tableProduits = new JTable(tableModel);
        setupTableStyle();

        initializeComponents();
        loadData();
    }

    private void setupTableStyle() {
        // Style moderne pour la table
        tableProduits.setShowGrid(true);
        tableProduits.setGridColor(new Color(230, 230, 230));
        tableProduits.setBackground(Color.WHITE);
        tableProduits.setSelectionBackground(new Color(232, 240, 254));
        tableProduits.setSelectionForeground(new Color(33, 33, 33));
        tableProduits.getTableHeader().setBackground(new Color(245, 246, 247));
        tableProduits.getTableHeader().setForeground(new Color(66, 66, 66));
        tableProduits.setRowHeight(40);
        tableProduits.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableProduits.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Ajustement des colonnes
        tableProduits.getColumnModel().getColumn(0).setMaxWidth(30);
        tableProduits.getColumnModel().getColumn(0).setMinWidth(30);
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

        // ScrollPane pour la table avec style moderne
        JScrollPane scrollPane = new JScrollPane(tableProduits);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        mainPanel.add(contentPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);

        // Titre avec icône
        JLabel titleLabel = new JLabel("Gestion des Produits");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(33, 33, 33));

        // Barre de recherche
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

        // Placeholder et style
        field.setText("Rechercher un produit...");
        field.setForeground(Color.GRAY);

        // Gestionnaire d'événements pour le placeholder
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (field.getText().equals("Rechercher un produit...")) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (field.getText().isEmpty()) {
                    field.setText("Rechercher un produit...");
                    field.setForeground(Color.GRAY);
                }
            }
        });

        // Événement de recherche (à implémenter)
        field.addActionListener(e -> {
            String searchTerm = field.getText();
            if (!searchTerm.equals("Rechercher un produit...")) {
                // TODO: Implémenter la recherche
                // updateTableWithSearch(searchTerm);
            }
        });

        return field;
    }

    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);

        // Création des boutons avec style moderne
        JButton ajouterBtn = createStyledButton("Nouveau", MaterialDesign.MDI_PLUS_BOX, new Color(76, 175, 80));
        JButton modifierBtn = createStyledButton("Modifier", MaterialDesign.MDI_PENCIL_BOX, new Color(33, 150, 243));
        JButton supprimerBtn = createStyledButton("Supprimer", MaterialDesign.MDI_MINUS_BOX, new Color(244, 67, 54));
        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH, new Color(156, 39, 176));

        // Ajout des gestionnaires d'événements
        ajouterBtn.addActionListener(e -> showProduitDialog(null));
        modifierBtn.addActionListener(e -> modifierProduitSelectionne());
        supprimerBtn.addActionListener(e -> supprimerProduitSelectionne());
        actualiserBtn.addActionListener(e -> loadData());

        // Ajout des boutons au panel
        actionPanel.add(ajouterBtn);
        actionPanel.add(modifierBtn);
        actionPanel.add(supprimerBtn);
        actionPanel.add(actualiserBtn);

        return actionPanel;
    }

    private void modifierProduitSelectionne() {
        int selectedRow = tableProduits.getSelectedRow();
        if (selectedRow >= 0) {
            showProduitDialog(controller.getProduits().get(selectedRow));
        } else {
            showWarningMessage("Veuillez sélectionner un produit à modifier");
        }
    }

    private void supprimerProduitSelectionne() {
        int selectedRow = tableProduits.getSelectedRow();
        if (selectedRow >= 0) {
            if (showConfirmDialog("Êtes-vous sûr de vouloir supprimer ce produit ?")) {
                try {
                    controller.supprimerProduit(controller.getProduits().get(selectedRow));
                    refreshTable();
                    showSuccessMessage("Produit supprimé avec succès");
                } catch (Exception ex) {
                    showErrorMessage(ex.getMessage());
                }
            }
        } else {
            showWarningMessage("Veuillez sélectionner un produit à supprimer");
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

    private void showProduitDialog(Produit produit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
                                  produit == null ? "Nouveau produit" : "Modifier produit",
                                  true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Champs du formulaire avec style moderne
        JTextField nomField = createStyledTextField();
        JComboBox<String> categorieCombo = new JComboBox<>(new String[]{"Frais", "Surgelé", "Transformé"});
        JTextField prixAchatField = createStyledTextField();
        JTextField prixVenteField = createStyledTextField();
        JTextField stockField = createStyledTextField();
        JTextField seuilField = createStyledTextField();

        // Layout
        addFormField(panel, gbc, "Nom:", nomField, 0);
        addFormField(panel, gbc, "Catégorie:", categorieCombo, 1);
        addFormField(panel, gbc, "Prix d'achat (€):", prixAchatField, 2);
        addFormField(panel, gbc, "Prix de vente (€):", prixVenteField, 3);
        addFormField(panel, gbc, "Stock:", stockField, 4);
        addFormField(panel, gbc, "Seuil d'alerte:", seuilField, 5);

        // Pré-remplissage si modification
        if (produit != null) {
            nomField.setText(produit.getNom());
            categorieCombo.setSelectedItem(produit.getCategorie());
            prixAchatField.setText(String.format("%.2f", produit.getPrixAchat()));
            prixVenteField.setText(String.format("%.2f", produit.getPrixVente()));
            stockField.setText(String.valueOf(produit.getStock()));
            seuilField.setText(String.valueOf(produit.getSeuilAlerte()));
        }

        // Boutons avec style moderne
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton okButton = createStyledButton("Enregistrer", MaterialDesign.MDI_CONTENT_SAVE, new Color(76, 175, 80));
        JButton cancelButton = createStyledButton("Annuler", MaterialDesign.MDI_CLOSE, new Color(158, 158, 158));

        // Gestionnaires d'événements
        okButton.addActionListener(evt -> {
            try {
                validateAndSaveProduit(produit, nomField, categorieCombo, prixAchatField,
                                    prixVenteField, stockField, seuilField);
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

    private void validateAndSaveProduit(Produit produit, JTextField nomField,
                                      JComboBox<String> categorieCombo, JTextField prixAchatField,
                                      JTextField prixVenteField, JTextField stockField,
                                      JTextField seuilField) {
        String nom = nomField.getText().trim();
        String categorie = (String) categorieCombo.getSelectedItem();
        String prixAchatText = prixAchatField.getText().trim().replace(",", ".");
        String prixVenteText = prixVenteField.getText().trim().replace(",", ".");
        String stockText = stockField.getText().trim();
        String seuilText = seuilField.getText().trim();

        // Validation
        if (nom.isEmpty()) throw new IllegalArgumentException("Le nom est obligatoire");

        double prixAchat = validateDouble(prixAchatText, "Prix d'achat invalide");
        if (prixAchat < 0) throw new IllegalArgumentException("Le prix d'achat ne peut pas être négatif");

        double prixVente = validateDouble(prixVenteText, "Prix de vente invalide");
        if (prixVente < 0) throw new IllegalArgumentException("Le prix de vente ne peut pas être négatif");
        if (prixVente < prixAchat) throw new IllegalArgumentException("Le prix de vente doit être supérieur au prix d'achat");

        int stock = validateInt(stockText, "Stock invalide");
        if (stock < 0) throw new IllegalArgumentException("Le stock ne peut pas être négatif");

        int seuil = validateInt(seuilText, "Seuil d'alerte invalide");
        if (seuil < 0) throw new IllegalArgumentException("Le seuil d'alerte ne peut pas être négatif");

        // Sauvegarde
        if (produit == null) {
            controller.ajouterProduit(new Produit(0, nom, categorie, prixAchat, prixVente, stock, seuil));
        } else {
            produit.setNom(nom);
            produit.setCategorie(categorie);
            produit.setPrixAchat(prixAchat);
            produit.setPrixVente(prixVente);
            produit.setStock(stock);
            produit.setSeuilAlerte(seuil);
            controller.mettreAJourProduit(produit);
        }
        refreshTable();
    }

    private double validateDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private int validateInt(String value, String errorMessage) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void showWarningMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Attention",
            JOptionPane.WARNING_MESSAGE);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Erreur",
            JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(mainPanel, message, "Succès",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean showConfirmDialog(String message) {
        return JOptionPane.showConfirmDialog(mainPanel, message, "Confirmation",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Produit produit : controller.getProduits()) {
            FontIcon icon;
            if (produit.getStock() <= produit.getSeuilAlerte()) {
                icon = FontIcon.of(MaterialDesign.MDI_ALERT_CIRCLE);
                icon.setIconColor(new Color(220, 53, 69)); // Rouge pour stock bas
            } else {
                icon = FontIcon.of(MaterialDesign.MDI_PACKAGE_VARIANT);
                icon.setIconColor(new Color(40, 167, 69)); // Vert pour stock normal
            }

            tableModel.addRow(new Object[]{
                icon,
                produit.getNom(),
                produit.getCategorie(),
                String.format("%.2f €", produit.getPrixAchat()),
                String.format("%.2f €", produit.getPrixVente()),
                String.format("%.1f%%", produit.getTauxMarge()),
                produit.getStock(),
                produit.getSeuilAlerte()
            });
        }
    }

    private void loadData() {
        try {
            controller.chargerProduits();
            refreshTable();
        } catch (Exception e) {
            showErrorMessage("Erreur lors du chargement des produits : " + e.getMessage());
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}