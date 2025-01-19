package com.poissonnerie.view;

import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.swing.FontIcon;

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
        ventesJourLabel = createKPICard("Ventes (Aujourd'hui)", "0.00 €", 
            MaterialDesignC.CASH_MULTIPLE, new Color(76, 175, 80));

        produitsRuptureLabel = createKPICard("Produits en rupture", "0", 
            MaterialDesignA.ALERT_CIRCLE, new Color(244, 67, 54));

        encaissementsJourLabel = createKPICard("Encaissements (Aujourd'hui)", "0.00 €", 
            MaterialDesignC.CASH, new Color(33, 150, 243));

        chiffreAffairesLabel = createKPICard("Chiffre d'affaires", "0.00 €", 
            MaterialDesignC.CHART_LINE, new Color(156, 39, 176));

        // Ajout des cartes au panel
        kpiPanel.add(createKPIPanel(ventesJourLabel, "Ventes (Aujourd'hui)", 
            MaterialDesignC.CASH_MULTIPLE, new Color(76, 175, 80)));

        kpiPanel.add(createKPIPanel(produitsRuptureLabel, "Produits en rupture", 
            MaterialDesignA.ALERT_CIRCLE, new Color(244, 67, 54)));

        kpiPanel.add(createKPIPanel(encaissementsJourLabel, "Encaissements (Aujourd'hui)", 
            MaterialDesignC.CASH, new Color(33, 150, 243)));

        kpiPanel.add(createKPIPanel(chiffreAffairesLabel, "Chiffre d'affaires", 
            MaterialDesignC.CHART_LINE, new Color(156, 39, 176)));

        // Bouton d'actualisation
        JButton refreshButton = createStyledButton("Actualiser", MaterialDesignC.REFRESH);
        refreshButton.addActionListener(e -> loadData());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(refreshButton);

        // Layout final
        mainPanel.add(kpiPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createKPIPanel(JLabel valueLabel, String title, Object icon, Color color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(Color.WHITE);

        // Icône
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(32);
        fontIcon.setIconColor(color);
        JLabel iconLabel = new JLabel(fontIcon);

        // Titre
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(33, 33, 33));

        // Panel pour l'icône et le titre
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Valeur
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createKPICard(String title, String initialValue, Object icon, Color color) {
        JLabel label = new JLabel(initialValue);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
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

    private JButton createStyledButton(String text, Object iconCode) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);

        JButton button = new JButton(text);
        button.setIcon(icon);
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