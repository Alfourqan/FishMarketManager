package com.poissonnerie.view;

import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AccueilViewSwing {
    private static final Logger LOGGER = Logger.getLogger(AccueilViewSwing.class.getName());
    private final JPanel mainPanel;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final CaisseController caisseController;

    // Labels pour les KPIs
    private JLabel ventesJourLabel;
    private JLabel produitsRuptureLabel;
    private JLabel encaissementsJourLabel;
    private JLabel chiffreAffairesLabel;

    public AccueilViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        venteController = new VenteController();
        produitController = new ProduitController();
        caisseController = new CaisseController();

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        // Panel principal pour les KPIs avec GridLayout 2x2
        JPanel kpiPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        kpiPanel.setBackground(Color.WHITE);

        // Création des cartes KPI
        kpiPanel.add(createKPIPanel("Ventes (Aujourd'hui)", ventesJourLabel = new JLabel("0.00 €"), 
            new Color(76, 175, 80)));

        kpiPanel.add(createKPIPanel("Produits en rupture", produitsRuptureLabel = new JLabel("0"), 
            new Color(244, 67, 54)));

        kpiPanel.add(createKPIPanel("Encaissements (Aujourd'hui)", encaissementsJourLabel = new JLabel("0.00 €"), 
            new Color(33, 150, 243)));

        kpiPanel.add(createKPIPanel("Chiffre d'affaires", chiffreAffairesLabel = new JLabel("0.00 €"), 
            new Color(156, 39, 176)));

        // Bouton d'actualisation
        JButton refreshButton = createStyledButton("Actualiser");
        refreshButton.addActionListener(e -> loadData());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(refreshButton);

        // Layout final
        mainPanel.add(kpiPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createKPIPanel(String title, JLabel valueLabel, Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(Color.WHITE);

        // Titre
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(33, 33, 33));

        // Valeur
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private void loadData() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Désactiver l'interface pendant le chargement
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Charger les données
                venteController.chargerVentes();
                produitController.chargerProduits();
                caisseController.chargerMouvements();

                // Calculer les KPIs
                updateVentesJour();
                updateProduitsRupture();
                updateEncaissementsJour();
                updateChiffreAffaires();

                LOGGER.info("Données du tableau de bord mises à jour avec succès");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors du chargement des données", e);
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors du chargement des données : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            } finally {
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private void updateVentesJour() {
        LocalDate today = LocalDate.now();
        double totalVentes = venteController.getVentes().stream()
            .filter(v -> v.getDate().toLocalDate().equals(today))
            .mapToDouble(Vente::getTotal)
            .sum();
        ventesJourLabel.setText(String.format("%.2f €", totalVentes));
    }

    private void updateProduitsRupture() {
        long produitsRupture = produitController.getProduits().stream()
            .filter(p -> p.getQuantite() == 0)
            .count();
        produitsRuptureLabel.setText(String.valueOf(produitsRupture));
    }

    private void updateEncaissementsJour() {
        LocalDate today = LocalDate.now();
        double totalEncaissements = caisseController.getMouvements().stream()
            .filter(m -> m.getDate().toLocalDate().equals(today))
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
        encaissementsJourLabel.setText(String.format("%.2f €", totalEncaissements));
    }

    private void updateChiffreAffaires() {
        double totalCA = venteController.getVentes().stream()
            .mapToDouble(Vente::getTotal)
            .sum();
        chiffreAffairesLabel.setText(String.format("%.2f €", totalCA));
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(new Color(33, 150, 243));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(33, 150, 243).darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(33, 150, 243));
            }
        });

        return button;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}