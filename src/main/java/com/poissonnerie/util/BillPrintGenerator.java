package com.poissonnerie.util;

import com.poissonnerie.model.Vente;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.*;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.MediaSizeName;

public class BillPrintGenerator implements Printable {
    private static final Logger LOGGER = Logger.getLogger(BillPrintGenerator.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    private final Vente vente;
    private static final String BUSINESS_NAME = "BUSINESS NAME";
    private static final String BUSINESS_ADDRESS = "1234 Main Street";
    private static final String BUSINESS_SUITE = "Suite 567";
    private static final String BUSINESS_CITY = "City Name, State 54321";
    private static final String BUSINESS_PHONE = "123-456-7890";

    // Merchant information
    private static final String MERCHANT_ID = "987-654321";
    private static final String TERMINAL_ID = "0123456789";

    public BillPrintGenerator(Vente vente) {
        this.vente = vente;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        int y = 10;
        int leftMargin = 10;

        // Business Information
        g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
        centerText(g2d, BUSINESS_NAME, pageFormat, y);
        y += 15;

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        centerText(g2d, BUSINESS_ADDRESS, pageFormat, y);
        y += 15;
        centerText(g2d, BUSINESS_SUITE, pageFormat, y);
        y += 15;
        centerText(g2d, BUSINESS_CITY, pageFormat, y);
        y += 15;
        centerText(g2d, BUSINESS_PHONE, pageFormat, y);
        y += 20;

        // Merchant Information
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        String merchantInfo = String.format("Merchant ID:      %s", MERCHANT_ID);
        g2d.drawString(merchantInfo, leftMargin, y);
        y += 15;
        String terminalInfo = String.format("Terminal ID:      %s", TERMINAL_ID);
        g2d.drawString(terminalInfo, leftMargin, y);
        y += 15;

        // Transaction Information
        String transactionId = String.format("Transaction ID:   #%s", vente.getId());
        g2d.drawString(transactionId, leftMargin, y);
        y += 15;
        String type = String.format("Type:            %s", vente.getModePaiement().getLibelle());
        g2d.drawString(type, leftMargin, y);
        y += 20;

        // Purchase Date
        String purchaseTitle = "PURCHASE";
        centerText(g2d, purchaseTitle, pageFormat, y);
        y += 15;
        String date = DATE_FORMATTER.format(vente.getDate());
        centerText(g2d, date, pageFormat, y);
        y += 20;

        // Payment card details (masked)
        String cardNumber = "************1234";
        g2d.drawString("Number:          " + cardNumber, leftMargin, y);
        y += 15;
        g2d.drawString("Entry Mode:      Swiped", leftMargin, y);
        y += 15;
        g2d.drawString("Card:            Card Name", leftMargin, y);
        y += 15;
        g2d.drawString("Response:        APPROVED", leftMargin, y);
        y += 15;
        g2d.drawString("Approval Code:   789-1234", leftMargin, y);
        y += 20;

        // Ligne de séparation
        drawDottedLine(g2d, leftMargin, y, (int)pageFormat.getImageableWidth() - 20);
        y += 15;

        // Articles
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        for (Vente.LigneVente ligne : vente.getLignes()) {
            String article = String.format("%-20s %8.2f", 
                truncateString(ligne.getProduit().getNom(), 20),
                ligne.getPrixUnitaire() * ligne.getQuantite());
            g2d.drawString(article, leftMargin, y);
            y += 15;
        }

        y += 5;
        drawDottedLine(g2d, leftMargin, y, (int)pageFormat.getImageableWidth() - 20);
        y += 15;

        // Totaux
        String subTotal = String.format("Sub Total        %8.2f", vente.getTotalHT());
        g2d.drawString(subTotal, leftMargin, y);
        y += 15;

        String salesTax = String.format("Sales Tax        %8.2f", vente.getMontantTVA());
        g2d.drawString(salesTax, leftMargin, y);
        y += 15;

        drawDottedLine(g2d, leftMargin, y, (int)pageFormat.getImageableWidth() - 20);
        y += 15;

        // Total final
        g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
        String total = String.format("TOTAL (USD)      %8.2f", vente.getTotal());
        g2d.drawString(total, leftMargin, y);
        y += 25;

        // Message de remerciement
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        centerText(g2d, "THANK YOU FOR", pageFormat, y);
        y += 15;
        centerText(g2d, "YOUR PURCHASE", pageFormat, y);

        return PAGE_EXISTS;
    }

    private void centerText(Graphics2D g2d, String text, PageFormat pageFormat, int y) {
        int stringWidth = g2d.getFontMetrics().stringWidth(text);
        int x = ((int) pageFormat.getImageableWidth() - stringWidth) / 2;
        g2d.drawString(text, x, y);
    }

    private void drawDottedLine(Graphics2D g2d, int x, int y, int width) {
        for (int i = x; i < width; i += 2) {
            g2d.drawString(".", i, y);
        }
    }

    private String truncateString(String str, int length) {
        if (str.length() <= length) {
            return str;
        }
        return str.substring(0, length - 3) + "...";
    }

    public boolean print() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);

            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            attributes.add(new Copies(1));
            attributes.add(MediaSizeName.INVOICE);

            // Chercher l'imprimante de tickets
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService selectedService = null;

            LOGGER.info("Recherche d'une imprimante de tickets...");
            for (PrintService service : services) {
                String printerName = service.getName().toLowerCase();
                LOGGER.info("Imprimante trouvée: " + service.getName());

                if (printerName.contains("receipt") || 
                    printerName.contains("ticket") ||
                    printerName.contains("thermal")) {
                    selectedService = service;
                    LOGGER.info("Imprimante de tickets sélectionnée: " + service.getName());
                    break;
                }
            }

            if (selectedService != null) {
                job.setPrintService(selectedService);
                LOGGER.info("Configuration de l'impression avec l'imprimante: " + selectedService.getName());

                if (job.printDialog(attributes)) {
                    job.print(attributes);
                    LOGGER.info("Ticket imprimé avec succès");
                    return true;
                } else {
                    LOGGER.info("Impression annulée par l'utilisateur");
                    return false;
                }
            } else {
                LOGGER.warning("Aucune imprimante de tickets trouvée. Utilisation de l'imprimante par défaut.");
                if (job.printDialog(attributes)) {
                    job.print(attributes);
                    LOGGER.info("Ticket imprimé avec succès sur l'imprimante par défaut");
                    return true;
                }
            }

            return false;
        } catch (PrinterException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression du ticket", e);
            throw new RuntimeException("Erreur d'impression: " + e.getMessage());
        }
    }
}