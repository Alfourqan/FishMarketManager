package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import com.poissonnerie.util.PDFGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import javax.swing.table.*;
import javax.swing.border.TitledBorder;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.jfree.chart.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.awt.event.ItemEvent;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import javax.imageio.ImageIO;


public class ReportViewSwing {
    private static final Logger LOGGER = Logger.getLogger(ReportViewSwing.class.getName());

    // UI Constants
    private static final Color PRIMARY_COLOR = new Color(0, 135, 136);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    private static final String MSG_ERREUR_GENERATION = "Erreur lors de la génération du rapport : ";
    private static final String MSG_SUCCES_GENERATION = "Le rapport a été généré dans le fichier : ";

    private final JPanel mainPanel;
    private final ReportController reportController;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final FournisseurController fournisseurController;
    private JPanel statistiquesPanel;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private JComboBox<String> categorieCombo;
    private JComboBox<String> periodeCombo;
    private JComboBox<String> modePaiementCombo;

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
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void genererRapport(String type, List<?> donnees, String nomFichier) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            switch (type.toLowerCase()) {
                case "ventes":
                    reportController.genererRapportVentesPDF(dateDebut.atStartOfDay(),
                        dateFin.atTime(23, 59, 59), outputStream);
                    break;
                case "stocks":
                    Map<String, Double> statsStocks = reportController.calculerStatistiquesStocks(
                        (List<Produit>) donnees);
                    reportController.genererRapportStocksPDF((List<Produit>) donnees,
                        statsStocks, outputStream);
                    break;
                case "fournisseurs":
                    reportController.genererRapportFournisseursPDF(outputStream);
                    break;
                case "creances":
                    reportController.genererRapportCreancesPDF(outputStream);
                    break;
                case "chiffre_affaires":
                    reportController.genererRapportFinancierPDF(dateDebut.atStartOfDay(),
                        dateFin.atTime(23, 59, 59), outputStream);
                    break;
            }

            try (FileOutputStream fos = new FileOutputStream(nomFichier)) {
                fos.write(outputStream.toByteArray());
            }


