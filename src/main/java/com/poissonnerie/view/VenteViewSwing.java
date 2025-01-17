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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

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
    private JComboBox<Object> clientCombo;
    private JComboBox<Object> produitCombo;
    private JCheckBox creditCheck;
    private JLabel totalLabel;

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
            System.out.println("Chargement des données de vente...");

            // Sauvegarder la sélection actuelle
            int selectedRow = tableVentes.getSelectedRow();

            // Désactiver les contrôles pendant le chargement
            setControlsEnabled(false);
            mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try {
                // Charger les données
                produitController.chargerProduits();
                System.out.println("Produits chargés");

                clientController.chargerClients();
                System.out.println("Clients chargés");

                venteController.chargerVentes();
                System.out.println("Ventes chargées: " + venteController.getVentes().size() + " ventes");

                // Mettre à jour l'interface
                refreshComboBoxes();
                refreshVentesTable();

                // Restaurer la sélection si possible
                if (selectedRow >= 0 && selectedRow < tableVentes.getRowCount()) {
                    tableVentes.setRowSelectionInterval(selectedRow, selectedRow);
                }

                System.out.println("Données chargées avec succès");
            } finally {
                // Réactiver les contrôles
                setControlsEnabled(true);
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des données : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        clientCombo.setEnabled(enabled && creditCheck.isSelected());
        produitCombo.setEnabled(enabled);
        creditCheck.setEnabled(enabled);
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Section nouvelle vente
        JPanel nouvelleVentePanel = createNouvelleVentePanel();
        JPanel historiquePanel = createHistoriquePanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                nouvelleVentePanel, historiquePanel);
        splitPane.setResizeWeight(0.5);

        // Bouton d'actualisation
        JPanel actionPanel = createActionPanel();

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
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
        ajouterBtn.addActionListener(e -> {/* TODO */});
        modifierBtn.addActionListener(e -> {/* TODO */});
        supprimerBtn.addActionListener(e -> {/* TODO */});
        actualiserBtn.addActionListener(e -> actualiserDonnees());

        // Ajout des boutons au panel
        actionPanel.add(ajouterBtn);
        actionPanel.add(modifierBtn);
        actionPanel.add(supprimerBtn);
        actionPanel.add(actualiserBtn);

        return actionPanel;
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

    private void actualiserDonnees() {
        try {
            System.out.println("Début de l'actualisation des données...");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Sauvegarder l'état actuel
            int selectedRow = tableVentes.getSelectedRow();

            // Recharger les données
            loadData();

            // Restaurer la sélection
            if (selectedRow >= 0 && selectedRow < tableVentes.getRowCount()) {
                tableVentes.setRowSelectionInterval(selectedRow, selectedRow);
            }

            System.out.println("Actualisation terminée avec succès");
            JOptionPane.showMessageDialog(mainPanel,
                "Données actualisées avec succès",
                "Succès",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            System.err.println("Erreur lors de l'actualisation: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors de l'actualisation : " + ex.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
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
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD));
        JButton validerBtn = new JButton("Valider la vente");
        JButton annulerBtn = new JButton("Annuler");

        footerPanel.add(totalLabel);
        footerPanel.add(validerBtn);
        footerPanel.add(annulerBtn);

        // Event handlers
        ajouterBtn.addActionListener(e -> {
            try {
                Object selectedItem = produitCombo.getSelectedItem();
                if (!(selectedItem instanceof Produit)) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Veuillez sélectionner un produit valide",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Produit produit = (Produit) selectedItem;
                String quantiteText = quantiteField.getText().trim();
                if (quantiteText.isEmpty()) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Veuillez entrer une quantité",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int quantite;
                try {
                    quantite = Integer.parseInt(quantiteText);
                    if (quantite <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "La quantité doit être un nombre entier positif",
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

                // Vérifier si le produit est déjà dans le panier
                Optional<Vente.LigneVente> ligneExistante = panier.stream()
                    .filter(ligne -> ligne.getProduit().getId() == produit.getId())
                    .findFirst();

                if (ligneExistante.isPresent()) {
                    Vente.LigneVente ligne = ligneExistante.get();
                    int nouvelleQuantite = ligne.getQuantite() + quantite;
                    if (nouvelleQuantite > produit.getStock()) {
                        JOptionPane.showMessageDialog(mainPanel,
                            "Stock insuffisant pour ajouter " + quantite + " unités supplémentaires.\n" +
                                "Quantité déjà dans le panier : " + ligne.getQuantite() + "\n" +
                                "Stock disponible : " + produit.getStock(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ligne.setQuantite(nouvelleQuantite);
                } else {
                    Vente.LigneVente ligne = new Vente.LigneVente(produit, quantite, produit.getPrixVente());
                    panier.add(ligne);
                }

                updatePanierTable();
                quantiteField.setText("");
                produitCombo.setSelectedIndex(-1);
                quantiteField.requestFocus();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de l'ajout au panier : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        validerBtn.addActionListener(e -> {
            if (panier.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Le panier est vide",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (creditCheck.isSelected() && clientCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez sélectionner un client pour une vente à crédit",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                Client selectedClient = creditCheck.isSelected() ?
                    (Client) clientCombo.getSelectedItem() : null;

                Vente vente = new Vente(
                    0,
                    LocalDateTime.now(),
                    selectedClient,
                    creditCheck.isSelected(),
                    calculateTotal()
                );
                vente.setLignes(new ArrayList<>(panier));

                // Générer la prévisualisation du ticket
                String preview = PDFGenerator.genererPreviewTicket(vente);

                // Créer une fenêtre de prévisualisation
                JDialog previewDialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(mainPanel),
                    "Prévisualisation du ticket", true);
                previewDialog.setLayout(new BorderLayout(10, 10));

                // Zone de texte pour la prévisualisation
                JTextArea previewArea = new JTextArea(preview);
                previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                previewArea.setEditable(false);
                previewArea.setBackground(Color.WHITE);
                JScrollPane scrollPane = new JScrollPane(previewArea);
                previewDialog.add(scrollPane, BorderLayout.CENTER);

                // Panneau de boutons
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton confirmerBtn = new JButton("Confirmer et imprimer");
                JButton cancelBtn = new JButton("Annuler");

                confirmerBtn.addActionListener(confirmEvent -> {
                    try {
                        venteController.enregistrerVente(vente);
                        PDFGenerator.genererTicket(vente, "ticket_" + vente.getId() + ".pdf");
                        previewDialog.dispose();

                        resetForm();
                        refreshComboBoxes();
                        refreshVentesTable();

                        JOptionPane.showMessageDialog(mainPanel,
                            "<html>Vente enregistrée avec succès<br>Ticket généré: <b>ticket_" +
                                vente.getId() + ".pdf</b></html>",
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(previewDialog,
                            "Erreur lors de l'enregistrement de la vente : " + ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });

                cancelBtn.addActionListener(cancelEvent -> previewDialog.dispose());

                buttonPanel.add(confirmerBtn);
                buttonPanel.add(cancelBtn);
                previewDialog.add(buttonPanel, BorderLayout.SOUTH);

                // Configurer et afficher la fenêtre de prévisualisation
                previewDialog.setSize(500, 600);
                previewDialog.setLocationRelativeTo(mainPanel);
                previewDialog.setVisible(true);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de la prévisualisation : " + ex.getMessage(),
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
                String.format("%s (%s)", ligne.getProduit().getNom(),
                    ligne.getProduit().getCategorie()),
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

        List<Vente> ventesTriees = new ArrayList<>(venteController.getVentes());
        // Tri des ventes par date décroissante
        ventesTriees.sort((v1, v2) -> v2.getDate().compareTo(v1.getDate()));

        for (Vente vente : ventesTriees) {
            ventesModel.addRow(new Object[]{
                vente.getDate().format(formatter),
                vente.getClient() != null ? vente.getClient().getNom() : "Vente comptant",
                vente.isCredit() ? "Crédit" : "Comptant",
                String.format("%.2f €", vente.getTotal())
            });
        }

        System.out.println("Table des ventes mise à jour avec " + ventesTriees.size() + " ventes");
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
        DefaultComboBoxModel<Object> clientModel = new DefaultComboBoxModel<>();
        clientModel.addElement(null);
        List<Client> clients = new ArrayList<>(clientController.getClients());
        clients.sort(Comparator.comparing(Client::getNom));
        clients.forEach(clientModel::addElement);
        clientCombo.setModel(clientModel);

        // Mise à jour ComboBox produits
        DefaultComboBoxModel<Object> produitModel = new DefaultComboBoxModel<>();
        produitModel.addElement(null);

        // Regrouper les produits par catégorie
        Map<String, List<Produit>> produitsParCategorie = produitController.getProduits().stream()
            .filter(p -> p.getStock() > 0)
            .collect(Collectors.groupingBy(
                Produit::getCategorie,
                TreeMap::new,
                Collectors.toList()
            ));

        // Ajouter les produits par catégorie
        produitsParCategorie.forEach((categorie, produits) -> {
            produitModel.addElement("━━━ " + categorie.toUpperCase() + " ━━━");
            produits.stream()
                .sorted(Comparator.comparing(Produit::getNom))
                .forEach(produitModel::addElement);
        });

        produitCombo.setModel(produitModel);

        // Configuration des renderers personnalisés
        clientCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                if (value == null) {
                    value = "⚪ Sélectionner un client";
                } else if (value instanceof Client) {
                    Client client = (Client) value;
                    value = String.format("%s%s",
                        client.getSolde() > 0 ? "⚠️ " : "👤 ",
                        client.getNom() + (client.getSolde() > 0 ?
                            String.format(" (Crédit: %.2f €)", client.getSolde()) : "")
                    );
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        produitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                if (value == null) {
                    value = "⚪ Sélectionner un produit";
                } else if (value instanceof String && ((String) value).startsWith("━━━")) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value,
                                                                              index, isSelected,
                                                                              cellHasFocus);
                    label.setBackground(new Color(240, 240, 240));
                    label.setForeground(new Color(70, 70, 70));
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                    return label;
                } else if (value instanceof Produit) {
                    Produit produit = (Produit) value;
                    String stockInfo = produit.getStock() <= produit.getSeuilAlerte() ?
                        String.format("⚠️ %d en stock", produit.getStock()) :
                        String.format("📦 %d en stock", produit.getStock());

                    value = String.format("%s • %.2f € • %s",
                        produit.getNom(),
                        produit.getPrixVente(),
                        stockInfo
                    );

                    Component c = super.getListCellRendererComponent(list, value, index,
                                                                    isSelected, cellHasFocus);
                    if (produit.getStock() <= produit.getSeuilAlerte()) {
                        c.setForeground(new Color(200, 0, 0));
                    }
                    return c;
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        // Empêcher la sélection des séparateurs
        produitCombo.addActionListener(e -> {
            Object selectedItem = produitCombo.getSelectedItem();
            if (selectedItem instanceof String && ((String) selectedItem).startsWith("━━━")) {
                produitCombo.setSelectedItem(null);
            }
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
    private void setCursor(Cursor cursor) {
        mainPanel.setCursor(cursor);
    }
}