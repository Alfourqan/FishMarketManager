package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
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
    private JPanel chartPanel; // Pour les graphiques
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
        // Panel principal divisé en deux
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        // Panel gauche pour les boutons de génération de rapports
        JPanel leftPanel = createLeftPanel();

        // Panel droit pour l'affichage des graphiques/statistiques
        JPanel rightPanel = createRightPanel();

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(236, 239, 241));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Sélecteur de période
        JPanel periodPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        periodPanel.setBackground(panel.getBackground());
        periodPanel.setBorder(BorderFactory.createTitledBorder("Période"));

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

        // Boutons de génération de rapports
        JButton ventesBtn = createReportButton("Rapport des ventes", MaterialDesign.MDI_CART);
        JButton stocksBtn = createReportButton("Rapport des stocks", MaterialDesign.MDI_PACKAGE_VARIANT);
        JButton fournisseursBtn = createReportButton("Rapport fournisseurs", MaterialDesign.MDI_TRUCK_DELIVERY);
        JButton statistiquesBtn = createReportButton("Statistiques", MaterialDesign.MDI_CHART_BAR);

        // Gestionnaires d'événements
        ventesBtn.addActionListener(e -> genererRapportVentes());
        stocksBtn.addActionListener(e -> genererRapportStocks());
        fournisseursBtn.addActionListener(e -> genererRapportFournisseurs());
        statistiquesBtn.addActionListener(e -> afficherStatistiques());

        panel.add(periodPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(ventesBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(stocksBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(fournisseursBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(statistiquesBtn);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Titre du panel
        JLabel titleLabel = new JLabel("Statistiques et graphiques", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Panel pour les graphiques
        chartPanel = new JPanel();
        chartPanel.setBackground(Color.WHITE);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JButton createReportButton(String text, MaterialDesign iconCode) {
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

        return button;
    }

    private JButton createPeriodButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
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
        // Mise à jour des statistiques dans le panel de droite
        chartPanel.removeAll();
        chartPanel.setLayout(new GridLayout(2, 2, 10, 10));

        // Exemple de statistiques (à remplacer par de vrais graphiques)
        addStatPanel("Ventes du jour", "15 commandes\n2500 €");
        addStatPanel("Produits en stock", "150 produits\n25 en alerte");
        addStatPanel("Fournisseurs actifs", "8 fournisseurs\n3 commandes en cours");
        addStatPanel("Chiffre d'affaires", "Mensuel: 45000 €\nAnnuel: 520000 €");

        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private void addStatPanel(String title, String content) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(new Color(250, 250, 250));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea contentArea = new JTextArea(content);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        contentArea.setEditable(false);
        contentArea.setBackground(panel.getBackground());
        contentArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(contentArea);

        chartPanel.add(panel);
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