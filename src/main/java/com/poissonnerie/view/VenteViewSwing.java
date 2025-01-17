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
    private final DefaultTableModel ventesModel;

    public VenteViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        venteController = new VenteController();
        produitController = new ProduitController();
        clientController = new ClientController();

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

            int selectedRow = tableVentes.getSelectedRow();
            setControlsEnabled(false);
            mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            try {
                venteController.chargerVentes();
                System.out.println("Ventes chargées: " + venteController.getVentes().size() + " ventes");

                refreshVentesTable();

                if (selectedRow >= 0 && selectedRow < tableVentes.getRowCount()) {
                    tableVentes.setRowSelectionInterval(selectedRow, selectedRow);
                }

                System.out.println("Données chargées avec succès");
            } finally {
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
        tableVentes.setEnabled(enabled);
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Section historique
        JPanel historiquePanel = createHistoriquePanel();

        // Bouton d'actualisation
        JPanel actionPanel = createActionPanel();

        mainPanel.add(historiquePanel, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
    }

    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);

        // Création du bouton avec style moderne
        JButton actualiserBtn = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH, new Color(156, 39, 176));

        // Ajout du gestionnaire d'événements
        actualiserBtn.addActionListener(e -> actualiserDonnees());

        // Ajout du bouton au panel
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

    private JPanel createHistoriquePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Historique des Ventes"));

        JScrollPane scrollPane = new JScrollPane(tableVentes);
        tableVentes.setFillsViewportHeight(true);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
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

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void setCursor(Cursor cursor) {
        mainPanel.setCursor(cursor);
    }
}