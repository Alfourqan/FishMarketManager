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

            // En-tête du ticket (format standardisé)
            writeBytes(ticketData, ESC_INIT);
            writeBytes(ticketData, ESC_ALIGN_CENTER);
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, "BUSINESS NAME");
            writeBytes(ticketData, ESC_BOLD_OFF);

            // Adresse
            String[] adresse = {
                "1234 Main Street",
                "Suite 567",
                "City Name, State 54321",
                "123-456-7890"
            };

            for (String ligne : adresse) {
                writeLine(ticketData, ligne);
            }
            writeBytes(ticketData, LF);

            // Ligne de séparation
            writeLine(ticketData, "...................................................");

            // Informations de transaction
            writeBytes(ticketData, ESC_ALIGN_LEFT);
            writeLine(ticketData, String.format("Merchant ID:      %s", "987-654321"));
            writeLine(ticketData, String.format("Terminal ID:      %s", "0123456789"));
            writeBytes(ticketData, LF);

            writeLine(ticketData, String.format("Transaction ID:   #%d", vente.getId()));
            writeLine(ticketData, String.format("Type:            %s", vente.getModePaiement().getLibelle()));

            // Date d'achat
            writeBytes(ticketData, ESC_ALIGN_CENTER);
            writeLine(ticketData, "PURCHASE");
            writeLine(ticketData, DATE_FORMATTER.format(vente.getDate()));

            // Informations de carte
            writeBytes(ticketData, ESC_ALIGN_LEFT);
            writeLine(ticketData, "Number:          ************1234");
            writeLine(ticketData, "Entry Mode:      Swiped");
            writeLine(ticketData, "Card:            Card Name");
            writeLine(ticketData, "Response:        APPROVED");
            writeLine(ticketData, "Approval Code:   789-1234");

            // Ligne de séparation
            writeLine(ticketData, "...................................................");

            // Détails des articles
            for (Vente.LigneVente ligne : vente.getLignes()) {
                String nomProduit = ligne.getProduit().getNom();
                if (nomProduit.length() > 28) {
                    nomProduit = nomProduit.substring(0, 25) + "...";
                }

                if (ligne.getQuantite() > 1) {
                    writeLine(ticketData, nomProduit);
                    String detail = String.format("  %d x $%.2f", 
                        ligne.getQuantite(), 
                        ligne.getPrixUnitaire());
                    writeLine(ticketData, String.format("%-28s$%8.2f", 
                        detail, 
                        ligne.getQuantite() * ligne.getPrixUnitaire()));
                } else {
                    writeLine(ticketData, String.format("%-28s$%8.2f", 
                        nomProduit, 
                        ligne.getPrixUnitaire()));
                }
            }

            writeLine(ticketData, "...................................................");

            // Totaux
            double totalHT = vente.getTotal() / 1.20;
            double tva = vente.getTotal() - totalHT;

            writeBytes(ticketData, ESC_ALIGN_RIGHT);
            writeLine(ticketData, String.format("Sub Total        $%8.2f", totalHT));
            writeLine(ticketData, String.format("Sales Tax        $%8.2f", tva));
            writeLine(ticketData, "...................................................");
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, String.format("TOTAL (USD)      $%8.2f", vente.getTotal()));
            writeBytes(ticketData, ESC_BOLD_OFF);
            writeBytes(ticketData, LF);

            // Message de remerciement
            writeBytes(ticketData, ESC_ALIGN_CENTER);
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, "THANK YOU FOR");
            writeLine(ticketData, "YOUR PURCHASE");
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
        out.write(text.getBytes());
        writeBytes(out, CR);
        writeBytes(out, LF);
    }
}