package com.poissonnerie.view;

import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.model.Produit;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ProduitViewSwing {
    private final JPanel mainPanel;
    private final ProduitController controller;
    private final JTable tableProduits;
    private final DefaultTableModel tableModel;

    public ProduitViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new ProduitController();

        // Création du modèle de table
        String[] columnNames = {"Nom", "Catégorie", "Prix", "Stock"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableProduits = new JTable(tableModel);

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Boutons d'action
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton ajouterBtn = new JButton("Ajouter");
        JButton modifierBtn = new JButton("Modifier");
        JButton supprimerBtn = new JButton("Supprimer");

        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);

        // Table avec scroll
        JScrollPane scrollPane = new JScrollPane(tableProduits);
        tableProduits.setFillsViewportHeight(true);

        // Gestionnaires d'événements
        ajouterBtn.addActionListener(e -> showProduitDialog(null));
        modifierBtn.addActionListener(e -> {
            int selectedRow = tableProduits.getSelectedRow();
            if (selectedRow >= 0) {
                showProduitDialog(controller.getProduits().get(selectedRow));
            }
        });
        supprimerBtn.addActionListener(e -> {
            int selectedRow = tableProduits.getSelectedRow();
            if (selectedRow >= 0) {
                if (JOptionPane.showConfirmDialog(mainPanel,
                    "Êtes-vous sûr de vouloir supprimer ce produit ?",
                    "Confirmation de suppression",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    controller.supprimerProduit(controller.getProduits().get(selectedRow));
                    refreshTable();
                }
            }
        });

        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void showProduitDialog(Produit produit) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
                                   produit == null ? "Nouveau produit" : "Modifier produit",
                                   true);
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
                double prix = Double.parseDouble(prixField.getText());
                int stock = Integer.parseInt(stockField.getText());
                int seuil = Integer.parseInt(seuilField.getText());

                if (nom.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Le nom est obligatoire");
                    return;
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
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Veuillez entrer des valeurs numériques valides pour le prix, le stock et le seuil");
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

    private void loadData() {
        controller.chargerProduits();
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Produit produit : controller.getProduits()) {
            tableModel.addRow(new Object[]{
                produit.getNom(),
                produit.getCategorie(),
                String.format("%.2f €", produit.getPrix()),
                produit.getStock()
            });
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}