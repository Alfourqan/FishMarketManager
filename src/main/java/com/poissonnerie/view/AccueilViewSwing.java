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
    private Timer refreshTimer;

    // Couleurs et polices modernisées
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font VALUE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    public AccueilViewSwing() {
        mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(BACKGROUND_COLOR);

        venteController = new VenteController();
        produitController = new ProduitController();
        caisseController = new CaisseController();

        initializeComponents();
        loadData();

        // Mise à jour automatique toutes les minutes
        refreshTimer = new Timer(60000, e -> loadData());
        refreshTimer.start();
    }

    private void initializeComponents() {
        // Panel principal avec GridBagLayout pour plus de flexibilité
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 10, 10, 10);

        // En-tête avec titre et date
        JPanel headerPanel = createHeaderPanel();

        // Initialisation des labels avec formatage monétaire
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        ventesJourLabel = new JLabel(currencyFormat.format(0), SwingConstants.CENTER);
        produitsRuptureLabel = new JLabel("0", SwingConstants.CENTER);
        encaissementsJourLabel = new JLabel(currencyFormat.format(0), SwingConstants.CENTER);
        chiffreAffairesLabel = new JLabel(currencyFormat.format(0), SwingConstants.CENTER);

        // Création des cartes KPI avec disposition en grille 2x2
        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(createKPIPanel("Ventes du jour", ventesJourLabel,
            MaterialDesign.MDI_CART_OUTLINE, SUCCESS_COLOR,
            "Total des ventes réalisées aujourd'hui"), gbc);

        gbc.gridx = 1;
        contentPanel.add(createKPIPanel("Produits en rupture", produitsRuptureLabel,
            MaterialDesign.MDI_ALERT_CIRCLE_OUTLINE, DANGER_COLOR,
            "Nombre de produits en rupture de stock"), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        contentPanel.add(createKPIPanel("Encaissements", encaissementsJourLabel,
            MaterialDesign.MDI_CASH_MULTIPLE, WARNING_COLOR,
            "Total des encaissements du jour"), gbc);

        gbc.gridx = 1;
        contentPanel.add(createKPIPanel("Chiffre d'affaires", chiffreAffairesLabel,
            MaterialDesign.MDI_CHART_LINE, PRIMARY_COLOR,
            "Chiffre d'affaires total"), gbc);

        // Bouton d'actualisation avec animation
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        JButton refreshButton = createAnimatedButton("Actualiser", MaterialDesign.MDI_REFRESH);
        buttonPanel.add(refreshButton);

        // Assemblage final
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 5));
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Titre principal
        JLabel titleLabel = new JLabel("Tableau de bord");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(33, 33, 33));

        // Date du jour
        JLabel dateLabel = new JLabel(LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRANCE)));
        dateLabel.setFont(SUBTITLE_FONT);
        dateLabel.setForeground(new Color(117, 117, 117));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(dateLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    private JPanel createKPIPanel(String title, JLabel valueLabel, MaterialDesign icon, Color color, String tooltip) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        panel.setBackground(Color.WHITE);
        panel.setToolTipText(tooltip);

        // Icône
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(32);
        fontIcon.setIconColor(color);
        JLabel iconLabel = new JLabel(fontIcon);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Titre
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(color);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Valeur
        valueLabel.setFont(VALUE_FONT);
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(valueLabel);

        // Effet de survol
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                panel.setBackground(new Color(250, 250, 250));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                panel.setBackground(Color.WHITE);
            }
        });

        return panel;
    }

    private JButton createAnimatedButton(String text, MaterialDesign iconCode) {
        JButton button = new JButton(text);
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);
        button.setIcon(icon);

        button.setFont(TITLE_FONT);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Animation de rotation pour l'icône lors du clic
        button.addActionListener(e -> {
            button.setEnabled(false);
            loadData();
            Timer enableTimer = new Timer(1000, ev -> button.setEnabled(true));
            enableTimer.setRepeats(false);
            enableTimer.start();
        });

        // Effet de survol
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_COLOR.darker());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_COLOR);
                }
            }
        });

        return button;
    }

    private void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private double ventesJour = 0;
            private int produitsRupture = 0;
            private double encaissementsJour = 0;
            private double chiffreAffaires = 0;

            @Override
            protected Void doInBackground() {
                try {
                    ventesJour = calculerVentesJour();
                    produitsRupture = calculerProduitsRupture();
                    encaissementsJour = calculerEncaissementsJour();
                    chiffreAffaires = calculerChiffreAffaires();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors du calcul des KPIs", e);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    // Mise à jour de l'interface avec animation
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
                    updateLabelWithAnimation(ventesJourLabel, ventesJour, currencyFormat);
                    updateLabelWithAnimation(produitsRuptureLabel, produitsRupture, null);
                    updateLabelWithAnimation(encaissementsJourLabel, encaissementsJour, currencyFormat);
                    updateLabelWithAnimation(chiffreAffairesLabel, chiffreAffaires, currencyFormat);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de l'interface", e);
                    JOptionPane.showMessageDialog(mainPanel,
                        "Erreur lors de la mise à jour des données : " + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateLabelWithAnimation(JLabel label, Number value, NumberFormat format) {
        Timer timer = new Timer(50, null);
        final int steps = 10;
        final double increment = value.doubleValue() / steps;
        final double[] current = {0};

        timer.addActionListener(e -> {
            current[0] += increment;
            if (current[0] >= value.doubleValue()) {
                current[0] = value.doubleValue();
                timer.stop();
            }
            if (format != null) {
                label.setText(format.format(current[0]));
            } else {
                label.setText(String.format("%d", (int)current[0]));
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
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.ENTREE)
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
    }

    private double calculerChiffreAffaires() {
        return venteController.getVentes().stream()
            .mapToDouble(Vente::getTotal)
            .sum();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}