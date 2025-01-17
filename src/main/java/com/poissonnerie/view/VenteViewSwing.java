package com.poissonnerie.view;

import com.poissonnerie.controller.VenteController;
import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.*;
import com.poissonnerie.util.PDFGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

public class VenteViewSwing {
    private final JPanel mainPanel;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final ClientController clientController;
    private final JTable tableVentes;
    private final JTable tablePanier;
    private final DefaultTableModel panierModel;
    private final DefaultTableModel ventesModel;
    private final List<Vente.LigneVente> panier;
    private JComboBox<Client> clientCombo;
    private JCheckBox creditCheck;
    private JLabel totalLabel;
    private JComboBox<Produit> produitCombo;

    public VenteViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        venteController = new VenteController();
        produitController = new ProduitController();
        clientController = new ClientController();
        panier = new ArrayList<>();

        String[] panierColumns = {"Produit", "Quantité", "Prix unitaire", "Total"};
        panierModel = new DefaultTableModel(panierColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablePanier = new JTable(panierModel);

        String[] ventesColumns = {"Date", "Client", "Type", "Total"};
        ventesModel = new DefaultTableModel(ventesColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableVentes = new JTable(ventesModel);

        initializeComponents();
        loadData();
    }

    private void loadData() {
        try {
            System.out.println("Chargement des données...");
            produitController.chargerProduits();
            clientController.chargerClients();
            venteController.chargerVentes();
            refreshComboBoxes();
            refreshVentesTable();
            System.out.println("Données chargées avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des données : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Section nouvelle vente
        JPanel nouvelleVentePanel = createNouvelleVentePanel();
        JPanel historiquePanel = createHistoriquePanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                nouvelleVentePanel, historiquePanel);
        splitPane.setResizeWeight(0.5);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createNouvelleVentePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Nouvelle Vente"));

        // En-tête de vente
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clientCombo = new JComboBox<>();
        creditCheck = new JCheckBox("Vente à crédit");

        clientCombo.setEnabled(false);
        creditCheck.addActionListener(e -> {
            clientCombo.setEnabled(creditCheck.isSelected());
            if (!creditCheck.isSelected()) {
                clientCombo.setSelectedIndex(-1);
            }
        });

        headerPanel.add(new JLabel("Client:"));
        headerPanel.add(clientCombo);
        headerPanel.add(creditCheck);

        // Sélection des produits
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        produitCombo = new JComboBox<>();
        JTextField quantiteField = new JTextField(5);
        JButton ajouterBtn = new JButton("Ajouter au panier");

        selectionPanel.add(new JLabel("Produit:"));
        selectionPanel.add(produitCombo);
        selectionPanel.add(new JLabel("Quantité:"));
        selectionPanel.add(quantiteField);
        selectionPanel.add(ajouterBtn);

        // Panier
        JScrollPane panierScroll = new JScrollPane(tablePanier);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalLabel = new JLabel("Total: 0.00 €");
        JButton validerBtn = new JButton("Valider la vente");
        JButton annulerBtn = new JButton("Annuler");

        footerPanel.add(totalLabel);
        footerPanel.add(validerBtn);
        footerPanel.add(annulerBtn);

        // Event handlers
        ajouterBtn.addActionListener(e -> {
            try {
                Produit produit = (Produit) produitCombo.getSelectedItem();
                if (produit == null) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Veuillez sélectionner un produit",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String quantiteText = quantiteField.getText().trim();
                if (quantiteText.isEmpty()) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Veuillez entrer une quantité",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int quantite = Integer.parseInt(quantiteText);
                if (quantite <= 0) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "La quantité doit être supérieure à 0",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (quantite > produit.getStock()) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Stock insuffisant. Disponible : " + produit.getStock(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Vente.LigneVente ligne = new Vente.LigneVente(
                    produit,
                    quantite,
                    produit.getPrix()
                );
                panier.add(ligne);
                updatePanierTable();

                // Réinitialiser les champs
                quantiteField.setText("");
                produitCombo.setSelectedIndex(-1);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez entrer une quantité valide",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        validerBtn.addActionListener(e -> {
            if (!panier.isEmpty()) {
                if (creditCheck.isSelected() && clientCombo.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Veuillez sélectionner un client pour une vente à crédit",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Vente vente = new Vente(
                    0,
                    LocalDateTime.now(),
                    creditCheck.isSelected() ? (Client) clientCombo.getSelectedItem() : null,
                    creditCheck.isSelected(),
                    calculateTotal()
                );
                vente.setLignes(new ArrayList<>(panier));

                try {
                    venteController.enregistrerVente(vente);
                    PDFGenerator.genererFacture(vente, "facture_" + vente.getId() + ".pdf");

                    resetForm();
                    refreshComboBoxes(); // Rafraîchir les listes après la vente
                    refreshVentesTable();

                    JOptionPane.showMessageDialog(mainPanel,
                        "Vente enregistrée avec succès\nFacture générée: facture_" + vente.getId() + ".pdf",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Erreur lors de l'enregistrement de la vente : " + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(mainPanel,
                    "Le panier est vide",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        annulerBtn.addActionListener(e -> resetForm());

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(selectionPanel, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(panierScroll, BorderLayout.CENTER);
        panel.add(footerPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createHistoriquePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Historique des Ventes"));

        JScrollPane scrollPane = new JScrollPane(tableVentes);
        tableVentes.setFillsViewportHeight(true);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void updatePanierTable() {
        panierModel.setRowCount(0);
        double total = 0;

        for (Vente.LigneVente ligne : panier) {
            double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire();
            panierModel.addRow(new Object[]{
                ligne.getProduit().getNom(),
                ligne.getQuantite(),
                String.format("%.2f €", ligne.getPrixUnitaire()),
                String.format("%.2f €", sousTotal)
            });
            total += sousTotal;
        }

        totalLabel.setText(String.format("Total: %.2f €", total));
    }

    private void refreshVentesTable() {
        ventesModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Vente vente : venteController.getVentes()) {
            ventesModel.addRow(new Object[]{
                vente.getDate().format(formatter),
                vente.getClient() != null ? vente.getClient().getNom() : "Vente comptant",
                vente.isCredit() ? "Crédit" : "Comptant",
                String.format("%.2f €", vente.getTotal())
            });
        }
    }

    private double calculateTotal() {
        return panier.stream()
            .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
            .sum();
    }

    private void resetForm() {
        clientCombo.setSelectedIndex(-1);
        creditCheck.setSelected(false);
        clientCombo.setEnabled(false);
        produitCombo.setSelectedIndex(-1);
        panier.clear();
        updatePanierTable();
    }

    private void refreshComboBoxes() {
        // Mise à jour ComboBox clients
        DefaultComboBoxModel<Client> clientModel = new DefaultComboBoxModel<>();
        clientModel.addElement(null); // Option vide
        List<Client> clients = new ArrayList<>(clientController.getClients());
        Collections.sort(clients, Comparator.comparing(Client::getNom));
        for (Client client : clients) {
            clientModel.addElement(client);
        }
        clientCombo.setModel(clientModel);

        // Mise à jour ComboBox produits
        DefaultComboBoxModel<Produit> produitModel = new DefaultComboBoxModel<>();
        produitModel.addElement(null); // Option vide
        List<Produit> produits = new ArrayList<>(produitController.getProduits());
        Collections.sort(produits, Comparator.comparing(Produit::getNom));
        for (Produit produit : produits) {
            if (produit.getStock() > 0) { // N'afficher que les produits en stock
                produitModel.addElement(produit);
            }
        }
        produitCombo.setModel(produitModel);

        // Mise à jour des renderers pour un meilleur affichage
        clientCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    value = "-- Sélectionner un client --";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        produitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    value = "-- Sélectionner un produit --";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}