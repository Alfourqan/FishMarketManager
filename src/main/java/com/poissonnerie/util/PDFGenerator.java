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

    // Design constants
    private static final float MARGIN = 50;
    private static final float HEADER_HEIGHT = 120;
    private static final float TABLE_START_Y = HEADER_HEIGHT + 60;
    private static final float ROW_HEIGHT = 20;
    private static final float CELL_MARGIN = 5;

    // Colors
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);  // Bleu principal
    private static final Color ACCENT_COLOR = new Color(230, 126, 34);   // Orange accent
    private static final Color LIGHT_TEXT = new Color(236, 240, 241);    // Texte clair
    private static final Color HEADER_COLOR = new Color(41, 128, 185);   // Bleu header
    private static final Color TEXT_COLOR = new Color(44, 62, 80);       // Texte foncé
    private static final Color ALTERNATE_ROW = new Color(236, 240, 241); // Lignes alternées

    private static class PDFTable {
        private final PDDocument document;
        private final float startY;
        private final float[] columnWidths;
        private float currentY;
        private PDPage currentPage;
        private PDPageContentStream contentStream;
        private final float margin;
        private boolean isAlternateRow = false;

        public PDFTable(PDDocument document, PDPage firstPage, float startY, float[] columnWidths, float margin) throws IOException {
            this.document = document;
            this.startY = startY;
            this.columnWidths = columnWidths;
            this.currentY = startY;
            this.currentPage = firstPage;
            this.margin = margin;
            this.contentStream = new PDPageContentStream(document, currentPage);
        }

        public void addHeaderRow(String[] headers) throws IOException {
            contentStream.setNonStrokingColor(HEADER_COLOR);
            float xPosition = margin;

            for (int i = 0; i < headers.length; i++) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(xPosition + CELL_MARGIN, currentY);
                contentStream.setNonStrokingColor(LIGHT_TEXT);
                contentStream.showText(headers[i]);
                contentStream.endText();
                xPosition += columnWidths[i];
            }

            currentY -= ROW_HEIGHT;
        }

        public void addRow(String[] cells) throws IOException {
            if (currentY <= MARGIN + ROW_HEIGHT) {
                contentStream.close();
                PDPage newPage = new PDPage(currentPage.getMediaBox());
                document.addPage(newPage);
                currentPage = newPage;
                contentStream = new PDPageContentStream(document, currentPage);
                currentY = startY;
            }

            if (isAlternateRow) {
                contentStream.setNonStrokingColor(ALTERNATE_ROW);
                contentStream.addRect(margin, currentY - ROW_HEIGHT + 5, 
                    currentPage.getMediaBox().getWidth() - 2 * margin, ROW_HEIGHT);
                contentStream.fill();
            }

            float xPosition = margin;
            contentStream.setNonStrokingColor(TEXT_COLOR);

            for (int i = 0; i < cells.length; i++) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(xPosition + CELL_MARGIN, currentY);
                contentStream.showText(cells[i] != null ? cells[i] : "");
                contentStream.endText();
                xPosition += columnWidths[i];
            }

            currentY -= ROW_HEIGHT;
            isAlternateRow = !isAlternateRow;
        }

        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }

    // Méthode utilitaire pour créer l'en-tête de page
    private static void addPageHeader(PDPageContentStream stream, PDPage page, String title) throws IOException {
        stream.setNonStrokingColor(PRIMARY_COLOR);
        stream.addRect(0, page.getMediaBox().getHeight() - HEADER_HEIGHT, 
            page.getMediaBox().getWidth(), HEADER_HEIGHT);
        stream.fill();

        stream.setNonStrokingColor(ACCENT_COLOR);
        stream.addRect(0, page.getMediaBox().getHeight() - HEADER_HEIGHT, 
            page.getMediaBox().getWidth(), 8);
        stream.fill();

        stream.setNonStrokingColor(LIGHT_TEXT);
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA_BOLD, 24);
        stream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - 60);
        stream.showText("MA POISSONNERIE");
        stream.endText();

        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        stream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - 90);
        stream.showText(title);
        stream.endText();

        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, 12);
        stream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - 110);
        stream.showText("Généré le " + DATE_FORMATTER.format(LocalDateTime.now()));
        stream.endText();
    }

    public static void genererRapportFinancier(
            Map<String, Double> chiffreAffaires,
            Map<String, Double> couts,
            Map<String, Double> benefices,
            Map<String, Double> marges,
            ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = null;

            try {
                stream = new PDPageContentStream(document, page);
                addPageHeader(stream, page, "Rapport Financier");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {200, 100};
                PDFTable table = new PDFTable(document, page, y, columnWidths, MARGIN);

                // Calcul des totaux
                double totalCA = chiffreAffaires.values().stream().mapToDouble(Double::doubleValue).sum();
                double totalCouts = couts.values().stream().mapToDouble(Double::doubleValue).sum();
                double totalBenefices = benefices.values().stream().mapToDouble(Double::doubleValue).sum();
                double margeMoyenne = marges.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                // Données du tableau
                String[][] rows = {
                    {"Chiffre d'Affaires Total", String.format("%.2f €", totalCA)},
                    {"Coûts Totaux", String.format("%.2f €", totalCouts)},
                    {"Bénéfices Totaux", String.format("%.2f €", totalBenefices)},
                    {"Marge Moyenne", String.format("%.2f %%", margeMoyenne)}
                };

                table.addHeaderRow(new String[]{"Indicateur", "Montant"});
                for (String[] row : rows) {
                    table.addRow(row);
                }

                table.close();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du stream", e);
                    }
                }
            }

            document.save(outputStream);
            LOGGER.info("Rapport financier généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public static void genererRapportCreances(List<Client> clients, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = null;

            try {
                stream = new PDPageContentStream(document, page);
                addPageHeader(stream, page, "État des Créances");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {150, 100, 100, 100};
                PDFTable table = new PDFTable(document, page, y, columnWidths, MARGIN);

                String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};
                table.addHeaderRow(headers);

                boolean highlight = false;
                for (Client client : clients) {
                    if (client.getSolde() > 0) {
                        String[] row = {client.getNom(), client.getTelephone(), String.format("%.2f €", client.getSolde()), "-"};
                        table.addRow(row);
                        highlight = !highlight;
                    }
                }
                table.close();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du stream", e);
                    }
                }
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
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = null;

            try {
                stream = new PDPageContentStream(document, page);
                addPageHeader(stream, page, "État des Stocks");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {60, 100, 80, 70, 70, 50, 50, 70};
                PDFTable table = new PDFTable(document, page, y, columnWidths, MARGIN);

                String[] headers = {
                        "Référence", "Nom", "Catégorie", "Prix Achat", "Prix Vente",
                        "Stock", "Seuil", "Valeur"
                };
                table.addHeaderRow(headers);

                double valeurTotaleStock = 0;
                boolean highlight = false;
                for (Produit p : produits) {
                    double valeurStock = p.getStock() * p.getPrixAchat();
                    valeurTotaleStock += valeurStock;
                    String[] row = {String.valueOf(p.getId()), p.getNom(), p.getCategorie(),
                            String.format("%.2f €", p.getPrixAchat()), String.format("%.2f €", p.getPrixVente()),
                            String.valueOf(p.getStock()), String.valueOf(p.getSeuilAlerte()),
                            String.format("%.2f €", valeurStock)};
                    table.addRow(row);
                    highlight = !highlight;
                }
                table.close();

                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                stream.newLineAtOffset(50, y - 40);
                stream.showText(String.format("Valeur totale du stock: %.2f €", valeurTotaleStock));
                stream.endText();

            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du stream", e);
                    }
                }
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
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = null;

            try {
                stream = new PDPageContentStream(document, page);
                addPageHeader(stream, page, "Rapport des Ventes");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {70, 100, 50, 70, 70, 70, 70, 70};
                PDFTable table = new PDFTable(document, page, y, columnWidths, MARGIN);

                String[] headers = {
                        "Date", "Client", "Produits", "Total HT", "TVA",
                        "Total TTC", "Mode", "Marge"
                };
                table.addHeaderRow(headers);

                boolean highlight = false;
                for (Vente v : ventes) {
                    double margeVente = 0.0;
                    for (Vente.LigneVente ligne : v.getLignes()) {
                        double marge = (ligne.getPrixUnitaire() - ligne.getProduit().getPrixAchat()) * ligne.getQuantite();
                        margeVente += marge;
                    }
                    String[] row = {v.getDate().format(DATE_FORMATTER), v.getClient() != null ? v.getClient().getNom() : "Vente comptant",
                            String.valueOf(v.getLignes().size()), String.format("%.2f €", v.getTotalHT()),
                            String.format("%.2f €", v.getMontantTVA()), String.format("%.2f €", v.getTotal()),
                            v.getModePaiement().getLibelle(), String.format("%.2f €", margeVente)};
                    table.addRow(row);
                    highlight = !highlight;
                }
                table.close();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du stream", e);
                    }
                }
            }
            document.save(outputStream);
            LOGGER.info("Rapport des ventes généré avec succès");
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
        contentStream.addRect(MARGIN, MARGIN + 35, pageWidth - 2 * MARGIN, 2);
        contentStream.fill();

        // Pagination élégante
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.setNonStrokingColor(TEXT_COLOR);
        contentStream.newLineAtOffset(pageWidth / 2 - 40, MARGIN + 15);
        contentStream.showText(String.format("Page %d sur %d", pageNumber, totalPages));
        contentStream.endText();

        // Copyright moderne
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(MARGIN, MARGIN + 15);
        contentStream.showText("© " + LocalDateTime.now().getYear() + " Ma Poissonnerie - Tous droits réservés");
        contentStream.endText();
    }
    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = null;

            try {
                stream = new PDPageContentStream(document, page);
                addPageHeader(stream, page, "Rapport des Fournisseurs");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] xPositions = {MARGIN, MARGIN + 150, MARGIN + 300, MARGIN + 450};
                String[] headers = {"Nom", "Contact", "Téléphone", "Email"};

                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);

                for (int i = 0; i < headers.length; i++) {
                    stream.beginText();
                    stream.newLineAtOffset(xPositions[i], y);
                    stream.showText(headers[i]);
                    stream.endText();
                }

                stream.setFont(PDType1Font.HELVETICA, 10);
                y -= ROW_HEIGHT + 10;
                boolean alternate = false;

                for (Fournisseur f : fournisseurs) {
                    if (y < MARGIN + 50) {
                        stream.close();
                        stream = null;

                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        y = page.getMediaBox().getHeight() - TABLE_START_Y;

                        // Add headers to new page
                        stream.setNonStrokingColor(TEXT_COLOR);
                        stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        for (int i = 0; i < headers.length; i++) {
                            stream.beginText();
                            stream.newLineAtOffset(xPositions[i], y);
                            stream.showText(headers[i]);
                            stream.endText();
                        }
                        y -= ROW_HEIGHT + 10;
                    }

                    if (alternate) {
                        stream.setNonStrokingColor(ALTERNATE_ROW);
                        stream.addRect(MARGIN - 5, y - 5, page.getMediaBox().getWidth() - 2 * MARGIN + 10, ROW_HEIGHT + 10);
                        stream.fill();
                    }

                    stream.setNonStrokingColor(TEXT_COLOR);
                    stream.beginText();
                    stream.newLineAtOffset(xPositions[0], y);
                    stream.showText(f.getNom() != null ? f.getNom() : "");
                    stream.endText();

                    stream.beginText();
                    stream.newLineAtOffset(xPositions[1], y);
                    stream.showText(f.getContact() != null ? f.getContact() : "");
                    stream.endText();

                    stream.beginText();
                    stream.newLineAtOffset(xPositions[2], y);
                    stream.showText(f.getTelephone() != null ? f.getTelephone() : "");
                    stream.endText();

                    stream.beginText();
                    stream.newLineAtOffset(xPositions[3], y);
                    stream.showText(f.getEmail() != null ? f.getEmail() : "");
                    stream.endText();

                    y -= ROW_HEIGHT + 5;
                    alternate = !alternate;
                }
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors de la fermeture du stream", e);
                    }
                }
            }

            document.save(outputStream);
            LOGGER.info("Rapport des fournisseurs généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }
}