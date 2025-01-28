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
                for(int i = 0; i < rows.length; i++){
                    stream.beginText();
                    stream.newLineAtOffset(xPosition, y);
                    stream.showText(rows[i][0]);
                    stream.endText();
                    stream.beginText();
                    stream.newLineAtOffset(xPosition + 200, y);
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
                String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};

                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float xPosition = MARGIN;
                for(int i = 0; i < headers.length; i++){
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
                        String[] row = {
                                client.getNom(),
                                client.getTelephone(),
                                String.format("%.2f €", client.getSolde()),
                                "-"
                        };
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
                createHeader(stream, page, "État des Stocks");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {80, 150, 80, 100, 100};
                String[] headers = {"Référence", "Nom", "Stock", "Prix", "Valeur"};

                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float xPosition = MARGIN;

                for(int i = 0; i < headers.length; i++){
                    stream.beginText();
                    stream.newLineAtOffset(xPosition, y);
                    stream.showText(headers[i]);
                    stream.endText();
                    xPosition += columnWidths[i];
                }

                y -= ROW_HEIGHT;
                stream.setFont(PDType1Font.HELVETICA, 10);
                double totalValue = 0;

                for (Produit produit : produits) {
                    double valeur = produit.getStock() * produit.getPrixAchat();
                    totalValue += valeur;

                    String[] rowData = {
                            String.valueOf(produit.getId()),
                            produit.getNom(),
                            String.valueOf(produit.getStock()),
                            String.format("%.2f €", produit.getPrixVente()),
                            String.format("%.2f €", valeur)
                    };

                    xPosition = MARGIN;
                    for(int i = 0; i < rowData.length; i++){
                        stream.beginText();
                        stream.newLineAtOffset(xPosition, y);
                        stream.showText(rowData[i]);
                        stream.endText();
                        xPosition += columnWidths[i];
                    }
                    y -= ROW_HEIGHT;
                }

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
                createHeader(stream, page, "Rapport des Ventes");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {120, 120, 80, 100, 100};
                String[] headers = {"Date", "Client", "Produits", "Total", "Mode"};

                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float xPosition = MARGIN;

                for(int i = 0; i < headers.length; i++){
                    stream.beginText();
                    stream.newLineAtOffset(xPosition, y);
                    stream.showText(headers[i]);
                    stream.endText();
                    xPosition += columnWidths[i];
                }

                y -= ROW_HEIGHT;
                stream.setFont(PDType1Font.HELVETICA, 10);
                double totalVentes = 0;

                LOGGER.info("Début de la génération du rapport des ventes avec " + 
                    (ventes != null ? ventes.size() : 0) + " ventes");

                if (ventes != null && !ventes.isEmpty()) {
                    for (Vente vente : ventes) {
                        if (y < 100) { // Si on arrive en bas de la page
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

                        xPosition = MARGIN;
                        for(int i = 0; i < rowData.length; i++){
                            stream.beginText();
                            stream.newLineAtOffset(xPosition, y);
                            stream.showText(rowData[i]);
                            stream.endText();
                            xPosition += columnWidths[i];
                        }
                        y -= ROW_HEIGHT;
                    }

                    stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    stream.beginText();
                    stream.newLineAtOffset(MARGIN, y - ROW_HEIGHT);
                    stream.showText(String.format("Total des ventes: %.2f €", totalVentes));
                    stream.endText();
                } else {
                    stream.beginText();
                    stream.newLineAtOffset(MARGIN, y - ROW_HEIGHT);
                    stream.showText("Aucune vente sur la période sélectionnée");
                    stream.endText();
                }
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
            PDPage page = new PDPage();
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

    public static void genererPreviewTicket(Vente vente, String cheminFichier) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - 50;

                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("MA POISSONNERIE");
                stream.endText();

                y -= 30;
                stream.setFont(PDType1Font.HELVETICA, 12);

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
                }
            }

            document.save(new FileOutputStream(cheminFichier));
            LOGGER.info("Preview du ticket généré avec succès: " + cheminFichier);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération de la preview du ticket", e);
            throw new RuntimeException("Erreur lors de la génération de la preview", e);
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
    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, ByteArrayOutputStream outputStream) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                createHeader(stream, page, "Rapport des Fournisseurs");

                float y = page.getMediaBox().getHeight() - TABLE_START_Y;
                float[] columnWidths = {150, 150, 100, 150};
                String[] headers = {"Nom", "Contact", "Téléphone", "Email"};

                stream.setNonStrokingColor(TEXT_COLOR);
                stream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                float xPosition = MARGIN;

                for(int i = 0; i < headers.length; i++){
                    stream.beginText();
                    stream.newLineAtOffset(xPosition, y);
                    stream.showText(headers[i]);
                    stream.endText();
                    xPosition += columnWidths[i];
                }

                y -= ROW_HEIGHT;
                stream.setFont(PDType1Font.HELVETICA, 10);

                for (Fournisseur f : fournisseurs) {
                    String[] rowData = {
                            f.getNom() != null ? f.getNom() : "",
                            f.getContact() != null ? f.getContact() : "",
                            f.getTelephone() != null ? f.getTelephone() : "",
                            f.getEmail() != null ? f.getEmail() : ""
                    };
                    xPosition = MARGIN;
                    for(int i = 0; i < rowData.length; i++){
                        stream.beginText();
                        stream.newLineAtOffset(xPosition, y);
                        stream.showText(rowData[i]);
                        stream.endText();
                        xPosition += columnWidths[i];
                    }
                    y -= ROW_HEIGHT;
                }

            }
            document.save(outputStream);
            LOGGER.info("Rapport des fournisseurs généré avec succès");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport", e);
            throw new RuntimeException("Erreur lors de la génération du rapport", e);
        }
    }

    public static void genererReglementCreance(Client client, double montantPaye, double nouveauSolde, String cheminFichier) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - 50;

                // En-tête
                stream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Reçu de Paiement - Créance");
                stream.endText();

                y -= 30;
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Client: " + client.getNom());
                stream.endText();

                y -= 30;
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Téléphone: " + client.getTelephone());
                stream.endText();

                y -= 30;
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Détails du paiement:");
                stream.endText();

                y -= 30;
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Montant payé: " + String.format("%.2f €", montantPaye));
                stream.endText();

                y -= 20;
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Nouveau solde: " + String.format("%.2f €", nouveauSolde));
                stream.endText();

                y -= 20;
                stream.beginText();
                stream.newLineAtOffset(MARGIN, y);
                stream.showText("Date: " + DATE_FORMATTER.format(LocalDateTime.now()));
                stream.endText();
            }

            document.save(new FileOutputStream(cheminFichier));
            LOGGER.info("Reçu de paiement généré avec succès: " + cheminFichier);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du reçu de paiement", e);
            throw new RuntimeException("Erreur lors de la génération du reçu", e);
        }
    }
}