package com.poissonnerie.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.poissonnerie.model.*;
import com.poissonnerie.model.Vente.ModePaiement;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.time.LocalDateTime;

public class PDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(PDFGenerator.class.getName());
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font HIGHLIGHT_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.RED);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static class SimpleFooter extends PdfPageEventHelper {
        Font ffont = new Font(Font.FontFamily.HELVETICA, 8);

        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase("Page " + writer.getPageNumber(), ffont);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    footer,
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - 10, 0);
        }
    }

    public static void genererRapportFinancier(
            Map<String, Double> chiffreAffaires,
            Map<String, Double> couts,
            Map<String, Double> benefices,
            Map<String, Double> marges,
            ByteArrayOutputStream outputStream) throws DocumentException {
        Document document = null;
        try {
            document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // En-tête avec date et titre
            Paragraph header = new Paragraph();
            header.add(new Chunk("Rapport Financier - Généré le " +
                    DATE_FORMATTER.format(LocalDateTime.now()) + "\n\n", SUBTITLE_FONT));
            document.add(header);

            // Résumé financier
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(10f);
            summaryTable.setSpacingAfter(10f);

            // Calcul des totaux
            double totalCA = chiffreAffaires.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalCouts = couts.values().stream().mapToDouble(Double::doubleValue).sum();
            double totalBenefices = benefices.values().stream().mapToDouble(Double::doubleValue).sum();
            double margeMoyenne = marges.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            // Ajout du résumé
            addTableCell(summaryTable, "Total Chiffre d'Affaires", String.format("%.2f €", totalCA), true);
            addTableCell(summaryTable, "Total Coûts", String.format("%.2f €", totalCouts), true);
            addTableCell(summaryTable, "Total Bénéfices", String.format("%.2f €", totalBenefices), true);
            addTableCell(summaryTable, "Marge Moyenne", String.format("%.2f %%", margeMoyenne), true);

            document.add(summaryTable);
            document.add(Chunk.NEWLINE);

            // Sections détaillées
            ajouterSectionFinanciere(document, "Évolution du Chiffre d'Affaires", chiffreAffaires);
            document.add(Chunk.NEWLINE);
            ajouterSectionFinanciere(document, "Analyse des Coûts", couts);
            document.add(Chunk.NEWLINE);
            ajouterSectionFinanciere(document, "Suivi des Bénéfices", benefices);
            document.add(Chunk.NEWLINE);
            ajouterSectionFinanciere(document, "Analyse des Marges", marges);

            LOGGER.info("Rapport financier PDF généré avec succès");
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    private static void addTableCell(PdfPTable table, String label, String value, boolean isHeader) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, isHeader ? SUBTITLE_FONT : NORMAL_FONT));
        PdfPCell valueCell = new PdfPCell(new Phrase(value, isHeader ? SUBTITLE_FONT : NORMAL_FONT));

        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        if (isHeader) {
            labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            valueCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        }

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static void ajouterSectionFinanciere(Document document, String titre, Map<String, Double> donnees)
            throws DocumentException {
        Paragraph titreParagraph = new Paragraph(titre, SUBTITLE_FONT);
        titreParagraph.setSpacingBefore(10f);
        document.add(titreParagraph);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);
        table.setSpacingAfter(10f);

        // En-têtes
        PdfPCell headerCell1 = new PdfPCell(new Phrase("Période", SUBTITLE_FONT));
        PdfPCell headerCell2 = new PdfPCell(new Phrase("Montant", SUBTITLE_FONT));

        headerCell1.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell2.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(headerCell1);
        table.addCell(headerCell2);

        // Tri des données par période
        TreeMap<String, Double> donneesTriees = new TreeMap<>(donnees);

        for (Map.Entry<String, Double> entry : donneesTriees.entrySet()) {
            PdfPCell periodeCell = new PdfPCell(new Phrase(entry.getKey(), NORMAL_FONT));
            PdfPCell montantCell = new PdfPCell(new Phrase(
                    String.format("%.2f €", entry.getValue()), NORMAL_FONT));

            periodeCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            montantCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            table.addCell(periodeCell);
            table.addCell(montantCell);
        }

        document.add(table);
    }

    public static void genererRapportStocks(List<Produit> produits, Map<String, Double> statistiques, ByteArrayOutputStream outputStream) throws DocumentException {
        Document document = null;
        try {
            document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // En-tête
            Paragraph titre = new Paragraph("État des Stocks", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            // Tableau principal
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes du tableau
            String[] headers = {
                    "Référence", "Nom", "Catégorie", "Prix Achat", "Prix Vente",
                    "Stock", "Seuil", "Valeur Stock"
            };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données et calculs
            double valeurTotaleStock = 0;
            Map<String, Integer> produitsParCategorie = new HashMap<>();
            Map<String, Double> valeurParCategorie = new HashMap<>();

            for (Produit p : produits) {
                table.addCell(new Phrase(String.valueOf(p.getId()), NORMAL_FONT));
                table.addCell(new Phrase(p.getNom(), NORMAL_FONT));
                table.addCell(new Phrase(p.getCategorie(), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", p.getPrixAchat()), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", p.getPrixVente()), NORMAL_FONT));

                // Mise en évidence des stocks bas
                PdfPCell stockCell = new PdfPCell();
                if (p.getQuantite() <= p.getSeuilAlerte()) {
                    stockCell.addElement(new Phrase(String.valueOf(p.getQuantite()), HIGHLIGHT_FONT));
                } else {
                    stockCell.addElement(new Phrase(String.valueOf(p.getQuantite()), NORMAL_FONT));
                }
                stockCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(stockCell);

                table.addCell(new Phrase(String.valueOf(p.getSeuilAlerte()), NORMAL_FONT));

                double valeurStock = p.getQuantite() * p.getPrixAchat();
                table.addCell(new Phrase(String.format("%.2f €", valeurStock), NORMAL_FONT));

                // Calculs pour analyses
                valeurTotaleStock += valeurStock;
                produitsParCategorie.merge(p.getCategorie(), 1, Integer::sum);
                valeurParCategorie.merge(p.getCategorie(), valeurStock, Double::sum);
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Section Analyses
            Paragraph analyseTitre = new Paragraph("Analyses des Stocks", SUBTITLE_FONT);
            analyseTitre.setSpacingBefore(20f);
            document.add(analyseTitre);

            // Tableau d'analyses
            PdfPTable analyseTable = new PdfPTable(2);
            analyseTable.setWidthPercentage(70);
            analyseTable.setSpacingBefore(10f);

            // Valeur totale
            addTableCell(analyseTable, "Valeur totale du stock",
                    String.format("%.2f €", valeurTotaleStock), true);

            // Distribution par catégorie
            for (Map.Entry<String, Integer> entry : produitsParCategorie.entrySet()) {
                String categorie = entry.getKey();
                addTableCell(analyseTable,
                        String.format("Catégorie %s (%d produits)", categorie, entry.getValue()),
                        String.format("%.2f €", valeurParCategorie.get(categorie)),
                        false);
            }

            // Statistiques supplémentaires
            if (statistiques != null && !statistiques.isEmpty()) {
                for (Map.Entry<String, Double> stat : statistiques.entrySet()) {
                    addTableCell(analyseTable, stat.getKey(),
                            String.format("%.2f", stat.getValue()), false);
                }
            }

            document.add(analyseTable);

            LOGGER.info("Rapport des stocks PDF généré avec succès");
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    public static void genererRapportVentes(List<Vente> ventes, ByteArrayOutputStream outputStream) throws DocumentException {
        Document document = null;
        try {
            document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // En-tête
            Paragraph titre = new Paragraph("Rapport des Ventes", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            // Tableau principal des ventes
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {
                    "Date", "Client", "Produits", "Total HT", "TVA",
                    "Total TTC", "Mode", "Marge"
            };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Variables pour analyses
            double totalHT = 0;
            double totalTVA = 0;
            double totalTTC = 0;
            double totalMarge = 0;
            Map<String, Double> ventesParJour = new TreeMap<>();
            Map<ModePaiement, Double> ventesParMode = new EnumMap<>(ModePaiement.class);

            // Données
            for (Vente v : ventes) {
                table.addCell(new Phrase(v.getDate().format(DATE_FORMATTER), NORMAL_FONT));
                table.addCell(new Phrase(v.getClient() != null ? v.getClient().getNom() : "Vente comptant", NORMAL_FONT));
                table.addCell(new Phrase(String.valueOf(v.getLignes().size()), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", v.getTotalHT()), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", v.getMontantTVA()), NORMAL_FONT));
                table.addCell(new Phrase(String.format("%.2f €", v.getTotal()), NORMAL_FONT));
                table.addCell(new Phrase(v.getModePaiement().getLibelle(), NORMAL_FONT));

                // Calcul de la marge
                double margeVente = 0.0;
                for (Vente.LigneVente ligne : v.getLignes()) {
                    double marge = (ligne.getPrixUnitaire() - ligne.getProduit().getPrixAchat()) * ligne.getQuantite();
                    margeVente += marge;
                }
                table.addCell(new Phrase(String.format("%.2f €", margeVente), NORMAL_FONT));

                // Cumuls pour analyses
                totalHT += v.getTotalHT();
                totalTVA += v.getMontantTVA();
                totalTTC += v.getTotal();
                totalMarge += margeVente;

                // Agrégations
                String dateKey = v.getDate().format(DATE_FORMATTER);
                ventesParJour.merge(dateKey, v.getTotal(), Double::sum);
                ventesParMode.merge(v.getModePaiement(), v.getTotal(), Double::sum);
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // Section Analyses
            Paragraph analyseTitre = new Paragraph("Analyses des Ventes", SUBTITLE_FONT);
            analyseTitre.setSpacingBefore(20f);
            document.add(analyseTitre);

            // Tableau récapitulatif
            PdfPTable analyseTable = new PdfPTable(2);
            analyseTable.setWidthPercentage(70);
            analyseTable.setSpacingBefore(10f);

            // Totaux
            addTableCell(analyseTable, "Total HT", String.format("%.2f €", totalHT), true);
            addTableCell(analyseTable, "Total TVA", String.format("%.2f €", totalTVA), true);
            addTableCell(analyseTable, "Total TTC", String.format("%.2f €", totalTTC), true);
            addTableCell(analyseTable, "Marge totale", String.format("%.2f €", totalMarge), true);
            addTableCell(analyseTable, "Taux de marge",
                    String.format("%.2f %%", totalHT > 0 ? (totalMarge / totalHT) * 100 : 0), true);

            document.add(analyseTable);
            document.add(Chunk.NEWLINE);

            // Répartition par mode de paiement
            Paragraph modeTitre = new Paragraph("Répartition par mode de paiement", SUBTITLE_FONT);
            document.add(modeTitre);

            PdfPTable modeTable = new PdfPTable(2);
            modeTable.setWidthPercentage(70);
            modeTable.setSpacingBefore(10f);

            for (Map.Entry<ModePaiement, Double> entry : ventesParMode.entrySet()) {
                addTableCell(modeTable, entry.getKey().getLibelle(),
                        String.format("%.2f €", entry.getValue()), false);
            }

            document.add(modeTable);

            LOGGER.info("Rapport des ventes PDF généré avec succès");
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    public static void genererRapportCreances(List<Client> clients, ByteArrayOutputStream outputStream) throws DocumentException {
        Document document = null;
        try {
            document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            Paragraph titre = new Paragraph("État des Créances", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Client client : clients) {
                if (client.getSolde() > 0) {
                    table.addCell(new Phrase(client.getNom(), NORMAL_FONT));
                    table.addCell(new Phrase(client.getTelephone(), NORMAL_FONT));
                    table.addCell(new Phrase(String.format("%.2f €", client.getSolde()), NORMAL_FONT));
                    table.addCell(new Phrase("-", NORMAL_FONT)); // TODO: Implémenter la date d'échéance
                }
            }

            document.add(table);
            LOGGER.info("Rapport des créances PDF généré avec succès");
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, ByteArrayOutputStream outputStream) throws DocumentException {
        Document document = null;
        try {
            document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            Paragraph titre = new Paragraph("Liste des Fournisseurs", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {"Nom", "Contact", "Téléphone", "Email"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Fournisseur fournisseur : fournisseurs) {
                table.addCell(new Phrase(fournisseur.getNom(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getContact(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getTelephone(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getEmail(), NORMAL_FONT));
            }

            document.add(table);
            LOGGER.info("Rapport des fournisseurs PDF généré avec succès");
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }
    public static void genererReglementCreance(Client client, double montantPaye, double nouveauSolde, String cheminFichier) {
        Document document = null;
        try {
            document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            SimpleFooter footer = new SimpleFooter();
            writer.setPageEvent(footer);
            document.open();

            // En-tête
            Paragraph titre = new Paragraph("Reçu de Paiement - Créance", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            // Informations client
            document.add(new Paragraph("Client: " + client.getNom(), SUBTITLE_FONT));
            document.add(new Paragraph("Téléphone: " + client.getTelephone(), NORMAL_FONT));
            document.add(Chunk.NEWLINE);

            // Détails du paiement
            document.add(new Paragraph("Détails du paiement:", SUBTITLE_FONT));
            document.add(new Paragraph("Montant payé: " + String.format("%.2f €", montantPaye), NORMAL_FONT));
            document.add(new Paragraph("Nouveau solde: " + String.format("%.2f €", nouveauSolde), NORMAL_FONT));
            document.add(new Paragraph("Date: " + DATE_FORMATTER.format(java.time.LocalDateTime.now()), NORMAL_FONT));

            LOGGER.info("Reçu de paiement créance généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du reçu de paiement", e);
            throw new RuntimeException("Erreur lors de la génération du reçu", e);
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    public static String genererPreviewTicket(Vente vente) {
        Document document = null;
        String tempFile = "preview_ticket_" + System.currentTimeMillis() + ".pdf";
        try {
            document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));
            SimpleFooter footer = new SimpleFooter();
            writer.setPageEvent(footer);
            document.open();
            genererContenuTicket(document, vente);
            return tempFile;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération de la prévisualisation du ticket", e);
            throw new RuntimeException("Erreur lors de la génération de la prévisualisation", e);
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    public static void genererTicket(Vente vente, String cheminFichier) {
        // Créer le PDF comme avant
        Document document = null;
        try {
            document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            SimpleFooter footer = new SimpleFooter();
            writer.setPageEvent(footer);
            document.open();
            genererContenuTicket(document, vente);
            LOGGER.info("Ticket de vente généré avec succès: " + cheminFichier);

            // Imprimer directement via BillPrint
            BillPrintGenerator billPrinter = new BillPrintGenerator(vente);
            billPrinter.imprimer();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du ticket", e);
            throw new RuntimeException("Erreur lors de la génération du ticket", e);
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    private static void genererContenuTicket(Document document, Vente vente) throws DocumentException {
        // Configuration de la page pour le format ticket
        document.setMargins(20, 20, 20, 20);

        // En-tête
        Paragraph businessName = new Paragraph("BUSINESS NAME", new Font(Font.FontFamily.COURIER, 14, Font.BOLD));
        businessName.setAlignment(Element.ALIGN_CENTER);
        document.add(businessName);

        // Adresse
        Font normalFont = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL);
        String[] addressLines = {
            "1234 Main Street",
            "Suite 567",
            "City Name, State 54321",
            "123-456-7890"
        };

        for (String line : addressLines) {
            Paragraph address = new Paragraph(line, normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);
        }

        // Ligne de séparation
        document.add(new Paragraph("..........................................................", normalFont));

        // Informations de transaction
        document.add(new Paragraph(String.format("Merchant ID:      %s", "987-654321"), normalFont));
        document.add(new Paragraph(String.format("Terminal ID:      %s", "0123456789"), normalFont));
        document.add(new Paragraph("\n", normalFont));

        document.add(new Paragraph(String.format("Transaction ID:   #%s", vente.getId()), normalFont));
        document.add(new Paragraph(String.format("Type:            %s", vente.getModePaiement().getLibelle()), normalFont));

        // Date d'achat
        Paragraph purchaseTitle = new Paragraph("PURCHASE", normalFont);
        purchaseTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(purchaseTitle);

        Paragraph dateTime = new Paragraph(DATE_FORMATTER.format(vente.getDate()), normalFont);
        dateTime.setAlignment(Element.ALIGN_CENTER);
        document.add(dateTime);

        // Informations de carte
        document.add(new Paragraph("Number:          " + "************1234", normalFont));
        document.add(new Paragraph("Entry Mode:      Swiped", normalFont));
        document.add(new Paragraph("Card:            Card Name", normalFont));
        document.add(new Paragraph("Response:        APPROVED", normalFont));
        document.add(new Paragraph("Approval Code:   789-1234", normalFont));

        // Ligne de séparation
        document.add(new Paragraph("..........................................................", normalFont));

        // Détails des articles
        for (Vente.LigneVente ligne : vente.getLignes()) {
            String nomProduit = ligne.getProduit().getNom();
            if (nomProduit.length() > 20) {
                nomProduit = nomProduit.substring(0, 17) + "...";
            }

            // Format: Nom du produit (aligné à gauche) Prix (aligné à droite)
            String articleLine = String.format("%-30s$%8.2f",
                nomProduit,
                ligne.getPrixUnitaire() * ligne.getQuantite());
            document.add(new Paragraph(articleLine, normalFont));

            // Si quantité > 1, ajouter une ligne de détail
            if (ligne.getQuantite() > 1) {
                String detailLine = String.format("  %d x $%.2f", 
                    ligne.getQuantite(), 
                    ligne.getPrixUnitaire());
                document.add(new Paragraph(detailLine, normalFont));
            }
        }

        // Ligne de séparation
        document.add(new Paragraph("..........................................................", normalFont));

        // Totaux
        Font boldFont = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);

        // Sous-total
        String subTotalLine = String.format("Sub Total        $%8.2f", vente.getTotalHT());
        Paragraph subTotal = new Paragraph(subTotalLine, normalFont);
        subTotal.setAlignment(Element.ALIGN_RIGHT);
        document.add(subTotal);

        // TVA (Sales Tax)
        String taxLine = String.format("Sales Tax        $%8.2f", vente.getMontantTVA());
        Paragraph tax = new Paragraph(taxLine, normalFont);
        tax.setAlignment(Element.ALIGN_RIGHT);
        document.add(tax);

        // Ligne de séparation
        document.add(new Paragraph("..........................................................", normalFont));

        // Total
        String totalLine = String.format("TOTAL (USD)      $%8.2f", vente.getTotal());
        Paragraph total = new Paragraph(totalLine, boldFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.add(new Paragraph("\n", normalFont));

        // Message de remerciement
        Paragraph thankYou = new Paragraph("THANK YOU FOR", boldFont);
        thankYou.setAlignment(Element.ALIGN_CENTER);
        document.add(thankYou);

        Paragraph yourPurchase = new Paragraph("YOUR PURCHASE", boldFont);
        yourPurchase.setAlignment(Element.ALIGN_CENTER);
        document.add(yourPurchase);
    }

    public static void genererRapportCreances(List<Client> clients, String cheminFichier) {
        Document document = null;
        try {
            document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            SimpleFooter footer = new SimpleFooter();
            writer.setPageEvent(footer);
            document.open();

            Paragraph titre = new Paragraph("État des Créances", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {"Client", "Téléphone", "Solde", "Dernière Échéance"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Client client : clients) {
                if (client.getSolde() > 0) {
                    table.addCell(new Phrase(client.getNom(), NORMAL_FONT));
                    table.addCell(new Phrase(client.getTelephone(), NORMAL_FONT));
                    table.addCell(new Phrase(String.format("%.2f €", client.getSolde()), NORMAL_FONT));
                    table.addCell(new Phrase("-", NORMAL_FONT)); // TODO: Implémenter la date d'échéance
                }
            }

            document.add(table);
            LOGGER.info("Rapport des créances PDF généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, String cheminFichier) {
        Document document = null;
        try {
            document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            SimpleFooter footer = new SimpleFooter();
            writer.setPageEvent(footer);
            document.open();

            Paragraph titre = new Paragraph("Liste des Fournisseurs", TITLE_FONT);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes
            String[] headers = {"Nom", "Contact", "Téléphone", "Email"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, SUBTITLE_FONT));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Données
            for (Fournisseur fournisseur : fournisseurs) {
                table.addCell(new Phrase(fournisseur.getNom(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getContact(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getTelephone(), NORMAL_FONT));
                table.addCell(new Phrase(fournisseur.getEmail(), NORMAL_FONT));
            }

            document.add(table);
            LOGGER.info("Rapport des fournisseurs PDF généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport PDF des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }
}