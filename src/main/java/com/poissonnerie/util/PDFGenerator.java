package com.poissonnerie.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.BaseFont;
import com.poissonnerie.model.Vente;
import com.poissonnerie.controller.ConfigurationController;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class PDFGenerator {
    private static final float TICKET_WIDTH = 226.77f; // 8 cm en points
    private static final float MARGIN = 14.17f; // 5mm en points

    public static String genererPreviewTicket(Vente vente) {
        StringBuilder preview = new StringBuilder();
        ConfigurationController configController = new ConfigurationController();
        configController.chargerConfigurations();

        // En-tête
        String nomEntreprise = configController.getValeur("NOM_ENTREPRISE");
        String adresse = configController.getValeur("ADRESSE_ENTREPRISE");
        String telephone = configController.getValeur("TELEPHONE_ENTREPRISE");
        String tauxTVA = configController.getValeur("TAUX_TVA");

        preview.append("\n");  // Espace en haut
        preview.append(centerText(nomEntreprise.toUpperCase(), 40)).append("\n");
        preview.append(centerText(adresse, 40)).append("\n");
        preview.append(centerText(telephone, 40)).append("\n\n");
        preview.append(repeatChar('=', 40)).append("\n");

        // Informations de la facture
        preview.append(String.format("Ticket N°: %d\n", vente.getId()));
        preview.append(String.format("Date: %s\n", 
            vente.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        preview.append(repeatChar('-', 40)).append("\n");

        // Client si vente à crédit
        if (vente.isCredit() && vente.getClient() != null) {
            preview.append(String.format("Client: %s\n", vente.getClient().getNom()));
            if (vente.getClient().getTelephone() != null && !vente.getClient().getTelephone().isEmpty()) {
                preview.append(String.format("Tél: %s\n", vente.getClient().getTelephone()));
            }
            preview.append(repeatChar('-', 40)).append("\n");
        }

        // En-tête articles
        preview.append(String.format("%-20s %6s %6s %6s\n", "Article", "Qté", "P.U.", "Total"));
        preview.append(repeatChar('=', 40)).append("\n");

        // Articles
        double totalHT = 0;
        for (Vente.LigneVente ligne : vente.getLignes()) {
            String nom = ligne.getProduit().getNom();
            if (nom.length() > 20) {
                nom = nom.substring(0, 17) + "...";
            }
            preview.append(String.format("%-20s\n", nom));
            double sousTotal = ligne.getQuantite() * ligne.getPrixUnitaire();
            preview.append(String.format("%20s %3d x %6.2f %6.2f\n", 
                "", ligne.getQuantite(), ligne.getPrixUnitaire(), sousTotal));
            totalHT += sousTotal;
        }

        // Séparateur avant totaux
        preview.append(repeatChar('=', 40)).append("\n");

        // Totaux et TVA
        double tva = totalHT * (Double.parseDouble(tauxTVA) / 100);
        preview.append(String.format("%28s %10.2f€\n", "Total HT:", totalHT));
        preview.append(String.format("%28s %10.2f€\n", "TVA " + tauxTVA + "%:", tva));
        preview.append(repeatChar('-', 40)).append("\n");
        preview.append(String.format("%28s %10.2f€\n", "Total TTC:", totalHT + tva));
        preview.append(repeatChar('=', 40)).append("\n\n");

        // Mode de paiement
        preview.append(centerText(vente.isCredit() ? "*** VENTE À CRÉDIT ***" : "*** PAIEMENT COMPTANT ***", 40)).append("\n");
        preview.append(repeatChar('-', 40)).append("\n\n");

        // Pied de page
        String piedPage = configController.getValeur("PIED_PAGE_RECU");
        preview.append(centerText(piedPage, 40)).append("\n");
        preview.append("\n");  // Espace final

        return preview.toString();
    }

    public static void genererTicket(Vente vente, String cheminFichier) {
        try {
            Document document = new Document(new Rectangle(TICKET_WIDTH, PageSize.A4.getHeight()));
            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // Police monospace pour meilleur alignement
            BaseFont baseFont = BaseFont.createFont(BaseFont.COURIER, BaseFont.CP1252, BaseFont.EMBEDDED);
            Font normalFont = new Font(baseFont, 8);
            Font boldFont = new Font(baseFont, 8, Font.BOLD);
            Font titleFont = new Font(baseFont, 10, Font.BOLD);

            // Convertir le preview en PDF
            String preview = genererPreviewTicket(vente);
            for (String line : preview.split("\n")) {
                Font currentFont = normalFont;
                if (line.startsWith("===")) {
                    currentFont = boldFont;
                } else if (line.equals(line.toUpperCase()) && !line.startsWith("---")) {
                    currentFont = titleFont;
                }

                Paragraph p = new Paragraph(line, currentFont);
                p.setAlignment(Element.ALIGN_LEFT);
                if (line.startsWith("Total TTC:")) {
                    p.setSpacingBefore(5);
                    p.setSpacingAfter(5);
                }
                document.add(p);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du ticket: " + e.getMessage());
        }
    }

    private static String centerText(String text, int width) {
        if (text == null || text.isEmpty()) return repeatChar(' ', width);
        int padding = (width - text.length()) / 2;
        if (padding <= 0) return text;
        return String.format("%" + padding + "s%s%" + padding + "s", "", text, "");
    }

    private static String repeatChar(char c, int count) {
        return new String(new char[count]).replace('\0', c);
    }
}