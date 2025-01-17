package com.poissonnerie.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
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
import java.util.Map;
import java.util.stream.Collectors;

public class PDFGenerator {
    private static final float TICKET_WIDTH = 170.079f; // 6 cm en points
    private static final float MARGIN = 14.17f; // 5mm en points

    // Utility methods
    private static String truncateString(String str, int length) {
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    private static String centerText(String text, int width) {
        if (text == null || text.isEmpty()) return repeatChar(' ', width);
        int padding = (width - text.length()) / 2;
        return repeatChar(' ', padding) + text + repeatChar(' ', width - text.length() - padding);
    }

    private static String repeatChar(char c, int count) {
        return new String(new char[count]).replace('\0', c);
    }

    // Main methods for generating tickets and reports
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

            ConfigurationController configController = new ConfigurationController();
            configController.chargerConfigurations();

            // En-tête avec logo si configuré
            String logoPath = configController.getValeur(ConfigurationParam.CLE_LOGO_PATH);
            if (logoPath != null && !logoPath.isEmpty()) {
                try {
                    Image logo = Image.getInstance(logoPath);
                    float maxWidth = document.getPageSize().getWidth() - (2 * MARGIN);
                    float maxHeight = 50f; // Hauteur maximale du logo
                    if (logo.getWidth() > maxWidth || logo.getHeight() > maxHeight) {
                        logo.scaleToFit(maxWidth, maxHeight);
                    }
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                    document.add(new Paragraph(" ")); // Espace après le logo
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement du logo: " + e.getMessage());
                }
            }

            // Informations de l'entreprise
            addEntrepriseParagraph(document, configController, titleFont);

            // Informations de la vente
            addVenteInfos(document, vente, normalFont, configController);

            // Articles
            if (configController.getValeur(ConfigurationParam.CLE_FORMAT_RECU).equals("DETAILLE")) {
                addDetailedArticles(document, vente, normalFont, boldFont, configController);
            } else {
                addCompactArticles(document, vente, normalFont, boldFont, configController);
            }

            // Pied de page
            addFooter(document, configController, normalFont);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du ticket: " + e.getMessage());
        }
    }

    private static void addEntrepriseParagraph(Document document, ConfigurationController config, Font titleFont) throws DocumentException {
        Paragraph entrepriseInfo = new Paragraph();
        entrepriseInfo.setAlignment(Element.ALIGN_CENTER);

        String nomEntreprise = config.getValeur(ConfigurationParam.CLE_NOM_ENTREPRISE);
        if (!nomEntreprise.isEmpty()) {
            Chunk nomChunk = new Chunk(nomEntreprise.toUpperCase() + "\n", titleFont);
            entrepriseInfo.add(nomChunk);
        }

        String adresse = config.getValeur(ConfigurationParam.CLE_ADRESSE_ENTREPRISE);
        if (!adresse.isEmpty()) {
            entrepriseInfo.add(new Chunk(adresse + "\n", titleFont));
        }

        String telephone = config.getValeur(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE);
        if (!telephone.isEmpty()) {
            entrepriseInfo.add(new Chunk("Tél : " + telephone + "\n", titleFont));
        }

        String siret = config.getValeur(ConfigurationParam.CLE_SIRET_ENTREPRISE);
        if (!siret.isEmpty()) {
            entrepriseInfo.add(new Chunk("SIRET : " + siret + "\n", titleFont));
        }

        String enTete = config.getValeur(ConfigurationParam.CLE_EN_TETE_RECU);
        if (!enTete.isEmpty()) {
            entrepriseInfo.add(new Chunk(enTete + "\n", titleFont));
        }

        if (entrepriseInfo.size() > 0) {
            document.add(entrepriseInfo);
            document.add(new Paragraph("----------------------------------------", titleFont));
            document.add(new Paragraph(" "));
        }
    }

    private static void addVenteInfos(Document document, Vente vente, Font normalFont, ConfigurationController config) throws DocumentException {
        Paragraph venteInfo = new Paragraph();
        venteInfo.add(new Chunk("Ticket N°: " + vente.getId() + "\n", normalFont));
        venteInfo.add(new Chunk("Date: " + vente.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n", normalFont));

        if (vente.getClient() != null) {
            venteInfo.add(new Chunk("Client: " + vente.getClient().getNom() + "\n", normalFont));
            if (vente.getClient().getTelephone() != null && !vente.getClient().getTelephone().isEmpty()) {
                venteInfo.add(new Chunk("Tél: " + vente.getClient().getTelephone() + "\n", normalFont));
            }
        }

        document.add(venteInfo);
        document.add(new Paragraph("----------------------------------------", normalFont));
    }

    private static void addDetailedArticles(Document document, Vente vente, Font normalFont, Font boldFont, ConfigurationController config) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 6, 3, 3});

        // En-têtes
        Stream.of("Qté", "Article", "P.U.", "Total")
            .forEach(title -> table.addCell(new Phrase(title, boldFont)));

        double totalHT = 0;
        for (Vente.LigneVente ligne : vente.getLignes()) {
            table.addCell(new Phrase(String.valueOf(ligne.getQuantite()), normalFont));
            table.addCell(new Phrase(ligne.getProduit().getNom(), normalFont));
            table.addCell(new Phrase(String.format("%.2f€", ligne.getPrixUnitaire()), normalFont));
            double total = ligne.getQuantite() * ligne.getPrixUnitaire();
            table.addCell(new Phrase(String.format("%.2f€", total), normalFont));
            totalHT += total;
        }

        document.add(table);
        document.add(new Paragraph(" "));

        // Totaux avec TVA si activée
        addTotaux(document, totalHT, config, boldFont);
    }

    private static void addCompactArticles(Document document, Vente vente, Font normalFont, Font boldFont, ConfigurationController config) throws DocumentException {
        double totalHT = 0;
        for (Vente.LigneVente ligne : vente.getLignes()) {
            Paragraph ligneParagraph = new Paragraph();
            double total = ligne.getQuantite() * ligne.getPrixUnitaire();
            ligneParagraph.add(new Chunk(String.format("%3d %-20s %7.2f€\n",
                ligne.getQuantite(),
                truncateString(ligne.getProduit().getNom(), 20),
                total), normalFont));
            document.add(ligneParagraph);
            totalHT += total;
        }

        document.add(new Paragraph(" "));
        addTotaux(document, totalHT, config, boldFont);
    }

    private static void addTotaux(Document document, double totalHT, ConfigurationController config, Font boldFont) throws DocumentException {
        Paragraph totauxParagraph = new Paragraph();
        totauxParagraph.setAlignment(Element.ALIGN_RIGHT);

        boolean tvaEnabled = Boolean.parseBoolean(config.getValeur(ConfigurationParam.CLE_TVA_ENABLED));
        if (tvaEnabled) {
            double tauxTVA = Double.parseDouble(config.getValeur(ConfigurationParam.CLE_TAUX_TVA));
            double montantTVA = totalHT * (tauxTVA / 100);

            totauxParagraph.add(new Chunk(String.format("Total HT: %8.2f€\n", totalHT), boldFont));
            totauxParagraph.add(new Chunk(String.format("TVA %s%%: %8.2f€\n", tauxTVA, montantTVA), boldFont));
            totauxParagraph.add(new Chunk(String.format("Total TTC: %8.2f€\n", totalHT + montantTVA), boldFont));
        } else {
            totauxParagraph.add(new Chunk(String.format("Total: %8.2f€\n", totalHT), boldFont));
        }

        document.add(new Paragraph("----------------------------------------", boldFont));
        document.add(totauxParagraph);
    }

    private static void addFooter(Document document, ConfigurationController config, Font normalFont) throws DocumentException {
        document.add(new Paragraph("----------------------------------------", normalFont));

        String piedPage = config.getValeur(ConfigurationParam.CLE_PIED_PAGE_RECU);
        if (!piedPage.isEmpty()) {
            Paragraph footer = new Paragraph(piedPage, normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        }
    }

    public static String genererPreviewTicket(Vente vente) {
        StringBuilder preview = new StringBuilder();
        ConfigurationController configController = new ConfigurationController();
        configController.chargerConfigurations();

        // En-tête
        String nomEntreprise = configController.getValeur("NOM_ENTREPRISE");
        String adresse = configController.getValeur("ADRESSE_ENTREPRISE");
        String telephone = configController.getValeur("TELEPHONE_ENTREPRISE");
        String siret = configController.getValeur("SIRET_ENTREPRISE");
        String enTete = configController.getValeur("EN_TETE_RECU");

        // Configuration TVA
        boolean tvaEnabled = Boolean.parseBoolean(configController.getValeur("TVA_ENABLED"));
        String tauxTVA = configController.getValeur("TAUX_TVA");

        preview.append("\n");  // Espace en haut
        preview.append(centerText(nomEntreprise.toUpperCase(), 40)).append("\n");
        preview.append(centerText(adresse, 40)).append("\n");
        preview.append(centerText("Tél : " + telephone, 40)).append("\n");
        if (!siret.isEmpty()) {
            preview.append(centerText("SIRET : " + siret, 40)).append("\n");
        }
        if (!enTete.isEmpty()) {
            preview.append(centerText(enTete, 40)).append("\n");
        }
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

        if (tvaEnabled) {
            double tva = totalHT * (Double.parseDouble(tauxTVA) / 100);
            preview.append(String.format("TVA %s%%%32.2f€\n", tauxTVA, tva));
            preview.append(repeatChar('-', 40)).append("\n");
            preview.append(String.format("TOTAL TTC%30.2f€\n", totalHT + tva));
        } else {
            preview.append(String.format("TOTAL%34.2f€\n", totalHT));
        }

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
            Font redFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.RED);

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
            PdfPTable table = new PdfPTable(new float[]{3, 2, 2, 2, 2});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            // En-têtes
            Stream.of("Client", "Téléphone", "Solde", "Dernière vente", "Statut")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell(new Phrase(columnTitle, headerFont));
                    header.setBackgroundColor(new BaseColor(240, 240, 240));
                    header.setPadding(8);
                    header.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(header);
                });

            // Données
            double totalCreances = 0;
            int clientsAvecCreances = 0;
            double plusGrandeCreance = 0;
            String clientPlusGrandeCreance = "";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (Client client : clients) {
                if (client.getSolde() > 0) {
                    table.addCell(new Phrase(client.getNom(), normalFont));
                    table.addCell(new Phrase(client.getTelephone() != null ? client.getTelephone() : "-", normalFont));

                    // Formatage du solde avec couleur rouge si > 1000€
                    Phrase soldePhrase;
                    if (client.getSolde() > 1000) {
                        soldePhrase = new Phrase(String.format("%.2f €", client.getSolde()), redFont);
                    } else {
                        soldePhrase = new Phrase(String.format("%.2f €", client.getSolde()), normalFont);
                    }
                    PdfPCell soldeCell = new PdfPCell(soldePhrase);
                    soldeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    soldeCell.setPadding(5);
                    table.addCell(soldeCell);

                    // Dernière vente (à implémenter selon votre modèle de données)
                    table.addCell(new Phrase("À définir", normalFont));

                    // Statut basé sur le montant
                    String statut;
                    if (client.getSolde() > 1000) {
                        statut = "Critique";
                    } else if (client.getSolde() > 500) {
                        statut = "À suivre";
                    } else {
                        statut = "Normal";
                    }
                    table.addCell(new Phrase(statut, normalFont));

                    totalCreances += client.getSolde();
                    clientsAvecCreances++;

                    if (client.getSolde() > plusGrandeCreance) {
                        plusGrandeCreance = client.getSolde();
                        clientPlusGrandeCreance = client.getNom();
                    }
                }
            }

            document.add(table);

            // Résumé et statistiques
            Paragraph resume = new Paragraph("\nRésumé des créances:", headerFont);
            resume.setSpacingBefore(20);
            document.add(resume);

            document.add(new Paragraph(String.format("Nombre de clients avec créances: %d", clientsAvecCreances), normalFont));
            document.add(new Paragraph(String.format("Montant total des créances: %.2f €", totalCreances), normalFont));
            document.add(new Paragraph(String.format("Moyenne par client: %.2f €", totalCreances / clientsAvecCreances), normalFont));

            if (!clientPlusGrandeCreance.isEmpty()) {
                document.add(new Paragraph(String.format("Plus grande créance: %.2f € (%s)", 
                    plusGrandeCreance, clientPlusGrandeCreance), normalFont));
            }

            // Pied de page avec date et heure
            Paragraph footer = new Paragraph("\nDocument généré le " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

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
                table.addCell(mouvement.getType().toString());                table.addCell(String.format("%.2f €", mouvement.getMontant()));

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

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, String cheminFichier) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("Liste des Fournisseurs", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date du rapport
            Paragraph date = new Paragraph("Date du rapport: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont);
            date.setSpacingAfter(20);
            document.add(date);

            // Tableau des fournisseurs
            PdfPTable table = new PdfPTable(new float[]{3, 2, 3, 2});
            table.setWidthPercentage(100);

            // En-têtes
            Stream.of("Nom", "Téléphone", "Adresse", "Statut")
                .forEach(columnTitle -> {
                    Phrase phrase = new Phrase(columnTitle, headerFont);
                    table.addCell(phrase);
                });

            // Données
            for (Fournisseur fournisseur : fournisseurs) {
                table.addCell(fournisseur.getNom());
                table.addCell(fournisseur.getTelephone() != null ? fournisseur.getTelephone() : "");
                table.addCell(fournisseur.getAdresse() != null ? fournisseur.getAdresse() : "");
                table.addCell(fournisseur.getStatut());
            }

            document.add(table);

            // Résumé
            Paragraph resume = new Paragraph("\nRésumé:", headerFont);
            resume.setSpacingBefore(20);
            document.add(resume);
            document.add(new Paragraph(String.format("Nombre total de fournisseurs: %d", fournisseurs.size()),
                normalFont));

            Map<String, Long> statutCount = fournisseurs.stream()
                .collect(Collectors.groupingBy(
                    Fournisseur::getStatut,
                    Collectors.counting()
                ));

            statutCount.forEach((statut, count) -> {
                try {
                    document.add(new Paragraph(String.format("Fournisseurs %s: %d",
                        statut.toLowerCase(), count), normalFont));
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
            });

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du rapport des fournisseurs: " +
                e.getMessage());
        }
    }
}