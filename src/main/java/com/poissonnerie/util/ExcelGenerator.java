package com.poissonnerie.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.poissonnerie.model.*;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;

public class ExcelGenerator {
    private static final Logger LOGGER = Logger.getLogger(ExcelGenerator.class.getName());
    private static final String OUTPUT_DIR = "generated_excel";

    public static void genererRapportStocks(List<Produit> produits, String nomFichier) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Feuille des données
            Sheet sheetDonnees = workbook.createSheet("État des Stocks");

            // Style pour les en-têtes
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // En-têtes
            Row headerRow = sheetDonnees.createRow(0);
            String[] columns = {"Référence", "Nom", "Prix", "Quantité", "Seuil Alerte", "Statut Stock"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            int rowNum = 1;
            for (Produit produit : produits) {
                Row row = sheetDonnees.createRow(rowNum++);
                row.createCell(0).setCellValue(produit.getReference());
                row.createCell(1).setCellValue(produit.getNom());
                row.createCell(2).setCellValue(produit.getPrix());
                row.createCell(3).setCellValue(produit.getQuantite());
                row.createCell(4).setCellValue(produit.getSeuilAlerte());
                row.createCell(5).setCellValue(getStatutStock(produit));
            }

            // Auto-dimensionnement des colonnes
            for (int i = 0; i < columns.length; i++) {
                sheetDonnees.autoSizeColumn(i);
            }

            // Feuille des statistiques
            Sheet sheetStats = workbook.createSheet("Statistiques");
            Map<String, Object> stats = ReportStatisticsManager.analyserStocks(produits);

            // En-tête des statistiques
            Row statsHeader = sheetStats.createRow(0);
            Cell statsHeaderCell = statsHeader.createCell(0);
            statsHeaderCell.setCellValue("Statistiques des Stocks");
            statsHeaderCell.setCellStyle(headerStyle);

            // Données statistiques
            int statsRow = 2;
            Row totalRow = sheetStats.createRow(statsRow++);
            totalRow.createCell(0).setCellValue("Nombre total de produits");
            totalRow.createCell(1).setCellValue(((Number) stats.get("totalProduits")).intValue());

            Row valeurRow = sheetStats.createRow(statsRow++);
            valeurRow.createCell(0).setCellValue("Valeur totale du stock");
            valeurRow.createCell(1).setCellValue(((Number) stats.get("valeurTotaleStock")).doubleValue());

            // Ajout des graphiques et analyses supplémentaires
            Map<String, Long> statutsCount = (Map<String, Long>) stats.get("statutsCount");
            Row statutHeader = sheetStats.createRow(statsRow++);
            statutHeader.createCell(0).setCellValue("Répartition par statut");

            for (Map.Entry<String, Long> entry : statutsCount.entrySet()) {
                Row statutRow = sheetStats.createRow(statsRow++);
                statutRow.createCell(0).setCellValue(entry.getKey());
                statutRow.createCell(1).setCellValue(entry.getValue());
            }

            // Écriture du fichier
            try (FileOutputStream fileOut = new FileOutputStream(nomFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des stocks généré avec succès: " + nomFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    public static void genererRapportVentes(List<Vente> ventes, String nomFichier, LocalDate dateDebut, LocalDate dateFin) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheetVentes = workbook.createSheet("Rapport des Ventes");
            CellStyle headerStyle = createHeaderStyle(workbook);

            // En-têtes
            Row headerRow = sheetVentes.createRow(0);
            String[] columns = {"Date", "Client", "Nombre Produits", "Total HT", "TVA", "Total TTC", "Mode Paiement"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            int rowNum = 1;
            for (Vente vente : ventes) {
                Row row = sheetVentes.createRow(rowNum++);
                row.createCell(0).setCellValue(
                    vente.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                row.createCell(1).setCellValue(vente.getClient() != null ? 
                    vente.getClient().getNom() : "Vente comptant");
                row.createCell(2).setCellValue(vente.getLignes().size());
                row.createCell(3).setCellValue(vente.getTotalHT());
                row.createCell(4).setCellValue(vente.getMontantTVA());
                row.createCell(5).setCellValue(vente.getTotal());
                row.createCell(6).setCellValue(vente.getModePaiement().toString());
            }

            // Auto-dimensionnement
            for (int i = 0; i < columns.length; i++) {
                sheetVentes.autoSizeColumn(i);
            }

            // Feuille des statistiques
            Sheet sheetStats = workbook.createSheet("Analyses");
            Map<String, Object> stats = ReportStatisticsManager.analyserVentes(ventes, dateDebut, dateFin);

            // En-tête des analyses
            Row statsHeader = sheetStats.createRow(0);
            Cell statsHeaderCell = statsHeader.createCell(0);
            statsHeaderCell.setCellValue("Analyses des Ventes");
            statsHeaderCell.setCellStyle(headerStyle);

            // Statistiques globales
            int statsRow = 2;
            Row caRow = sheetStats.createRow(statsRow++);
            caRow.createCell(0).setCellValue("Chiffre d'affaires total");
            caRow.createCell(1).setCellValue(((Number) stats.get("chiffreAffaires")).doubleValue());

            // Ventes par mode de paiement
            Row modeHeader = sheetStats.createRow(statsRow++);
            modeHeader.createCell(0).setCellValue("Répartition par mode de paiement");

            Map<ModePaiement, Long> ventesParMode = (Map<ModePaiement, Long>) stats.get("ventesParMode");
            for (Map.Entry<ModePaiement, Long> entry : ventesParMode.entrySet()) {
                Row modeRow = sheetStats.createRow(statsRow++);
                modeRow.createCell(0).setCellValue(entry.getKey().toString());
                modeRow.createCell(1).setCellValue(entry.getValue());
            }

            try (FileOutputStream fileOut = new FileOutputStream(nomFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des ventes généré avec succès: " + nomFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    public static void genererRapportCreances(List<Client> clients, String nomFichier) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("État des Créances");
            CellStyle headerStyle = createHeaderStyle(workbook);

            // En-têtes
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Client", "Total Créances", "Dernière Vente", "Statut", "Téléphone"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            int rowNum = 1;
            for (Client client : clients) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(client.getNom());
                row.createCell(1).setCellValue(client.getTotalCreances());
                row.createCell(2).setCellValue(client.getDerniereVente() != null ? 
                    client.getDerniereVente().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
                row.createCell(3).setCellValue(client.getStatutCreances().toString());
                row.createCell(4).setCellValue(client.getTelephone());
            }

            // Feuille d'analyse
            Sheet sheetAnalyse = workbook.createSheet("Analyse Créances");
            Map<String, Object> stats = ReportStatisticsManager.analyserCreances(clients);

            // Statistiques
            int statsRow = 0;
            Row totalRow = sheetAnalyse.createRow(statsRow++);
            totalRow.createCell(0).setCellValue("Total des créances");
            totalRow.createCell(1).setCellValue(((Number) stats.get("totalCreances")).doubleValue());

            // Répartition par statut
            statsRow++;
            Row statutHeader = sheetAnalyse.createRow(statsRow++);
            statutHeader.createCell(0).setCellValue("Répartition par statut");

            Map<StatutCreances, Long> creancesParStatut = (Map<StatutCreances, Long>) stats.get("creancesParStatut");
            for (Map.Entry<StatutCreances, Long> entry : creancesParStatut.entrySet()) {
                Row statutRow = sheetAnalyse.createRow(statsRow++);
                statutRow.createCell(0).setCellValue(entry.getKey().toString());
                statutRow.createCell(1).setCellValue(entry.getValue());
            }

            // Auto-dimensionnement
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(nomFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des créances généré avec succès: " + nomFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    private static String getStatutStock(Produit produit) {
        if (produit.getQuantite() <= 0) {
            return "RUPTURE";
        } else if (produit.getQuantite() <= produit.getSeuilAlerte()) {
            return "ALERTE";
        } else if (produit.getQuantite() >= produit.getSeuilAlerte() * 3) {
            return "SURSTOCK";
        } else {
            return "NORMAL";
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}