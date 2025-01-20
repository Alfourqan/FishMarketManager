package com.poissonnerie.util;

import com.poissonnerie.model.Vente;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.print.*;
import java.io.*;
import java.time.format.DateTimeFormatter;

public class BillPrint {
    private static final Logger LOGGER = Logger.getLogger(BillPrint.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    // Commandes ESC/POS standard
    private static final byte[] ESC_INIT = {0x1B, 0x40};          // Initialize printer
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01}; // Center alignment
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};   // Left alignment
    private static final byte[] ESC_ALIGN_RIGHT = {0x1B, 0x61, 0x02};  // Right alignment
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};      // Bold ON
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};     // Bold OFF
    private static final byte[] LF = {0x0A};                      // Line Feed
    private static final byte[] CR = {0x0D};                      // Carriage Return
    private static final byte[] CUT_PAPER = {0x1D, 0x56, 0x41};  // Full cut

    private final Vente vente;
    private static final int X_MARGIN = 40;     // Marge horizontale comme dans le code Python
    private static final int Y_MARGIN = 60;     // Marge verticale comme dans le code Python

    public BillPrint(Vente vente) {
        this.vente = vente;
    }

    public void imprimer() {
        try {
            // Recherche d'une imprimante de tickets
            PrintService imprimanteTicket = null;
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

            for (PrintService service : services) {
                String nomImprimante = service.getName().toLowerCase();
                if (nomImprimante.contains("ticket") || 
                    nomImprimante.contains("receipt") || 
                    nomImprimante.contains("thermal")) {
                    imprimanteTicket = service;
                    break;
                }
            }

            if (imprimanteTicket == null) {
                LOGGER.warning("Aucune imprimante de tickets trouvée!");
                return;
            }

            // Création du document à imprimer
            ByteArrayOutputStream ticketData = new ByteArrayOutputStream();

            // En-tête du ticket (comme dans le code Python)
            writeBytes(ticketData, ESC_INIT);
            writeBytes(ticketData, ESC_ALIGN_CENTER);
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, "MA POISSONNERIE");
            writeBytes(ticketData, ESC_BOLD_OFF);

            // Adresse (comme dans le code Python)
            String[] adresse = {
                "123 Rue de la Mer",
                "75001 PARIS",
                "Tel: 01 23 45 67 89",
                "SIRET: 123 568 941 00056"
            };

            for (String ligne : adresse) {
                writeLine(ticketData, ligne);
            }
            writeBytes(ticketData, LF);

            // Ligne de séparation (comme dans le code Python)
            writeBytes(ticketData, ESC_ALIGN_LEFT);
            writeLine(ticketData, "-".repeat(40));

            // Articles (comme dans le code Python)
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, String.format("%-28s%14s", "ARTICLES", "PRIX"));
            writeBytes(ticketData, ESC_BOLD_OFF);
            writeLine(ticketData, "-".repeat(40));

            // Détails des articles
            for (Vente.LigneVente ligne : vente.getLignes()) {
                String nomProduit = ligne.getProduit().getNom();
                if (nomProduit.length() > 28) {
                    nomProduit = nomProduit.substring(0, 25) + "...";
                }

                if (ligne.getQuantite() > 1) {
                    writeLine(ticketData, nomProduit);
                    String detail = String.format("  %d x %.2f", 
                        ligne.getQuantite(), 
                        ligne.getPrixUnitaire());
                    writeLine(ticketData, String.format("%-28s%14.2f", 
                        detail, 
                        ligne.getQuantite() * ligne.getPrixUnitaire()));
                } else {
                    writeLine(ticketData, String.format("%-28s%14.2f", 
                        nomProduit, 
                        ligne.getPrixUnitaire()));
                }
            }

            writeLine(ticketData, "-".repeat(40));

            // Totaux (comme dans le code Python)
            double totalHT = vente.getTotal() / 1.20;
            double tva = vente.getTotal() - totalHT;

            writeBytes(ticketData, ESC_ALIGN_RIGHT);
            writeLine(ticketData, String.format("Sub Total:%14.2f", totalHT));
            writeLine(ticketData, String.format("TVA (20%%):%14.2f", tva));
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, String.format("TOTAL:%14.2f", vente.getTotal()));
            writeBytes(ticketData, ESC_BOLD_OFF);
            writeBytes(ticketData, LF);

            // Mode de paiement
            writeBytes(ticketData, ESC_ALIGN_LEFT);
            writeLine(ticketData, String.format("Paid By: %s", vente.getModePaiement().getLibelle()));
            if (vente.isCredit() && vente.getClient() != null) {
                writeLine(ticketData, String.format("Client: %s", vente.getClient().getNom()));
            }
            writeBytes(ticketData, LF);

            // Message de remerciement (comme dans le code Python)
            writeBytes(ticketData, ESC_ALIGN_CENTER);
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, "Thank You For Supporting Local Business!");
            writeBytes(ticketData, ESC_BOLD_OFF);
            writeBytes(ticketData, LF);

            // Couper le papier
            writeBytes(ticketData, CUT_PAPER);

            // Impression
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(ticketData.toByteArray(), flavor, null);
            DocPrintJob job = imprimanteTicket.createPrintJob();
            job.print(doc, null);

            LOGGER.info("Ticket imprimé avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'impression du ticket", e);
            throw new RuntimeException("Erreur d'impression: " + e.getMessage());
        }
    }

    private void writeBytes(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(bytes);
    }

    private void writeLine(ByteArrayOutputStream out, String text) throws IOException {
        // Ajouter les marges comme dans le code Python
        String paddedText = " ".repeat(X_MARGIN) + text;
        out.write(paddedText.getBytes());
        writeBytes(out, CR);
        writeBytes(out, LF);
    }
}