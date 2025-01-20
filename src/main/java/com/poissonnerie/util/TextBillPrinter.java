package com.poissonnerie.util;

import com.poissonnerie.model.Client;
import java.awt.print.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TextBillPrinter implements Printable {
    private static final Logger LOGGER = Logger.getLogger(TextBillPrinter.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final StringBuilder bill;
    private JDialog previewDialog;
    private final String type;
    private final Client client;
    private final double montantRegle;
    private final double nouveauSolde;
    private final LocalDateTime date;

    public TextBillPrinter(String type, Client client, double montantRegle, double nouveauSolde) {
        this.type = type;
        this.client = client;
        this.montantRegle = montantRegle;
        this.nouveauSolde = nouveauSolde;
        this.date = LocalDateTime.now();
        this.bill = new StringBuilder();
        generateBillContent();
    }

    private void generateBillContent() {
        bill.setLength(0);

        // En-tête
        bill.append("                         MA POISSONNERIE\n");
        bill.append("\t123 Rue de la Mer\n");
        bill.append("\t75001 PARIS\n");
        bill.append("\t+33 1 23 45 67 89\n");
        bill.append("----------------------------------------------------------------\n");

        // Type de document et date
        bill.append(String.format("Type: %s\n", type));
        bill.append(String.format("Date: %s\n", DATE_FORMATTER.format(date)));
        bill.append("----------------------------------------------------------------\n");

        // Informations client
        bill.append("INFORMATIONS CLIENT\n");
        bill.append(String.format("Nom: %s\n", client.getNom()));
        if (client.getTelephone() != null && !client.getTelephone().isEmpty()) {
            bill.append(String.format("Tél: %s\n", client.getTelephone()));
        }
        bill.append("----------------------------------------------------------------\n");

        // Détails du règlement
        bill.append("DÉTAILS DU RÈGLEMENT\n");
        bill.append(String.format("Solde précédent  : %41.2f €\n", montantRegle + nouveauSolde));
        bill.append(String.format("Montant réglé    : %41.2f €\n", montantRegle));
        bill.append(String.format("Nouveau solde    : %41.2f €\n", nouveauSolde));

        bill.append("\n====================================\n");
        bill.append("                     Merci de votre confiance !\n");
        bill.append("----------------------------------------------------------------\n");
        bill.append("                     Software by MA POISSONNERIE\n");
    }

    public void imprimer() {
        showPreview();
    }

    private void showPreview() {
        try {
            previewDialog = new JDialog((Frame)null, "Prévisualisation du reçu", true);
            previewDialog.setLayout(new BorderLayout());

            JTextArea previewArea = new JTextArea(bill.toString());
            previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            previewArea.setEditable(false);
            previewArea.setBackground(Color.WHITE);

            JScrollPane scrollPane = new JScrollPane(previewArea);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            JButton printButton = new JButton("Imprimer");
            JButton cancelButton = new JButton("Annuler");

            printButton.addActionListener(e -> {
                previewDialog.dispose();
                doPrint();
            });

            cancelButton.addActionListener(e -> previewDialog.dispose());

            buttonPanel.add(printButton);
            buttonPanel.add(cancelButton);

            previewDialog.add(scrollPane, BorderLayout.CENTER);
            previewDialog.add(buttonPanel, BorderLayout.SOUTH);

            previewDialog.setSize(500, 600);
            previewDialog.setLocationRelativeTo(null);
            previewDialog.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la prévisualisation du reçu", e);
            throw new RuntimeException("Erreur de prévisualisation: " + e.getMessage());
        }
    }

    private void doPrint() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);

            if (job.printDialog()) {
                job.print();
                LOGGER.info("Impression du reçu réussie");
            }
        } catch (PrinterException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression du reçu", e);
            throw new RuntimeException("Erreur d'impression: " + e.getMessage());
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));

        String[] lines = bill.toString().split("\n");
        int y = 15;
        for (String line : lines) {
            g2d.drawString(line, 10, y);
            y += 12;
        }

        return PAGE_EXISTS;
    }

    public String getBillContent() {
        return bill.toString();
    }
}