package com.poissonnerie.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import com.poissonnerie.model.*;
import com.poissonnerie.model.Vente.ModePaiement; // Added import
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
        style.setAlignment(HorizontalAlignment.CENTER);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        cell.setCellStyle(style);
    }

    private static void applyCurrencyStyle(XSSFWorkbook workbook, XSSFCell cell) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00€"));
        cell.setCellStyle(style);
    }

    public static void genererRapportStocks(List<Produit> produits, Map<String, Double> statistiques, String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet stockSheet = workbook.createSheet("État des Stocks");

            // En-tête avec analyses
            Row headerRow = stockSheet.createRow(0);
            String[] headers = {"Référence", "Nom", "Catégorie", "Prix d'achat", "Prix de vente", "Stock", "Seuil Alerte", "Statut", "Valeur Stock"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                applyHeaderStyle(workbook, (XSSFCell)cell);
                stockSheet.setColumnWidth(i, 256 * 15);
            }

            double valeurTotaleStock = 0.0;
            int rowNum = 1;
            Map<String, Integer> produitsParCategorie = new HashMap<>();

            for (Produit p : produits) {
                Row row = stockSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getNom());
                row.createCell(2).setCellValue(p.getCategorie());

                Cell prixAchatCell = row.createCell(3);
                prixAchatCell.setCellValue(p.getPrixAchat());
                applyCurrencyStyle(workbook, (XSSFCell)prixAchatCell);

                Cell prixVenteCell = row.createCell(4);
                prixVenteCell.setCellValue(p.getPrixVente());
                applyCurrencyStyle(workbook, (XSSFCell)prixVenteCell);

                row.createCell(5).setCellValue(p.getQuantite());
                row.createCell(6).setCellValue(p.getSeuilAlerte());
                row.createCell(7).setCellValue(p.getQuantite() <= p.getSeuilAlerte() ? "ALERTE" : "OK");

                double valeurStock = p.getQuantite() * p.getPrixAchat();
                Cell valeurStockCell = row.createCell(8);
                valeurStockCell.setCellValue(valeurStock);
                applyCurrencyStyle(workbook, (XSSFCell)valeurStockCell);

                valeurTotaleStock += valeurStock;
                produitsParCategorie.merge(p.getCategorie(), 1, Integer::sum);
            }

            // Création d'une feuille pour les analyses
            XSSFSheet analyseSheet = workbook.createSheet("Analyses Stocks");
            rowNum = 0;

            // Titre
            Row titleRow = analyseSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Analyses des Stocks");
            applyHeaderStyle(workbook, (XSSFCell)titleCell);

            // Valeur totale du stock
            Row totalRow = analyseSheet.createRow(rowNum++);
            totalRow.createCell(0).setCellValue("Valeur totale du stock");
            Cell totalValueCell = totalRow.createCell(1);
            totalValueCell.setCellValue(valeurTotaleStock);
            applyCurrencyStyle(workbook, (XSSFCell)totalValueCell);

            // Distribution par catégorie
            rowNum++;
            Row catTitle = analyseSheet.createRow(rowNum++);
            catTitle.createCell(0).setCellValue("Distribution par catégorie");
            applyHeaderStyle(workbook, (XSSFCell)catTitle.getCell(0));

            for (Map.Entry<String, Integer> entry : produitsParCategorie.entrySet()) {
                Row row = analyseSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }

            // Statistiques supplémentaires
            if (statistiques != null && !statistiques.isEmpty()) {
                rowNum += 2;
                Row statsTitle = analyseSheet.createRow(rowNum++);
                statsTitle.createCell(0).setCellValue("Statistiques complémentaires");
                applyHeaderStyle(workbook, (XSSFCell)statsTitle.getCell(0));

                for (Map.Entry<String, Double> stat : statistiques.entrySet()) {
                    Row row = analyseSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(stat.getKey());
                    Cell valueCell = row.createCell(1);
                    valueCell.setCellValue(stat.getValue());
                    applyCurrencyStyle(workbook, (XSSFCell)valueCell);
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

            // En-tête
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Client", "Téléphone", "Solde", "Dernière vente", "Statut", "Jours depuis dernière vente"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                applyHeaderStyle(workbook, (XSSFCell)cell);
                sheet.setColumnWidth(i, 256 * 15);
            }

            // Données et analyses
            int rowNum = 1;
            double totalCreances = 0;
            int clientsEnRetard = 0;
            int clientsCritiques = 0;

            for (Client c : clients) {
                if (c.getSolde() > 0) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(c.getNom());
                    row.createCell(1).setCellValue(c.getTelephone());

                    Cell soldeCell = row.createCell(2);
                    soldeCell.setCellValue(c.getSolde());
                    applyCurrencyStyle(workbook, (XSSFCell)soldeCell);

                    LocalDateTime derniereVente = c.getDerniereVente();
                    row.createCell(3).setCellValue(derniereVente != null ?
                        DATE_TIME_FORMATTER.format(derniereVente) : "-");
                    row.createCell(4).setCellValue(c.getStatutCreances().toString());

                    // Calcul des jours depuis la dernière vente
                    if (derniereVente != null) {
                        long joursDernierVente = java.time.temporal.ChronoUnit.DAYS.between(
                            derniereVente.toLocalDate(),
                            LocalDateTime.now().toLocalDate()
                        );
                        row.createCell(5).setCellValue(joursDernierVente);
                    } else {
                        row.createCell(5).setCellValue("-");
                    }

                    totalCreances += c.getSolde();
                    if (c.getStatutCreances() == Client.StatutCreances.EN_RETARD) {
                        clientsEnRetard++;
                    } else if (c.getStatutCreances() == Client.StatutCreances.CRITIQUE) {
                        clientsCritiques++;
                    }
                }
            }

            // Feuille d'analyse
            XSSFSheet analyseSheet = workbook.createSheet("Analyse Créances");
            rowNum = 0;

            Row titleRow = analyseSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Analyse des Créances");
            applyHeaderStyle(workbook, (XSSFCell)titleCell);

            // Total des créances
            Row totalRow = analyseSheet.createRow(rowNum++);
            totalRow.createCell(0).setCellValue("Total des créances");
            Cell totalCell = totalRow.createCell(1);
            totalCell.setCellValue(totalCreances);
            applyCurrencyStyle(workbook, (XSSFCell)totalCell);

            // Statistiques des statuts
            rowNum++;
            Row statusTitle = analyseSheet.createRow(rowNum++);
            statusTitle.createCell(0).setCellValue("Répartition par statut");
            applyHeaderStyle(workbook, (XSSFCell)statusTitle.getCell(0));

            Row enRetardRow = analyseSheet.createRow(rowNum++);
            enRetardRow.createCell(0).setCellValue("Clients en retard");
            enRetardRow.createCell(1).setCellValue(clientsEnRetard);

            Row critiqueRow = analyseSheet.createRow(rowNum++);
            critiqueRow.createCell(0).setCellValue("Clients en situation critique");
            critiqueRow.createCell(1).setCellValue(clientsCritiques);

            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des créances généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    public static void genererRapportVentes(
            List<Vente> ventes,
            Map<String, Double> analyses,
            String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet venteSheet = workbook.createSheet("Ventes");

            // En-tête amélioré
            Row headerRow = venteSheet.createRow(0);
            String[] headers = {
                "Date", "Client", "Nb Produits", "Total HT", "TVA", "Total TTC",
                "Mode Paiement", "Marge"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                applyHeaderStyle(workbook, (XSSFCell)cell);
                venteSheet.setColumnWidth(i, 256 * 15);
            }

            // Données détaillées des ventes
            int rowNum = 1;
            Map<String, Double> ventesParJour = new TreeMap<>();
            Map<ModePaiement, Double> ventesParMode = new EnumMap<>(ModePaiement.class);
            Map<String, Double> ventesParCategorie = new HashMap<>();
            double totalMarge = 0.0;

            for (Vente v : ventes) {
                Row row = venteSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(v.getDate().format(DATE_TIME_FORMATTER));
                row.createCell(1).setCellValue(v.getClient() != null ? v.getClient().getNom() : "Vente comptant");
                row.createCell(2).setCellValue(v.getLignes().size());

                Cell htCell = row.createCell(3);
                htCell.setCellValue(v.getTotalHT());
                applyCurrencyStyle(workbook, (XSSFCell)htCell);

                Cell tvaCell = row.createCell(4);
                tvaCell.setCellValue(v.getMontantTVA());
                applyCurrencyStyle(workbook, (XSSFCell)tvaCell);

                Cell ttcCell = row.createCell(5);
                ttcCell.setCellValue(v.getTotal());
                applyCurrencyStyle(workbook, (XSSFCell)ttcCell);

                row.createCell(6).setCellValue(v.getModePaiement().getLibelle());

                // Calcul de la marge
                double margeVente = calculerMargeVente(v);
                Cell margeCell = row.createCell(7);
                margeCell.setCellValue(margeVente);
                applyCurrencyStyle(workbook, (XSSFCell)margeCell);

                totalMarge += margeVente;

                // Agrégations
                String dateKey = v.getDate().format(DATE_FORMATTER);
                ventesParJour.merge(dateKey, v.getTotal(), Double::sum);
                ventesParMode.merge(v.getModePaiement(), v.getTotal(), Double::sum);

                // Agrégation par catégorie de produit
                for (Vente.LigneVente ligne : v.getLignes()) {
                    ventesParCategorie.merge(ligne.getProduit().getCategorie(),
                        ligne.getPrixUnitaire() * ligne.getQuantite(),
                        Double::sum);
                }
            }

            // Création des feuilles d'analyse
            creerFeuilleAnalysesVentes(workbook, ventesParJour, ventesParMode, ventesParCategorie, totalMarge);
            creerFeuilleAnalysesTendances(workbook, analyses);

            try (FileOutputStream fileOut = new FileOutputStream(cheminFichier)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Rapport Excel des ventes généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport Excel des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport Excel", e);
        }
    }

    private static double calculerMargeVente(Vente vente) {
        return vente.getLignes().stream()
            .mapToDouble(ligne -> {
                double prixVente = ligne.getPrixUnitaire() * ligne.getQuantite();
                double coutAchat = ligne.getProduit().getPrixAchat() * ligne.getQuantite();
                return prixVente - coutAchat;
            })
            .sum();
    }

    private static void creerFeuilleAnalysesVentes(
            XSSFWorkbook workbook,
            Map<String, Double> ventesParJour,
            Map<ModePaiement, Double> ventesParMode,
            Map<String, Double> ventesParCategorie,
            double totalMarge) {

        XSSFSheet analyseSheet = workbook.createSheet("Analyses");
        int rowNum = 0;

        // Section 1: Ventes par jour
        Row titleRow = analyseSheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Ventes par jour");
        applyHeaderStyle(workbook, (XSSFCell)titleCell);

        for (Map.Entry<String, Double> entry : ventesParJour.entrySet()) {
            Row row = analyseSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(entry.getValue());
            applyCurrencyStyle(workbook, (XSSFCell)valueCell);
        }

        // Section 2: Ventes par mode de paiement
        rowNum += 2;
        Row modeTitle = analyseSheet.createRow(rowNum++);
        modeTitle.createCell(0).setCellValue("Ventes par mode de paiement");
        applyHeaderStyle(workbook, (XSSFCell)modeTitle.getCell(0));

        for (Map.Entry<ModePaiement, Double> entry : ventesParMode.entrySet()) {
            Row row = analyseSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey().getLibelle());
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(entry.getValue());
            applyCurrencyStyle(workbook, (XSSFCell)valueCell);
        }

        // Section 3: Ventes par catégorie
        rowNum += 2;
        Row catTitle = analyseSheet.createRow(rowNum++);
        catTitle.createCell(0).setCellValue("Ventes par catégorie");
        applyHeaderStyle(workbook, (XSSFCell)catTitle.getCell(0));

        for (Map.Entry<String, Double> entry : ventesParCategorie.entrySet()) {
            Row row = analyseSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(entry.getValue());
            applyCurrencyStyle(workbook, (XSSFCell)valueCell);
        }

        // Section 4: Marge totale
        rowNum += 2;
        Row margeTitle = analyseSheet.createRow(rowNum++);
        margeTitle.createCell(0).setCellValue("Marge totale");
        Cell margeTotaleCell = margeTitle.createCell(1);
        margeTotaleCell.setCellValue(totalMarge);
        applyCurrencyStyle(workbook, (XSSFCell)margeTotaleCell);
    }

    private static void creerFeuilleAnalysesTendances(
            XSSFWorkbook workbook,
            Map<String, Double> analyses) {
        if (analyses == null || analyses.isEmpty()) return;

        XSSFSheet tendanceSheet = workbook.createSheet("Tendances");
        int rowNum = 0;

        // Titre principal
        Row titleRow = tendanceSheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Analyse des Tendances");
        applyHeaderStyle(workbook, (XSSFCell)titleCell);

        // En-têtes
        Row headerRow = tendanceSheet.createRow(rowNum++);
        String[] headers = {"Indicateur", "Valeur"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            applyHeaderStyle(workbook, (XSSFCell)cell);
        }

        // Données de tendance
        for (Map.Entry<String, Double> entry : analyses.entrySet()) {
            Row row = tendanceSheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            Cell valueCell = row.createCell(1);

            // Appliquer le style monétaire ou pourcentage selon le type d'indicateur
            if (entry.getKey().startsWith("Croissance") || entry.getKey().startsWith("Rentabilité")) {
                XSSFCellStyle percentStyle = workbook.createCellStyle();
                percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
                valueCell.setCellStyle(percentStyle);
                valueCell.setCellValue(entry.getValue() / 100);
            } else {
                applyCurrencyStyle(workbook, (XSSFCell)valueCell);
                valueCell.setCellValue(entry.getValue());
            }
        }

        // Ajuster la largeur des colonnes
        for (int i = 0; i < headers.length; i++) {
            tendanceSheet.autoSizeColumn(i);
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, String cheminFichier) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Fournisseurs");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Nom", "Contact", "Téléphone", "Email", "Dernière commande", "Statut"};

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

                LocalDateTime derniereCommande = f.getDerniereCommande();
                row.createCell(4).setCellValue(derniereCommande != null ?
                    DATE_TIME_FORMATTER.format(derniereCommande) : "-");
                row.createCell(5).setCellValue(f.getStatut());
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
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(entry.getValue());
            applyCurrencyStyle(workbook, (XSSFCell)valueCell);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}