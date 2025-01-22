package com.poissonnerie.view;

import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.model.Produit;
import com.poissonnerie.model.InventaireManager;
import com.poissonnerie.model.InventaireManager.InventaireObserver;
import com.poissonnerie.model.InventaireManager.AjustementStock;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

public class InventaireViewSwing {
    private static final Logger LOGGER = Logger.getLogger(InventaireViewSwing.class.getName());
    private final JPanel mainPanel;
    private final ProduitController produitController;
    private final InventaireManager inventaireManager;
    private final JTable tableInventaire;
    private final DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private TableRowSorter<DefaultTableModel> sorter;
    private JPanel statsPanel;

    public InventaireViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        produitController = new ProduitController();
        inventaireManager = new InventaireManager();
        LOGGER.info("Initialisation de InventaireViewSwing");

        // Configuration de la table
        String[] columnNames = {"Nom", "Catégorie", "Stock actuel", "Seuil d'alerte", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableInventaire = new JTable(tableModel);
        configureTable();

        // Ajout du système de tri et filtrage
        sorter = new TableRowSorter<>(tableModel);
        tableInventaire.setRowSorter(sorter);

        // Observer pour les alertes de stock
        setupStockObserver();

        initializeComponents();
        loadData();
    }

    private void configureTable() {
        tableInventaire.setRowHeight(45);
        tableInventaire.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableInventaire.setShowGrid(true);
        tableInventaire.setGridColor(new Color(226, 232, 240));
        tableInventaire.setBackground(Color.WHITE);
        tableInventaire.setSelectionBackground(new Color(219, 234, 254));
        tableInventaire.setSelectionForeground(new Color(15, 23, 42));
        tableInventaire.setIntercellSpacing(new Dimension(1, 1));

        // Style des lignes alternées
        tableInventaire.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }

                // Coloration des lignes selon le niveau de stock
                if (!isSelected && column == 2) {
                    int stock = Integer.parseInt(value.toString());
                    int seuilAlerte = Integer.parseInt(table.getValueAt(row, 3).toString());
                    if (stock == 0) {
                        c.setBackground(new Color(254, 226, 226));
                        c.setForeground(new Color(185, 28, 28));
                    } else if (stock <= seuilAlerte) {
                        c.setBackground(new Color(254, 243, 199));
                        c.setForeground(new Color(161, 98, 7));
                    }
                }

