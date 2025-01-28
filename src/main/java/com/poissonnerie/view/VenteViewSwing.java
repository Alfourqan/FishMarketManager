package com.poissonnerie.view;

import com.poissonnerie.controller.VenteController;
import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.*;
import com.poissonnerie.util.PDFGenerator;
import com.poissonnerie.util.TextBillPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import org.jdesktop.swingx.JXDatePicker;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.ZoneId;

public class VenteViewSwing {
    private static final Logger LOGGER = Logger.getLogger(VenteViewSwing.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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
    private static final int MAX_QUANTITE = 9999;
    private static final double MAX_PRIX_UNITAIRE = 99999.99;
    private volatile boolean isProcessingOperation = false;
    private JXDatePicker dateDebut;
    private JXDatePicker dateFin;

    public VenteViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        venteController = new VenteController();
        produitController = new ProduitController();
        clientController = new ClientController();
        panier = Collections.synchronizedList(new ArrayList<>());

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
            LOGGER.info("Chargement des données de vente...");

            int selectedRow = tableVentes.getSelectedRow();
            setControlsEnabled(false);
            mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try {
                produitController.chargerProduits();
                LOGGER.info("Produits chargés");

                clientController.chargerClients();
                LOGGER.info("Clients chargés");

                venteController.chargerVentes();
                LOGGER.info("Ventes chargées: " + venteController.getVentes().size() + " ventes");

                refreshComboBoxes();
                refreshVentesTable();

                if (selectedRow >= 0 && selectedRow < tableVentes.getRowCount()) {
                    tableVentes.setRowSelectionInterval(selectedRow, selectedRow);
                }

                LOGGER.info("Données chargées avec succès");
            } finally {
                setControlsEnabled(true);
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des données", e);
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(mainPanel,
                            "Erreur lors du chargement des données : " + e.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE));
        }
    }

    private void setControlsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            clientCombo.setEnabled(enabled && creditCheck.isSelected());
            produitCombo.setEnabled(enabled);
            creditCheck.setEnabled(enabled);
        });
    }

    private synchronized boolean checkAndSetProcessing() {
        if (isProcessingOperation) {
            LOGGER.warning("Tentative d'opération pendant qu'une autre est en cours");
            return false;
        }
        isProcessingOperation = true;
        return true;
    }

    private void releaseProcessing() {
        isProcessingOperation = false;
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[<>\"'%;)(&+]", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean validateQuantite(String quantiteText) {
        try {
            int quantite = Integer.parseInt(quantiteText);
            return quantite > 0 && quantite <= MAX_QUANTITE;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void actualiserDonnees() {
        if (!checkAndSetProcessing()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Une opération est déjà en cours, veuillez patienter",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            LOGGER.info("Début de l'actualisation des données...");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            int selectedRow = tableVentes.getSelectedRow();
            loadData();

            if (selectedRow >= 0 && selectedRow < tableVentes.getRowCount()) {
                tableVentes.setRowSelectionInterval(selectedRow, selectedRow);
            }

            LOGGER.info("Actualisation terminée avec succès");
            JOptionPane.showMessageDialog(mainPanel,
                    "Données actualisées avec succès",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'actualisation", ex);
            JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de l'actualisation : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            setCursor(Cursor.getDefaultCursor());
            releaseProcessing();
        }
    }

    private void ajouterAuPanier(Produit produit, String quantiteText) {
        if (!validateQuantite(quantiteText)) {
            JOptionPane.showMessageDialog(mainPanel,
                    String.format("La quantité doit être un nombre entier entre 1 et %d", MAX_QUANTITE),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantite = Integer.parseInt(quantiteText);

        synchronized (panier) {
            try {
                if (quantite > produit.getStock()) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Stock insuffisant. Disponible : " + produit.getStock(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Optional<Vente.LigneVente> ligneExistante = panier.stream()
                        .filter(ligne -> ligne.getProduit().getId() == produit.getId())
                        .findFirst();

                if (ligneExistante.isPresent()) {
                    Vente.LigneVente ancienneLigne = ligneExistante.get();
                    int nouvelleQuantite = ancienneLigne.getQuantite() + quantite;
                    if (nouvelleQuantite > produit.getStock()) {
                        JOptionPane.showMessageDialog(mainPanel,
                                "Stock insuffisant pour ajouter " + quantite + " unités supplémentaires.\n" +
                                        "Quantité déjà dans le panier : " + ancienneLigne.getQuantite() + "\n" +
                                        "Stock disponible : " + produit.getStock(),
                                "Erreur",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    panier.remove(ancienneLigne);
                    Vente.LigneVente nouvelleLigne = new Vente.LigneVente(produit, nouvelleQuantite, produit.getPrixVente());
                    panier.add(nouvelleLigne);
                } else {
                    Vente.LigneVente ligne = new Vente.LigneVente(produit, quantite, produit.getPrixVente());
                    panier.add(ligne);
                }

                LOGGER.info(String.format("Produit ajouté au panier: %s, quantité: %d",
                        produit.getNom(), quantite));
                updatePanierTable();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout au panier", ex);
                JOptionPane.showMessageDialog(mainPanel,
                        "Erreur lors de l'ajout au panier : " + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void validerVente() {
        if (!checkAndSetProcessing()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Une opération est déjà en cours, veuillez patienter",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            synchronized (panier) {
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
                            calculateTotal(),
                            creditCheck.isSelected() ? Vente.ModePaiement.CREDIT : Vente.ModePaiement.ESPECES
                    );
                    vente.setLignes(new ArrayList<>(panier));

                    // Utiliser uniquement TextBillPrinter pour la prévisualisation
                    TextBillPrinter printer = new TextBillPrinter(vente);
                    printer.imprimer();
                    previewText.append("MA POISSONNERIE\n\n");
                    previewText.append("Date: ").append(DATE_FORMATTER.format(vente.getDate())).append("\n");
                    if (vente.getClient() != null) {
                        previewText.append("Client: ").append(vente.getClient().getNom()).append("\n");
                    }
                    previewText.append("\nProduits:\n");
                    for (Vente.LigneVente ligne : vente.getLignes()) {
                        previewText.append(String.format("%s x%d : %.2f €\n",
                                ligne.getProduit().getNom(),
                                ligne.getQuantite(),
                                ligne.getQuantite() * ligne.getPrixUnitaire()));
                    }
                    previewText.append("\nTotal: ").append(String.format("%.2f €", vente.getTotal()));

                    JTextArea previewArea = new JTextArea(previewText.toString());
                    previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                    previewArea.setEditable(false);
                    previewArea.setBackground(Color.WHITE);
                    JScrollPane scrollPane = new JScrollPane(previewArea);
                    previewDialog.add(scrollPane, BorderLayout.CENTER);

                    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                    JButton confirmerBtn = createStyledButton("Confirmer et imprimer",
                            MaterialDesign.MDI_PRINTER, new Color(76, 175, 80));
                    JButton cancelBtn = createStyledButton("Annuler",
                            MaterialDesign.MDI_CLOSE, new Color(244, 67, 54));

                    confirmerBtn.addActionListener(confirmEvent -> {
                        try {
                            venteController.enregistrerVente(vente);

                            // Utilisation du constructeur simple pour ticket de vente
                            TextBillPrinter printer = new TextBillPrinter(vente);
                            printer.imprimer();

                            previewDialog.dispose();
                            resetForm();
                            refreshComboBoxes();
                            refreshVentesTable();

                            LOGGER.info(String.format("Vente enregistrée avec succès: ID=%d, Total=%.2f€",
                                    vente.getId(), vente.getTotal()));

                            JOptionPane.showMessageDialog(mainPanel,
                                    "Vente enregistrée avec succès",
                                    "Succès",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Erreur lors de l'enregistrement de la vente", ex);
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

                    previewDialog.setSize(500, 600);
                    previewDialog.setLocationRelativeTo(mainPanel);
                    previewDialog.setVisible(true);

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la prévisualisation", ex);
                    JOptionPane.showMessageDialog(mainPanel,
                            "Erreur lors de la prévisualisation : " + ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } finally {
            releaseProcessing();
        }
    }

    private synchronized void updatePanierTable() {
        SwingUtilities.invokeLater(() -> {
            panierModel.setRowCount(0);
            double total = 0;

            synchronized (panier) {
                for (Vente.LigneVente ligne : panier) {
                    double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire();
                    panierModel.addRow(new Object[]{
                            String.format("%s (%s)",
                                    sanitizeInput(ligne.getProduit().getNom()),
                                    sanitizeInput(ligne.getProduit().getCategorie())),
                            ligne.getQuantite(),
                            String.format("%.2f €", ligne.getPrixUnitaire()),
                            String.format("%.2f €", sousTotal)
                    });
                    total += sousTotal;
                }
            }

            totalLabel.setText(String.format("Total: %.2f €", total));
        });
    }

    private synchronized void refreshVentesTable() {
        SwingUtilities.invokeLater(() -> {
            ventesModel.setRowCount(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            List<Vente> ventesTriees = new ArrayList<>(venteController.getVentes());
            ventesTriees.sort((v1, v2) -> v2.getDate().compareTo(v1.getDate()));

            for (Vente vente : ventesTriees) {
                ventesModel.addRow(new Object[]{
                        vente.getDate().format(formatter),
                        vente.getClient() != null ? sanitizeInput(vente.getClient().getNom()) : "Vente comptant",
                        vente.isCredit() ? "Crédit" : "Comptant",
                        String.format("%.2f €", vente.getTotal())
                });
            }

            LOGGER.info("Table des ventes mise à jour avec " + ventesTriees.size() + " ventes");
        });
    }

    private synchronized double calculateTotal() {
        synchronized (panier) {
            return panier.stream()
                    .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
                    .sum();
        }
    }

    private void resetForm() {
        SwingUtilities.invokeLater(() -> {
            clientCombo.setSelectedIndex(-1);
            creditCheck.setSelected(false);
            clientCombo.setEnabled(false);
            produitCombo.setSelectedIndex(-1);
            synchronized (panier) {
                panier.clear();
            }
            updatePanierTable();
        });
    }

    private synchronized void refreshComboBoxes() {
        SwingUtilities.invokeLater(() -> {
            DefaultComboBoxModel<Object> clientModel = new DefaultComboBoxModel<>();
            clientModel.addElement(null);
            List<Client> clients = new ArrayList<>(clientController.getClients());
            clients.sort(Comparator.comparing(Client::getNom));
            clients.forEach(client -> clientModel.addElement(client));
            clientCombo.setModel(clientModel);

            DefaultComboBoxModel<Object> produitModel = new DefaultComboBoxModel<>();
            produitModel.addElement(null);

            Map<String, List<Produit>> produitsParCategorie = produitController.getProduits().stream()
                    .filter(p -> p.getStock() > 0)
                    .collect(Collectors.groupingBy(
                            Produit::getCategorie,
                            TreeMap::new,
                            Collectors.toList()
                    ));

            produitsParCategorie.forEach((categorie, produits) -> {
                produitModel.addElement("━━━ " + sanitizeInput(categorie).toUpperCase() + " ━━━");
                produits.stream()
                        .sorted(Comparator.comparing(Produit::getNom))
                        .forEach(produitModel::addElement);
            });

            produitCombo.setModel(produitModel);

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
                                sanitizeInput(client.getNom()) + (client.getSolde() > 0 ?
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
                                index, isSelected, cellHasFocus);
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
                                sanitizeInput(produit.getNom()),
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
                    return super.getListCellRendererComponent(list, value, index, isSelected,
                            cellHasFocus);
                }
            });

            produitCombo.addActionListener(e -> {
                Object selectedItem = produitCombo.getSelectedItem();
                if (selectedItem instanceof String && ((String) selectedItem).startsWith("━━━")) {
                    produitCombo.setSelectedItem(null);
                }
            });
        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void setCursor(Cursor cursor) {
        mainPanel.setCursor(cursor);
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
                if (button.isEnabled()) {
                    button.setBackground(color.darker());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(color);
                }
            }
        });

        return button;
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel nouvelleVentePanel = createNouvelleVentePanel();
        JPanel historiquePanel = createHistoriquePanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                nouvelleVentePanel, historiquePanel);
        splitPane.setResizeWeight(0.5);

        JPanel actionPanel = createActionPanel();

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH, new Color(156, 39, 176));
        actualiserBtn.addActionListener(e -> actualiserDonnees());
        actionPanel.add(actualiserBtn);

        return actionPanel;
    }


    private JPanel createNouvelleVentePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Nouvelle Vente"));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clientCombo = new JComboBox<>();
        creditCheck = new JCheckBox("Vente à crédit");

        FontIcon creditIcon = FontIcon.of(MaterialDesign.MDI_CREDIT_CARD);
        creditIcon.setIconSize(16);
        creditCheck.setIcon(creditIcon);

        clientCombo.setEnabled(false);
        creditCheck.addActionListener(e -> {
            clientCombo.setEnabled(creditCheck.isSelected());
            if (!creditCheck.isSelected()) {
                clientCombo.setSelectedIndex(-1);
            }
        });

        JLabel clientLabel = new JLabel("Client:");
        FontIcon clientIcon = FontIcon.of(MaterialDesign.MDI_ACCOUNT);
        clientIcon.setIconSize(16);
        clientLabel.setIcon(clientIcon);

        headerPanel.add(clientLabel);
        headerPanel.add(clientCombo);
        headerPanel.add(creditCheck);

        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        produitCombo = new JComboBox<>();
        JTextField quantiteField = new JTextField(5);
        JButton ajouterBtn = createStyledButton("Ajouter au panier", MaterialDesign.MDI_CART_PLUS, new Color(33, 150, 243));

        JLabel produitLabel = new JLabel("Produit:");
        FontIcon produitIcon = FontIcon.of(MaterialDesign.MDI_PACKAGE);
        produitIcon.setIconSize(16);
        produitLabel.setIcon(produitIcon);

        JLabel quantiteLabel = new JLabel("Quantité:");
        FontIcon quantiteIcon = FontIcon.of(MaterialDesign.MDI_NUMERIC);
        quantiteIcon.setIconSize(16);
        quantiteLabel.setIcon(quantiteIcon);

        selectionPanel.add(produitLabel);
        selectionPanel.add(produitCombo);
        selectionPanel.add(quantiteLabel);
        selectionPanel.add(quantiteField);
        selectionPanel.add(ajouterBtn);

        JScrollPane panierScroll = new JScrollPane(tablePanier);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        totalLabel = new JLabel("Total: 0.00 €");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        FontIcon totalIcon = FontIcon.of(MaterialDesign.MDI_CASH);
        totalIcon.setIconSize(18);
        totalIcon.setIconColor(new Color(76, 175, 80));
        totalLabel.setIcon(totalIcon);

        JButton validerBtn = createStyledButton("Valider la vente", MaterialDesign.MDI_CHECK_CIRCLE, new Color(76, 175, 80));
        JButton annulerBtn = createStyledButton("Annuler", MaterialDesign.MDI_CLOSE_CIRCLE, new Color(244, 67, 54));

        footerPanel.add(totalLabel);
        footerPanel.add(validerBtn);
        footerPanel.add(annulerBtn);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(selectionPanel, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(panierScroll, BorderLayout.CENTER);
        panel.add(footerPanel, BorderLayout.SOUTH);

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

                ajouterAuPanier(produit, quantiteText);
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

        validerBtn.addActionListener(e -> validerVente());

        annulerBtn.addActionListener(e -> resetForm());

        return panel;
    }

    private JPanel createHistoriquePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Search panel for date filtering
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        FontIcon searchIcon = FontIcon.of(MaterialDesign.MDI_CALENDAR);
        searchIcon.setIconSize(18);
        searchIcon.setIconColor(new Color(33, 150, 243));

        JLabel dateDebutLabel = new JLabel("Du:");
        dateDebutLabel.setIcon(searchIcon);
        dateDebut = new JXDatePicker();
        dateDebut.setDate(new Date());
        dateDebut.setFormats(new SimpleDateFormat("dd/MM/yyyy"));

        JLabel dateFinLabel = new JLabel("Au:");
        FontIcon searchIcon2 = FontIcon.of(MaterialDesign.MDI_CALENDAR);
        searchIcon2.setIconSize(18);
        searchIcon2.setIconColor(new Color(33, 150, 243));
        dateFinLabel.setIcon(searchIcon2);
        dateFin = new JXDatePicker();
        dateFin.setDate(new Date());
        dateFin.setFormats(new SimpleDateFormat("dd/MM/yyyy"));

        JButton searchButton = createStyledButton("Rechercher", MaterialDesign.MDI_MAGNIFY, new Color(33, 150, 243));
        JButton resetButton = createStyledButton("Réinitialiser", MaterialDesign.MDI_REFRESH, new Color(244, 67, 54));

        searchPanel.add(dateDebutLabel);
        searchPanel.add(dateDebut);
        searchPanel.add(dateFinLabel);
        searchPanel.add(dateFin);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        // Title panel with search
        JPanel titlePanel = new JPanel(new BorderLayout());
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        FontIcon historyIcon = FontIcon.of(MaterialDesign.MDI_HISTORY);
        historyIcon.setIconSize(18);
        historyIcon.setIconColor(new Color(33, 150, 243));

        JLabel titleLabel = new JLabel("Historique des Ventes");
        titleLabel.setIcon(historyIcon);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelPanel.add(titleLabel);

        titlePanel.add(labelPanel, BorderLayout.WEST);
        titlePanel.add(searchPanel, BorderLayout.CENTER);

        // Rest of the panel setup
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(tableVentes);
        tableVentes.setFillsViewportHeight(true);

        // Add action listeners for search
        searchButton.addActionListener(e -> rechercherVentesParDates());
        resetButton.addActionListener(e -> {
            dateDebut.setDate(new Date());
            dateFin.setDate(new Date());
            refreshVentesTable(); // Reset to show all sales
        });

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void rechercherVentesParDates() {
        if (dateDebut.getDate() == null || dateFin.getDate() == null) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez sélectionner une date de début et de fin",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDateTime dateDebutLD = dateDebut.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .withHour(0).withMinute(0).withSecond(0);

        LocalDateTime dateFinLD = dateFin.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .withHour(23).withMinute(59).withSecond(59);

        if (dateDebutLD.isAfter(dateFinLD)) {
            JOptionPane.showMessageDialog(mainPanel,
                    "La date de début doit être antérieure à la date de fin",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            ventesModel.setRowCount(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            List<Vente> ventesTriees = venteController.getVentes().stream()
                    .filter(vente -> !vente.getDate().isBefore(dateDebutLD) && !vente.getDate().isAfter(dateFinLD))
                    .sorted((v1, v2) -> v2.getDate().compareTo(v1.getDate()))
                    .collect(Collectors.toList());

            for (Vente vente : ventesTriees) {
                ventesModel.addRow(new Object[]{
                        vente.getDate().format(formatter),
                        vente.getClient() != null ? sanitizeInput(vente.getClient().getNom()) : "Vente comptant",
                        vente.isCredit() ? "Crédit" : "Comptant",
                        String.format("%.2f €", vente.getTotal())
                });
            }

            if (ventesTriees.isEmpty()) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Aucune vente trouvée pour cette période",
                        "Information",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void validerReglement(Client client, double montantRegle) {
        if (!checkAndSetProcessing()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Une opération est déjà en cours, veuillez patienter",
                    "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // Mettre à jour le solde du client
            double ancienSolde = client.getSolde();
            double nouveauSolde = ancienSolde - montantRegle;
            client.setSolde(nouveauSolde);
            clientController.mettreAJourClient(client);

            // Générer et imprimer le reçu
            TextBillPrinter printer = new TextBillPrinter(
                    "REÇU DE RÈGLEMENT",
                    client,
                    montantRegle,
                    nouveauSolde
            );
            printer.imprimer();

            // Rafraîchir l'affichage
            refreshComboBoxes();
            refreshVentesTable();

            LOGGER.info(String.format("Règlement effectué pour le client %s: %.2f€, nouveau solde: %.2f€",
                    client.getNom(), montantRegle, nouveauSolde));

                        JOptionPane.showMessageDialog(mainPanel,
                    "Règlement enregistré avec succès",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur lors du règlement", ex);
            JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors du règlement : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            releaseProcessing();
        }
    }

    private JButton createReglerButton(Client client) {
        JButton reglerBtn = createStyledButton("Régler", MaterialDesign.MDI_CASH_MULTIPLE, new Color(0, 150, 136));
        reglerBtn.addActionListener(e -> {
            String montantStr = JOptionPane.showInputDialog(mainPanel,
                    String.format("Solde actuel: %.2f€\nMontant à régler:", client.getSolde()));

            if (montantStr != null && !montantStr.trim().isEmpty()) {
                try {                    double montant = Double.parseDouble(montantStr.replace(",", ".").trim());
                    if (montant <= 0) {
                        throw new IllegalArgumentException("Le montant doit être positif");
                    }
                    if (montant > client.getSolde()) {
                        throw new IllegalArgumentException("Le montant ne peut pas dépasser le solde");
                    }
                    validerReglement(client, montant);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Veuillez entrer un montant valide",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(mainPanel,
                            ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return reglerBtn;
    }
}