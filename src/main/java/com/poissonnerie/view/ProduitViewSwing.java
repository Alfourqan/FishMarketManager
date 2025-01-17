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
        String[] columnNames = {"", "Nom", "Catégorie", "Prix", "Stock", "Seuil d'alerte"};
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
        tableProduits.setRowHeight(25);

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Boutons d'action avec icônes
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 10, 5)
        ));

        JButton ajouterBtn = createStyledButton("Ajouter", MaterialDesign.MDI_PLUS_BOX);
        JButton modifierBtn = createStyledButton("Modifier", MaterialDesign.MDI_PENCIL_BOX);
        JButton supprimerBtn = createStyledButton("Supprimer", MaterialDesign.MDI_MINUS_BOX);
        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH);

        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);
        buttonPanel.add(actualiserBtn);

        // Table avec scroll
        JScrollPane scrollPane = new JScrollPane(tableProduits);
        tableProduits.setFillsViewportHeight(true);

        // Style de la table
        tableProduits.setShowGrid(false);
        tableProduits.setIntercellSpacing(new Dimension(0, 0));

        // Gestionnaires d'événements
        ajouterBtn.addActionListener(e -> showProduitDialog(null));
        modifierBtn.addActionListener(e -> {
            int selectedRow = tableProduits.getSelectedRow();
            if (selectedRow >= 0) {
                showProduitDialog(controller.getProduits().get(selectedRow));
            } else {
                JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez sélectionner un produit à modifier",
                    "Aucune sélection",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        supprimerBtn.addActionListener(e -> {
            int selectedRow = tableProduits.getSelectedRow();
            if (selectedRow >= 0) {
                if (JOptionPane.showConfirmDialog(mainPanel,
                    "Êtes-vous sûr de vouloir supprimer ce produit ?",
                    "Confirmation de suppression",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    try {
                        controller.supprimerProduit(controller.getProduits().get(selectedRow));
                        refreshTable();
                        JOptionPane.showMessageDialog(mainPanel,
                            "Produit supprimé avec succès",
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainPanel,
                            ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez sélectionner un produit à supprimer",
                    "Aucune sélection",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        actualiserBtn.addActionListener(e -> {
            loadData();
        });

        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, Ikon iconCode) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(16);
        JButton button = new JButton(text, icon);
        button.setMargin(new Insets(5, 10, 5, 10));
        return button;
    }

    private void loadData() {
        try {
            controller.chargerProduits();
            refreshTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des produits : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
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
                String.format("%.2f €", produit.getPrix()),
                produit.getStock(),
                produit.getSeuilAlerte()
            });
        }
    }

    private void showProduitDialog(Produit produit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
                                   produit == null ? "Nouveau produit" : "Modifier produit",
                                   true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Champs du formulaire
        JTextField nomField = new JTextField(20);
        JComboBox<String> categorieCombo = new JComboBox<>(new String[]{"Frais", "Surgelé", "Transformé"});
        JTextField prixField = new JTextField(20);
        JTextField stockField = new JTextField(20);
        JTextField seuilField = new JTextField(20);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nom:"), gbc);
        gbc.gridx = 1;
        panel.add(nomField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Catégorie:"), gbc);
        gbc.gridx = 1;
        panel.add(categorieCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Prix:"), gbc);
        gbc.gridx = 1;
        panel.add(prixField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1;
        panel.add(stockField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Seuil d'alerte:"), gbc);
        gbc.gridx = 1;
        panel.add(seuilField, gbc);

        // Pré-remplissage si modification
        if (produit != null) {
            nomField.setText(produit.getNom());
            categorieCombo.setSelectedItem(produit.getCategorie());
            prixField.setText(String.valueOf(produit.getPrix()));
            stockField.setText(String.valueOf(produit.getStock()));
            seuilField.setText(String.valueOf(produit.getSeuilAlerte()));
        }

        // Boutons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annuler");

        okButton.addActionListener(evt -> {
            try {
                String nom = nomField.getText().trim();
                String categorie = (String) categorieCombo.getSelectedItem();
                String prixText = prixField.getText().trim().replace(",", ".");
                String stockText = stockField.getText().trim();
                String seuilText = seuilField.getText().trim();

                if (nom.isEmpty()) {
                    throw new IllegalArgumentException("Le nom est obligatoire");
                }

                double prix;
                try {
                    prix = Double.parseDouble(prixText);
                    if (prix <= 0) {
                        throw new IllegalArgumentException("Le prix doit être positif");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Prix invalide");
                }

                int stock;
                try {
                    stock = Integer.parseInt(stockText);
                    if (stock < 0) {
                        throw new IllegalArgumentException("Le stock ne peut pas être négatif");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Stock invalide");
                }

                int seuil;
                try {
                    seuil = Integer.parseInt(seuilText);
                    if (seuil < 0) {
                        throw new IllegalArgumentException("Le seuil d'alerte ne peut pas être négatif");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Seuil d'alerte invalide");
                }

                if (produit == null) {
                    Produit nouveauProduit = new Produit(0, nom, categorie, prix, stock, seuil);
                    controller.ajouterProduit(nouveauProduit);
                } else {
                    produit.setNom(nom);
                    produit.setCategorie(categorie);
                    produit.setPrix(prix);
                    produit.setStock(stock);
                    produit.setSeuilAlerte(seuil);
                    controller.mettreAJourProduit(produit);
                }
                refreshTable();
                dialog.dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialog,
                    e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(evt -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Finalisation du dialog
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}