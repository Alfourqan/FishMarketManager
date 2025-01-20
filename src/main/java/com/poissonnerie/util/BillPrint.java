package com.poissonnerie.util;

import com.poissonnerie.model.Vente;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.print.*;
import java.io.*;
import java.time.format.DateTimeFormatter;

public class BillPrint {
    private static final Logger LOGGER = Logger.getLogger(BillPrint.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Commandes ESC/POS standard
    private static final byte[] ESC_INIT = {0x1B, 0x40};          // Initialize printer
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01}; // Center alignment
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};   // Left alignment
    private static final byte[] ESC_ALIGN_RIGHT = {0x1B, 0x61, 0x02};  // Right alignment
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};      // Bold ON
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};     // Bold OFF
    private static final byte[] ESC_DOUBLE_ON = {0x1B, 0x21, 0x30};    // Double height/width ON
    private static final byte[] ESC_DOUBLE_OFF = {0x1B, 0x21, 0x00};   // Double height/width OFF
    private static final byte[] ESC_UNDERLINE_ON = {0x1B, 0x2D, 0x01}; // Underline ON
    private static final byte[] ESC_UNDERLINE_OFF = {0x1B, 0x2D, 0x00};// Underline OFF
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

            // En-tête - Logo et nom du magasin
            writeBytes(ticketData, ESC_INIT);
            writeBytes(ticketData, ESC_ALIGN_CENTER);
            writeBytes(ticketData, ESC_DOUBLE_ON);
            writeLine(ticketData, "MA POISSONNERIE");
            writeBytes(ticketData, ESC_DOUBLE_OFF);
            writeBytes(ticketData, ESC_BOLD_OFF);
            writeLine(ticketData, "123 Rue de la Mer");
            writeLine(ticketData, "75001 PARIS");
            writeLine(ticketData, "Tel: 01 23 45 67 89");
            writeLine(ticketData, "SIRET: 123 568 941 00056");
            writeBytes(ticketData, LF);

            // Date et numéro de ticket
            writeBytes(ticketData, ESC_ALIGN_LEFT);
            writeLine(ticketData, String.format("DATE: %s", DATE_FORMATTER.format(vente.getDate())));
            writeLine(ticketData, String.format("TICKET N°: %06d", vente.getId()));
            writeBytes(ticketData, LF);

            // Ligne de séparation
            writeLine(ticketData, "----------------------------------------");
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, "ARTICLES                          PRIX");
            writeBytes(ticketData, ESC_BOLD_OFF);
            writeLine(ticketData, "----------------------------------------");

            // Articles
            for (Vente.LigneVente ligne : vente.getLignes()) {
                String nomProduit = ligne.getProduit().getNom();
                if (nomProduit.length() > 25) {
                    nomProduit = nomProduit.substring(0, 22) + "...";
                }

                // Afficher la quantité si > 1
                if (ligne.getQuantite() > 1) {
                    writeLine(ticketData, String.format("%s", nomProduit));
                    writeLine(ticketData, String.format("  %d x %.2f                    %.2f",
                        ligne.getQuantite(),
                        ligne.getPrixUnitaire(),
                        ligne.getQuantite() * ligne.getPrixUnitaire()));
                } else {
                    writeLine(ticketData, String.format("%-28s%9.2f",
                        nomProduit,
                        ligne.getPrixUnitaire()));
                }
            }

            writeLine(ticketData, "----------------------------------------");

            // Total
            writeBytes(ticketData, ESC_ALIGN_RIGHT);
            writeLine(ticketData, String.format("TOTAL HT:           %.2f€", vente.getTotal() / 1.20));
            writeLine(ticketData, String.format("TVA (20%%):          %.2f€", vente.getTotal() * 0.20));
            writeBytes(ticketData, ESC_BOLD_ON);
            writeLine(ticketData, String.format("TOTAL TTC:          %.2f€", vente.getTotal()));
            writeBytes(ticketData, ESC_BOLD_OFF);
            writeBytes(ticketData, LF);

            // Mode de paiement
            writeBytes(ticketData, ESC_ALIGN_LEFT);
            writeLine(ticketData, String.format("Mode de paiement: %s", vente.getModePaiement().getLibelle()));
            if (vente.isCredit() && vente.getClient() != null) {
                writeLine(ticketData, String.format("Client: %s", vente.getClient().getNom()));
            }
            writeBytes(ticketData, LF);

            // Message de remerciement
            writeBytes(ticketData, ESC_ALIGN_CENTER);
            writeLine(ticketData, "Merci de votre visite");
            writeLine(ticketData, "et à bientôt !");
            writeBytes(ticketData, LF);

            // TVA et informations légales
            writeLine(ticketData, "TVA intracommunautaire: FR 82 123568941");
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