package com.poissonnerie.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.BaseFont;
import com.poissonnerie.model.*;
import com.poissonnerie.controller.ConfigurationController;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public class PDFGenerator {
    private static final float TICKET_WIDTH = 170.079f; // 6 cm en points
    private static final float MARGIN = 14.17f; // 5mm en points

    public static String genererPreviewTicket(Vente vente) {
        StringBuilder preview = new StringBuilder();
        ConfigurationController configController = new ConfigurationController();
        configController.chargerConfigurations();

        // En-tête
        String nomEntreprise = configController.getValeur("NOM_ENTREPRISE");
        String adresse = configController.getValeur("ADRESSE_ENTREPRISE");
        String telephone = configController.getValeur("TELEPHONE_ENTREPRISE");
        String siret = configController.getValeur("SIRET_ENTREPRISE");
        String tauxTVA = configController.getValeur("TAUX_TVA");

        preview.append("\n");  // Espace en haut
        preview.append(centerText(nomEntreprise.toUpperCase(), 40)).append("\n");
        preview.append(centerText(adresse, 40)).append("\n");
        preview.append(centerText("Tél : " + telephone, 40)).append("\n");
        preview.append(centerText("SIRET : " + siret, 40)).append("\n");
        preview.append(repeatChar('=', 40)).append("\n\n");

        // Informations de la facture
        preview.append(String.format("Ticket N°: %d\n", vente.getId()));
        preview.append(String.format("Date: %s\n", 
            vente.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        if (vente.isCredit() && vente.getClient() != null) {
            preview.append(String.format("Client: %s\n", vente.getClient().getNom()));
            if (vente.getClient().getTelephone() != null && !vente.getClient().getTelephone().isEmpty()) {
                preview.append(String.format("Tél: %s\n", vente.getClient().getTelephone()));
            }
        }
        preview.append(repeatChar('-', 40)).append("\n");

        // Articles
        preview.append(String.format("%-3s %-20s %7s %7s\n", "Qté", "Article", "P.U.", "Total"));
        preview.append(repeatChar('-', 40)).append("\n");

        double totalHT = 0;
        int totalArticles = 0;

        for (Vente.LigneVente ligne : vente.getLignes()) {
            String nom = ligne.getProduit().getNom();
            if (nom.length() > 20) {
                nom = nom.substring(0, 17) + "...";
            }
            preview.append(String.format("%3d %-20s %7.2f %7.2f\n",
                ligne.getQuantite(),
                nom,
                ligne.getPrixUnitaire(),
                ligne.getQuantite() * ligne.getPrixUnitaire()));
            totalHT += ligne.getQuantite() * ligne.getPrixUnitaire();
            totalArticles += ligne.getQuantite();
        }

        // Totaux et TVA
        preview.append("\n").append(repeatChar('=', 40)).append("\n");
        preview.append(String.format("TOTAL HT%32.2f€\n", totalHT));
        double tva = totalHT * (Double.parseDouble(tauxTVA) / 100);
        preview.append(String.format("TVA %s%%%32.2f€\n", tauxTVA, tva));
        preview.append(repeatChar('-', 40)).append("\n");
        preview.append(String.format("TOTAL TTC%30.2f€\n", totalHT + tva));
        preview.append(repeatChar('=', 40)).append("\n");
        preview.append(String.format("Nombre d'articles: %d\n", totalArticles));

        // Mode de paiement
        String modeReglement = vente.isCredit() ? "*** VENTE À CRÉDIT ***" : "*** PAIEMENT COMPTANT ***";
        preview.append("\n").append(centerText(modeReglement, 40)).append("\n");
        if (vente.isCredit() && vente.getClient() != null) {
            preview.append(String.format("Solde après achat: %.2f€\n", 
                vente.getClient().getSolde() + vente.getTotal()));
        }

        // Pied de page
        preview.append(repeatChar('-', 40)).append("\n\n");
        String piedPage = configController.getValeur("PIED_PAGE_RECU");
        preview.append(centerText(piedPage, 40)).append("\n");
        preview.append(centerText("Merci de votre visite", 40)).append("\n");
        preview.append(centerText("* * *", 40)).append("\n\n");

        return preview.toString();
    }

    public static String genererPreviewReglementCreance(Client client, double montant, double nouveauSolde) {
        StringBuilder preview = new StringBuilder();
        ConfigurationController configController = new ConfigurationController();
        configController.chargerConfigurations();

        // En-tête
        String nomEntreprise = configController.getValeur("NOM_ENTREPRISE");
        String adresse = configController.getValeur("ADRESSE_ENTREPRISE");
        String telephone = configController.getValeur("TELEPHONE_ENTREPRISE");

        preview.append("\n");  // Espace en haut
        preview.append(centerText(nomEntreprise.toUpperCase(), 40)).append("\n");
        preview.append(centerText(adresse, 40)).append("\n");
        preview.append(centerText(telephone, 40)).append("\n\n");
        preview.append(repeatChar('=', 40)).append("\n");

        // Informations du reçu
        preview.append(centerText("REÇU DE RÈGLEMENT", 40)).append("\n");
        preview.append(String.format("Date: %s\n", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        preview.append(repeatChar('-', 40)).append("\n");

        // Informations client
        preview.append(String.format("Client: %s\n", client.getNom()));
        if (client.getTelephone() != null && !client.getTelephone().isEmpty()) {
            preview.append(String.format("Tél: %s\n", client.getTelephone()));
        }
        preview.append(repeatChar('-', 40)).append("\n\n");

        // Détails du règlement
        preview.append("DÉTAILS DU RÈGLEMENT\n");
        preview.append(repeatChar('-', 40)).append("\n");
        preview.append(String.format("Ancien solde:%28.2f€\n", client.getSolde()));
        preview.append(String.format("Montant réglé:%27.2f€\n", montant));
        preview.append(repeatChar('-', 40)).append("\n");
        preview.append(String.format("Nouveau solde:%27.2f€\n", nouveauSolde));
        preview.append(repeatChar('=', 40)).append("\n\n");

        // Signatures
        preview.append("Signature client:\n\n\n");
        preview.append("Signature vendeur:\n\n\n");
        preview.append(repeatChar('-', 40)).append("\n\n");

        // Pied de page
        String piedPage = configController.getValeur("PIED_PAGE_RECU");
        preview.append(centerText(piedPage, 40)).append("\n");
        preview.append(centerText("* * *", 40)).append("\n");
        preview.append("\n");  // Espace final

        return preview.toString();
    }

    public static void genererReglementCreance(Client client, double montant, double nouveauSolde, String cheminFichier) {
        try {
            Document document = new Document(new Rectangle(TICKET_WIDTH, PageSize.A4.getHeight()));
            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // Police monospace pour meilleur alignement
            BaseFont baseFont = BaseFont.createFont(BaseFont.COURIER, BaseFont.CP1252, BaseFont.EMBEDDED);
            Font normalFont = new Font(baseFont, 8);
            Font boldFont = new Font(baseFont, 8, Font.BOLD);
            Font titleFont = new Font(baseFont, 10, Font.BOLD);

            // Convertir le preview en PDF
            String preview = genererPreviewReglementCreance(client, montant, nouveauSolde);
            for (String line : preview.split("\n")) {
                Font currentFont = normalFont;
                if (line.matches(".*TOTAL.*|.*DÉTAILS DU RÈGLEMENT.*") || line.startsWith("===")) {
                    currentFont = boldFont;
                } else if (line.equals(line.toUpperCase()) && !line.startsWith("---")) {
                    currentFont = titleFont;
                }

                Paragraph p = new Paragraph(line, currentFont);
                p.setAlignment(Element.ALIGN_LEFT);
                if (line.matches(".*Nouveau solde:.*")) {
                    p.setSpacingBefore(5);
                    p.setSpacingAfter(5);
                }
                document.add(p);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du reçu: " + e.getMessage());
        }
    }

    public static void genererTicket(Vente vente, String cheminFichier) {
        try {
            Document document = new Document(new Rectangle(TICKET_WIDTH, PageSize.A4.getHeight()));
            document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // Police monospace pour meilleur alignement
            BaseFont baseFont = BaseFont.createFont(BaseFont.COURIER, BaseFont.CP1252, BaseFont.EMBEDDED);
            Font normalFont = new Font(baseFont, 8);
            Font boldFont = new Font(baseFont, 8, Font.BOLD);
            Font titleFont = new Font(baseFont, 10, Font.BOLD);

            // Convertir le preview en PDF
            String preview = genererPreviewTicket(vente);
            for (String line : preview.split("\n")) {
                Font currentFont = normalFont;
                if (line.matches(".*TOTAL TTC.*|.*TOTAL HT.*|.*TVA.*") || line.startsWith("===")) {
                    currentFont = boldFont;
                } else if (line.equals(line.toUpperCase()) && !line.startsWith("---")) {
                    currentFont = titleFont;
                }

                Paragraph p = new Paragraph(line, currentFont);
                p.setAlignment(Element.ALIGN_LEFT);
                if (line.matches(".*TOTAL TTC.*")) {
                    p.setSpacingBefore(5);
                    p.setSpacingAfter(5);
                }
                document.add(p);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du ticket: " + e.getMessage());
        }
    }

    public static void genererRapportVentes(List<Vente> ventes, String cheminFichier) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("Rapport des Ventes", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date du rapport
            Paragraph date = new Paragraph("Date du rapport: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont);
            date.setSpacingAfter(20);
            document.add(date);

            // Tableau des ventes
            PdfPTable table = new PdfPTable(new float[]{2, 3, 2, 2, 2});
            table.setWidthPercentage(100);

            // En-têtes
            Stream.of("Date", "Client", "Type", "Montant", "Statut")
                .forEach(columnTitle -> {
                    Phrase phrase = new Phrase(columnTitle, headerFont);
                    table.addCell(phrase);
                });

            // Données
            double totalVentes = 0;
            int nombreVentes = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Vente vente : ventes) {
                table.addCell(vente.getDate().format(formatter));
                table.addCell(vente.getClient() != null ? vente.getClient().getNom() : "Vente comptant");
                table.addCell(vente.isCredit() ? "Crédit" : "Comptant");
                table.addCell(String.format("%.2f €", vente.getTotal()));
                table.addCell(vente.isCredit() ? "À payer" : "Payée");

                totalVentes += vente.getTotal();
                nombreVentes++;
            }

            document.add(table);

            // Résumé
            Paragraph resume = new Paragraph("\nRésumé des ventes:", headerFont);
            resume.setSpacingBefore(20);
            document.add(resume);

            document.add(new Paragraph(String.format("Nombre total de ventes: %d", nombreVentes), normalFont));
            document.add(new Paragraph(String.format("Montant total des ventes: %.2f €", totalVentes), normalFont));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du rapport des ventes: " + e.getMessage());
        }
    }

    public static void genererRapportStocks(List<Produit> produits, String cheminFichier) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("État des Stocks", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date du rapport
            Paragraph date = new Paragraph("Date du rapport: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont);
            date.setSpacingAfter(20);
            document.add(date);

            // Tableau des stocks
            PdfPTable table = new PdfPTable(new float[]{3, 2, 2, 2, 2, 2});
            table.setWidthPercentage(100);

            // En-têtes
            Stream.of("Produit", "Catégorie", "Stock", "Seuil", "Prix Achat", "Prix Vente")
                .forEach(columnTitle -> {
                    Phrase phrase = new Phrase(columnTitle, headerFont);
                    table.addCell(phrase);
                });

            // Données
            int totalProduits = 0;
            int produitsEnAlerte = 0;

            for (Produit produit : produits) {
                table.addCell(produit.getNom());
                table.addCell(produit.getCategorie());
                table.addCell(String.valueOf(produit.getStock()));
                table.addCell(String.valueOf(produit.getSeuilAlerte()));
                table.addCell(String.format("%.2f €", produit.getPrixAchat()));
                table.addCell(String.format("%.2f €", produit.getPrixVente()));

                totalProduits++;
                if (produit.getStock() <= produit.getSeuilAlerte()) {
                    produitsEnAlerte++;
                }
            }

            document.add(table);

            // Résumé
            Paragraph resume = new Paragraph("\nRésumé des stocks:", headerFont);
            resume.setSpacingBefore(20);
            document.add(resume);

            document.add(new Paragraph(String.format("Nombre total de produits: %d", totalProduits), normalFont));
            document.add(new Paragraph(String.format("Produits en alerte de stock: %d", produitsEnAlerte), normalFont));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du rapport des stocks: " + e.getMessage());
        }
    }

    public static void genererRapportCreances(List<Client> clients, String cheminFichier) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("État des Créances", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date du rapport
            Paragraph date = new Paragraph("Date du rapport: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont);
            date.setSpacingAfter(20);
            document.add(date);

            // Tableau des créances
            PdfPTable table = new PdfPTable(new float[]{3, 2, 2, 2});
            table.setWidthPercentage(100);

            // En-têtes
            Stream.of("Client", "Téléphone", "Solde", "Statut")
                .forEach(columnTitle -> {
                    Phrase phrase = new Phrase(columnTitle, headerFont);
                    table.addCell(phrase);
                });

            // Données
            double totalCreances = 0;
            int clientsAvecCreances = 0;

            for (Client client : clients) {
                if (client.getSolde() > 0) {
                    table.addCell(client.getNom());
                    table.addCell(client.getTelephone() != null ? client.getTelephone() : "");
                    table.addCell(String.format("%.2f €", client.getSolde()));
                    table.addCell("À régler");

                    totalCreances += client.getSolde();
                    clientsAvecCreances++;
                }
            }

            document.add(table);

            // Résumé
            Paragraph resume = new Paragraph("\nRésumé des créances:", headerFont);
            resume.setSpacingBefore(20);
            document.add(resume);

            document.add(new Paragraph(String.format("Nombre de clients avec créances: %d", clientsAvecCreances), normalFont));
            document.add(new Paragraph(String.format("Montant total des créances: %.2f €", totalCreances), normalFont));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du rapport des créances: " + e.getMessage());
        }
    }

    public static void genererRapportCaisse(List<MouvementCaisse> mouvements, String cheminFichier) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("Journal de Caisse", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date du rapport
            Paragraph date = new Paragraph("Date du rapport: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont);
            date.setSpacingAfter(20);
            document.add(date);

            // Tableau des mouvements
            PdfPTable table = new PdfPTable(new float[]{2, 3, 2, 2});
            table.setWidthPercentage(100);

            // En-têtes
            Stream.of("Date", "Description", "Type", "Montant")
                .forEach(columnTitle -> {
                    Phrase phrase = new Phrase(columnTitle, headerFont);
                    table.addCell(phrase);
                });

            // Données
            double totalEntrees = 0;
            double totalSorties = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (MouvementCaisse mouvement : mouvements) {
                table.addCell(mouvement.getDate().format(formatter));
                table.addCell(mouvement.getDescription());
                table.addCell(mouvement.getType().toString());
                table.addCell(String.format("%.2f €", mouvement.getMontant()));

                if (mouvement.getType() == MouvementCaisse.TypeMouvement.ENTREE) {
                    totalEntrees += mouvement.getMontant();
                } else {
                    totalSorties += mouvement.getMontant();
                }
            }

            document.add(table);

            // Résumé
            Paragraph resume = new Paragraph("\nRésumé des mouvements:", headerFont);
            resume.setSpacingBefore(20);
            document.add(resume);

            document.add(new Paragraph(String.format("Total des entrées: %.2f €", totalEntrees), normalFont));
            document.add(new Paragraph(String.format("Total des sorties: %.2f €", totalSorties), normalFont));
            document.add(new Paragraph(String.format("Solde: %.2f €", totalEntrees - totalSorties), normalFont));

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du journal de caisse: " + e.getMessage());
        }
    }

    private static String centerText(String text, int width) {
        if (text == null || text.isEmpty()) return repeatChar(' ', width);
        int padding = (width - text.length()) / 2;
        if (padding <= 0) return text;
        return String.format("%" + padding + "s%s%" + padding + "s", "", text, "");
    }

    private static String repeatChar(char c, int count) {
        return new String(new char[count]).replace('\0', c);
    }
}