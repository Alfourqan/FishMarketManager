package com.poissonnerie.util;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import com.poissonnerie.model.*;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.awt.Color;

public class PDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(PDFGenerator.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static class PDFTable {
        private float margin = 50;
        private float yPosition;
        private float[] columnWidths;
        private float rowHeight = 20;
        private PDPageContentStream contentStream;
        private PDPage page;
        private PDDocument document;
        private float tableWidth;
        private float cellMargin = 5f;

        public PDFTable(PDDocument document, PDPage page, PDPageContentStream contentStream,
                       float yPosition, float[] columnWidths) {
            this.document = document;
            this.page = page;
            this.contentStream = contentStream;
            this.yPosition = yPosition;
            this.columnWidths = columnWidths;
            this.tableWidth = 0;
            for (float width : columnWidths) {
                tableWidth += width;
            }
        }

        public void addCell(String text, int column, boolean isHeader) throws IOException {
            float xPosition = margin;
            for (int i = 0; i < column; i++) {
                xPosition += columnWidths[i];
            }

            contentStream.setFont(PDType1Font.HELVETICA_BOLD, isHeader ? 12 : 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition + cellMargin, yPosition);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
        }

        public void drawRowLines() throws IOException {
            float xPosition = margin;
            contentStream.moveTo(xPosition, yPosition - 5);
            contentStream.lineTo(xPosition + tableWidth, yPosition - 5);
            contentStream.stroke();

            float currentX = xPosition;
            contentStream.moveTo(currentX, yPosition + rowHeight);
            contentStream.lineTo(currentX, yPosition - rowHeight);
            contentStream.stroke();

            for (float width : columnWidths) {
                currentX += width;
                contentStream.moveTo(currentX, yPosition + rowHeight);
                contentStream.lineTo(currentX, yPosition - rowHeight);
                contentStream.stroke();
            }
        }

        public void nextRow() {
            yPosition -= rowHeight;
            if (yPosition < 50) {
                try {
                    contentStream.close();
                    PDPage newPage;
                    if (page.getMediaBox().getWidth() > page.getMediaBox().getHeight()) {
                        // For landscape pages
                        newPage = new PDPage(new PDRectangle(
                            PDRectangle.A4.getHeight(),
                            PDRectangle.A4.getWidth()
                        ));
                    } else {
                        // For portrait pages
                        newPage = new PDPage(PDRectangle.A4);
                    }
                    document.addPage(newPage);
                    contentStream = new PDPageContentStream(document, newPage);
                    yPosition = newPage.getMediaBox().getHeight() - 50;
                    page = newPage;
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la création d'une nouvelle page", e);
                }
            }
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yStart = page.getMediaBox().getHeight() - 50;

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, yStart);
                contentStream.showText("Liste des Fournisseurs - " + DATE_FORMATTER.format(LocalDateTime.now()));
                contentStream.endText();

                float[] columnWidths = {150, 100, 100, 150};
                PDFTable table = new PDFTable(document, page, contentStream, yStart - 40, columnWidths);

                String[] headers = {"Nom", "Contact", "Téléphone", "Email"};
                for (int i = 0; i < headers.length; i++) {
                    table.addCell(headers[i], i, true);
                }
                table.drawRowLines();
                table.nextRow();

                for (Fournisseur fournisseur : fournisseurs) {
                    table.addCell(fournisseur.getNom(), 0, false);
                    table.addCell(fournisseur.getContact(), 1, false);
                    table.addCell(fournisseur.getTelephone(), 2, false);
                    table.addCell(fournisseur.getEmail(), 3, false);
                    table.drawRowLines();
                    table.nextRow();
                }
            }

            document.save(outputStream);
            LOGGER.info("Rapport des fournisseurs PDF généré avec succès");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }

    private static PDPage createLandscapePage() {
        return new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
    }

    public static void sauvegarderPDF(byte[] pdfData, String nomFichier) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(nomFichier)) {
            fos.write(pdfData);
            LOGGER.info("PDF sauvegardé avec succès : " + nomFichier);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde du PDF", e);
            throw e;
        }
    }
    public static void genererRapportFinancier(
            Map<String, Double> chiffreAffaires,
            Map<String, Double> couts,
            Map<String, Double> benefices,
            Map<String, Double> marges,
            ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = createLandscapePage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 70);
                contentStream.showText("Rapport Financier - Généré le " + DATE_FORMATTER.format(LocalDateTime.now()));
                contentStream.endText();

                float[] columnWidths = {150, 100};
                PDFTable summaryTable = new PDFTable(document, page, contentStream, page.getMediaBox().getHeight() - 100, columnWidths);
                double totalCA = chiffreAffaires.values().stream().mapToDouble(Double::doubleValue).sum();
                double totalCouts = couts.values().stream().mapToDouble(Double::doubleValue).sum();
                double totalBenefices = benefices.values().stream().mapToDouble(Double::doubleValue).sum();
                double margeMoyenne = marges.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                summaryTable.addCell("Total Chiffre d'Affaires", 0, true);
                summaryTable.addCell(String.format("%.2f €", totalCA), 1, true);
                summaryTable.nextRow();
                summaryTable.addCell("Total Coûts", 0, true);
                summaryTable.addCell(String.format("%.2f €", totalCouts), 1, true);
                summaryTable.nextRow();
                summaryTable.addCell("Total Bénéfices", 0, true);
                summaryTable.addCell(String.format("%.2f €", totalBenefices), 1, true);
                summaryTable.nextRow();
                summaryTable.addCell("Marge Moyenne", 0, true);
                summaryTable.addCell(String.format("%.2f %%", margeMoyenne), 1, true);

                summaryTable.drawRowLines();
            }
            document.save(outputStream);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public static void genererRapportCreances(List<Client> clients, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 50);
                contentStream.showText("État des Créances");
                contentStream.endText();

                float[] columnWidths = {150, 100, 100, 100};
                PDFTable table = new PDFTable(document, page, contentStream, page.getMediaBox().getHeight() - 100, columnWidths);

                String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};
                for (int i = 0; i < headers.length; i++) {
                    table.addCell(headers[i], i, true);
                }
                table.drawRowLines();
                table.nextRow();

                for (Client client : clients) {
                    if (client.getSolde() > 0) {
                        table.addCell(client.getNom(), 0, false);
                        table.addCell(client.getTelephone(), 1, false);
                        table.addCell(String.format("%.2f €", client.getSolde()), 2, false);
                        table.addCell("-", 3, false);
                        table.drawRowLines();
                        table.nextRow();
                    }
                }
            }
            document.save(outputStream);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }


    public static void genererRapportStocks(List<Produit> produits, Map<String, Double> statistiques,
                                          ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yStart = page.getMediaBox().getHeight() - 50;
                float tableWidth = page.getMediaBox().getWidth() - 100;

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, yStart);
                contentStream.showText("État des Stocks");
                contentStream.endText();

                float[] columnWidths = {60, 100, 80, 70, 70, 50, 50, 70};
                PDFTable table = new PDFTable(document, page, contentStream, yStart - 40, columnWidths);

                String[] headers = {
                        "Référence", "Nom", "Catégorie", "Prix Achat", "Prix Vente",
                        "Stock", "Seuil", "Valeur"
                };

                for (int i = 0; i < headers.length; i++) {
                    table.addCell(headers[i], i, true);
                }
                table.drawRowLines();
                table.nextRow();

                double valeurTotaleStock = 0;
                for (Produit p : produits) {
                    table.addCell(String.valueOf(p.getId()), 0, false);
                    table.addCell(p.getNom(), 1, false);
                    table.addCell(p.getCategorie(), 2, false);
                    table.addCell(String.format("%.2f €", p.getPrixAchat()), 3, false);
                    table.addCell(String.format("%.2f €", p.getPrixVente()), 4, false);
                    table.addCell(String.valueOf(p.getStock()), 5, false);
                    table.addCell(String.valueOf(p.getSeuilAlerte()), 6, false);

                    double valeurStock = p.getStock() * p.getPrixAchat();
                    table.addCell(String.format("%.2f €", valeurStock), 7, false);
                    valeurTotaleStock += valeurStock;

                    table.drawRowLines();
                    table.nextRow();
                }

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(50, table.yPosition - 30);
                contentStream.showText(String.format("Valeur totale du stock: %.2f €", valeurTotaleStock));
                contentStream.endText();
            }
            document.save(outputStream);
            LOGGER.info("Rapport des stocks PDF généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }

    public static void genererRapportVentes(List<Vente> ventes, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = createLandscapePage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 50);
                contentStream.showText("Rapport des Ventes");
                contentStream.endText();

                float[] columnWidths = {70, 100, 50, 70, 70, 70, 70, 70};
                PDFTable table = new PDFTable(document, page, contentStream, page.getMediaBox().getHeight() - 100, columnWidths);
                String[] headers = {
                        "Date", "Client", "Produits", "Total HT", "TVA",
                        "Total TTC", "Mode", "Marge"
                };

                for (int i = 0; i < headers.length; i++) {
                    table.addCell(headers[i], i, true);
                }
                table.drawRowLines();
                table.nextRow();

                for (Vente v : ventes) {
                    table.addCell(v.getDate().format(DATE_FORMATTER), 0, false);
                    table.addCell(v.getClient() != null ? v.getClient().getNom() : "Vente comptant", 1, false);
                    table.addCell(String.valueOf(v.getLignes().size()), 2, false);
                    table.addCell(String.format("%.2f €", v.getTotalHT()), 3, false);
                    table.addCell(String.format("%.2f €", v.getMontantTVA()), 4, false);
                    table.addCell(String.format("%.2f €", v.getTotal()), 5, false);
                    table.addCell(v.getModePaiement().getLibelle(), 6, false);

                    double margeVente = 0.0;
                    for (Vente.LigneVente ligne : v.getLignes()) {
                        double marge = (ligne.getPrixUnitaire() - ligne.getProduit().getPrixAchat()) * ligne.getQuantite();
                        margeVente += marge;
                    }
                    table.addCell(String.format("%.2f €", margeVente), 7, false);
                    table.drawRowLines();
                    table.nextRow();
                }
            }
            document.save(outputStream);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public static void genererReglementCreance(Client client, double montantPaye, double nouveauSolde, String cheminFichier) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 50);
                contentStream.showText("Reçu de Paiement - Créance");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 80);
                contentStream.showText("Client: " + client.getNom());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 110);
                contentStream.showText("Téléphone: " + client.getTelephone());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 140);
                contentStream.showText("Détails du paiement:");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 170);
                contentStream.showText("Montant payé: " + String.format("%.2f €", montantPaye));
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 200);
                contentStream.showText("Nouveau solde: " + String.format("%.2f €", nouveauSolde));
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 230);
                contentStream.showText("Date: " + DATE_FORMATTER.format(LocalDateTime.now()));
                contentStream.endText();
            }
            document.save(new FileOutputStream(cheminFichier));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du reçu de paiement", e);
            throw new RuntimeException("Erreur lors de la génération du reçu", e);
        }
    }

    public static void genererTicket(Vente vente, String cheminFichier) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 70);
                contentStream.showText("MA POISSONNERIE");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 100);
                contentStream.showText("Date: " + DATE_FORMATTER.format(vente.getDate()));
                contentStream.endText();
            }
            document.save(new FileOutputStream(cheminFichier));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du ticket", e);
            throw new RuntimeException("Erreur lors de la génération du ticket", e);
        }
    }

    public static String genererPreviewTicket(Vente vente) {
        String tempFile = "preview_ticket_" + System.currentTimeMillis() + ".pdf";
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 70);
                contentStream.showText("MA POISSONNERIE");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 100);
                contentStream.showText("Date: " + DATE_FORMATTER.format(vente.getDate()));
                contentStream.endText();
            }
            document.save(new FileOutputStream(tempFile));
            return tempFile;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération de la prévisualisation du ticket", e);
            throw new RuntimeException("Erreur lors de la génération de la prévisualisation", e);
        }
    }
}