package com.poissonnerie.util;

import com.poissonnerie.model.Vente;
import java.awt.print.*;
import java.awt.*;
import javax.swing.JTextArea;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TextBillPrinter implements Printable {
    private static final Logger LOGGER = Logger.getLogger(TextBillPrinter.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    private final Vente vente;
    private final StringBuilder bill;

    public TextBillPrinter(Vente vente) {
        this.vente = vente;
        this.bill = new StringBuilder();
    }

    private void generateBillContent() {
        // En-tête
        bill.append("                         MA POISSONNERIE\n");
        bill.append("\t123 Rue de la Mer\n");
        bill.append("\t75001 PARIS\n");
        bill.append("\t+33 1 23 45 67 89\n");
        bill.append("----------------------------------------------------------------\n");

        // En-tête des colonnes
        bill.append(" Iteam\t\tQty\tPrice\n");
        bill.append("----------------------------------------------------------------\n");

        // Articles
        for (Vente.LigneVente ligne : vente.getLignes()) {
            String nom = ligne.getProduit().getNom();
            if (nom.length() > 20) {
                nom = nom.substring(0, 17) + "...";
            }
            // Ajout d'espaces supplémentaires pour l'alignement
            bill.append(String.format("%-20s\t%d\t%.2f\n", 
                nom, 
                ligne.getQuantite(), 
                ligne.getPrixUnitaire() * ligne.getQuantite()));
        }

        bill.append("----------------------------------------------------------------\n");

        // Totaux avec alignement amélioré
        bill.append(String.format("SubTotal :\t\t%.2f\n", vente.getTotalHT()));
        bill.append(String.format("Cash :\t\t\t%.2f\n", vente.getTotal()));
        bill.append(String.format("Balance :\t\t%.2f\n", 0.00));
        bill.append("====================================\n");
        bill.append("                     Thanks For Your Business...!\n");
        bill.append("----------------------------------------------------------------\n");
        bill.append("                     Software by MA POISSONNERIE\n");
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        // Configuration de la police pour un meilleur alignement
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));

        // Impression ligne par ligne
        String[] lines = bill.toString().split("\n");
        int y = 15;
        for (String line : lines) {
            g2d.drawString(line, 10, y);
            y += 12;
        }

        return PAGE_EXISTS;
    }

    public void imprimer() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);

            // Prévisualisation dans un JTextArea pour debug si nécessaire
            JTextArea previewArea = new JTextArea();
            generateBillContent();
            previewArea.setText(bill.toString());

            if (job.printDialog()) {
                job.print();
                LOGGER.info("Impression du ticket réussie");
            }
        } catch (PrinterException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression du ticket", e);
            throw new RuntimeException("Erreur d'impression: " + e.getMessage());
        }
    }

    public String getBillContent() {
        if (bill.length() == 0) {
            generateBillContent();
        }
        return bill.toString();
    }
}