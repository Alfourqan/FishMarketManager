package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.swing.FontIcon;
import com.poissonnerie.util.PDFGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;

public class ReportViewSwing {
    private final JPanel mainPanel;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final FournisseurController fournisseurController;
    private JPanel chartPanel;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public ReportViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        venteController = new VenteController();
        produitController = new ProduitController();
        fournisseurController = new FournisseurController();

        dateDebut = LocalDate.now().minusMonths(1);
        dateFin = LocalDate.now();

        initializeComponents();
    }

    private void initializeComponents() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, MaterialDesignI iconCode, Color color) {
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

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(236, 239, 241));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Sélecteur de période
        JPanel periodPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        periodPanel.setBackground(panel.getBackground());
        periodPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 135, 136), 1),
            "Période",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(0, 135, 136)
        ));

        JButton aujourdhuiBtn = createPeriodButton("Aujourd'hui", () -> {
            dateDebut = LocalDate.now();
            dateFin = LocalDate.now();
            updateCharts();
        });

        JButton semaineBtn = createPeriodButton("Cette semaine", () -> {
            dateDebut = LocalDate.now().minusWeeks(1);
            dateFin = LocalDate.now();
            updateCharts();
        });

        JButton moisBtn = createPeriodButton("Ce mois", () -> {
            dateDebut = LocalDate.now().minusMonths(1);
            dateFin = LocalDate.now();
            updateCharts();
        });

        periodPanel.add(aujourdhuiBtn);
        periodPanel.add(semaineBtn);
        periodPanel.add(moisBtn);

        // Boutons de génération de rapports avec style moderne
        JPanel reportButtonsPanel = new JPanel();
        reportButtonsPanel.setLayout(new BoxLayout(reportButtonsPanel, BoxLayout.Y_AXIS));
        reportButtonsPanel.setBackground(panel.getBackground());
        reportButtonsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 135, 136), 1),
            "Rapports",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(0, 135, 136)
        ));

        JButton ventesBtn = createStyledButton("Rapport des ventes", MaterialDesignI.CART_OUTLINE, new Color(76, 175, 80));
        JButton stocksBtn = createStyledButton("Rapport des stocks", MaterialDesignI.PACKAGE_VARIANT_OUTLINE, new Color(33, 150, 243));
        JButton fournisseursBtn = createStyledButton("Rapport fournisseurs", MaterialDesignI.TRUCK_DELIVERY_OUTLINE, new Color(255, 152, 0));
        JButton statistiquesBtn = createStyledButton("Statistiques", MaterialDesignI.CHART_BAR_STACKED, new Color(156, 39, 176));

        // Gestionnaires d'événements
        ventesBtn.addActionListener(e -> genererRapportVentes());
        stocksBtn.addActionListener(e -> genererRapportStocks());
        fournisseursBtn.addActionListener(e -> genererRapportFournisseurs());
        statistiquesBtn.addActionListener(e -> afficherStatistiques());

        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(ventesBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(stocksBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(fournisseursBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(statistiquesBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));

        panel.add(periodPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(reportButtonsPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel titleLabel = new JLabel("Tableau de bord", SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        headerPanel.add(titleLabel, BorderLayout.CENTER);

        chartPanel = new JPanel();
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JButton createReportButton(String text, MaterialDesignI iconCode) {
        JButton button = new JButton(text);
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        button.setIcon(icon);

        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setMargin(new Insets(8, 15, 8, 15));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(new Color(33, 33, 33));
        button.setBackground(Color.WHITE);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Effet de survol
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
            }
        });

        return button;
    }

    private JButton createPeriodButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(33, 33, 33));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Effet de survol
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
            }
        });

        button.addActionListener(e -> action.run());
        return button;
    }

    private void genererRapportVentes() {
        try {
            venteController.chargerVentes();
            String nomFichier = "rapport_ventes_" + LocalDate.now() + ".pdf";

            List<Vente> ventesFiltered = venteController.getVentes().stream()
                .filter(v -> !v.getDate().toLocalDate().isBefore(dateDebut) && 
                           !v.getDate().toLocalDate().isAfter(dateFin))
                .collect(Collectors.toList());

            PDFGenerator.genererRapportVentes(ventesFiltered, nomFichier);
            afficherMessageSuccess("Rapport généré", 
                "Le rapport des ventes a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            afficherMessageErreur("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
        }
    }

    private void genererRapportStocks() {
        try {
            produitController.chargerProduits();
            String nomFichier = "rapport_stocks_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportStocks(produitController.getProduits(), nomFichier);
            afficherMessageSuccess("Rapport généré", 
                "Le rapport des stocks a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            afficherMessageErreur("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
        }
    }

    private void genererRapportFournisseurs() {
        try {
            fournisseurController.chargerFournisseurs();
            String nomFichier = "rapport_fournisseurs_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportFournisseurs(fournisseurController.getFournisseurs(), nomFichier);
            afficherMessageSuccess("Rapport généré", 
                "Le rapport des fournisseurs a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            afficherMessageErreur("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
        }
    }

    private void afficherStatistiques() {
        chartPanel.removeAll();
        chartPanel.setLayout(new GridLayout(2, 2, 15, 15));

        addStatPanel("Ventes", String.format(
            "Aujourd'hui: %.2f €\nCette semaine: %.2f €\nCe mois: %.2f €",
            calculerVentesTotal(LocalDate.now(), LocalDate.now()),
            calculerVentesTotal(LocalDate.now().minusWeeks(1), LocalDate.now()),
            calculerVentesTotal(LocalDate.now().minusMonths(1), LocalDate.now())
        ));

        addStatPanel("Stock", String.format(
            "Total produits: %d\nEn alerte: %d\nValeur totale: %.2f €",
            getNombreProduits(),
            getNombreProduitsEnAlerte(),
            getValeurTotaleStock()
        ));

        addStatPanel("Fournisseurs", String.format(
            "Total: %d\nCommandes en cours: %d",
            getNombreFournisseurs(),
            getCommandesEnCours()
        ));

        addStatPanel("Performance", String.format(
            "Marge brute: %.2f %%\nRotation stock: %.1f jours",
            calculerMargeBrute(),
            calculerRotationStock()
        ));

        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private void addStatPanel(String title, String content) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(33, 33, 33));

        JTextArea contentArea = new JTextArea(content);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentArea.setEditable(false);
        contentArea.setBackground(panel.getBackground());
        contentArea.setBorder(null);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(contentArea, BorderLayout.CENTER);

        chartPanel.add(panel);
    }

    // Méthodes utilitaires pour les statistiques
    private double calculerVentesTotal(LocalDate debut, LocalDate fin) {
        try {
            venteController.chargerVentes();
            return venteController.getVentes().stream()
                .filter(v -> !v.getDate().toLocalDate().isBefore(debut) && 
                           !v.getDate().toLocalDate().isAfter(fin))
                .mapToDouble(Vente::getMontantTotal)
                .sum();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getNombreProduits() {
        try {
            produitController.chargerProduits();
            return produitController.getProduits().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getNombreProduitsEnAlerte() {
        try {
            produitController.chargerProduits();
            return (int) produitController.getProduits().stream()
                .filter(p -> p.getQuantite() <= p.getSeuilAlerte())
                .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private double getValeurTotaleStock() {
        try {
            produitController.chargerProduits();
            return produitController.getProduits().stream()
                .mapToDouble(p -> p.getPrixAchat() * p.getQuantite())
                .sum();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getNombreFournisseurs() {
        try {
            fournisseurController.chargerFournisseurs();
            return fournisseurController.getFournisseurs().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getCommandesEnCours() {
        // À implémenter selon la logique métier
        return 0;
    }

    private double calculerMargeBrute() {
        // À implémenter selon la logique métier
        return 25.5; // Exemple
    }

    private double calculerRotationStock() {
        // À implémenter selon la logique métier
        return 15.3; // Exemple
    }

    private void updateCharts() {
        afficherStatistiques();
    }

    private void ouvrirFichierPDF(String nomFichier) {
        try {
            File file = new File(nomFichier);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    private void afficherMessageSuccess(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherMessageErreur(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.ERROR_MESSAGE);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}