                // Ajouter un padding aux cellules
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
            }
        });

        // Style amélioré des en-têtes
        JTableHeader header = tableInventaire.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);

                label.setHorizontalAlignment(JLabel.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(51, 65, 85)),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)
                ));
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                label.setBackground(new Color(31, 41, 55));
                label.setForeground(Color.WHITE);
                label.setOpaque(true);

                return label;
            }
        });

        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 56));
    }

    private void setupStockObserver() {
        inventaireManager.ajouterObserver(new InventaireObserver() {
            @Override
            public void onStockBas(Produit produit) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus("⚠️ Stock bas pour " + produit.getNom());
                    refreshTable();
                    updateStatistiques();
                });
            }

            @Override
            public void onRuptureStock(Produit produit) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus("⛔ Rupture de stock pour " + produit.getNom());
                    refreshTable();
                    updateStatistiques();
                });
            }

            @Override
            public void onStockAjuste(Produit produit, int ancienStock, int nouveauStock) {
                SwingUtilities.invokeLater(() -> {
                    String message = String.format("Stock ajusté pour %s : %d → %d",
                        produit.getNom(), ancienStock, nouveauStock);
                    updateStatus(message);
                    refreshTable();
                    updateStatistiques();
                });
            }
        });
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(236, 239, 241));

        // Panel du haut avec recherche et filtres
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(236, 239, 241));

        // Panel de recherche
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);

        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(200, 35));
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher un produit...");

        categoryFilter = new JComboBox<>(new String[]{"Toutes les catégories"});
        categoryFilter.setPreferredSize(new Dimension(150, 35));

        // Panel des boutons aligné à droite
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton refreshBtn = createStyledButton("Actualiser", new Color(156, 39, 176));
        JButton historiqueBtn = createStyledButton("Historique", new Color(3, 169, 244));

        buttonPanel.add(historiqueBtn);
        buttonPanel.add(refreshBtn);

        searchPanel.add(searchField);
        searchPanel.add(categoryFilter);

        // Label de statut
        statusLabel = new JLabel("Prêt");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Panel des statistiques
        statsPanel = createStatsPanel();

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        // Configuration de la table
        JScrollPane scrollPane = new JScrollPane(tableInventaire);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableInventaire.getColumnModel().getColumn(4).setMaxWidth(150);

        // Événements
        setupEventListeners(refreshBtn, historiqueBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.EAST);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBackground(new Color(236, 239, 241));
        return panel;
    }

    private void updateStatistiques() {
        statsPanel.removeAll();
        List<Produit> produits = produitController.getProduits();
        Map<String, Double> stats = inventaireManager.calculerStatistiquesInventaire(produits);

        // Création des labels de statistiques avec style
        addStatLabel("Valeur totale du stock:",
            String.format("%.2f €", stats.get("valeur_totale")));
        addStatLabel("Taux de rotation moyen:",
            String.format("%.2f", stats.get("taux_rotation_moyen")));
        addStatLabel("Produits en alerte:",
            String.format("%.1f%%", stats.get("pourcentage_alerte")));

        statsPanel.revalidate();
        statsPanel.repaint();
    }

    private void addStatLabel(String title, String value) {
        JPanel statPanel = new JPanel(new BorderLayout(5, 2));
        statPanel.setOpaque(false);
        statPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(51, 65, 85));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueLabel.setForeground(new Color(15, 23, 42));

        statPanel.add(titleLabel, BorderLayout.NORTH);
        statPanel.add(valueLabel, BorderLayout.CENTER);

        statsPanel.add(statPanel);
        statsPanel.add(Box.createVerticalStrut(10));
    }

    private void setupEventListeners(JButton refreshBtn, JButton historiqueBtn) {
        refreshBtn.addActionListener(e -> loadData());

        historiqueBtn.addActionListener(e -> showHistoriqueDialog());

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        categoryFilter.addActionListener(e -> filter());
    }

    private void filter() {
        RowFilter<DefaultTableModel, Object> rf = null;
        List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();

        // Filtre de recherche
        String searchText = searchField.getText();
        if (searchText.length() > 0) {
            filters.add(RowFilter.regexFilter("(?i)" + searchText));
        }

        // Filtre de catégorie
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        if (selectedCategory != null && !selectedCategory.equals("Toutes les catégories")) {
            filters.add(RowFilter.regexFilter("^" + selectedCategory + "$", 1));
        }

        // Combiner les filtres
        if (!filters.isEmpty()) {
            rf = RowFilter.andFilter(filters);
        }

        sorter.setRowFilter(rf);
    }

    private void showHistoriqueDialog() {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(mainPanel),
            "Historique des ajustements", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Modèle de table pour l'historique
        String[] columns = {"Date", "Produit", "Ancien stock", "Nouveau stock", "Raison"};
        DefaultTableModel historiqueModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable historiqueTable = new JTable(historiqueModel);
        historiqueTable.setRowHeight(30);

        // Remplir la table avec l'historique
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        for (AjustementStock ajustement : inventaireManager.getHistorique()) {
            historiqueModel.addRow(new Object[]{
                ajustement.getDate().format(formatter),
                ajustement.getProduit().getNom(),
                ajustement.getAncienStock(),
                ajustement.getNouveauStock(),
                ajustement.getRaison()
            });
        }

        JScrollPane scrollPane = new JScrollPane(historiqueTable);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = createStyledButton("Fermer", new Color(244, 67, 54));
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(800, 400);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    private void showAjustementDialog(Produit produit) {
        try {
            LOGGER.info("Ouverture du dialogue d'ajustement pour " + produit.getNom());
            JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(mainPanel),
                "Ajuster le stock", true);
            dialog.setLayout(new BorderLayout(10, 10));

            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Informations actuelles
            JLabel stockActuelLabel = new JLabel("Stock actuel: " + produit.getStock());
            JLabel seuilLabel = new JLabel("Seuil d'alerte: " + produit.getSeuilAlerte());

            // Champs de saisie
            JTextField quantiteField = new JTextField(10);
            JLabel quantiteLabel = new JLabel("Quantité à ajouter/retirer:");

            JTextField raisonField = new JTextField(20);
            JLabel raisonLabel = new JLabel("Raison de l'ajustement:");

            // Layout
            gbc.gridx = 0; gbc.gridy = 0;
            formPanel.add(stockActuelLabel, gbc);
            gbc.gridy = 1;
            formPanel.add(seuilLabel, gbc);
            gbc.gridy = 2;
            formPanel.add(quantiteLabel, gbc);
            gbc.gridy = 3;
            formPanel.add(quantiteField, gbc);
            gbc.gridy = 4;
            formPanel.add(raisonLabel, gbc);
            gbc.gridy = 5;
            formPanel.add(raisonField, gbc);

            // Boutons avec style moderne
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = createStyledButton("Valider", new Color(76, 175, 80));
            JButton cancelButton = createStyledButton("Annuler", new Color(244, 67, 54));

            okButton.addActionListener(e -> {
                try {
                    String input = quantiteField.getText().trim();
                    if (input.isEmpty()) {
                        throw new IllegalArgumentException("Veuillez entrer une quantité");
                    }
                    int quantite = Integer.parseInt(input);
                    String raison = raisonField.getText().trim();
                    if (raison.isEmpty()) {
                        raison = "Ajustement manuel";
                    }

                    LOGGER.info("Tentative d'ajustement du stock de " + produit.getNom() + " de " + quantite);
                    inventaireManager.ajusterStock(produit, quantite, raison);
                    produitController.mettreAJourProduit(produit);
                    LOGGER.info("Ajustement réussi pour " + produit.getNom());

                    dialog.dispose();
                    refreshTable();
                } catch (NumberFormatException ex) {
                    LOGGER.warning("Erreur de format de nombre: " + ex.getMessage());
                    JOptionPane.showMessageDialog(dialog,
                        "Veuillez entrer un nombre valide",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'ajustement", ex);
                    JOptionPane.showMessageDialog(dialog,
                        "Erreur lors de l'ajustement : " + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            });

            cancelButton.addActionListener(e -> {
                LOGGER.info("Annulation de l'ajustement");
                dialog.dispose();
            });

            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);

            dialog.add(formPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.pack();
            dialog.setLocationRelativeTo(mainPanel);
            dialog.setVisible(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage du dialogue d'ajustement", e);
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors de l'ouverture du dialogue d'ajustement : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
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

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            statusLabel.setText(timestamp + " - " + message);
            LOGGER.info("Status mis à jour: " + message);
        });
    }

    private void refreshTable() {
        LOGGER.info("Rafraîchissement de la table d'inventaire");
        SwingUtilities.invokeLater(() -> {
            try {
                tableModel.setRowCount(0);
                for (Produit produit : produitController.getProduits()) {
                    // Stocker le produit pour l'utiliser dans l'action du bouton
                    final Produit produitFinal = produit;
                    JButton ajusterBtn = createStyledButton("Ajuster", new Color(255, 165, 0));
                    ajusterBtn.addActionListener(e -> showAjustementDialog(produitFinal));

                    tableModel.addRow(new Object[]{
                        produit.getNom(),
                        produit.getCategorie(),
                        produit.getStock(),
                        produit.getSeuilAlerte(),
                        ajusterBtn
                    });
                }

                // Configurer les renderers et editors pour la colonne des boutons
                tableInventaire.getColumn("Actions").setCellRenderer(new ButtonRenderer());
                tableInventaire.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox()));

                // Mettre à jour le filtre de catégorie
                updateCategoryFilter();

                // Forcer la mise à jour de l'affichage
                tableInventaire.repaint();
                LOGGER.info("Table rafraîchie avec succès");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors du rafraîchissement de la table", e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Erreur lors du rafraîchissement de la table : " + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    // Classes internes pour le rendu des boutons dans la table
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof JButton) {
                JButton btn = (JButton) value;
                setText(btn.getText());
                setBackground(btn.getBackground());
                setForeground(btn.getForeground());
            }
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if (value instanceof JButton) {
                JButton btn = (JButton) value;
                label = btn.getText();
                button.setText(label);
                button.setBackground(btn.getBackground());
                button.setForeground(btn.getForeground());

                // Récupérer le produit associé à cette ligne
                Produit produit = produitController.getProduits().get(row);
                button.addActionListener(e -> showAjustementDialog(produit));
            }
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            isPushed = false;
            return new JButton(label);
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private void updateCategoryFilter() {
        Set<String> categories = new HashSet<>();
        categories.add("Toutes les catégories");

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            categories.add((String) tableModel.getValueAt(i, 1));
        }

        String currentSelection = (String) categoryFilter.getSelectedItem();
        categoryFilter.setModel(new DefaultComboBoxModel<>(categories.toArray(new String[0])));

        if (categories.contains(currentSelection)) {
            categoryFilter.setSelectedItem(currentSelection);
        } else {
            categoryFilter.setSelectedItem("Toutes les catégories");
        }
    }

    private void loadData() {
        try {
            updateStatus("Chargement des données...");
            produitController.chargerProduits();
            refreshTable();
            updateStatistiques();
            updateStatus("Données chargées avec succès");
        } catch (Exception e) {
            updateStatus("Erreur: " + e.getMessage());
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des données : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}