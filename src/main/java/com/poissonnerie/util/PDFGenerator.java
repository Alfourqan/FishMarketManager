package com.poissonnerie.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.poissonnerie.model.Vente;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

public class PDFGenerator {
    public static void genererFacture(Vente vente, String cheminFichier) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("FACTURE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Informations de la facture
            document.add(new Paragraph("Facture N°: " + vente.getId()));
            document.add(new Paragraph("Date: " + 
                vente.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

            // Informations client si vente à crédit
            if (vente.isCredit() && vente.getClient() != null) {
                document.add(new Paragraph("Client: " + vente.getClient().getNom()));
                document.add(new Paragraph("Téléphone: " + vente.getClient().getTelephone()));
                document.add(new Paragraph("Adresse: " + vente.getClient().getAdresse()));
            }

            document.add(new Paragraph("\n"));

            // Tableau des produits
            com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Produit");
            table.addCell("Quantité");
            table.addCell("Prix unitaire");
            table.addCell("Total");

            for (Vente.LigneVente ligne : vente.getLignes()) {
                table.addCell(ligne.getProduit().getNom());
                table.addCell(String.valueOf(ligne.getQuantite()));
                table.addCell(String.format("%.2f €", ligne.getPrixUnitaire()));
                table.addCell(String.format("%.2f €", ligne.getQuantite() * ligne.getPrixUnitaire()));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            // Total
            Paragraph total = new Paragraph("Total: " + String.format("%.2f €", vente.getTotal()));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
