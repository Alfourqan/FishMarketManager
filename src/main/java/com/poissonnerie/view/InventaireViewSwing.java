package com.poissonnerie.view;

import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.model.Produit;
import com.poissonnerie.model.InventaireManager;
import com.poissonnerie.model.InventaireManager.InventaireObserver;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

public class InventaireViewSwing {
    private final JPanel mainPanel;
    private final ProduitController produitController;
    private final InventaireManager inventaireManager;
    private final JTable tableInventaire;
    private final DefaultTableModel tableModel;
    private JLabel statusLabel; 

    public InventaireViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        produitController = new ProduitController();
        inventaireManager = new InventaireManager();

        // Configuration de la table
        String[] columnNames = {"", "Nom", "Catégorie", "Stock actuel", "Seuil d'alerte", "Actions"};
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

        tableInventaire = new JTable(tableModel);
        tableInventaire.setRowHeight(35);

        // Observer pour les alertes de stock
        inventaireManager.ajouterObserver(new InventaireObserver() {
            @Override
            public void onStockBas(Produit produit) {
                updateStatus("⚠️ Stock bas pour " + produit.getNom());
                refreshTable();
            }

            @Override
            public void onRuptureStock(Produit produit) {
                updateStatus("⛔ Rupture de stock pour " + produit.getNom());
                refreshTable();
            }

            @Override
            public void onStockAjuste(Produit produit, int ancienStock, int nouveauStock) {
                String message = String.format("Stock ajusté pour %s : %d → %d", 
                    produit.getNom(), ancienStock, nouveauStock);
                updateStatus(message);
                refreshTable();
            }
        });

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel du haut avec titre et boutons
        JPanel topPanel = new JPanel(new BorderLayout());

        // Panel des boutons aligné à droite
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        // Création des boutons avec style moderne
        JButton refreshBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH, new Color(156, 39, 176));
        buttonPanel.add(refreshBtn);

        // Label de statut
        statusLabel = new JLabel("Prêt");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        topPanel.add(statusLabel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // Configuration de la table
        JScrollPane scrollPane = new JScrollPane(tableInventaire);
        tableInventaire.getColumnModel().getColumn(0).setMaxWidth(30);  // Colonne icône
        tableInventaire.getColumnModel().getColumn(5).setMaxWidth(150); // Colonne actions

        // Événements
        refreshBtn.addActionListener(e -> loadData());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void showAjustementDialog(Produit produit) {
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

        // Champ de saisie
        JTextField quantiteField = new JTextField(10);
        JLabel quantiteLabel = new JLabel("Quantité à ajouter/retirer:");

        // Layout
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(stockActuelLabel, gbc);
        gbc.gridy = 1;
        formPanel.add(seuilLabel, gbc);
        gbc.gridy = 2;
        formPanel.add(quantiteLabel, gbc);
        gbc.gridy = 3;
        formPanel.add(quantiteField, gbc);

        // Boutons avec style moderne
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = createStyledButton("Valider", MaterialDesign.MDI_CHECK_CIRCLE, new Color(76, 175, 80));
        JButton cancelButton = createStyledButton("Annuler", MaterialDesign.MDI_CLOSE_CIRCLE, new Color(244, 67, 54));

        okButton.addActionListener(e -> {
            try {
                int quantite = Integer.parseInt(quantiteField.getText());
                inventaireManager.ajusterStock(produit, quantite);
                produitController.mettreAJourProduit(produit);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Veuillez entrer un nombre valide",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Erreur lors de l'ajustement : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
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

    private void updateStatus(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        statusLabel.setText(timestamp + " - " + message);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);

        for (Produit produit : produitController.getProduits()) {
            // Icône d'état
            FontIcon icon;
            if (produit.getStock() == 0) {
                icon = FontIcon.of(MaterialDesign.MDI_ALERT);
                icon.setIconColor(new Color(220, 53, 69)); // Rouge pour rupture
            } else if (produit.getStock() <= produit.getSeuilAlerte()) {
                icon = FontIcon.of(MaterialDesign.MDI_ALERT_CIRCLE);
                icon.setIconColor(new Color(255, 193, 7)); // Jaune pour stock bas
            } else {
                icon = FontIcon.of(MaterialDesign.MDI_CHECKBOX_MARKED_CIRCLE);
                icon.setIconColor(new Color(40, 167, 69)); // Vert pour stock normal
            }

            // Bouton d'ajustement
            JButton ajusterBtn = createStyledButton("Ajuster", MaterialDesign.MDI_PENCIL, new Color(255,165,0));
            ajusterBtn.addActionListener(e -> showAjustementDialog(produit));

            tableModel.addRow(new Object[]{
                icon,
                produit.getNom(),
                produit.getCategorie(),
                produit.getStock(),
                produit.getSeuilAlerte(),
                ajusterBtn
            });
        }

        // Renderer spécial pour les boutons
        tableInventaire.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        tableInventaire.getColumn("Actions").setCellEditor(new ButtonEditor());
    }

    private void loadData() {
        try {
            updateStatus("Chargement des données...");
            produitController.chargerProduits();
            refreshTable();
            updateStatus("Données chargées avec succès");
        } catch (Exception e) {
            updateStatus("Erreur: " + e.getMessage());
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des données : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Classes utilitaires pour le rendu des boutons dans la table
    private class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
            if (value instanceof JButton) {
                JButton btn = (JButton) value;
                setText(btn.getText());
                setIcon(btn.getIcon());
                setBackground(btn.getBackground());
                setForeground(btn.getForeground());
            }
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        protected JButton button;

        public ButtonEditor() {
            super(new JTextField());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener((ActionEvent e) -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                   boolean isSelected, int row, int column) {
            if (value instanceof JButton) {
                JButton btn = (JButton) value;
                button.setText(btn.getText());
                button.setIcon(btn.getIcon());
                button.setBackground(btn.getBackground());
                button.setForeground(btn.getForeground());
                ActionListener[] listeners = btn.getActionListeners();
                if (listeners.length > 0) {
                    button.addActionListener(listeners[0]);
                }
            }
            return button;
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}