package com.poissonnerie.view;

import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import javax.swing.border.EmptyBorder;
import java.text.NumberFormat;
import java.util.Locale;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    // Couleurs et polices
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(0, 135, 136);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font VALUE_FONT = new Font("Arial", Font.BOLD, 24);
    private static final int ANIMATION_DELAY = 50;
    private static final int ANIMATION_STEPS = 10;

    public AccueilViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(BACKGROUND_COLOR);

        venteController = new VenteController();
        produitController = new ProduitController();
        caisseController = new CaisseController();

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        // Panel principal avec GridLayout 2x2
        JPanel kpiPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        kpiPanel.setBackground(BACKGROUND_COLOR);

        // Initialisation des labels avec formatage monétaire
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        ventesJourLabel = new JLabel(currencyFormat.format(0), SwingConstants.CENTER);
        produitsRuptureLabel = new JLabel("0", SwingConstants.CENTER);
        encaissementsJourLabel = new JLabel(currencyFormat.format(0), SwingConstants.CENTER);
        chiffreAffairesLabel = new JLabel(currencyFormat.format(0), SwingConstants.CENTER);

        // Création des cartes KPI avec icônes
        kpiPanel.add(createKPIPanel("Ventes du jour", ventesJourLabel, SUCCESS_COLOR, MaterialDesign.MDI_CART));
        kpiPanel.add(createKPIPanel("Produits en rupture", produitsRuptureLabel, DANGER_COLOR, MaterialDesign.MDI_ALERT_CIRCLE));
        kpiPanel.add(createKPIPanel("Encaissements du jour", encaissementsJourLabel, WARNING_COLOR, MaterialDesign.MDI_CASH));
        kpiPanel.add(createKPIPanel("Chiffre d'affaires", chiffreAffairesLabel, PRIMARY_COLOR, MaterialDesign.MDI_CHART_LINE));

        // Bouton d'actualisation avec icône
        JButton refreshButton = createStyledButton("Actualiser", MaterialDesign.MDI_REFRESH, PRIMARY_COLOR);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshButton.setEnabled(false);
                Timer timer = new Timer(1000, event -> {
                    loadData();
                    refreshButton.setEnabled(true);
                });
                timer.setRepeats(false);
                timer.start();
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.add(refreshButton);

        // Layout final
        mainPanel.add(kpiPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createKPIPanel(String title, JLabel valueLabel, Color color, MaterialDesign icon) {
        JPanel panel = new JPanel(new BorderLayout(5, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(BACKGROUND_COLOR);

        // Panel pour le titre avec icône
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        titlePanel.setBackground(BACKGROUND_COLOR);

        // Icône
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(24);
        fontIcon.setIconColor(color);
        JLabel iconLabel = new JLabel(fontIcon);

        // Titre
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(color);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);

        // Valeur
        valueLabel.setFont(VALUE_FONT);
        valueLabel.setForeground(color);

        // Effet de survol
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                panel.setBackground(new Color(245, 245, 245));
                titlePanel.setBackground(new Color(245, 245, 245));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                panel.setBackground(BACKGROUND_COLOR);
                titlePanel.setBackground(BACKGROUND_COLOR);
            }
        });

        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private void loadData() {
        SwingUtilities.invokeLater(() -> {
            try {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Chargement des données
                venteController.chargerVentes();
                produitController.chargerProduits();
                caisseController.chargerMouvements();

                // Mise à jour des KPIs avec animation
                animateKPIUpdate();

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

    private void animateKPIUpdate() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);

        // Calcul des valeurs finales
        double finalVentesJour = calculerVentesJour();
        int finalProduitsRupture = calculerProduitsRupture();
        double finalEncaissementsJour = calculerEncaissementsJour();
        double finalCA = calculerChiffreAffaires();

        // Animation des mises à jour
        Timer timer = new Timer(ANIMATION_DELAY, null);
        final int[] step = {0};

        timer.addActionListener(e -> {
            if (step[0] >= ANIMATION_STEPS) {
                // Valeurs finales
                ventesJourLabel.setText(currencyFormat.format(finalVentesJour));
                produitsRuptureLabel.setText(String.valueOf(finalProduitsRupture));
                encaissementsJourLabel.setText(currencyFormat.format(finalEncaissementsJour));
                chiffreAffairesLabel.setText(currencyFormat.format(finalCA));
                timer.stop();
            } else {
                // Valeurs intermédiaires
                double progress = (double) step[0] / ANIMATION_STEPS;
                ventesJourLabel.setText(currencyFormat.format(finalVentesJour * progress));
                produitsRuptureLabel.setText(String.valueOf((int)(finalProduitsRupture * progress)));
                encaissementsJourLabel.setText(currencyFormat.format(finalEncaissementsJour * progress));
                chiffreAffairesLabel.setText(currencyFormat.format(finalCA * progress));
                step[0]++;
            }
        });

        timer.start();
    }

    private double calculerVentesJour() {
        LocalDate today = LocalDate.now();
        return venteController.getVentes().stream()
            .filter(v -> v.getDate().toLocalDate().equals(today))
            .mapToDouble(Vente::getTotal)
            .sum();
    }

    private int calculerProduitsRupture() {
        return (int) produitController.getProduits().stream()
            .filter(p -> p.getStock() <= 0)
            .count();
    }

    private double calculerEncaissementsJour() {
        LocalDate today = LocalDate.now();
        return caisseController.getMouvements().stream()
            .filter(m -> m.getDate().toLocalDate().equals(today))
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
    }

    private double calculerChiffreAffaires() {
        return venteController.getVentes().stream()
            .mapToDouble(Vente::getTotal)
            .sum();
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode, Color color) {
        JButton button = new JButton(text);

        // Ajout de l'icône
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);
        button.setIcon(icon);

        button.setFont(TITLE_FONT);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Effet de survol amélioré
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

    public JPanel getMainPanel() {
        return mainPanel;
    }
}