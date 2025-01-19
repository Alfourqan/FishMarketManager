package com.poissonnerie.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.poissonnerie.model.*;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(PDFGenerator.class.getName());
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void genererRapportVentes(List<Vente> ventes, String cheminFichier) {
        try (Document document = new Document()) {
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Paragraph titre = new Paragraph("Rapport des Ventes", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            // Tableau des ventes
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes du tableau
            String[] headers = {"Date", "Client", "Total HT", "TVA", "Total TTC"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Vente vente : ventes) {
                table.addCell(new Phrase(vente.getDate().format(DATE_FORMATTER), NORMAL_FONT));
                table.addCell(new Phrase(vente.getClient() != null ? vente.getClient().getNom() : "Vente comptant", NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", vente.getTotalHT()), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", vente.getMontantTVA()), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", vente.getTotal()), NORMAL_FONT));
            }

            document.add(table);
            LOGGER.info("Rapport des ventes PDF généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }

    public static void genererRapportStocks(List<Produit> produits, String cheminFichier) {
        try (Document document = new Document()) {
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            Paragraph titre = new Paragraph("État des Stocks", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {"Référence", "Nom", "Catégorie", "Prix", "Stock", "Seuil Alerte"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Produit produit : produits) {
                table.addCell(new Phrase(String.valueOf(produit.getId()), NORMAL_FONT));
                table.addCell(new Phrase(produit.getNom(), NORMAL_FONT));
                table.addCell(new Phrase(produit.getCategorie(), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", produit.getPrixVente()), NORMAL_FONT));
                table.addCell(new Phrase(String.valueOf(produit.getStock()), NORMAL_FONT));
                table.addCell(new Phrase(String.valueOf(produit.getSeuilAlerte()), NORMAL_FONT));
            }

            document.add(table);
            LOGGER.info("Rapport des stocks PDF généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }

    public static void genererRapportCreances(List<Client> clients, String cheminFichier) {
        try (Document document = new Document()) {
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            Paragraph titre = new Paragraph("État des Créances", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Client client : clients) {
                if (client.getSolde() > 0) {
                    table.addCell(new Phrase(client.getNom(), NORMAL_FONT));
                    table.addCell(new Phrase(client.getTelephone(), NORMAL_FONT));
                    table.addCell(new Phrase(String.format("%.2f €", client.getSolde()), NORMAL_FONT));
                    table.addCell(new Phrase("-", NORMAL_FONT)); // TODO: Implémenter la date d'échéance
                }
            }

            document.add(table);
            LOGGER.info("Rapport des créances PDF généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, String cheminFichier) {
        try (Document document = new Document()) {
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            Paragraph titre = new Paragraph("Liste des Fournisseurs", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {"Nom", "Contact", "Téléphone", "Email"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Fournisseur fournisseur : fournisseurs) {
                table.addCell(new Phrase(fournisseur.getNom(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getContact(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getTelephone(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getEmail(), NORMAL_FONT));
            }

            document.add(table);
            LOGGER.info("Rapport des fournisseurs PDF généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }

    public static void genererRapportFinancier(
            Map<String, Double> chiffreAffaires,
            Map<String, Double> couts,
            Map<String, Double> benefices,
            Map<String, Double> marges,
            String cheminFichier) {
        try (Document document = new Document()) {
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // Titre principal
            Paragraph titrePrincipal = new Paragraph("Rapport Financier", TITLE_FONT);
            titrePrincipal.setAlignment(Element.ALIGN_CENTER);
            document.add(titrePrincipal);
            document.add(Chunk.NEWLINE);

            // Section Chiffre d'affaires
            ajouterSectionFinanciere(document, "Chiffre d'Affaires", chiffreAffaires);
            document.add(Chunk.NEWLINE);

            // Section Coûts
            ajouterSectionFinanciere(document, "Coûts", couts);
            document.add(Chunk.NEWLINE);

            // Section Bénéfices
            ajouterSectionFinanciere(document, "Bénéfices", benefices);
            document.add(Chunk.NEWLINE);

            // Section Marges
            ajouterSectionFinanciere(document, "Marges", marges);

            LOGGER.info("Rapport financier PDF généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport financier PDF", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }
    }

    private static void ajouterSectionFinanciere(Document document, String titre, Map<String, Double> donnees) throws DocumentException {
        // Titre de la section
        Paragraph titreParagraph = new Paragraph(titre, SUBTITLE_FONT);
        titreParagraph.setSpacingBefore(10f);
        document.add(titreParagraph);

        // Tableau des données
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);
        table.setSpacingAfter(10f);

        // En-têtes
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Indicateur", SUBTITLE_FONT));
        PdfPCell headerCell2 = new PdfPCell(new Phrase("Montant", SUBTITLE_FONT));
        headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(headerCell1);
        table.addCell(headerCell2);

        // Données
        for (Map.Entry<String, Double> entry : donnees.entrySet()) {
            table.addCell(new Phrase(entry.getKey(), NORMAL_FONT));
            table.addCell(new Phrase(String.format("%.2f €", entry.getValue()), NORMAL_FONT));
        }

        document.add(table);
    }
}