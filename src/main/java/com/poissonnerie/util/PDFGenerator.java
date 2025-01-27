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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.awt.Color;

public class PDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(PDFGenerator.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Constantes de mise en page optimisées
    private static final float MARGIN = 50;
    private static final float HEADER_HEIGHT = 140;
    private static final float FOOTER_HEIGHT = 50;
    private static final float CONTENT_PADDING = 20;

    // Palette de couleurs sophistiquée
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);      // Bleu élégant
    private static final Color ACCENT_COLOR = new Color(230, 126, 34);       // Orange vif
    private static final Color TEXT_COLOR = new Color(52, 73, 94);           // Gris foncé
    private static final Color LIGHT_TEXT = new Color(236, 240, 241);        // Gris très clair
    private static final Color BACKGROUND_LIGHT = new Color(245, 247, 250);  // Fond clair
    private static final Color TABLE_HEADER = new Color(52, 152, 219);       // Bleu header
    private static final Color TABLE_BORDER = new Color(189, 195, 199);      // Gris bordure

    private static class PDFTable {
        private float yPosition;
        private float[] columnWidths;
        private float rowHeight = 35;
        private PDPageContentStream contentStream;
        private PDPage page;
        private PDDocument document;
        private float tableWidth;
        private float cellMargin = 12f;
        private int currentPage = 1;
        private int totalPages;
        private float cornerRadius = 3f;

        public PDFTable(PDDocument document, PDPage page, PDPageContentStream contentStream,
                       float yPosition, float[] columnWidths, int totalPages) {
            this.document = document;
            this.page = page;
            this.contentStream = contentStream;
            this.yPosition = yPosition;
            this.columnWidths = columnWidths;
            this.tableWidth = 0;
            this.totalPages = totalPages;
            for (float width : columnWidths) {
                tableWidth += width;
            }
        }

        public void addHeaderCell(String text, int column) throws IOException {
            float xPosition = MARGIN + CONTENT_PADDING;
            for (int i = 0; i < column; i++) {
                xPosition += columnWidths[i];
            }

            // Fond du header avec coins arrondis
            contentStream.setNonStrokingColor(TABLE_HEADER);
            drawRoundedRect(contentStream, xPosition, yPosition - rowHeight + 5, 
                          columnWidths[column], rowHeight, cornerRadius);
            contentStream.fill();

            // Texte en blanc
            contentStream.setNonStrokingColor(LIGHT_TEXT);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
            contentStream.newLineAtOffset(xPosition + cellMargin, yPosition - rowHeight/2 + 5);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
        }

        private void drawRoundedRect(PDPageContentStream contentStream, float x, float y, 
                                   float width, float height, float radius) throws IOException {
            contentStream.moveTo(x + radius, y);
            contentStream.lineTo(x + width - radius, y);
            contentStream.curveTo(x + width, y, x + width, y, x + width, y + radius);
            contentStream.lineTo(x + width, y + height - radius);
            contentStream.curveTo(x + width, y + height, x + width, y + height, x + width - radius, y + height);
            contentStream.lineTo(x + radius, y + height);
            contentStream.curveTo(x, y + height, x, y + height, x, y + height - radius);
            contentStream.lineTo(x, y + radius);
            contentStream.curveTo(x, y, x, y, x + radius, y);
            contentStream.closePath();
        }

        public void addCell(String text, int column, boolean highlight) throws IOException {
            float xPosition = MARGIN + CONTENT_PADDING;
            for (int i = 0; i < column; i++) {
                xPosition += columnWidths[i];
            }

            if (highlight) {
                contentStream.setNonStrokingColor(BACKGROUND_LIGHT);
                drawRoundedRect(contentStream, xPosition, yPosition - rowHeight + 5,
                              columnWidths[column], rowHeight, cornerRadius);
                contentStream.fill();
            }

            contentStream.setNonStrokingColor(TEXT_COLOR);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(xPosition + cellMargin, yPosition - rowHeight/2 + 5);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
        }

        public void drawRowLines() throws IOException {
            float xPosition = MARGIN + CONTENT_PADDING;
            contentStream.setStrokingColor(TABLE_BORDER);
            contentStream.setLineWidth(0.5f);

            contentStream.moveTo(xPosition, yPosition - rowHeight + 4);
            contentStream.lineTo(xPosition + tableWidth, yPosition - rowHeight + 4);
            contentStream.stroke();

            contentStream.setLineWidth(1.0f);
            contentStream.setStrokingColor(TEXT_COLOR);
        }

        public void nextRow() throws IOException {
            yPosition -= rowHeight;
            if (yPosition < (MARGIN + FOOTER_HEIGHT + rowHeight)) {
                PDPageContentStream oldContentStream = this.contentStream;
                oldContentStream.close();

                currentPage++;
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);

                PDPageContentStream newContentStream = new PDPageContentStream(document, newPage,
                        PDPageContentStream.AppendMode.APPEND, true, true);
                addHeader(newContentStream, newPage);
                addFooter(newContentStream, newPage, currentPage, totalPages);

                this.contentStream = newContentStream;
                this.page = newPage;
                this.yPosition = newPage.getMediaBox().getHeight() - HEADER_HEIGHT - 40;
            }
        }

        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }

    private static void addHeader(PDPageContentStream contentStream, PDPage page) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        contentStream.setNonStrokingColor(PRIMARY_COLOR);
        contentStream.addRect(0, pageHeight - HEADER_HEIGHT, pageWidth, HEADER_HEIGHT);
        contentStream.fill();

        contentStream.setNonStrokingColor(ACCENT_COLOR);
        contentStream.addRect(0, pageHeight - HEADER_HEIGHT, pageWidth, 8);
        contentStream.fill();

        contentStream.setNonStrokingColor(LIGHT_TEXT);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 32);
        contentStream.newLineAtOffset(MARGIN, pageHeight - 80);
        contentStream.showText("MA POISSONNERIE");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 16);
        contentStream.newLineAtOffset(MARGIN, pageHeight - 110);
        contentStream.showText("La fraîcheur au quotidien");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.newLineAtOffset(pageWidth - 200, pageHeight - 80);
        contentStream.showText("Généré le: " + DATE_FORMATTER.format(LocalDateTime.now()));
        contentStream.endText();
    }

    private static void addFooter(PDPageContentStream contentStream, PDPage page, 
                                int pageNumber, int totalPages) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        contentStream.setNonStrokingColor(PRIMARY_COLOR);
        contentStream.addRect(MARGIN, MARGIN + 35, pageWidth - 2*MARGIN, 2);
        contentStream.fill();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        contentStream.newLineAtOffset(pageWidth/2 - 40, MARGIN + 15);
        contentStream.showText(String.format("Page %d sur %d", pageNumber, totalPages));
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(MARGIN, MARGIN + 15);
        contentStream.showText("© " + LocalDateTime.now().getYear() + " Ma Poissonnerie - Tous droits réservés");
        contentStream.endText();
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, ByteArrayOutputStream outputStream) {
        LOGGER.info("Début de la génération du rapport des fournisseurs");

        try (PDDocument document = new PDDocument()) {
            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);

            PDPageContentStream contentStream = new PDPageContentStream(document, firstPage,
                    PDPageContentStream.AppendMode.APPEND, true, true);

            int totalPages = (int) Math.ceil(fournisseurs.size() / 20.0) + 1;
            addHeader(contentStream, firstPage);

            contentStream.beginText();
            contentStream.setNonStrokingColor(PRIMARY_COLOR);
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.newLineAtOffset(MARGIN, firstPage.getMediaBox().getHeight() - HEADER_HEIGHT - 30);
            contentStream.showText("Liste des Fournisseurs");
            contentStream.endText();

            addFooter(contentStream, firstPage, 1, totalPages);

            float[] columnWidths = {150, 100, 100, 150};
            float startY = firstPage.getMediaBox().getHeight() - HEADER_HEIGHT - 60;

            PDFTable table = new PDFTable(document, firstPage, contentStream, startY, columnWidths, totalPages);

            try {
                String[] headers = {"Nom", "Contact", "Téléphone", "Email"};
                for (int i = 0; i < headers.length; i++) {
                    table.addHeaderCell(headers[i], i);
                }
                table.drawRowLines();
                table.nextRow();

                boolean highlight = false;
                for (Fournisseur fournisseur : fournisseurs) {
                    table.addCell(fournisseur.getNom(), 0, highlight);
                    table.addCell(fournisseur.getContact(), 1, highlight);
                    table.addCell(fournisseur.getTelephone(), 2, highlight);
                    table.addCell(fournisseur.getEmail(), 3, highlight);
                    table.drawRowLines();
                    table.nextRow();
                    highlight = !highlight;
                }
            } finally {
                table.close();
            }

            document.save(outputStream);
            LOGGER.info("Rapport des fournisseurs généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }


    private static void addHeader(PDPageContentStream contentStream, PDPage page) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        // En-tête moderne avec dégradé
        contentStream.setNonStrokingColor(PRIMARY_COLOR);
        contentStream.addRect(0, pageHeight - HEADER_HEIGHT, pageWidth, HEADER_HEIGHT);
        contentStream.fill();

        // Bande décorative
        contentStream.setNonStrokingColor(ACCENT_COLOR);
        contentStream.addRect(0, pageHeight - HEADER_HEIGHT, pageWidth, 8);
        contentStream.fill();

        // Logo et titre élégant
        contentStream.setNonStrokingColor(LIGHT_TEXT);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 32);
        contentStream.newLineAtOffset(MARGIN, pageHeight - 80);
        contentStream.showText("MA POISSONNERIE");
        contentStream.endText();

        // Sous-titre stylisé
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 16);
        contentStream.newLineAtOffset(MARGIN, pageHeight - 110);
        contentStream.showText("La fraîcheur au quotidien");
        contentStream.endText();

        // Information de génération
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.newLineAtOffset(pageWidth - 200, pageHeight - 80);
        contentStream.showText("Généré le: " + DATE_FORMATTER.format(LocalDateTime.now()));
        contentStream.endText();
    }

    private static void addFooter(PDPageContentStream contentStream, PDPage page, 
                                int pageNumber, int totalPages) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        // Barre décorative du bas
        contentStream.setNonStrokingColor(PRIMARY_COLOR);
        contentStream.addRect(MARGIN, MARGIN + 35, pageWidth - 2*MARGIN, 2);
        contentStream.fill();

        // Pagination élégante
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        contentStream.newLineAtOffset(pageWidth/2 - 40, MARGIN + 15);
        contentStream.showText(String.format("Page %d sur %d", pageNumber, totalPages));
        contentStream.endText();

        // Copyright moderne
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(MARGIN, MARGIN + 15);
        contentStream.showText("© " + LocalDateTime.now().getYear() + " Ma Poissonnerie - Tous droits réservés");
        contentStream.endText();
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
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 70);
                contentStream.showText("Rapport Financier - Généré le " + DATE_FORMATTER.format(LocalDateTime.now()));
                contentStream.endText();

                float[] columnWidths = {150, 100};
                PDFTable summaryTable = new PDFTable(document, page, contentStream, page.getMediaBox().getHeight() - 100, columnWidths,1);
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
                summaryTable.close();
            }
            document.save(outputStream);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }


    public static void genererRapportCreances(List<Client> clients, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            int totalPages = (int) Math.ceil(clients.size() / 20.0) + 1;
            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);

            PDPageContentStream contentStream = new PDPageContentStream(document, firstPage,
                    PDPageContentStream.AppendMode.APPEND, true, true);
            try {
                addHeader(contentStream, firstPage);

                contentStream.beginText();
                contentStream.setNonStrokingColor(PRIMARY_COLOR);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(MARGIN, firstPage.getMediaBox().getHeight() - HEADER_HEIGHT - 30);
                contentStream.showText("État des Créances");
                contentStream.endText();

                addFooter(contentStream, firstPage, 1, totalPages);

                float[] columnWidths = {150, 100, 100, 100};
                float startY = firstPage.getMediaBox().getHeight() - HEADER_HEIGHT - 60;
                PDFTable table = new PDFTable(document, firstPage, contentStream, startY, columnWidths, totalPages);

                String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};
                for (int i = 0; i < headers.length; i++) {
                    table.addHeaderCell(headers[i], i);
                }
                table.drawRowLines();
                table.nextRow();

                contentStream.setNonStrokingColor(Color.BLACK);
                boolean highlight = false;
                for (Client client : clients) {
                    if (client.getSolde() > 0) {
                        table.addCell(client.getNom(), 0, highlight);
                        table.addCell(client.getTelephone(), 1, highlight);
                        table.addCell(String.format("%.2f €", client.getSolde()), 2, highlight);
                        table.addCell("-", 3, highlight);
                        table.drawRowLines();
                        table.nextRow();
                        highlight = !highlight;
                    }
                }
                table.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des créances", e);
                throw new RuntimeException("Erreur lors de la génération du rapport", e);
            }
            document.save(outputStream);
            LOGGER.info("Rapport des créances généré avec succès");
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
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                float yStart = page.getMediaBox().getHeight() - 50;
                float tableWidth = page.getMediaBox().getWidth() - 100;

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, yStart);
                contentStream.showText("État des Stocks");
                contentStream.endText();

                float[] columnWidths = {60, 100, 80, 70, 70, 50, 50, 70};
                PDFTable table = new PDFTable(document, page, contentStream, yStart - 40, columnWidths,1);

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
                boolean highlight = false;
                for (Produit p : produits) {
                    table.addCell(String.valueOf(p.getId()), 0, highlight);
                    table.addCell(p.getNom(), 1, highlight);
                    table.addCell(p.getCategorie(), 2, highlight);
                    table.addCell(String.format("%.2f €", p.getPrixAchat()), 3, highlight);
                    table.addCell(String.format("%.2f €", p.getPrixVente()), 4, highlight);
                    table.addCell(String.valueOf(p.getStock()), 5, highlight);
                    table.addCell(String.valueOf(p.getSeuilAlerte()), 6, highlight);

                    double valeurStock = p.getStock() * p.getPrixAchat();
                    table.addCell(String.format("%.2f €", valeurStock), 7, highlight);
                    valeurTotaleStock += valeurStock;

                    table.drawRowLines();
                    table.nextRow();
                    highlight = !highlight;
                }
                table.close();

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
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, page.getMediaBox().getHeight() - 50);
                contentStream.showText("Rapport des Ventes");
                contentStream.endText();

                float[] columnWidths = {70, 100, 50, 70, 70, 70, 70, 70};
                PDFTable table = new PDFTable(document, page, contentStream, page.getMediaBox().getHeight() - 100, columnWidths,1);
                String[] headers = {
                        "Date", "Client", "Produits", "Total HT", "TVA",
                        "Total TTC", "Mode", "Marge"
                };

                for (int i = 0; i < headers.length; i++) {
                    table.addCell(headers[i], i, true);
                }
                table.drawRowLines();
                table.nextRow();
                boolean highlight = false;
                for (Vente v : ventes) {
                    table.addCell(v.getDate().format(DATE_FORMATTER), 0, highlight);
                    table.addCell(v.getClient() != null ? v.getClient().getNom() : "Vente comptant", 1, highlight);
                    table.addCell(String.valueOf(v.getLignes().size()), 2, highlight);
                    table.addCell(String.format("%.2f €", v.getTotalHT()), 3, highlight);
                    table.addCell(String.format("%.2f €", v.getMontantTVA()), 4, highlight);
                    table.addCell(String.format("%.2f €", v.getTotal()), 5, highlight);
                    table.addCell(v.getModePaiement().getLibelle(), 6, highlight);

                    double margeVente = 0.0;
                    for (Vente.LigneVente ligne : v.getLignes()) {
                        double marge = (ligne.getPrixUnitaire() - ligne.getProduit().getPrixAchat()) * ligne.getQuantite();
                        margeVente += marge;
                    }
                    table.addCell(String.format("%.2f €", margeVente), 7, highlight);
                    table.drawRowLines();
                    table.nextRow();
                    highlight = !highlight;
                }
                table.close();
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
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
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
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
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
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
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
}