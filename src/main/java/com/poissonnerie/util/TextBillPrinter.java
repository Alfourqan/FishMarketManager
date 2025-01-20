package com.poissonnerie.util;

import com.poissonnerie.model.Client;
import com.poissonnerie.model.Vente;
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

    // Variables pour le reçu de règlement
    private String type;
    private Client client;
    private double montantRegle;
    private double nouveauSolde;
    private LocalDateTime date;

    // Variable pour le ticket de vente
    private Vente vente;

    // Constructeur pour ticket de vente
    public TextBillPrinter(Vente vente) {
        this.vente = vente;
        this.bill = new StringBuilder();
        generateVenteBillContent();
    }

    // Constructeur pour reçu de règlement
    public TextBillPrinter(String type, Client client, double montantRegle, double nouveauSolde) {
        this.type = type;
        this.client = client;
        this.montantRegle = montantRegle;
        this.nouveauSolde = nouveauSolde;
        this.date = LocalDateTime.now();
        this.bill = new StringBuilder();
        generateReglementBillContent();
    }

    private void generateVenteBillContent() {
        bill.setLength(0);

        // En-tête
        bill.append("                         MA POISSONNERIE\n");
        bill.append("\t123 Rue de la Mer\n");
        bill.append("\t75001 PARIS\n");
        bill.append("\t+33 1 23 45 67 89\n");
        bill.append("----------------------------------------------------------------\n");

        // Informations de la vente
        bill.append(String.format("Date: %s\n", DATE_FORMATTER.format(vente.getDate())));
        bill.append(String.format("Type: %s\n", vente.getModePaiement().getLibelle()));
        if (vente.getClient() != null) {
            bill.append(String.format("Client: %s\n", vente.getClient().getNom()));
        }
        bill.append("----------------------------------------------------------------\n");

        // En-tête des colonnes
        bill.append(String.format("%-20s %8s %12s %10s\n", "Article", "Qté", "P.U.", "Total"));
        bill.append("----------------------------------------------------------------\n");

        // Articles
        for (Vente.LigneVente ligne : vente.getLignes()) {
            String nom = ligne.getProduit().getNom();
            if (nom.length() > 20) {
                nom = nom.substring(0, 17) + "...";
            }
            bill.append(String.format("%-20s %8d %12.2f %10.2f\n",
                nom,
                ligne.getQuantite(),
                ligne.getPrixUnitaire(),
                ligne.getPrixUnitaire() * ligne.getQuantite()));
        }

        bill.append("----------------------------------------------------------------\n");

        // Totaux avec alignement amélioré
        bill.append(String.format("Total HT  : %41.2f\n", vente.getTotalHT()));
        bill.append(String.format("TVA       : %41.2f\n", vente.getMontantTVA()));
        bill.append(String.format("Total TTC : %41.2f\n", vente.getTotal()));

        if (vente.getModePaiement() == Vente.ModePaiement.CREDIT && vente.getClient() != null) {
            bill.append(String.format("Nouveau solde client : %31.2f\n", vente.getClient().getSolde()));
        }

        bill.append("====================================\n");
        bill.append("                     Merci de votre confiance !\n");
        bill.append("----------------------------------------------------------------\n");
        bill.append("                     Software by MA POISSONNERIE\n");
    }

    private void generateReglementBillContent() {
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
            previewDialog = new JDialog((Frame)null, "Prévisualisation", true);
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
            LOGGER.log(Level.SEVERE, "Erreur lors de la prévisualisation", e);
            throw new RuntimeException("Erreur de prévisualisation: " + e.getMessage());
        }
    }

    private void doPrint() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);

            if (job.printDialog()) {
                job.print();
                LOGGER.info("Impression réussie");
            }
        } catch (PrinterException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression", e);
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