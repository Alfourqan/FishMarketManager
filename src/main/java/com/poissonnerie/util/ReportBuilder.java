package com.poissonnerie.util;

import com.poissonnerie.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class ReportBuilder {
    private static final Logger LOGGER = Logger.getLogger(ReportBuilder.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

    public void genererRapportStocks(String cheminFichier, List<Produit> produits, String categorie, 
                                     boolean formatPDF) {
        try {
            Map<String, Object> stats = ReportStatisticsManager.analyserStocks(produits, categorie);

            if (formatPDF) {
                genererRapportStocksPDF(cheminFichier, stats);
            } else {
                genererRapportStocksExcel(cheminFichier, stats);
            }

            LOGGER.info("Rapport des stocks généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public void genererRapportVentes(String cheminFichier, List<Vente> ventes, 
                                     LocalDate debut, LocalDate fin, ModePaiement modePaiement, boolean formatPDF) {
        try {
            Map<String, Object> stats = ReportStatisticsManager.analyserVentes(ventes, debut, fin, modePaiement);

            if (formatPDF) {
                genererRapportVentesPDF(cheminFichier, stats, debut, fin);
            } else {
                genererRapportVentesExcel(cheminFichier, stats, debut, fin);
            }

            LOGGER.info("Rapport des ventes généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public void genererRapportCreances(String cheminFichier, List<Client> clients, boolean formatPDF) {
        try {
            Map<String, Object> stats = ReportStatisticsManager.analyserCreances(clients);

            if (formatPDF) {
                genererRapportCreancesPDF(cheminFichier, stats);
            } else {
                genererRapportCreancesExcel(cheminFichier, stats);
            }

            LOGGER.info("Rapport des créances généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public void genererRapportFinancier(String cheminFichier, List<Vente> ventes, 
                                        List<MouvementCaisse> mouvements, LocalDate debut, LocalDate fin, boolean formatPDF) {
        try {
            Map<String, Object> stats = ReportStatisticsManager.analyserFinances(ventes, mouvements, debut, fin);

            if (formatPDF) {
                genererRapportFinancierPDF(cheminFichier, stats, debut, fin);
            } else {
                genererRapportFinancierExcel(cheminFichier, stats, debut, fin);
            }

            LOGGER.info("Rapport financier généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    private void genererRapportStocksPDF(String cheminFichier, Map<String, Object> stats) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        // En-tête du rapport
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        document.add(new Paragraph("Rapport des Stocks", titleFont));
        document.add(new Paragraph("Généré le " + LocalDateTime.now().format(DATE_FORMATTER)));
        document.add(Chunk.NEWLINE);

        // Statistiques globales
        document.add(new Paragraph("Statistiques Globales"));
        document.add(new Paragraph("Total produits: " + stats.get("totalProduits")));
        document.add(new Paragraph("Valeur totale: " + String.format("%.2f €", stats.get("valeurTotaleStock"))));
        document.add(Chunk.NEWLINE);

        // Produits sous alerte
        @SuppressWarnings("unchecked")
        List<Produit> produitsSousAlerte = (List<Produit>) stats.get("produitsSousAlerte");
        if (produitsSousAlerte != null && !produitsSousAlerte.isEmpty()) {
            document.add(new Paragraph("Produits sous seuil d'alerte"));
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            ajouterEnteteTableauProduits(table);

            for (Produit produit : produitsSousAlerte) {
                ajouterLigneProduit(table, produit);
            }
            document.add(table);
        }

        document.close();
    }

    private void genererRapportStocksExcel(String cheminFichier, Map<String, Object> stats) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Rapport Stocks");

        // Styles
        CellStyle headerStyle = creerStyleEnTete(workbook);
        CellStyle dataStyle = creerStyleDonnees(workbook);

        // En-tête
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Référence", "Désignation", "Stock", "Prix", "Catégorie"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données
        int rowNum = 1;
        @SuppressWarnings("unchecked")
        Map<String, List<Produit>> produitsParStatut = (Map<String, List<Produit>>) stats.get("produitsParStatut");
        for (Map.Entry<String, List<Produit>> entry : produitsParStatut.entrySet()) {
            for (Produit produit : entry.getValue()) {
                Row row = sheet.createRow(rowNum++);
                ajouterLigneProduitExcel(row, produit, dataStyle);
            }
        }

        // Ajuster largeur des colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    private void ajouterEnteteTableauProduits(PdfPTable table) {
        String[] headers = {"Référence", "Désignation", "Stock", "Prix"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
    }

    private void ajouterLigneProduit(PdfPTable table, Produit produit) {
        table.addCell(produit.getReference());
        table.addCell(produit.getDesignation());
        table.addCell(String.valueOf(produit.getStock()));
        table.addCell(String.format("%.2f €", produit.getPrixVente()));
    }

    private CellStyle creerStyleEnTete(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle creerStyleDonnees(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private void ajouterLigneProduitExcel(Row row, Produit produit, CellStyle style) {
        Cell cell = row.createCell(0);
        cell.setCellValue(produit.getReference());
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue(produit.getDesignation());
        cell.setCellStyle(style);

        cell = row.createCell(2);
        cell.setCellValue(produit.getStock());
        cell.setCellStyle(style);

        cell = row.createCell(3);
        cell.setCellValue(produit.getPrixVente());
        cell.setCellStyle(style);

        cell = row.createCell(4);
        cell.setCellValue(produit.getCategorie());
        cell.setCellStyle(style);
    }

    private void genererGraphiqueVentes(String cheminFichier, Map<String, Object> stats) throws Exception {
        @SuppressWarnings("unchecked")
        Map<LocalDate, Double> ventesParJour = (Map<LocalDate, Double>) stats.get("ventesParJour");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<LocalDate, Double> entry : ventesParJour.entrySet()) {
            dataset.addValue(entry.getValue(), "Ventes", entry.getKey().format(DATE_FORMATTER));
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Évolution des ventes",
                "Date",
                "Montant (€)",
                dataset
        );
        int width = 600;
        int height = 400;
        File chartFile = new File(cheminFichier);
        ChartUtilities.saveChartAsJPEG(chartFile, chart, width, height);

    }

    private void genererRapportVentesPDF(String cheminFichier, Map<String, Object> stats, 
                                         LocalDate debut, LocalDate fin) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        // En-tête
        document.add(new Paragraph("Rapport des Ventes", TITLE_FONT));
        document.add(new Paragraph("Période: " + debut.format(DATE_FORMATTER) + " - " + fin.format(DATE_FORMATTER)));
        document.add(Chunk.NEWLINE);

        // Statistiques globales
        document.add(new Paragraph("Statistiques Globales", SUBTITLE_FONT));
        double ca = (Double) stats.get("chiffreAffaires");
        document.add(new Paragraph("Chiffre d'affaires: " + CURRENCY_FORMATTER.format(ca), NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Analyse par mode de paiement
        @SuppressWarnings("unchecked")
        Map<ModePaiement, DoubleSummaryStatistics> statsParMode = 
            (Map<ModePaiement, DoubleSummaryStatistics>) stats.get("statsParMode");
        document.add(new Paragraph("Répartition par Mode de Paiement", SUBTITLE_FONT));

        PdfPTable tableModes = new PdfPTable(3);
        tableModes.setWidthPercentage(100);
        ajouterEnTeteTableau(tableModes, new String[]{"Mode", "Nombre", "Total"});

        for (Map.Entry<ModePaiement, DoubleSummaryStatistics> entry : statsParMode.entrySet()) {
            tableModes.addCell(entry.getKey().toString());
            tableModes.addCell(String.valueOf(entry.getValue().getCount()));
            tableModes.addCell(CURRENCY_FORMATTER.format(entry.getValue().getSum()));
        }
        document.add(tableModes);
        document.add(Chunk.NEWLINE);

        // Graphique d'évolution des ventes
        String tempGraphFile = "temp_ventes_graph.jpg";
        genererGraphiqueVentes(tempGraphFile, stats);
        Image graph = Image.getInstance(tempGraphFile);
        graph.scaleToFit(500, 300);
        document.add(graph);
        new File(tempGraphFile).delete();

        document.close();
    }

    private void genererRapportVentesExcel(String cheminFichier, Map<String, Object> stats,
                                         LocalDate debut, LocalDate fin) throws Exception {
        // Implement Excel report generation for sales
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Rapport Ventes");
        // Add code to populate the sheet with sales data

        try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    private void genererRapportCreancesPDF(String cheminFichier, Map<String, Object> stats) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        // En-tête
        document.add(new Paragraph("Rapport des Créances", TITLE_FONT));
        document.add(new Paragraph("Généré le " + LocalDate.now().format(DATE_FORMATTER)));
        document.add(Chunk.NEWLINE);

        // Statistiques globales
        document.add(new Paragraph("Vue d'ensemble", SUBTITLE_FONT));
        double totalCreances = (Double) stats.get("totalCreances");
        document.add(new Paragraph("Total des créances: " + CURRENCY_FORMATTER.format(totalCreances), NORMAL_FONT));
        document.add(Chunk.NEWLINE);

        // Analyse par statut
        @SuppressWarnings("unchecked")
        Map<String, DoubleSummaryStatistics> statsParStatut = 
            (Map<String, DoubleSummaryStatistics>) stats.get("statsParStatut");
        document.add(new Paragraph("Analyse par Statut", SUBTITLE_FONT));

        PdfPTable tableStatuts = new PdfPTable(4);
        tableStatuts.setWidthPercentage(100);
        ajouterEnTeteTableau(tableStatuts, new String[]{"Statut", "Nombre", "Montant moyen", "Total"});

        for (Map.Entry<String, DoubleSummaryStatistics> entry : statsParStatut.entrySet()) {
            tableStatuts.addCell(entry.getKey());
            tableStatuts.addCell(String.valueOf(entry.getValue().getCount()));
            tableStatuts.addCell(CURRENCY_FORMATTER.format(entry.getValue().getAverage()));
            tableStatuts.addCell(CURRENCY_FORMATTER.format(entry.getValue().getSum()));
        }
        document.add(tableStatuts);
        document.add(Chunk.NEWLINE);

        // Clients en retard de paiement
        document.add(new Paragraph("Clients en Retard de Paiement", SUBTITLE_FONT));
        @SuppressWarnings("unchecked")
        List<Client> clientsRetard = (List<Client>) stats.get("clientsRetard");
        if (!clientsRetard.isEmpty()) {
            PdfPTable tableRetards = new PdfPTable(3);
            tableRetards.setWidthPercentage(100);
            ajouterEnTeteTableau(tableRetards, new String[]{"Client", "Solde", "Statut"});

            for (Client client : clientsRetard) {
                tableRetards.addCell(client.getNom());
                tableRetards.addCell(CURRENCY_FORMATTER.format(client.getSolde()));
                tableRetards.addCell(client.getStatutCreances().toString());
            }
            document.add(tableRetards);
        } else {
            document.add(new Paragraph("Aucun client en retard de paiement", NORMAL_FONT));
        }

        document.close();
    }

    private void genererRapportCreancesExcel(String cheminFichier, Map<String, Object> stats) throws Exception {
        // Implement Excel report generation for receivables
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Rapport Créances");
        // Add code to populate the sheet with receivables data

        try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    private void genererRapportFinancierPDF(String cheminFichier, Map<String, Object> stats,
                                            LocalDate debut, LocalDate fin) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        // En-tête
        document.add(new Paragraph("Rapport Financier", TITLE_FONT));
        document.add(new Paragraph("Période: " + debut.format(DATE_FORMATTER) + " - " + fin.format(DATE_FORMATTER)));
        document.add(Chunk.NEWLINE);

        // Résultats globaux
        document.add(new Paragraph("Résultats Globaux", SUBTITLE_FONT));
        double caTotal = (Double) stats.get("chiffreAffairesTotal");
        double depensesTotal = (Double) stats.get("depensesTotal");
        double beneficeNet = (Double) stats.get("beneficeNet");

        PdfPTable tableResultats = new PdfPTable(2);
        tableResultats.setWidthPercentage(100);

        tableResultats.addCell("Chiffre d'affaires");
        tableResultats.addCell(CURRENCY_FORMATTER.format(caTotal));

        tableResultats.addCell("Total des dépenses");
        tableResultats.addCell(CURRENCY_FORMATTER.format(depensesTotal));

        tableResultats.addCell("Bénéfice net");
        tableResultats.addCell(CURRENCY_FORMATTER.format(beneficeNet));

        if (stats.containsKey("tauxMargeBrute")) {
            double tauxMarge = (Double) stats.get("tauxMargeBrute");
            tableResultats.addCell("Taux de marge brute");
            tableResultats.addCell(String.format("%.2f%%", tauxMarge));
        }

        document.add(tableResultats);
        document.add(Chunk.NEWLINE);

        // Évolution quotidienne
        document.add(new Paragraph("Évolution Quotidienne", SUBTITLE_FONT));
        @SuppressWarnings("unchecked")
        Map<LocalDate, Double> caParJour = (Map<LocalDate, Double>) stats.get("chiffreAffairesParJour");
        @SuppressWarnings("unchecked")
        Map<LocalDate, Double> depensesParJour = (Map<LocalDate, Double>) stats.get("depensesParJour");

        PdfPTable tableEvolution = new PdfPTable(3);
        tableEvolution.setWidthPercentage(100);
        ajouterEnTeteTableau(tableEvolution, new String[]{"Date", "CA", "Dépenses"});

        Set<LocalDate> dates = new TreeSet<>();
        dates.addAll(caParJour.keySet());
        dates.addAll(depensesParJour.keySet());

        for (LocalDate date : dates) {
            tableEvolution.addCell(date.format(DATE_FORMATTER));
            tableEvolution.addCell(CURRENCY_FORMATTER.format(caParJour.getOrDefault(date, 0.0)));
            tableEvolution.addCell(CURRENCY_FORMATTER.format(depensesParJour.getOrDefault(date, 0.0)));
        }
        document.add(tableEvolution);

        document.close();
    }

    private void genererRapportFinancierExcel(String cheminFichier, Map<String, Object> stats,
                                             LocalDate debut, LocalDate fin) throws Exception {
        // Implement Excel report generation for finances
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Rapport Financier");
        // Add code to populate the sheet with financial data

        try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    private void ajouterEnTeteTableau(PdfPTable table, String[] headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, SMALL_FONT));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }
}