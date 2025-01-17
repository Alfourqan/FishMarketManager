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

    public ProduitViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new ProduitController();

        // Création du modèle de table avec icône de statut
        String[] columnNames = {"", "Nom", "Catégorie", "Prix Achat", "Prix Vente", "Marge (%)", "Stock", "Seuil d'alerte"};
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
        tableProduits.getColumnModel().getColumn(0).setMaxWidth(30);
        tableProduits.setRowHeight(30);
        tableProduits.setFont(new Font(tableProduits.getFont().getName(), Font.PLAIN, 13));

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

        // Création des boutons avec icônes et style moderne
        JButton ajouterBtn = createStyledButton("Ajouter", MaterialDesign.MDI_PLUS_BOX);
        JButton modifierBtn = createStyledButton("Modifier", MaterialDesign.MDI_PENCIL_BOX);
        JButton supprimerBtn = createStyledButton("Supprimer", MaterialDesign.MDI_MINUS_BOX);
        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH);

        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);
        buttonPanel.add(actualiserBtn);

        // Style moderne pour la table
        JScrollPane scrollPane = new JScrollPane(tableProduits);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(new Color(236, 239, 241));
        scrollPane.getViewport().setBackground(new Color(236, 239, 241));

        tableProduits.setShowGrid(true);
        tableProduits.setGridColor(new Color(200, 200, 200));
        tableProduits.setBackground(new Color(245, 246, 247));
        tableProduits.setSelectionBackground(new Color(197, 202, 233));
        tableProduits.setSelectionForeground(new Color(33, 33, 33));
        tableProduits.setIntercellSpacing(new Dimension(0, 0));
        tableProduits.getTableHeader().setBackground(new Color(220, 224, 228));
        tableProduits.getTableHeader().setFont(tableProduits.getTableHeader().getFont().deriveFont(Font.BOLD));

        // Gestionnaires d'événements
        ajouterBtn.addActionListener(e -> showProduitDialog(null));
        modifierBtn.addActionListener(e -> {
            int selectedRow = tableProduits.getSelectedRow();
            if (selectedRow >= 0) {
                showProduitDialog(controller.getProduits().get(selectedRow));
            } else {
                showWarningMessage("Veuillez sélectionner un produit à modifier");
            }
        });

        supprimerBtn.addActionListener(e -> {
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
        });

        actualiserBtn.addActionListener(e -> loadData());

        mainPanel.setBackground(new Color(236, 239, 241));
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, Ikon iconCode) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        JButton button = new JButton(text, icon);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setFocusPainted(false);
        return button;
    }

    private void showProduitDialog(Produit produit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
                                  produit == null ? "Nouveau produit" : "Modifier produit",
                                  true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Panel principal avec padding
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Style moderne pour les champs
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
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annuler");

        // Gestionnaires d'événements
        okButton.addActionListener(evt -> {
            try {
                validateAndSaveProduit(produit, nomField, categorieCombo, prixAchatField, prixVenteField, stockField, seuilField);
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

    private void addFormField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field, int row) {
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

    private void validateAndSaveProduit(Produit produit, JTextField nomField, JComboBox<String> categorieCombo,
                                     JTextField prixAchatField, JTextField prixVenteField,
                                     JTextField stockField, JTextField seuilField) {
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