package com.poissonnerie.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import com.poissonnerie.model.*;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class ExcelGenerator {
    private static final Logger LOGGER = Logger.getLogger(ExcelGenerator.class.getName());
    private static final String OUTPUT_DIR = "generated_excel";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static {
        try {
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du répertoire de sortie", e);
        }
    }

    private static void applyHeaderStyle(XSSFWorkbook workbook, XSSFCell cell) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        cell.setCellStyle(style);
    }

    public static void genererRapportStocks(List<Produit> produits, Map<String, Double> statistiques, String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet stockSheet = workbook.createSheet("État des Stocks");

            Row headerRow = stockSheet.createRow(0);
            String[] headers = {"Référence", "Nom", "Catégorie", "Prix d'achat", "Prix de vente", "Stock", "Seuil Alerte", "Statut"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                applyHeaderStyle(workbook, (XSSFCell)cell);
                stockSheet.setColumnWidth(i, 256 * 15);
            }

            int rowNum = 1;
            for (Produit p : produits) {
                Row row = stockSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getNom());
                row.createCell(2).setCellValue(p.getCategorie());
                row.createCell(3).setCellValue(p.getPrixAchat());
                row.createCell(4).setCellValue(p.getPrixVente());
                row.createCell(5).setCellValue(p.getQuantite());
                row.createCell(6).setCellValue(p.getSeuilAlerte());
                row.createCell(7).setCellValue(p.getQuantite() <= p.getSeuilAlerte() ? "ALERTE" : "OK");
            }

            if (statistiques != null && !statistiques.isEmpty()) {
                XSSFSheet statsSheet = workbook.createSheet("Statistiques");
                rowNum = 0;
                Row titleRow = statsSheet.createRow(rowNum++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("Statistiques des Stocks");
                applyHeaderStyle(workbook, (XSSFCell)titleCell);

                for (Map.Entry<String, Double> stat : statistiques.entrySet()) {
                    Row row = statsSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(stat.getKey());
                    row.createCell(1).setCellValue(stat.getValue());
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des stocks généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    public static void genererRapportCreances(List<Client> clients, String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Créances");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Client", "Téléphone", "Solde", "Dernière transaction"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                applyHeaderStyle(workbook, (XSSFCell)cell);
                sheet.setColumnWidth(i, 256 * 15);
            }

            int rowNum = 1;
            for (Client c : clients) {
                if (c.getSolde() > 0) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(c.getNom());
                    row.createCell(1).setCellValue(c.getTelephone());
                    row.createCell(2).setCellValue(c.getSolde());
                    row.createCell(3).setCellValue(c.getDerniereTransaction() != null ? 
                        c.getDerniereTransaction().format(DATE_TIME_FORMATTER) : "-");
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des créances généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Fournisseurs");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nom", "Contact", "Téléphone", "Email", "Dernière commande"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                applyHeaderStyle(workbook, (XSSFCell)cell);
                sheet.setColumnWidth(i, 256 * 15);
            }

            int rowNum = 1;
            for (Fournisseur f : fournisseurs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(f.getNom());
                row.createCell(1).setCellValue(f.getContact());
                row.createCell(2).setCellValue(f.getTelephone());
                row.createCell(3).setCellValue(f.getEmail());
                row.createCell(4).setCellValue(f.getDerniereCommande() != null ? 
                    f.getDerniereCommande().format(DATE_TIME_FORMATTER) : "-");
            }

            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des fournisseurs généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    public static void genererRapportVentes(List<Vente> ventes, Map<String, Double> analyses, String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet venteSheet = workbook.createSheet("Ventes");

            Row headerRow = venteSheet.createRow(0);
            String[] headers = {"Date", "Client", "Nb Produits", "Total HT", "TVA", "Total TTC", "Mode"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                applyHeaderStyle(workbook, (XSSFCell)cell);
                venteSheet.setColumnWidth(i, 256 * 15);
            }

            int rowNum = 1;
            for (Vente v : ventes) {
                Row row = venteSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(v.getDate().format(DATE_TIME_FORMATTER));
                row.createCell(1).setCellValue(v.getClient() != null ? v.getClient().getNom() : "Vente comptant");
                row.createCell(2).setCellValue(v.getLignes().size());
                row.createCell(3).setCellValue(v.getTotalHT());
                row.createCell(4).setCellValue(v.getMontantTVA());
                row.createCell(5).setCellValue(v.getTotal());
                row.createCell(6).setCellValue(v.getModePaiement().getLibelle());
            }

            if (analyses != null && !analyses.isEmpty()) {
                XSSFSheet analyseSheet = workbook.createSheet("Analyses");
                rowNum = 0;
                Row titleRow = analyseSheet.createRow(rowNum++);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("Analyses des Ventes");
                applyHeaderStyle(workbook, (XSSFCell)titleCell);

                for (Map.Entry<String, Double> analyse : analyses.entrySet()) {
                    Row row = analyseSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(analyse.getKey());
                    row.createCell(1).setCellValue(analyse.getValue());
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des ventes généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    public static void genererRapportFinancier(
            Map<String, Double> chiffreAffaires,
            Map<String, Double> couts,
            Map<String, Double> benefices,
            Map<String, Double> marges,
            String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet caSheet = workbook.createSheet("Chiffre d'Affaires");
            creerFeuilleFinanciere(workbook, caSheet, "Évolution du Chiffre d'Affaires", chiffreAffaires);

            XSSFSheet coutsSheet = workbook.createSheet("Coûts");
            creerFeuilleFinanciere(workbook, coutsSheet, "Détail des Coûts", couts);

            XSSFSheet beneficesSheet = workbook.createSheet("Bénéfices");
            creerFeuilleFinanciere(workbook, beneficesSheet, "Analyse des Bénéfices", benefices);

            XSSFSheet margesSheet = workbook.createSheet("Marges");
            creerFeuilleFinanciere(workbook, margesSheet, "Analyse des Marges", marges);

            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport financier Excel généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier Excel", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    private static void creerFeuilleFinanciere(XSSFWorkbook workbook, XSSFSheet sheet, String titre, Map<String, Double> donnees) {
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titre);
        applyHeaderStyle(workbook, (XSSFCell)titleCell);

        Row headerRow = sheet.createRow(1);
        String[] headers = {"Période", "Montant"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            applyHeaderStyle(workbook, (XSSFCell)cell);
        }

        int rowNum = 2;
        for (Map.Entry<String, Double> entry : donnees.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}