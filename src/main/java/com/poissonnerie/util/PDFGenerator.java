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

    // Constants
    private static final float MARGIN = 50;
    private static final float ROW_HEIGHT = 20;
    private static final float HEADER_HEIGHT = 100;
    private static final float TABLE_START_Y = HEADER_HEIGHT + 50;

    // Colors
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color TEXT_COLOR = new Color(44, 62, 80);

    private static void createHeader(PDPageContentStream stream, PDPage page, String title) throws IOException {
        float pageHeight = page.getMediaBox().getHeight();

        // Header background
        stream.setNonStrokingColor(PRIMARY_COLOR);
        stream.addRect(0, pageHeight - HEADER_HEIGHT, page.getMediaBox().getWidth(), HEADER_HEIGHT);
        stream.fill();

        // Title
        stream.setNonStrokingColor(Color.WHITE);
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA_BOLD, 24);
        stream.newLineAtOffset(MARGIN, pageHeight - 50);
        stream.showText("MA POISSONNERIE");
        stream.endText();

        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA_BOLD, 16);
        stream.newLineAtOffset(MARGIN, pageHeight - 80);
        stream.showText(title);
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
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                createHeader(stream, page, "Rapport Financier");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {200, 100};
                //This section is replaced, but the logic remains the same.  The PDFTable class is removed.
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
                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float xPosition = MARGIN;
                for(int i =0; i < rows.length; i++){
                    stream.beginText();
                    stream.newLineAtOffset(xPosition, y);
                    stream.showText(rows[i][0]);
                    stream.endText();
                    stream.beginText();
                    stream.newLineAtOffset(xPosition + 200,y);
                    stream.showText(rows[i][1]);
                    stream.endText();
                    y -= ROW_HEIGHT;
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
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                createHeader(stream, page, "État des Créances");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {150, 100, 100, 100};
                //This section is replaced, but the logic remains the same.  The PDFTable class is removed.
                String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};
                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float xPosition = MARGIN;
                for(int i =0; i < headers.length; i++){
                    stream.beginText();
                    stream.newLineAtOffset(xPosition, y);
                    stream.showText(headers[i]);
                    stream.endText();
                    xPosition += columnWidths[i];
                }
                y -= ROW_HEIGHT;
                stream.setFont(PDType1Font.HELVETICA, 10);
                for (Client client : clients) {
                    if (client.getSolde() > 0) {
                        String[] row = {client.getNom(), client.getTelephone(), String.format("%.2f €", client.getSolde()), "-"};
                        xPosition = MARGIN;
                        for(int i = 0; i < row.length; i++){
                            stream.beginText();
                            stream.newLineAtOffset(xPosition, y);
                            stream.showText(row[i]);
                            stream.endText();
                            xPosition += columnWidths[i];
                        }
                        y -= ROW_HEIGHT;
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

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                LOGGER.info("Début de la génération du rapport des stocks");
                createHeader(stream, page, "État des Stocks");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] xPositions = {MARGIN, MARGIN + 120, MARGIN + 220, MARGIN + 320, MARGIN + 420};

                // Headers
                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                String[] headers = {"Référence", "Nom", "Stock", "Prix", "Valeur"};

                for (int i = 0; i < headers.length; i++) {
                    stream.beginText();
                    stream.newLineAtOffset(xPositions[i], y);
                    stream.showText(headers[i]);
                    stream.endText();
                }

                // Content
                y -= ROW_HEIGHT;
                stream.setFont(PDType1Font.HELVETICA, 10);
                double totalValue = 0;

                for (Produit produit : produits) {
                    if (y < MARGIN) {
                        stream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        y = page.getMediaBox().getHeight() - TABLE_START_Y;
                    }

                    double valeur = produit.getStock() * produit.getPrixAchat();
                    totalValue += valeur;

                    String[] rowData = {
                        String.valueOf(produit.getId()),
                        produit.getNom(),
                        String.valueOf(produit.getStock()),
                        String.format("%.2f €", produit.getPrixVente()),
                        String.format("%.2f €", valeur)
                    };

                    for (int i = 0; i < rowData.length; i++) {
                        stream.beginText();
                        stream.newLineAtOffset(xPositions[i], y);
                        stream.showText(rowData[i]);
                        stream.endText();
                    }

                    y -= ROW_HEIGHT;
                }

                // Total
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y - ROW_HEIGHT);
                stream.showText(String.format("Valeur totale du stock: %.2f €", totalValue));
                stream.endText();
            }

            document.save(outputStream);
            LOGGER.info("Rapport des stocks généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public static void genererRapportVentes(List<Vente> ventes, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                LOGGER.info("Début de la génération du rapport des ventes");
                createHeader(stream, page, "Rapport des Ventes");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] xPositions = {MARGIN, MARGIN + 150, MARGIN + 250, MARGIN + 350, MARGIN + 450};

                // Headers
                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                String[] headers = {"Date", "Client", "Produits", "Total", "Mode"};

                for (int i = 0; i < headers.length; i++) {
                    stream.beginText();
                    stream.newLineAtOffset(xPositions[i], y);
                    stream.showText(headers[i]);
                    stream.endText();
                }

                // Content
                y -= ROW_HEIGHT;
                stream.setFont(PDType1Font.HELVETICA, 10);
                double totalVentes = 0;

                for (Vente vente : ventes) {
                    if (y < MARGIN) {
                        stream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        y = page.getMediaBox().getHeight() - TABLE_START_Y;
                    }

                    totalVentes += vente.getTotal();

                    String[] rowData = {
                        vente.getDate().format(DATE_FORMATTER),
                        vente.getClient() != null ? vente.getClient().getNom() : "Vente comptant",
                        String.valueOf(vente.getLignes().size()),
                        String.format("%.2f €", vente.getTotal()),
                        vente.getModePaiement().getLibelle()
                    };

                    for (int i = 0; i < rowData.length; i++) {
                        stream.beginText();
                        stream.newLineAtOffset(xPositions[i], y);
                        stream.showText(rowData[i]);
                        stream.endText();
                    }

                    y -= ROW_HEIGHT;
                }

                // Total
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y - ROW_HEIGHT);
                stream.showText(String.format("Total des ventes: %.2f €", totalVentes));
                stream.endText();
            }

            document.save(outputStream);
            LOGGER.info("Rapport des ventes généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public static void genererTicket(Vente vente, String cheminFichier) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - 50;

                // En-tête
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("MA POISSONNERIE");
                stream.endText();

                y -= 30;
                stream.setFont(PDType1Font.HELVETICA, 12);

                // Info vente
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Date: " + DATE_FORMATTER.format(vente.getDate()));
                stream.endText();

                y -= 20;
                if (vente.getClient() != null) {
                    stream.beginText();
                    stream.newLineAtOffset(MARGIN, y);
                    stream.showText("Client: " + vente.getClient().getNom());
                    stream.endText();
                    y -= 20;
                }

                // Lignes
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                String[] headers = {"Produit", "Qté", "Prix", "Total"};
                float[] xPositions = {MARGIN, MARGIN + 200, MARGIN + 280, MARGIN + 360};

                for (int i = 0; i < headers.length; i++) {
                    stream.beginText();
                    stream.newLineAtOffset(xPositions[i], y);
                    stream.showText(headers[i]);
                    stream.endText();
                }

                y -= 20;
                stream.setFont(PDType1Font.HELVETICA, 10);

                for (Vente.LigneVente ligne : vente.getLignes()) {
                    String[] rowData = {
                        ligne.getProduit().getNom(),
                        String.valueOf(ligne.getQuantite()),
                        String.format("%.2f €", ligne.getPrixUnitaire()),
                        String.format("%.2f €", ligne.getQuantite() * ligne.getPrixUnitaire())
                    };

                    for (int i = 0; i < rowData.length; i++) {
                        stream.beginText();
                        stream.newLineAtOffset(xPositions[i], y);
                        stream.showText(rowData[i]);
                        stream.endText();
                    }
                    y -= 15;
                }

                // Total
                y -= 20;
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(String.format("Total TTC: %.2f €", vente.getTotal()));
                stream.endText();

                y -= 20;
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Mode de paiement: " + vente.getModePaiement().getLibelle());
                stream.endText();
            }

            document.save(new FileOutputStream(cheminFichier));
            LOGGER.info("Ticket généré avec succès: " + cheminFichier);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du ticket", e);
            throw new RuntimeException("Erreur lors de la génération du ticket", e);
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
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                createHeader(stream, page, "Rapport des Fournisseurs");

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
            }
            document.save(outputStream);
            LOGGER.info("Rapport des fournisseurs généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }
    public static void genererPreviewTicket(Vente vente, String cheminFichier) {
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
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération de la prévisualisation du ticket", e);
            throw new RuntimeException("Erreur lors de la génération de la prévisualisation", e);
        }
    }
}