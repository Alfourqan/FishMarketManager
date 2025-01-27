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
    private static final float MARGIN = 50;
    private static final float HEADER_HEIGHT = 100;
    private static final float FOOTER_HEIGHT = 30;
    private static final Color PRIMARY_COLOR = new Color(0, 135, 136);
    private static final Color HEADER_BG_COLOR = new Color(245, 245, 245);
    private static final Color TABLE_HEADER_BG = new Color(240, 240, 240);
    private static final Color TABLE_BORDER = new Color(220, 220, 220);

    private static class PDFTable {
        private float yPosition;
        private float[] columnWidths;
        private float rowHeight = 25;
        private PDPageContentStream contentStream;
        private PDPage page;
        private PDDocument document;
        private float tableWidth;
        private float cellMargin = 5f;
        private int currentPage = 1;
        private int totalPages;

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
            float xPosition = MARGIN;
            for (int i = 0; i < column; i++) {
                xPosition += columnWidths[i];
            }

            // Dessiner le fond de la cellule d'en-tête
            contentStream.setNonStrokingColor(TABLE_HEADER_BG);
            contentStream.addRect(xPosition, yPosition - 5, columnWidths[column], rowHeight);
            contentStream.fill();

            // Restaurer la couleur par défaut pour le texte
            contentStream.setNonStrokingColor(Color.BLACK);

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
            contentStream.newLineAtOffset(xPosition + cellMargin, yPosition + 5);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
        }

        public void addCell(String text, int column, boolean isHeader) throws IOException {
            float xPosition = MARGIN;
            for (int i = 0; i < column; i++) {
                xPosition += columnWidths[i];
            }

            contentStream.setFont(isHeader ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition + cellMargin, yPosition + 5);
            contentStream.showText(text != null ? text : "");
            contentStream.endText();
        }

        public void drawRowLines() throws IOException {
            float xPosition = MARGIN;

            // Ligne horizontale
            contentStream.setStrokingColor(TABLE_BORDER);
            contentStream.moveTo(xPosition, yPosition - 5);
            contentStream.lineTo(xPosition + tableWidth, yPosition - 5);
            contentStream.stroke();

            // Lignes verticales
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

            // Restaurer la couleur noire par défaut
            contentStream.setStrokingColor(Color.BLACK);
        }

        public void nextRow() throws IOException {
            yPosition -= rowHeight;
            if (yPosition < (MARGIN + FOOTER_HEIGHT + rowHeight)) {
                contentStream.close();
                currentPage++;
                PDPage newPage = new PDPage(PDRectangle.A4);
                document.addPage(newPage);
                contentStream = new PDPageContentStream(document, newPage);
                addHeader(contentStream, newPage);
                addFooter(contentStream, newPage, currentPage, totalPages);
                yPosition = page.getMediaBox().getHeight() - HEADER_HEIGHT - 40;
                page = newPage;
            }
        }
    }

    private static void addHeader(PDPageContentStream contentStream, PDPage page) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        // Rectangle d'en-tête
        contentStream.setNonStrokingColor(HEADER_BG_COLOR);
        contentStream.addRect(0, pageHeight - HEADER_HEIGHT, pageWidth, HEADER_HEIGHT);
        contentStream.fill();

        // Titre principal
        contentStream.setNonStrokingColor(PRIMARY_COLOR);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
        contentStream.newLineAtOffset(MARGIN, pageHeight - 60);
        contentStream.showText("MA POISSONNERIE");
        contentStream.endText();

        // Sous-titre
        contentStream.setNonStrokingColor(Color.DARK_GRAY);
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.newLineAtOffset(MARGIN, pageHeight - 80);
        contentStream.showText("La fraîcheur au quotidien");
        contentStream.endText();

        // Date
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.newLineAtOffset(pageWidth - 200, pageHeight - 60);
        contentStream.showText("Généré le: " + DATE_FORMATTER.format(LocalDateTime.now()));
        contentStream.endText();

        // Restaurer la couleur par défaut
        contentStream.setNonStrokingColor(Color.BLACK);
    }

    private static void addFooter(PDPageContentStream contentStream, PDPage page, int pageNumber, int totalPages) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();
        float pageWidth = page.getMediaBox().getWidth();

        // Ligne de séparation
        contentStream.setStrokingColor(TABLE_BORDER);
        contentStream.moveTo(MARGIN, MARGIN);
        contentStream.lineTo(pageWidth - MARGIN, MARGIN);
        contentStream.stroke();

        // Pagination
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(pageWidth / 2 - 30, MARGIN - 15);
        contentStream.showText(String.format("Page %d sur %d", pageNumber, totalPages));
        contentStream.endText();

        // Copyright
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 8);
        contentStream.newLineAtOffset(MARGIN, MARGIN - 15);
        contentStream.showText("© 2025 Ma Poissonnerie - Tous droits réservés");
        contentStream.endText();
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            int totalPages = (int) Math.ceil(fournisseurs.size() / 20.0) + 1; // Estimation du nombre de pages
            PDPage firstPage = new PDPage(PDRectangle.A4);
            document.addPage(firstPage);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, firstPage)) {
                // Ajouter l'en-tête
                addHeader(contentStream, firstPage);

                // Titre du rapport
                contentStream.beginText();
                contentStream.setNonStrokingColor(PRIMARY_COLOR);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(MARGIN, firstPage.getMediaBox().getHeight() - HEADER_HEIGHT - 30);
                contentStream.showText("Liste des Fournisseurs");
                contentStream.endText();

                // Ajouter le pied de page
                addFooter(contentStream, firstPage, 1, totalPages);

                // Configuration de la table
                float[] columnWidths = {150, 100, 100, 150};
                float startY = firstPage.getMediaBox().getHeight() - HEADER_HEIGHT - 60;
                PDFTable table = new PDFTable(document, firstPage, contentStream, startY, columnWidths, totalPages);

                // En-têtes de la table
                String[] headers = {"Nom", "Contact", "Téléphone", "Email"};
                for (int i = 0; i < headers.length; i++) {
                    table.addHeaderCell(headers[i], i);
                }
                table.drawRowLines();
                table.nextRow();

                // Données
                contentStream.setNonStrokingColor(Color.BLACK);
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
                PDFTable table = new PDFTable(document, page, contentStream, page.getMediaBox().getHeight() - 100, columnWidths,1);

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