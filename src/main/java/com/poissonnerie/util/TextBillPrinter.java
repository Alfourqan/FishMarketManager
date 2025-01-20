package com.poissonnerie.util;

import com.poissonnerie.model.Vente;
import java.awt.print.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TextBillPrinter implements Printable {
    private static final Logger LOGGER = Logger.getLogger(TextBillPrinter.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final Vente vente;
    private final StringBuilder bill;
    private JDialog previewDialog;

    public TextBillPrinter(Vente vente) {
        this.vente = vente;
        this.bill = new StringBuilder();
        generateBillContent(); // Générer le contenu immédiatement à la création
    }

    private void generateBillContent() {
        bill.setLength(0); // Nettoyer le contenu précédent

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

    public void imprimer() {
        showPreview();
    }

    private void showPreview() {
        try {
            // Créer la boîte de dialogue de prévisualisation
            previewDialog = new JDialog((Frame)null, "Prévisualisation du ticket", true);
            previewDialog.setLayout(new BorderLayout());

            // Zone de texte pour la prévisualisation
            JTextArea previewArea = new JTextArea(bill.toString());
            previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            previewArea.setEditable(false);
            previewArea.setBackground(Color.WHITE);

            // Panneau de défilement
            JScrollPane scrollPane = new JScrollPane(previewArea);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Panneau de boutons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            JButton printButton = new JButton("Imprimer");
            JButton cancelButton = new JButton("Annuler");

            // Action pour le bouton Imprimer
            printButton.addActionListener(e -> {
                previewDialog.dispose();
                doPrint();
            });

            // Action pour le bouton Annuler
            cancelButton.addActionListener(e -> previewDialog.dispose());

            buttonPanel.add(printButton);
            buttonPanel.add(cancelButton);

            // Ajouter les composants à la boîte de dialogue
            previewDialog.add(scrollPane, BorderLayout.CENTER);
            previewDialog.add(buttonPanel, BorderLayout.SOUTH);

            // Configurer et afficher la boîte de dialogue
            previewDialog.setSize(500, 600);
            previewDialog.setLocationRelativeTo(null);
            previewDialog.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la prévisualisation du ticket", e);
            throw new RuntimeException("Erreur de prévisualisation: " + e.getMessage());
        }
    }

    private void doPrint() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);

            if (job.printDialog()) {
                job.print();
                LOGGER.info("Impression du ticket réussie");
            }
        } catch (PrinterException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression du ticket", e);
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

    public String getBillContent() {
        return bill.toString();
    }
}