package com.poissonnerie.util;

import com.poissonnerie.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class ReportBuilder {
    private static final Logger LOGGER = Logger.getLogger(ReportBuilder.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private static final com.itextpdf.text.Font TITLE_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
    private static final com.itextpdf.text.Font SUBTITLE_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
    private static final com.itextpdf.text.Font NORMAL_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.NORMAL);
    private static final com.itextpdf.text.Font SMALL_FONT = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL);

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
        document.add(new Paragraph("Rapport des Stocks", TITLE_FONT));
        document.add(new Paragraph("Généré le " + LocalDateTime.now().format(DATE_FORMATTER)));
        document.add(Chunk.NEWLINE);

        // Statistiques globales
        document.add(new Paragraph("Statistiques globales", SUBTITLE_FONT));
        document.add(new Paragraph("Total produits: " + stats.get("totalProduits")));
        document.add(new Paragraph("Valeur totale: " + CURRENCY_FORMATTER.format(stats.get("valeurTotaleStock"))));
        document.add(Chunk.NEWLINE);

        // Produits sous alerte
        @SuppressWarnings("unchecked")
        List<Produit> produitsSousAlerte = (List<Produit>) stats.get("produitsSousAlerte");
        if (produitsSousAlerte != null && !produitsSousAlerte.isEmpty()) {
            document.add(new Paragraph("Produits sous seuil d'alerte", SUBTITLE_FONT));
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
        String[] headers = {"ID", "Nom", "Stock", "Prix", "Catégorie"};
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
        String[] headers = {"ID", "Nom", "Stock", "Prix"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, SMALL_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }
    }

    private void ajouterLigneProduit(PdfPTable table, Produit produit) {
        table.addCell(String.valueOf(produit.getId()));
        table.addCell(produit.getNom());
        table.addCell(String.valueOf(produit.getStock()));
        table.addCell(CURRENCY_FORMATTER.format(produit.getPrixVente()));
    }

    private CellStyle creerStyleEnTete(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
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
        cell.setCellValue(produit.getId());
        cell.setCellStyle(style);

        cell = row.createCell(1);
        cell.setCellValue(produit.getNom());
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
        ChartUtils.saveChartAsJPEG(chartFile, chart, width, height);
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

        // Tableau détaillé des ventes
        PdfPTable tableVentes = new PdfPTable(5);
        tableVentes.setWidthPercentage(100);
        ajouterEnTeteTableau(tableVentes, new String[]{"Date", "Client", "Produits", "Total", "Mode"});

        @SuppressWarnings("unchecked")
        List<Vente> ventesTriees = (List<Vente>) stats.get("ventesTriees");
        for (Vente v : ventesTriees) {
            tableVentes.addCell(v.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            tableVentes.addCell(v.getClient() != null ? v.getClient().getNom() : "Vente comptant");
            tableVentes.addCell(String.valueOf(v.getLignes().size()));
            tableVentes.addCell(CURRENCY_FORMATTER.format(v.getMontantTotal()));
            tableVentes.addCell(v.getModePaiement().toString());
        }
        document.add(tableVentes);

        document.close();
    }

    private void genererRapportVentesExcel(String cheminFichier, Map<String, Object> stats,
                                            LocalDate debut, LocalDate fin) throws Exception {
        Workbook workbook = new XSSFWorkbook();

        // Feuille des statistiques globales
        Sheet sheetStats = workbook.createSheet("Statistiques Globales");
        CellStyle headerStyle = creerStyleEnTete(workbook);
        CellStyle dataStyle = creerStyleDonnees(workbook);

        // En-tête
        Row headerRow = sheetStats.createRow(0);
        headerRow.createCell(0).setCellValue("Période du rapport");
        headerRow.createCell(1).setCellValue(debut.format(DATE_FORMATTER) + " au " + fin.format(DATE_FORMATTER));

        // Statistiques principales
        Row row1 = sheetStats.createRow(2);
        row1.createCell(0).setCellValue("Chiffre d'affaires");
        row1.createCell(1).setCellValue(((Number)stats.get("chiffreAffaires")).doubleValue());

        Row row2 = sheetStats.createRow(3);
        row2.createCell(0).setCellValue("Nombre de ventes");
        row2.createCell(1).setCellValue(((Number)stats.get("nombreVentes")).intValue());

        Row row3 = sheetStats.createRow(4);
        row3.createCell(0).setCellValue("Panier moyen");
        row3.createCell(1).setCellValue(((Number)stats.get("panierMoyen")).doubleValue());

        // Feuille des ventes détaillées
        Sheet sheetVentes = workbook.createSheet("Détail des Ventes");
        Row ventesHeader = sheetVentes.createRow(0);
        String[] headers = {"Date", "Client", "Nombre Produits", "Total", "Mode Paiement"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = ventesHeader.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Données des ventes
        @SuppressWarnings("unchecked")
        List<Vente> ventesTriees = (List<Vente>) stats.get("ventesTriees");
        int rowNum = 1;
        for (Vente vente : ventesTriees) {
            Row row = sheetVentes.createRow(rowNum++);
            row.createCell(0).setCellValue(vente.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            row.createCell(1).setCellValue(vente.getClient() != null ? vente.getClient().getNom() : "Vente comptant");
            row.createCell(2).setCellValue(vente.getLignes().size());
            row.createCell(3).setCellValue(vente.getMontantTotal());
            row.createCell(4).setCellValue(vente.getModePaiement().toString());
        }

        // Feuille d'analyse par mode de paiement
        Sheet sheetModes = workbook.createSheet("Analyse par Mode");
        Row modesHeader = sheetModes.createRow(0);
        String[] headersModes = {"Mode de Paiement", "Nombre", "Total", "Moyenne"};

        for (int i = 0; i < headersModes.length; i++) {
            Cell cell = modesHeader.createCell(i);
            cell.setCellValue(headersModes[i]);
            cell.setCellStyle(headerStyle);
        }

        @SuppressWarnings("unchecked")
        Map<ModePaiement, DoubleSummaryStatistics> statsParMode =
            (Map<ModePaiement, DoubleSummaryStatistics>) stats.get("statsParMode");

        rowNum = 1;
        for (Map.Entry<ModePaiement, DoubleSummaryStatistics> entry : statsParMode.entrySet()) {
            Row row = sheetModes.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey().toString());
            row.createCell(1).setCellValue(entry.getValue().getCount());
            row.createCell(2).setCellValue(entry.getValue().getSum());
            row.createCell(3).setCellValue(entry.getValue().getAverage());
        }

        // Ajuster la largeur des colonnes
        for (Sheet sheet : new Sheet[]{sheetStats, sheetVentes, sheetModes}) {
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        // Sauvegarder le fichier
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
        Workbook workbook = new XSSFWorkbook();

        // Feuille principale
        Sheet sheet = workbook.createSheet("Rapport Créances");
        CellStyle headerStyle = creerStyleEnTete(workbook);
        CellStyle dataStyle = creerStyleDonnees(workbook);

        // En-tête avec statistiques globales
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Total des créances");
        headerRow.createCell(1).setCellValue(((Number)stats.get("totalCreances")).doubleValue());

        // Analyse par statut
        Row statutHeader = sheet.createRow(2);
        statutHeader.createCell(0).setCellValue("Analyse par Statut");

        Row statutLabels = sheet.createRow(3);
        String[] labels = {"Statut", "Nombre", "Montant Moyen", "Total"};
        for (int i = 0; i < labels.length; i++) {
            Cell cell = statutLabels.createCell(i);
            cell.setCellValue(labels[i]);
            cell.setCellStyle(headerStyle);
        }

        @SuppressWarnings("unchecked")
        Map<String, DoubleSummaryStatistics> statsParStatut = 
            (Map<String, DoubleSummaryStatistics>) stats.get("statsParStatut");

        int rowNum = 4;
        for (Map.Entry<String, DoubleSummaryStatistics> entry : statsParStatut.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue().getCount());
            row.createCell(2).setCellValue(entry.getValue().getAverage());
            row.createCell(3).setCellValue(entry.getValue().getSum());
        }

        // Liste des clients en retard
        Row retardHeader = sheet.createRow(rowNum + 1);
        retardHeader.createCell(0).setCellValue("Clients en Retard de Paiement");

        Row retardLabels = sheet.createRow(rowNum + 2);
        String[] retardHeaders = {"Client", "Solde", "Dernière Vente", "Statut"};
        for (int i = 0; i < retardHeaders.length; i++) {
            Cell cell = retardLabels.createCell(i);
            cell.setCellValue(retardHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        @SuppressWarnings("unchecked")
        List<Client> clientsRetard = (List<Client>) stats.get("clientsRetard");
        rowNum += 3;
        for (Client client : clientsRetard) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(client.getNom());
            row.createCell(1).setCellValue(client.getSolde());
            row.createCell(2).setCellValue(client.getDerniereVente() != null ? 
                client.getDerniereVente().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
            row.createCell(3).setCellValue(client.getStatutCreances().toString());
        }

        // Ajuster la largeur des colonnes
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }

        // Sauvegarder le fichier
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
        Workbook workbook = new XSSFWorkbook();
        CellStyle headerStyle = creerStyleEnTete(workbook);
        CellStyle dataStyle = creerStyleDonnees(workbook);

        // Feuille principale - Vue d'ensemble
        Sheet sheetVueEnsemble = workbook.createSheet("Vue d'ensemble");

        // En-tête
        Row headerRow = sheetVueEnsemble.createRow(0);
        headerRow.createCell(0).setCellValue("Rapport Financier");
        headerRow.createCell(1).setCellValue(debut.format(DATE_FORMATTER) + " au " + fin.format(DATE_FORMATTER));

        // Résultats globaux
        Row row1 = sheetVueEnsemble.createRow(2);
        row1.createCell(0).setCellValue("Chiffre d'affaires total");
        row1.createCell(1).setCellValue(((Number)stats.get("chiffreAffairesTotal")).doubleValue());

        Row row2 = sheetVueEnsemble.createRow(3);
        row2.createCell(0).setCellValue("Total des dépenses");
        row2.createCell(1).setCellValue(((Number)stats.get("depensesTotal")).doubleValue());

        Row row3 = sheetVueEnsemble.createRow(4);
        row3.createCell(0).setCellValue("Bénéfice net");
        row3.createCell(1).setCellValue(((Number)stats.get("beneficeNet")).doubleValue());

        if (stats.containsKey("tauxMargeBrute")) {
            Row row4 = sheetVueEnsemble.createRow(5);
            row4.createCell(0).setCellValue("Taux de marge brute");
            row4.createCell(1).setCellValue(((Number)stats.get("tauxMargeBrute")).doubleValue() + "%");
        }

        // Feuille - Évolution quotidienne
        Sheet sheetEvolution = workbook.createSheet("Évolution Quotidienne");
        Row evolutionHeader = sheetEvolution.createRow(0);
        String[] headers = {"Date", "Chiffre d'affaires", "Dépenses", "Bénéfice"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = evolutionHeader.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        @SuppressWarnings("unchecked")
        Map<LocalDate, Double> caParJour = (Map<LocalDate, Double>) stats.get("chiffreAffairesParJour");
        @SuppressWarnings("unchecked")
        Map<LocalDate, Double> depensesParJour = (Map<LocalDate, Double>) stats.get("depensesParJour");

        Set<LocalDate> dates = new TreeSet<>();
        dates.addAll(caParJour.keySet());
        dates.addAll(depensesParJour.keySet());

        int rowNum = 1;
        for (LocalDate date : dates) {
            Row row = sheetEvolution.createRow(rowNum++);
            double ca = caParJour.getOrDefault(date, 0.0);
            double depenses = depensesParJour.getOrDefault(date, 0.0);

            row.createCell(0).setCellValue(date.format(DATE_FORMATTER));
            row.createCell(1).setCellValue(ca);
            row.createCell(2).setCellValue(depenses);
            row.createCell(3).setCellValue(ca - depenses);
        }

        // Feuille - Analyse des tendances
        Sheet sheetTendances = workbook.createSheet("Analyse des Tendances");
        Row tendancesHeader = sheetTendances.createRow(0);
        String[] headersTendances = {"Indicateur", "Valeur", "Variation"};

        for (int i = 0; i < headersTendances.length; i++) {
            Cell cell = tendancesHeader.createCell(i);
            cell.setCellValue(headersTendances[i]);
            cell.setCellStyle(headerStyle);
        }

        @SuppressWarnings("unchecked")
        Map<String, Double> tendances = (Map<String, Double>) stats.get("tendances");
        if (tendances != null) {
            rowNum = 1;
            for (Map.Entry<String, Double> entry : tendances.entrySet()) {
                Row row = sheetTendances.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }
        }

        // Ajuster la largeur des colonnes
        for (Sheet sheet : new Sheet[]{sheetVueEnsemble, sheetEvolution, sheetTendances}) {
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
        }

        // Sauvegarder le fichier
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