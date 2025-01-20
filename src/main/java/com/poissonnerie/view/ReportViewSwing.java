package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import com.poissonnerie.util.PDFGenerator;
import com.poissonnerie.util.ExcelGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import java.util.ArrayList;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import javax.swing.border.TitledBorder;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.SpinnerNumberModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.HashMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.swing.ImageIcon;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Sides;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrinterName;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.awt.print.Book;
import java.awt.Graphics2D;


public class ReportViewSwing {
    private static final Logger LOGGER = Logger.getLogger(ReportViewSwing.class.getName());

    // Constantes UI
    private static final Color PRIMARY_COLOR = new Color(0, 135, 136);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);
    private static final Color INFO_COLOR = new Color(33, 150, 243);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // Messages d'erreur et succès
    private static final String MSG_ERREUR_GENERATION = "Erreur lors de la génération du rapport : ";
    private static final String MSG_ERREUR_CHARGEMENT = "Erreur lors du chargement des données : ";
    private static final String MSG_SUCCES_GENERATION = "Le rapport a été généré dans le fichier : ";

    private final JPanel mainPanel;
    private final ReportController reportController;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final FournisseurController fournisseurController;
    private JPanel chartPanel;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private JComboBox<String> categorieCombo;
    private JComboBox<String> periodeCombo;
    private JComboBox<String> modePaiementCombo;
    private JPanel statistiquesPanel;

    private JDialog previewDialog;
    private JLabel previewLabel;
    private int currentPage = 0;
    private PDDocument currentDocument;
    private PDFRenderer pdfRenderer;


    public ReportViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        reportController = new ReportController();
        venteController = new VenteController();
        produitController = new ProduitController();
        fournisseurController = new FournisseurController();

        dateDebut = LocalDate.now().minusMonths(1);
        dateFin = LocalDate.now();

        initializeComponents();
        LOGGER.info("ReportViewSwing initialisé avec succès");
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode, Color color) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);

        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setFont(REGULAR_FONT);
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

    // Added missing method required by MainViewSwing
    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void initializeComponents() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        JPanel leftPanel = createLeftPanel();
        createRightPanel();

        splitPane.setLeftComponent(leftPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(236, 239, 241));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Filtres
        JPanel filtresPanel = createFiltresPanel();

        // Boutons de génération de rapports
        JPanel reportButtonsPanel = createReportButtonsPanel();

        panel.add(filtresPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(reportButtonsPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createFiltresPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBackground(new Color(236, 239, 241));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR),
            "Filtres",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            REGULAR_FONT,
            PRIMARY_COLOR
        ));

        // Filtre Catégorie
        categorieCombo = new JComboBox<>(new String[]{"Toutes catégories", "Poissons frais", "Crustacés", "Coquillages"});
        panel.add(new JLabel("Catégorie:"));
        panel.add(categorieCombo);

        // Filtre Période
        periodeCombo = new JComboBox<>(new String[]{"Aujourd'hui", "Cette semaine", "Ce mois", "Cette année"});
        panel.add(new JLabel("Période:"));
        panel.add(periodeCombo);

        // Filtre Mode de paiement
        modePaiementCombo = new JComboBox<>(new String[]{"Tous modes", "Espèces", "Carte", "Crédit"});
        panel.add(new JLabel("Mode de paiement:"));
        panel.add(modePaiementCombo);

        // Événements des filtres
        categorieCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateCharts();
            }
        });

        periodeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDateRange();
                updateCharts();
            }
        });

        modePaiementCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateCharts();
            }
        });

        return panel;
    }

    private void updateDateRange() {
        LocalDate now = LocalDate.now();
        switch (periodeCombo.getSelectedIndex()) {
            case 0: // Aujourd'hui
                dateDebut = now;
                dateFin = now;
                break;
            case 1: // Cette semaine
                dateDebut = now.minusWeeks(1);
                dateFin = now;
                break;
            case 2: // Ce mois
                dateDebut = now.minusMonths(1);
                dateFin = now;
                break;
            case 3: // Cette année
                dateDebut = now.minusYears(1);
                dateFin = now;
                break;
        }
    }

    private JPanel createReportButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(236, 239, 241));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR),
            "Rapports",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            REGULAR_FONT,
            PRIMARY_COLOR
        ));

        // Boutons avec options d'export
        addReportButton(panel, "Ventes", MaterialDesign.MDI_CART);
        addReportButton(panel, "Stocks", MaterialDesign.MDI_PACKAGE_VARIANT);
        addReportButton(panel, "Fournisseurs", MaterialDesign.MDI_TRUCK_DELIVERY);
        addReportButton(panel, "Créances", MaterialDesign.MDI_CASH_MULTIPLE);
        addReportButton(panel, "Chiffre d'affaires", MaterialDesign.MDI_CHART_LINE);
        addReportButton(panel, "Ticket", MaterialDesign.MDI_TICKET); // Add Ticket button

        return panel;
    }

    private void addReportButton(JPanel panel, String type, MaterialDesign icon) {
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonGroup.setBackground(new Color(236, 239, 241));

        JButton pdfBtn = createStyledButton(type + " (PDF)", icon, PRIMARY_COLOR);
        JButton excelBtn = createStyledButton("Excel", MaterialDesign.MDI_FILE_EXCEL, SUCCESS_COLOR);

        pdfBtn.addActionListener(e -> genererRapportPDF(type));
        excelBtn.addActionListener(e -> genererRapportExcel(type));

        buttonGroup.add(pdfBtn);
        buttonGroup.add(excelBtn);
        panel.add(buttonGroup);
        panel.add(Box.createVerticalStrut(5));
    }

    private void genererRapportPDF(String type) {
        try {
            String nomFichier = "rapport_" + type.toLowerCase() + "_" + LocalDate.now() + ".pdf";
            List<?> donnees = null;
            switch (type) {
                case "Ventes":
                    venteController.chargerVentes();
                    donnees = venteController.getVentes();
                    break;
                case "Stocks":
                    produitController.chargerProduits();
                    donnees = produitController.getProduits();
                    break;
                case "Fournisseurs":
                    fournisseurController.chargerFournisseurs();
                    donnees = fournisseurController.getFournisseurs();
                    break;
                case "Créances":
                    ClientController clientController = new ClientController();
                    clientController.chargerClients();
                    donnees = clientController.getClients();
                    break;
                case "Chiffre d'affaires":
                    venteController.chargerVentes();
                    donnees = venteController.getVentes();
                    break;
                case "Ticket":
                    try {
                        // Créer une vente factice pour la prévisualisation
                        List<Vente> ventes = new ArrayList<>();
                        // Créer un client factice
                        Client clientDemo = new Client(1, "Client Démo", "0123456789", "demo@email.com", "123 rue Demo");
                        // Créer une vente avec les paramètres requis
                        Vente venteDemo = new Vente(
                            1,                          // id
                            LocalDateTime.now(),        // date
                            clientDemo,                 // client
                            false,                      // credit
                            100.0,                      // total (maintenant en double)
                            Vente.ModePaiement.ESPECES  // mode de paiement
                        );
                        ventes.add(venteDemo);
                        donnees = ventes;
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        reportController.genererTicketPDF(ventes.get(0), outputStream);
                        byte[] pdfData = PDFGenerator.getBytes(outputStream);
                        afficherPreviewTicket(pdfData);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erreur lors de la génération du ticket", e);
                        throw new RuntimeException("Erreur lors de la génération du ticket", e);
                    }
                    break;

            }
            genererRapport(type.toLowerCase(), donnees, nomFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF", e);
            showErrorMessage("Erreur", MSG_ERREUR_GENERATION + e.getMessage());
        }
    }

    private void genererRapportExcel(String type) {
        try {
            String nomFichier = "rapport_" + type.toLowerCase() + "_" + LocalDate.now() + ".xlsx";

            switch (type) {
                case "Ventes":
                    reportController.genererRapportVentesExcel(
                        dateDebut.atStartOfDay(),
                        dateFin.atTime(23, 59, 59),
                        nomFichier
                    );
                    break;
                case "Stocks":
                    reportController.genererRapportStocksExcel(nomFichier);
                    break;
                case "Fournisseurs":
                    reportController.genererRapportFournisseursExcel(nomFichier);
                    break;
                case "Créances":
                    reportController.genererRapportCreancesExcel(nomFichier);
                    break;
                case "Chiffre d'affaires":
                    reportController.genererRapportFinancierExcel(
                        dateDebut.atStartOfDay(),
                        dateFin.atTime(23, 59, 59),
                        nomFichier
                    );
                    break;
            }

            showSuccessMessage("Succès", MSG_SUCCES_GENERATION + nomFichier);
            ouvrirFichier(nomFichier);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel", e);
            showErrorMessage("Erreur", MSG_ERREUR_GENERATION + e.getMessage());
        }
    }

    private void createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // En-tête avec titre
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel titleLabel = new JLabel("Tableau de bord", SwingConstants.LEFT);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Panel des statistiques et graphiques avec GridLayout optimisé
        statistiquesPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        statistiquesPanel.setBackground(Color.WHITE);
        statistiquesPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // ScrollPane pour gérer le défilement
        JScrollPane scrollPane = new JScrollPane(statistiquesPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Ajout des composants
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Mise à jour initiale des graphiques
        updateCharts();

        mainPanel.add(panel, BorderLayout.CENTER);
    }





    private void updateCharts() {
        statistiquesPanel.removeAll();

        // Graphique des ventes
        addVentesChart();

        // Graphique de répartition des stocks
        addStocksChart();

        // Graphique des modes de paiement
        addPaiementsChart();

        // Graphique de performance du stock
        addPerformanceChart();

        // Graphique de rentabilité par produit
        addRentabiliteChart();

        // Rafraîchissement du panel
        statistiquesPanel.revalidate();
        statistiquesPanel.repaint();
    }

    private void addPerformanceChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            Map<String, Double> performances = reportController.analyserPerformanceStock();

            for (Map.Entry<String, Double> entry : performances.entrySet()) {
                dataset.addValue(entry.getValue(), "Performance", entry.getKey());
            }

            JFreeChart chart = ChartFactory.createBarChart(
                "Performance du stock",
                "Indicateur",
                "Valeur",
                dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            addStyledChartPanel(chartPanel, "Performance du stock");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du graphique de performance", e);
            showErrorMessage("Erreur", "Impossible de générer le graphique de performance");
        }
    }

    private void addVentesChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            Map<String, Double> ventesData = reportController.analyserVentesParPeriode(
                dateDebut.atStartOfDay(),
                dateFin.atTime(23, 59, 59)
            );

            for (Map.Entry<String, Double> entry : ventesData.entrySet()) {
                dataset.addValue(entry.getValue(), "Ventes", entry.getKey());
            }

            JFreeChart chart = ChartFactory.createLineChart(
                "Évolution des ventes",
                "Période",
                "Montant (€)",
                dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            addStyledChartPanel(chartPanel, "Évolution des ventes");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du graphique des ventes", e);
            showErrorMessage("Erreur", "Impossible de générer le graphique des ventes");
        }
    }

    private void addStocksChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        try {
            Map<String, Integer> stocksData = reportController.analyserStocksParCategorie();

            for (Map.Entry<String, Integer> entry : stocksData.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart chart = ChartFactory.createPieChart(
                "Répartition des stocks",
                dataset,
                true,
                true,
                false
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            addStyledChartPanel(chartPanel, "Répartition des stocks");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du graphique des stocks", e);
            showErrorMessage("Erreur", "Impossible de générer le graphique des stocks");
        }
    }

    private void addPaiementsChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        try {
            Map<String, Double> paiementsData = reportController.analyserModePaiement(
                dateDebut.atStartOfDay(),
                dateFin.atTime(23, 59, 59)
            );

            for (Map.Entry<String, Double> entry : paiementsData.entrySet()) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart chart = ChartFactory.createPieChart(
                "Modes de paiement",
                dataset,
                true,
                true,
                false
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            addStyledChartPanel(chartPanel, "Modes de paiement");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du graphique des paiements", e);
            showErrorMessage("Erreur", "Impossible de générer le graphique des modes de paiement");
        }
    }

    private void addFournisseursChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            Map<String, Double> fournisseursData = reportController.analyserAchatsFournisseurs(
                dateDebut.atStartOfDay(),
                dateFin.atTime(23, 59, 59)
            );

            for (Map.Entry<String, Double> entry : fournisseursData.entrySet()) {
                dataset.addValue(entry.getValue(), "Achats", entry.getKey());
            }

            JFreeChart chart = ChartFactory.createBarChart(
                "Achats par fournisseur",
                "Fournisseur",
                "Montant (€)",
                dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            addStyledChartPanel(chartPanel, "Achats par fournisseur");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du graphique des fournisseurs", e);
            showErrorMessage("Erreur", "Impossible de générer le graphique des fournisseurs");
        }
    }

    private void addRentabiliteChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            Map<String, Double> rentabilites = reportController.analyserRentabiliteParProduit(
                dateDebut.atStartOfDay(),
                dateFin.atTime(23, 59, 59)
            );

            for (Map.Entry<String, Double> entry : rentabilites.entrySet()) {
                dataset.addValue(entry.getValue(), "Rentabilité (%)", entry.getKey());
            }

            JFreeChart chart = ChartFactory.createBarChart(
                "Rentabilité par produit",
                "Produit",
                "Rentabilité (%)",
                dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            addStyledChartPanel(chartPanel, "Rentabilité par produit");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du graphique de rentabilité", e);
            showErrorMessage("Erreur", "Impossible de générer le graphique de rentabilité");
        }
    }

    private void addEvolutionVentesChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            Map<String, List<Double>> evolution = reportController.analyserEvolutionVentes(
                dateDebut.atStartOfDay(),
                dateFin.atTime(23, 59, 59)
            );

            List<Double> caJournalier = evolution.get("Chiffre d'affaires journalier");
            if (caJournalier != null) {
                for (int i = 0; i < caJournalier.size(); i++) {
                    dataset.addValue(caJournalier.get(i), "CA", "Jour " + (i + 1));
                }
            }

            JFreeChart chart = ChartFactory.createLineChart(
                "Évolution du chiffre d'affaires",
                "Période",
                "Montant (€)",
                dataset
            );

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(300, 200));
            addStyledChartPanel(chartPanel, "Évolution du CA");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du graphique d'évolution", e);
            showErrorMessage("Erreur", "Impossible de générer le graphique d'évolution");
        }
    }

    private void addStyledChartPanel(ChartPanel chartPanel, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SUBTITLE_FONT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);

        statistiquesPanel.add(panel);
    }

    private void ouvrirFichier(String nomFichier) {
        try {
            File file = new File(nomFichier);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Impossible d'ouvrir le fichier", e);
            System.err.println("Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    private void showSuccessMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private JButton createReportButton(String text, MaterialDesign iconCode) {
        return createStyledButton(text, iconCode, PRIMARY_COLOR);
    }

    private JButton createPeriodButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(REGULAR_FONT);
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

    private void genererRapport(String type, List<?> donnees, String nomFichier) {
        if (donnees == null) {
            LOGGER.severe("Les données ne peuvent pas être null");
            showErrorMessage("Erreur", "Données invalides pour la génération du rapport");
            return;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            switch (type.toLowerCase()) {
                case "ventes":
                    if (!(donnees.stream().allMatch(d -> d instanceof Vente))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des ventes");
                    }
                    reportController.genererRapportVentesPDF(
                        dateDebut.atStartOfDay(),
                        dateFin.atTime(23, 59, 59),
                        outputStream
                    );
                    break;
                case "stocks":
                    if (!(donnees.stream().allMatch(d -> d instanceof Produit))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des stocks");
                    }
                    List<Produit> produits = (List<Produit>) donnees;
                    Map<String, Double> statistiques = new HashMap<>();
                    double valeurTotale = produits.stream()
                        .mapToDouble(p -> p.getPrixAchat() * p.getQuantite())
                        .sum();
                    statistiques.put("Valeur totale du stock", valeurTotale);

                    reportController.genererRapportStocksPDF(produits, statistiques, outputStream);
                    break;
                case "fournisseurs":
                    if (!(donnees.stream().allMatch(d -> d instanceof Fournisseur))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des fournisseurs");
                    }
                    reportController.genererRapportFournisseursPDF(outputStream);
                    break;
                case "creances":
                    if (!(donnees.stream().allMatch(d -> d instanceof Client))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des créances");
                    }
                    reportController.genererRapportCreancesPDF(outputStream);
                    break;
                case "ticket":
                    if (!(donnees.get(0) instanceof Vente)) {
                        throw new IllegalArgumentException("Type de données incorrect pour le ticket");
                    }
                    reportController.genererTicketPDF((Vente) donnees.get(0), outputStream);
                    break;
                default:
                    throw new IllegalArgumentException("Type de rapport inconnu: " + type);
            }

            // Afficher la prévisualisation
            byte[] pdfData = PDFGenerator.getBytes(outputStream);
            if (type.equalsIgnoreCase("ticket")) {
                // Pour les tickets, on affiche directement dans la vue de configuration
                afficherPreviewTicket(pdfData);
            } else {
                // Pour les autres rapports, on utilise la prévisualisation standard
                afficherPrevisualisation(pdfData);
            }

            // Sauvegarder le fichier uniquement pour les rapports (pas pour les tickets)
            if (!type.equalsIgnoreCase("ticket")) {
                PDFGenerator.sauvegarderPDF(pdfData, nomFichier);
                showSuccessMessage("Succès", MSG_SUCCES_GENERATION + nomFichier);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport", e);
            showErrorMessage("Erreur", MSG_ERREUR_GENERATION + e.getMessage());
        }
    }

    private void ouvrirFichierPDF(String nomFichier) {
        try {
            File file = new File(nomFichier);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Impossible d'ouvrir le fichier PDF", e);
            System.err.println("Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    private void afficherMessageSuccess(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherMessageErreur(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.ERROR_MESSAGE);
    }

    private void afficherEtatCreances() {
        try {
            ClientController clientController = new ClientController();
            clientController.chargerClients();
            List<Client> clients = clientController.getClients();

            List<Client> clientsAvecCreances = clients.stream()
                .filter(c -> c.getSolde() > 0)
                .collect(Collectors.toList());

            if (clientsAvecCreances.isEmpty()) {
                showInfoMessage("État des créances", "Aucune créance en cours.");
                return;
            }

            JDialog dialog = createCreancesDialog(clientsAvecCreances);
            dialog.setVisible(true);

        } catch (Exception e) {
            handleError("affichage des créances", e);
        }
    }

    private JDialog createCreancesDialog(List<Client> clientsAvecCreances) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
            "État des créances", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        JTextField searchField = new JTextField(20);
        searchPanel.add(new JLabel("Rechercher: "));
        searchPanel.add(searchField);


        String[] colonnes = {"Client", "Téléphone", "Solde", "Actions"};
        Object[][] donnees = new Object[clientsAvecCreances.size()][4];

        for (int i = 0; i < clientsAvecCreances.size(); i++) {
            Client client = clientsAvecCreances.get(i);
            donnees[i][0] = client.getNom();
            donnees[i][1] = client.getTelephone();
            donnees[i][2] = String.format("%.2f €", client.getSolde());
            donnees[i][3] = client;
        }

        JTable table = new JTable(donnees, colonnes) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }

            private void filter() {
                String text = searchField.getText();
                if (text.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        table.setRowHeight(30);
        table.setIntercellSpacing(new Dimension(10, 5));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(230, 230, 230));

        table.getTableHeader().setBackground(new Color(240,240, 240));
        table.getTableHeader().setForeground(new Color(33, 33, 33));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        table.getColumnModel().getColumn(3).setCellRenderer((TableCellRenderer) (table1, value, isSelected, hasFocus, row, column) -> {
            JButton button = new JButton("Détails");
            button.setBackground(INFO_COLOR);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            return button;
        });

        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int col) {
                JButton button = new JButton("Détails");
                button.setBackground(INFO_COLOR);
                button.setForeground(Color.WHITE);
                button.setFocusPainted(false);
                button.setBorderPainted(false);

                button.addActionListener(e -> {
                    Client client = (Client) value;
                    afficherDetailCreancesClient(client);
                    fireEditingStopped();
                });

                return button;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        JButton rapportGlobalBtn = createStyledButton("Rapport global des créances",
            MaterialDesign.MDI_FILE_DOCUMENT, INFO_COLOR);
        rapportGlobalBtn.addActionListener(e -> {
            genererRapport("creances", clientsAvecCreances,
                "rapport_creances_global_" + LocalDate.now() + ".pdf");
            dialog.dispose();
        });
        buttonPanel.add(rapportGlobalBtn);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        dialog.add(contentPanel, BorderLayout.CENTER);
        return dialog;
    }

    private void genererRapportCreanceClient(Client client) {
        try {
            String nomFichier = "rapport_creance_" + client.getNom().toLowerCase().replace(" ", "_")
                + "_" + LocalDate.now() + ".pdf";

            List<Client> clientSeul = new ArrayList<>();
            clientSeul.add(client);

            genererRapport("creances", clientSeul, nomFichier);
        } catch (Exception e) {
            handleError("génération du rapport de créance client", e);
        }
    }

    private void genererRapportCreances(List<Client> clients, String nomFichier) {
        try {
            genererRapport("creances", clients, nomFichier);
        } catch (Exception e) {
            handleError("génération du rapport global de créances", e);
        }
    }


    private void handleError(String operation, Exception e) {
        LOGGER.log(Level.SEVERE, "Erreur lors de " + operation, e);
        showErrorMessage("Erreur", MSG_ERREUR_GENERATION + e.getMessage());
    }

    private void afficherDetailCreancesClient(Client client) {
        try {
            JDialog dialog = createDetailDialog(client);
            dialog.setVisible(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage des détails", e);
            showErrorMessage("Erreur", "Impossible d'afficher les détails : " + e.getMessage());
        }
    }
    private JDialog createDetailDialog(Client client) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
            "Détails créances - " + client.getNom(), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);

        // Panel principal
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Informations client
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        infoPanel.add(new JLabel("Nom:"));
        infoPanel.add(new JLabel(client.getNom()));
        infoPanel.add(new JLabel("Téléphone:"));
        infoPanel.add(new JLabel(client.getTelephone()));
        infoPanel.add(new JLabel("Solde actuel:"));
        infoPanel.add(new JLabel(String.format("%.2f €", client.getSolde())));

        panel.add(infoPanel, BorderLayout.NORTH);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = createStyledButton("Fermer", MaterialDesign.MDI_CLOSE, Color.GRAY);
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(panel);

        return dialog;
    }
    private void afficherPrevisualisation(byte[] pdfData) {
        try {
            if (previewDialog == null) {
                previewDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Prévisualisation du rapport", true);
                previewDialog.setLayout(new BorderLayout());
                previewDialog.setSize(800, 1000);
                previewDialog.setLocationRelativeTo(null);

                // Panel de navigation
                JPanel navigationPanel = new JPanel(new FlowLayout());
                JButton previousButton = createStyledButton("Précédent", MaterialDesign.MDI_CHEVRON_LEFT, PRIMARY_COLOR);
                JButton nextButton = createStyledButton("Suivant", MaterialDesign.MDI_CHEVRON_RIGHT, PRIMARY_COLOR);
                JButton printButton = createStyledButton("Imprimer", MaterialDesign.MDI_PRINTER, SUCCESS_COLOR);

                previousButton.addActionListener(e -> afficherPagePrecedente());
                nextButton.addActionListener(e -> afficherPageSuivante());
                printButton.addActionListener(e -> imprimerRapport());

                navigationPanel.add(previousButton);
                navigationPanel.add(nextButton);
                navigationPanel.add(printButton);

                // Label pour l'aperçu
                previewLabel = new JLabel();
                JScrollPane scrollPane = new JScrollPane(previewLabel);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);

                previewDialog.add(navigationPanel, BorderLayout.NORTH);
                previewDialog.add(scrollPane, BorderLayout.CENTER);
            }

            // Fermer le document précédent s'il existe
            if (currentDocument != null) {
                currentDocument.close();
            }

            // Charger le PDF
            ByteArrayInputStream bais = new ByteArrayInputStream(pdfData);
            currentDocument = PDDocument.load(bais);
            pdfRenderer = new PDFRenderer(currentDocument);
            currentPage = 0;

            // Afficher la première page
            afficherPage(currentPage);
            previewDialog.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage de la prévisualisation", e);
            showErrorMessage("Erreur", "Impossible d'afficher la prévisualisation : " + e.getMessage());
        }
    }

    private void afficherPage(int pageNumber) {
        try {
            if (currentDocument != null && pageNumber >= 0 && pageNumber < currentDocument.getNumberOfPages()) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, 100);
                ImageIcon icon = new ImageIcon(image);
                previewLabel.setIcon(icon);
                previewDialog.setTitle(String.format("Prévisualisation du rapport (Page %d/%d)", 
                    pageNumber + 1, currentDocument.getNumberOfPages()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage de la page " + pageNumber, e);
            showErrorMessage("Erreur", "Impossible d'afficher la page : " + e.getMessage());
        }
    }

    private void afficherPageSuivante() {
        if (currentDocument != null && currentPage < currentDocument.getNumberOfPages() - 1) {
            currentPage++;
            afficherPage(currentPage);
        }
    }

    private void afficherPagePrecedente() {
        if (currentDocument != null && currentPage > 0) {
            currentPage--;
            afficherPage(currentPage);
        }
    }

    private void imprimerRapport() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPageable(new PDFPageable(currentDocument));

            if (job.printDialog()) {
                job.print();
                showSuccessMessage("Impression", "Le rapport a été envoyé à l'imprimante avec succès");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression", e);
            showErrorMessage("Erreur", "Impossible d'imprimer le rapport : " + e.getMessage());
        }
    }

    private static class PDFPageable implements Printable, java.awt.print.Pageable {
        private PDDocument document;

        public PDFPageable(PDDocument document) {
            this.document = document;
        }

        @Override
        public int getNumberOfPages() {
            return document.getNumberOfPages();
        }

        @Override
        public PageFormat getPageFormat(int pageIndex) {
            return PrinterJob.getPrinterJob().defaultPage();
        }

        @Override
        public Printable getPrintable(int pageIndex) {
            return this;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex >= document.getNumberOfPages()) {
                return Printable.NO_SUCH_PAGE;
            }

            try {
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                BufferedImage image = new PDFRenderer(document).renderImageWithDPI(pageIndex, 300);
                double scale = Math.min(
                    pageFormat.getImageableWidth() / image.getWidth(),
                    pageFormat.getImageableHeight() / image.getHeight()
                );

                g2d.scale(scale, scale);
                g2d.drawImage(image, 0, 0, null);

                return Printable.PAGE_EXISTS;
            } catch (IOException e) {
                throw new PrinterException(e.getMessage());
            }
        }
    }

    private void afficherPreviewTicket(byte[] pdfData) {
        try {
            // Créer une nouvelle fenêtre de prévisualisation pour le ticket
            if (previewDialog != null && previewDialog.isDisplayable()) {
                previewDialog.dispose();
            }

            previewDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Aperçu du ticket", true);
            previewDialog.setLayout(new BorderLayout());

            // Charger le PDF en mémoire
            currentDocument = PDDocument.load(new ByteArrayInputStream(pdfData));
            pdfRenderer = new PDFRenderer(currentDocument);
            currentPage = 0;

            // Créer le panel de prévisualisation
            JPanel previewPanel = new JPanel(new BorderLayout());
            previewLabel = new JLabel();
            previewLabel.setHorizontalAlignment(JLabel.CENTER);

            // Afficher la première page
            afficherPage(currentPage);

            // Ajouter les contrôles de navigation
            JPanel navigationPanel = new JPanel(new FlowLayout());
            JButton prevButton = new JButton("<<");
            JButton nextButton = new JButton(">>");
            JButton printButton = new JButton("Imprimer");

            prevButton.addActionListener(e -> {
                if (currentPage > 0) {
                    currentPage--;
                    afficherPage(currentPage);
                }
            });

            nextButton.addActionListener(e -> {
                if (currentPage < currentDocument.getNumberOfPages() - 1) {
                    currentPage++;
                    afficherPage(currentPage);
                }
            });

            printButton.addActionListener(e -> imprimerTicket());

            navigationPanel.add(prevButton);
            navigationPanel.add(printButton);
            navigationPanel.add(nextButton);

            JScrollPane scrollPane = new JScrollPane(previewLabel);
            previewPanel.add(scrollPane, BorderLayout.CENTER);
            previewPanel.add(navigationPanel, BorderLayout.SOUTH);

            previewDialog.add(previewPanel);
            previewDialog.setSize(600, 800);
            previewDialog.setLocationRelativeTo(mainPanel);
            previewDialog.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage de la prévisualisation du ticket", e);
            showErrorMessage("Erreur", "Impossible d'afficher la prévisualisation : " + e.getMessage());
        }
    }

    private void imprimerTicket() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Impression Ticket");

            PageFormat pageFormat = job.defaultPage();
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            job.setPrintable((graphics, pf, pageIndex) -> {
                if (pageIndex != 0) return Printable.NO_SUCH_PAGE;

                try {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300);
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pf.getImageableX(), pf.getImageableY());

                    // Ajuster l'échelle pour s'adapter à la page
                    double scaleX = pf.getImageableWidth() / image.getWidth();
                    double scaleY = pf.getImageableHeight() / image.getHeight();
                    double scale = Math.min(scaleX, scaleY);

                    g2d.scale(scale, scale);
                    g2d.drawImage(image, 0, 0, null);

                    return Printable.PAGE_EXISTS;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'impression", e);
                    return Printable.NO_SUCH_PAGE;
                }
            });

            if (job.printDialog()) {
                job.print();
                showSuccessMessage("Succès", "Impression lancée avec succès");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression", e);
            showErrorMessage("Erreur", "Impossible d'imprimer : " + e.getMessage());
        }
    }

}