            showSuccessMessage("Succès", MSG_SUCCES_GENERATION + nomFichier);
            ouvrirFichier(nomFichier);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport", e);
            showErrorMessage("Erreur", MSG_ERREUR_GENERATION + e.getMessage());
        }
    }

    private void updateCharts() {
        statistiquesPanel.removeAll();

        try {
            addChartFromData(
                reportController.analyserVentesParPeriode(dateDebut.atStartOfDay(),
                    dateFin.atTime(23, 59, 59)),
                "Évolution des ventes", "Période", "Montant (€)",
                ChartType.LINE
            );

            addChartFromData(
                reportController.analyserModePaiement(dateDebut.atStartOfDay(),
                    dateFin.atTime(23, 59, 59)),
                "Modes de paiement", null, null,
                ChartType.PIE
            );

            Map<String, Double> rentabiliteData = reportController.analyserTendancesVentes(
                dateDebut.atStartOfDay(), dateFin.atTime(23, 59, 59));

            Map<String, Double> rentabiliteParProduit = rentabiliteData.entrySet().stream()
                .filter(e -> e.getKey().startsWith("Rentabilité "))
                .collect(Collectors.toMap(
                    e -> e.getKey().replace("Rentabilité ", ""),
                    Map.Entry::getValue
                ));

            addChartFromData(
                rentabiliteParProduit,
                "Rentabilité par produit", "Produit", "Rentabilité (%)",
                ChartType.BAR
            );

            Map<String, Double> stocksDoubleData = reportController.analyserStocksParCategorie()
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> (double) e.getValue()
                ));

            addChartFromData(
                stocksDoubleData,
                "Répartition des stocks", "Catégorie", "Quantité",
                ChartType.PIE
            );

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour des graphiques", e);
            showErrorMessage("Erreur", "Impossible de mettre à jour les graphiques : " + e.getMessage());
        }

        statistiquesPanel.revalidate();
        statistiquesPanel.repaint();
    }

    private enum ChartType {
        LINE, BAR, PIE
    }

    private void addChartFromData(Map<String, Double> data, String title, String xLabel,
                                  String yLabel, ChartType type) {
        if (data == null || data.isEmpty()) return;

        JFreeChart chart;
        switch (type) {
            case LINE:
                DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
                data.forEach((key, value) -> lineDataset.addValue(value, "Série", key));
                chart = ChartFactory.createLineChart(title, xLabel, yLabel, lineDataset);
                break;
            case BAR:
                DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
                data.forEach((key, value) -> barDataset.addValue(value, "Série", key));
                chart = ChartFactory.createBarChart(title, xLabel, yLabel, barDataset);
                break;
            case PIE:
                DefaultPieDataset pieDataset = new DefaultPieDataset();
                data.forEach(pieDataset::setValue);
                chart = ChartFactory.createPieChart(title, pieDataset, true, true, false);
                break;
            default:
                return;
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));
        addStyledChartPanel(chartPanel, title);
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

    private void showSuccessMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.ERROR_MESSAGE);
    }

    private void ouvrirFichier(String nomFichier) {
        try {
            File file = new File(nomFichier);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Impossible d'ouvrir le fichier", e);
        }
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

        panel.add(createFiltresPanel());
        panel.add(Box.createVerticalStrut(15));
        panel.add(createReportButtonsPanel());
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createFiltresPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBackground(new Color(236, 239, 241));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR),
            "Filtres", TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION, REGULAR_FONT, PRIMARY_COLOR
        ));

        categorieCombo = new JComboBox<>(new String[]{
            "Toutes catégories", "Poissons frais", "Crustacés", "Coquillages"});
        periodeCombo = new JComboBox<>(new String[]{
            "Aujourd'hui", "Cette semaine", "Ce mois", "Cette année"});
        modePaiementCombo = new JComboBox<>(new String[]{
            "Tous modes", "Espèces", "Carte", "Crédit"});

        panel.add(new JLabel("Catégorie:"));
        panel.add(categorieCombo);
        panel.add(new JLabel("Période:"));
        panel.add(periodeCombo);
        panel.add(new JLabel("Mode de paiement:"));
        panel.add(modePaiementCombo);

        categorieCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) updateCharts();
        });

        periodeCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDateRange();
                updateCharts();
                updateKPIs();
            }
        });

        modePaiementCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) updateCharts();
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
            "Rapports", TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION, REGULAR_FONT, PRIMARY_COLOR
        ));

        addReportButton(panel, "Ventes", MaterialDesign.MDI_CART);
        addReportButton(panel, "Stocks", MaterialDesign.MDI_PACKAGE_VARIANT);
        addReportButton(panel, "Fournisseurs", MaterialDesign.MDI_TRUCK_DELIVERY);
        addReportButton(panel, "Créances", MaterialDesign.MDI_CASH_MULTIPLE);
        addReportButton(panel, "Chiffre d'affaires", MaterialDesign.MDI_CHART_LINE);

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
                        dateDebut.atStartOfDay(), dateFin.atTime(23, 59, 59), nomFichier);
                    break;
                case "Chiffre d'affaires":
                    reportController.genererRapportFinancierExcel(
                        dateDebut.atStartOfDay(), dateFin.atTime(23, 59, 59), nomFichier);
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

        JPanel headerPanel = createHeaderPanel();
        JPanel kpiPanel = createKPIPanel();

        statistiquesPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        statistiquesPanel.setBackground(Color.WHITE);
        statistiquesPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(Color.WHITE);
        mainContent.add(kpiPanel, BorderLayout.NORTH);
        mainContent.add(statistiquesPanel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        updateKPIs();
        updateCharts();

        mainPanel.add(panel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel titleLabel = new JLabel("Tableau de bord", SwingConstants.LEFT);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel periodLabel = new JLabel("Période : " + formatPeriod(), SwingConstants.RIGHT);
        periodLabel.setFont(REGULAR_FONT);
        periodLabel.setForeground(new Color(100, 100, 100));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(periodLabel, BorderLayout.EAST);

        return headerPanel;
    }

    private String formatPeriod() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateDebut.format(formatter) + " - " + dateFin.format(formatter);
    }

    private JPanel createKPIPanel() {
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiPanel.setBackground(Color.WHITE);
        kpiPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        return kpiPanel;
    }

    private void updateKPIs() {
        try {
            updateKPIDisplay(reportController.calculerKPIs(
                dateDebut.atStartOfDay(), dateFin.atTime(23, 59, 59)));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour des KPIs", e);
            showErrorMessage("Erreur", "Impossible de mettre à jour les KPIs : " + e.getMessage());
        }
    }

    private void updateKPIDisplay(Map<String, Double> kpis) {
        JPanel kpiPanel = createKPIPanel();
        kpiPanel.removeAll();

        addKPICard(kpiPanel, "Chiffre d'affaires",
            String.format("%.2f €", kpis.getOrDefault("Chiffre d'affaires", 0.0)),
            MaterialDesign.MDI_CURRENCY_EUR);

        addKPICard(kpiPanel, "Panier moyen",
            String.format("%.2f €", kpis.getOrDefault("Panier moyen", 0.0)),
            MaterialDesign.MDI_CART);

        addKPICard(kpiPanel, "Marge moyenne",
            String.format("%.1f%%", kpis.getOrDefault("Taux de marge moyen (%)", 0.0)),
            MaterialDesign.MDI_CHART_LINE);

        addKPICard(kpiPanel, "Rotation stock",
            String.format("%.1f", kpis.getOrDefault("Taux de rotation du stock", 0.0)),
            MaterialDesign.MDI_REFRESH);

        kpiPanel.revalidate();
        kpiPanel.repaint();
    }

    private void addKPICard(JPanel container, String title, String value, MaterialDesign iconCode) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(24);
        icon.setIconColor(PRIMARY_COLOR);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(100, 100, 100));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(new Color(33, 33, 33));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(Color.WHITE);
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        card.add(new JLabel(icon), BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        container.add(card);
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
    private JDialog previewDialog;
    private JLabel previewLabel;
    private byte[] currentPdfData;
    private int currentPage;
    private int totalPages;

    private void afficherPrevisualisation(byte[] pdfData) {
        try {
            if (previewDialog == null) {
                initializePreviewDialog();
            }

            this.currentPdfData = pdfData;
            this.currentPage = 1;

            // Calculer le nombre total de pages
            PdfReader reader = new PdfReader(pdfData);
            this.totalPages = reader.getNumberOfPages();
            reader.close();

            // Afficher la première page
            afficherPage(1);
            previewDialog.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage de la prévisualisation", e);
            showErrorMessage("Erreur", "Impossible d'afficher la prévisualisation : " + e.getMessage());
        }
    }

    private void initializePreviewDialog() {
        previewDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
            "Prévisualisation du rapport", true);
        previewDialog.setLayout(new BorderLayout());
        previewDialog.setSize(800, 1000);
        previewDialog.setLocationRelativeTo(null);

        JPanel navigationPanel = new JPanel(new FlowLayout());
        JButton previousButton = createStyledButton("Précédent",
            MaterialDesign.MDI_CHEVRON_LEFT, PRIMARY_COLOR);
        JButton nextButton = createStyledButton("Suivant",
            MaterialDesign.MDI_CHEVRON_RIGHT, PRIMARY_COLOR);
        JButton printButton = createStyledButton("Imprimer",
            MaterialDesign.MDI_PRINTER, SUCCESS_COLOR);

        previousButton.addActionListener(e -> afficherPagePrecedente());
        nextButton.addActionListener(e -> afficherPageSuivante());
        printButton.addActionListener(e -> imprimerRapport());

        navigationPanel.add(previousButton);
        navigationPanel.add(nextButton);
        navigationPanel.add(printButton);

        previewLabel = new JLabel();
        JScrollPane scrollPane = new JScrollPane(previewLabel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        previewDialog.add(navigationPanel, BorderLayout.NORTH);
        previewDialog.add(scrollPane, BorderLayout.CENTER);
    }

    private void afficherPage(int pageNumber) {
        try {
            if (currentPdfData != null && pageNumber > 0 && pageNumber <= totalPages) {
                // Créer une image à partir de la page PDF
                PdfReader reader = new PdfReader(currentPdfData);
                BufferedImage image = generateImageFromPDF(reader, pageNumber);
                reader.close();

                if (image != null) {
                    ImageIcon icon = new ImageIcon(image);
                    previewLabel.setIcon(icon);
                    previewDialog.setTitle(String.format("Prévisualisation du rapport (Page %d/%d)",
                        pageNumber, totalPages));
                    currentPage = pageNumber;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage de la page " + pageNumber, e);
            showErrorMessage("Erreur", "Impossible d'afficher la page : " + e.getMessage());
        }
    }

    private BufferedImage generateImageFromPDF(PdfReader reader, int pageNumber) {
        try {
            // Créer une image avec une résolution suffisante
            BufferedImage image = new BufferedImage(2000, 2800, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());

            // Extraire le texte de la page
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            SimpleTextExtractionStrategy strategy = parser.processContent(
                pageNumber, new SimpleTextExtractionStrategy());
            String text = strategy.getResultantText();

            // Dessiner le texte sur l'image
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Serif", Font.PLAIN, 12));
            drawPDFContent(g2d, text);
            g2d.dispose();

            return image;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération de l'image", e);
            return null;
        }
    }

    private void drawPDFContent(Graphics2D g2d, String text) {
        int y = 50;
        for (String line : text.split("\n")) {
            g2d.drawString(line, 50, y);
            y += 15;
        }
    }

    private void afficherPageSuivante() {
        if (currentPage < totalPages) {
            afficherPage(currentPage + 1);
        }
    }

    private void afficherPagePrecedente() {
        if (currentPage > 1) {
            afficherPage(currentPage - 1);
        }
    }

    private void imprimerRapport() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            PageFormat pageFormat = job.defaultPage();
            job.setPrintable(new Printable() {
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                    throws PrinterException {
                    if (pageIndex >= totalPages) {
                        return Printable.NO_SUCH_PAGE;
                    }

                    try {
                        Graphics2D g2d = (Graphics2D) graphics;
                        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                        // Générer l'image de la page
                        PdfReader reader = new PdfReader(currentPdfData);
                        BufferedImage image = generateImageFromPDF(reader, pageIndex + 1);
                        reader.close();

                        if (image != null) {
                            double scale = Math.min(
                                pageFormat.getImageableWidth() / image.getWidth(),
                                pageFormat.getImageableHeight() / image.getHeight()
                            );
                            g2d.scale(scale, scale);
                            g2d.drawImage(image, 0, 0, null);
                        }

                        return Printable.PAGE_EXISTS;
                    } catch (Exception e) {
                        throw new PrinterException(e.getMessage());
                    }
                }
            });

            if (job.printDialog()) {
                job.print();
                showSuccessMessage("Impression", "Le rapport a été envoyé à l'imprimante avec succès");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression", e);
            showErrorMessage("Erreur", "Impossible d'imprimer le rapport : " + e.getMessage());
        }
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

        table.getTableHeader().setBackground(new Color(240, 240, 240));
        table.getTableHeader().setForeground(new Color(33, 33, 33));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        table.getColumnModel().getColumn(3).setCellRenderer((TableCellRenderer) (table1, value, isSelected, hasFocus, row, column) -> {
            JButton button = new JButton("Détails");
            button.setBackground(PRIMARY_COLOR);
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
                button.setBackground(PRIMARY_COLOR);
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
            MaterialDesign.MDI_FILE_DOCUMENT, PRIMARY_COLOR);
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
}