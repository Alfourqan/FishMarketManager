package com.poissonnerie.view;

import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.model.Produit;
import com.poissonnerie.model.InventaireManager;
import com.poissonnerie.model.InventaireManager.InventaireObserver;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class InventaireViewSwing {
    private static final Logger LOGGER = Logger.getLogger(InventaireViewSwing.class.getName());
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

                // Configuration du style amélioré
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

        // Observer pour les alertes de stock
        inventaireManager.ajouterObserver(new InventaireObserver() {
            @Override
            public void onStockBas(Produit produit) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus("⚠️ Stock bas pour " + produit.getNom());
                    refreshTable();
                });
            }

            @Override
            public void onRuptureStock(Produit produit) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus("⛔ Rupture de stock pour " + produit.getNom());
                    refreshTable();
                });
            }

            @Override
            public void onStockAjuste(Produit produit, int ancienStock, int nouveauStock) {
                SwingUtilities.invokeLater(() -> {
                    String message = String.format("Stock ajusté pour %s : %d → %d", 
                        produit.getNom(), ancienStock, nouveauStock);
                    updateStatus(message);
                    refreshTable();
                });
            }
        });

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(236, 239, 241));

        // Panel du haut avec titre et boutons
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(236, 239, 241));

        // Panel des boutons aligné à droite
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        // Création des boutons
        JButton refreshBtn = createStyledButton("Actualiser", new Color(156, 39, 176));
        buttonPanel.add(refreshBtn);

        // Label de statut
        statusLabel = new JLabel("Prêt");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        topPanel.add(statusLabel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // Configuration de la table
        JScrollPane scrollPane = new JScrollPane(tableInventaire);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableInventaire.getColumnModel().getColumn(4).setMaxWidth(150); // Colonne actions

        // Événements
        refreshBtn.addActionListener(e -> loadData());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
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
            JButton okButton = createStyledButton("Valider", new Color(76, 175, 80));
            JButton cancelButton = createStyledButton("Annuler", new Color(244, 67, 54));

            okButton.addActionListener(e -> {
                try {
                    String input = quantiteField.getText().trim();
                    if (input.isEmpty()) {
                        throw new IllegalArgumentException("Veuillez entrer une quantité");
                    }
                    int quantite = Integer.parseInt(input);
                    LOGGER.info("Tentative d'ajustement du stock de " + produit.getNom() + " de " + quantite);

                    inventaireManager.ajusterStock(produit, quantite);
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
                    JButton ajusterBtn = createStyledButton("Ajuster", new Color(255, 165, 0));
                    final Produit produitFinal = produit;
                    ajusterBtn.addActionListener(e -> showAjustementDialog(produitFinal));

                    tableModel.addRow(new Object[]{
                        produit.getNom(),
                        produit.getCategorie(),
                        produit.getStock(),
                        produit.getSeuilAlerte(),
                        ajusterBtn
                    });
                }

                // Configurer les renderers et editors
                tableInventaire.getColumn("Actions").setCellRenderer(new ButtonRenderer());
                tableInventaire.getColumn("Actions").setCellEditor(new ButtonEditor());

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

    public JPanel getMainPanel() {
        return mainPanel;
    }

    // Classes internes pour le rendu des boutons dans la table
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
        private ActionListener actionListener;

        public ButtonEditor() {
            super(new JTextField());
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                   boolean isSelected, int row, int column) {
            if (value instanceof JButton) {
                JButton btn = (JButton) value;
                button.setText(btn.getText());
                button.setBackground(btn.getBackground());
                button.setForeground(btn.getForeground());

                // Supprimer l'ancien listener s'il existe
                if (actionListener != null) {
                    button.removeActionListener(actionListener);
                }

                // Récupérer le nouveau listener
                ActionListener[] listeners = btn.getActionListeners();
                if (listeners.length > 0) {
                    actionListener = listeners[0];
                    button.addActionListener(actionListener);
                }
            }
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Exécuter l'action après que l'édition soit terminée
                if (actionListener != null) {
                    actionListener.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, ""));
                }
            }
            isPushed = false;
            return button;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}