package com.poissonnerie.util;

import com.poissonnerie.model.*;
import java.awt.print.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TextBillPrinter implements Printable {
    private static final Logger LOGGER = Logger.getLogger(TextBillPrinter.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int TICKET_WIDTH = 42; // Largeur standard pour ticket thermique 80mm
    private static final Font PREVIEW_FONT = new Font("Monospaced", Font.PLAIN, 12);
    private static final Color BACKGROUND_COLOR = new Color(252, 252, 252);
    private static final Color BORDER_COLOR = new Color(224, 224, 224);
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

    public TextBillPrinter(Vente vente) {
        if (vente == null) {
            throw new IllegalArgumentException("La vente ne peut pas être null");
        }
        this.vente = vente;
        this.type = "TICKET DE VENTE";
        this.bill = new StringBuilder();
        generateVenteBillContent();
    }

    public TextBillPrinter(String type, Client client, double montantRegle, double nouveauSolde) {
        if (client == null) {
            throw new IllegalArgumentException("Le client ne peut pas être null");
        }
        if (montantRegle < 0) {
            throw new IllegalArgumentException("Le montant réglé ne peut pas être négatif");
        }
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

        // En-tête avec bordure
        appendCentered("MA POISSONNERIE");
        appendCentered("123 Rue de la Mer");
        appendCentered("75001 PARIS");
        appendCentered("Tél: +33 1 23 45 67 89");
        appendCentered("SIRET: 123 456 789 00012");
        appendSeparator();

        // Informations de la vente
        appendLine(String.format("N° Ticket: %s", formatTicketNumber(vente.getId())));
        appendLine(String.format("Date: %s", DATE_FORMATTER.format(vente.getDate())));
        appendLine(String.format("Mode: %s", vente.getModePaiement().getLibelle()));
        if (vente.getClient() != null) {
            appendLine(String.format("Client: %s", vente.getClient().getNom()));
        }
        appendSeparator();

        // En-tête des articles
        String header = String.format("%-18s %6s %7s %8s", 
            "Article", "Qté", "P.U.", "Total");
        appendLine(header);
        appendSeparator();

        // Articles avec alignement amélioré
        double totalHT = 0;
        for (Vente.LigneVente ligne : vente.getLignes()) {
            String nom = formatProductName(ligne.getProduit().getNom(), 18);
            appendLine(String.format("%-18s %6d %7.2f %8.2f",
                nom,
                ligne.getQuantite(),
                ligne.getPrixUnitaire(),
                ligne.getPrixUnitaire() * ligne.getQuantite()));
            totalHT += ligne.getPrixUnitaire() * ligne.getQuantite();
        }
        appendSeparator();

        // Totaux avec alignement à droite
        appendAlignedRight(String.format("Total HT:  %8.2f €", totalHT));
        appendAlignedRight(String.format("TVA %.1f%%: %8.2f €", 
            vente.getTauxTVA(), vente.getMontantTVA()));
        appendSeparator();
        appendAlignedRight(String.format("TOTAL TTC: %8.2f €", vente.getTotal()));

        // Pied de ticket
        appendSeparator();
        appendCentered("Merci de votre confiance !");
        appendCentered("À bientôt !");
        appendSeparator();
        appendCentered("MA POISSONNERIE");
        appendLine(DATE_FORMATTER.format(LocalDateTime.now()));
    }

    private String formatTicketNumber(int id) {
        return String.format("T%07d", id);
    }

    private String formatProductName(String name, int maxLength) {
        if (name.length() <= maxLength) {
            return name;
        }
        return name.substring(0, maxLength - 3) + "...";
    }

    private void generateReglementBillContent() {
        bill.setLength(0);

        // En-tête
        appendCentered("MA POISSONNERIE");
        appendCentered("123 Rue de la Mer");
        appendCentered("75001 PARIS");
        appendCentered("Tél: +33 1 23 45 67 89");
        appendSeparator();

        // Type de document et date
        appendCentered(type.toUpperCase());
        appendLine(String.format("Date: %s", DATE_FORMATTER.format(date)));
        appendSeparator();

        // Informations client
        appendLine("INFORMATIONS CLIENT");
        appendLine(String.format("Nom: %s", client.getNom()));
        if (client.getTelephone() != null && !client.getTelephone().isEmpty()) {
            appendLine(String.format("Tél: %s", client.getTelephone()));
        }
        appendSeparator();

        // Détails du règlement
        appendLine("DÉTAILS DU RÈGLEMENT");
        appendAlignedRight(String.format("Solde précédent: %8.2f €", montantRegle + nouveauSolde));
        appendAlignedRight(String.format("Montant réglé:   %8.2f €", montantRegle));
        appendSeparator();
        appendAlignedRight(String.format("Nouveau solde:   %8.2f €", nouveauSolde));

        // Pied de reçu
        appendSeparator();
        appendCentered("Merci de votre confiance !");
        appendSeparator();
    }

    private void appendLine(String text) {
        bill.append(text).append("\n");
    }

    private void appendSeparator() {
        bill.append("-".repeat(TICKET_WIDTH)).append("\n");
    }

    private void appendCentered(String text) {
        int padding = (TICKET_WIDTH - text.length()) / 2;
        bill.append(" ".repeat(padding))
            .append(text)
            .append("\n");
    }

    private void appendAlignedRight(String text) {
        int padding = TICKET_WIDTH - text.length();
        if (padding > 0) {
            bill.append(" ".repeat(padding));
        }
        bill.append(text).append("\n");
    }

    public void imprimer() {
        showPreview();
    }

    private void showPreview() {
        try {
            previewDialog = new JDialog((Frame)null, "Prévisualisation du Ticket", true);
            previewDialog.setLayout(new BorderLayout(10, 10));
            previewDialog.getContentPane().setBackground(new Color(245, 245, 245));

            // Panel principal
            JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
            mainPanel.setBackground(BACKGROUND_COLOR);
            mainPanel.setBorder(new CompoundBorder(
                new EmptyBorder(20, 20, 20, 20),
                new CompoundBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    new LineBorder(BORDER_COLOR, 1)
                )
            ));

            // Zone de prévisualisation
            JTextPane previewPane = new JTextPane();
            previewPane.setFont(PREVIEW_FONT);
            previewPane.setEditable(false);
            previewPane.setBackground(BACKGROUND_COLOR);
            previewPane.setText(bill.toString());
            previewPane.setBorder(new EmptyBorder(15, 15, 15, 15));

            // ScrollPane
            JScrollPane scrollPane = new JScrollPane(previewPane);
            scrollPane.setBorder(null);
            scrollPane.setBackground(BACKGROUND_COLOR);
            scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getVerticalScrollBar().setBorder(null);

            // Panel des boutons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            buttonPanel.setBackground(BACKGROUND_COLOR);
            buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

            // Boutons
            JButton printButton = createStyledButton("Imprimer", new Color(76, 175, 80));
            JButton cancelButton = createStyledButton("Annuler", new Color(244, 67, 54));

            // Actions des boutons
            printButton.addActionListener(e -> {
                previewDialog.dispose();
                doPrint();
            });
            cancelButton.addActionListener(e -> previewDialog.dispose());

            // Assemblage de l'interface
            buttonPanel.add(printButton);
            buttonPanel.add(cancelButton);

            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            previewDialog.add(mainPanel, BorderLayout.CENTER);
            previewDialog.setSize(450, 700);
            previewDialog.setLocationRelativeTo(null);
            previewDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            previewDialog.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la prévisualisation", e);
            throw new RuntimeException("Erreur de prévisualisation: " + e.getMessage());
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(120, 35));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Effet de survol
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(color.darker().darker());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
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