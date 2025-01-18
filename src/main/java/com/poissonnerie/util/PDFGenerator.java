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
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;

public class PDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(PDFGenerator.class.getName());
    private static final float TICKET_WIDTH = 170.079f; // 6 cm en points
    private static final float MARGIN = 14.17f; // 5mm en points
    private static final int MAX_FILE_SIZE_MB = 10;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
    private static final Set<String> FORMATS_IMAGE_AUTORISES = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
    private static final int MAX_IMAGE_SIZE = 1000; // pixels

    // Utility methods
    private static String truncateString(String str, int length) {
        if (str == null) return "";
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    private static String centerText(String text, int width) {
        if (text == null || text.isEmpty()) return repeatChar(' ', width);
        int padding = (width - text.length()) / 2;
        return repeatChar(' ', padding) + text + repeatChar(' ', width - text.length() - padding);
    }

    private static String repeatChar(char c, int count) {
        if (count < 0) return "";
        return new String(new char[count]).replace('\0', c);
    }

    private static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>\"'%;)(&+]", "")
                   .trim()
                   .replaceAll("\\s+", " ");
    }

    private static void validateFilePath(String cheminFichier) {
        if (cheminFichier == null || cheminFichier.trim().isEmpty()) {
            throw new IllegalArgumentException("Le chemin du fichier ne peut pas être vide");
        }

        try {
            Path path = Paths.get(cheminFichier).normalize();
            File file = path.toFile();
            File parentDir = file.getParentFile();

            // Vérification du chemin absolu
            if (!path.isAbsolute()) {
                path = path.toAbsolutePath();
            }

            // Vérification que le chemin est dans le répertoire de l'application
            Path baseDir = Paths.get(System.getProperty("user.dir")).normalize();
            if (!path.startsWith(baseDir)) {
                throw new SecurityException("Accès non autorisé en dehors du répertoire de l'application");
            }

            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new SecurityException("Impossible de créer le répertoire pour le fichier PDF");
            }

            if (file.exists()) {
                if (!file.canWrite()) {
                    throw new SecurityException("Permissions insuffisantes pour écrire le fichier PDF");
                }
                checkFileSize(file.getAbsolutePath());
            }
        } catch (SecurityException e) {
            LOGGER.log(Level.SEVERE, "Erreur de sécurité lors de la validation du chemin", e);
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la validation du chemin", e);
            throw new IllegalArgumentException("Chemin de fichier invalide: " + e.getMessage());
        }
    }

    private static void validateImage(String logoPath) {
        try {
            if (logoPath == null || logoPath.trim().isEmpty()) {
                return;
            }

            Path path = Paths.get(logoPath).normalize();
            if (!path.isAbsolute()) {
                path = path.toAbsolutePath();
            }

            // Vérification du chemin
            Path baseDir = Paths.get(System.getProperty("user.dir")).normalize();
            if (!path.startsWith(baseDir)) {
                throw new SecurityException("Accès non autorisé en dehors du répertoire de l'application");
            }

            File file = path.toFile();
            if (!file.exists() || !file.isFile()) {
                throw new IllegalArgumentException("Le fichier image n'existe pas");
            }

            // Vérification de la taille du fichier
            if (file.length() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("La taille du fichier image dépasse la limite de " + MAX_FILE_SIZE_MB + "MB");
            }

            // Vérification du format
            String extension = getFileExtension(file.getName()).toLowerCase();
            if (!FORMATS_IMAGE_AUTORISES.contains(extension)) {
                throw new IllegalArgumentException("Format d'image non autorisé. Formats acceptés : " + 
                    String.join(", ", FORMATS_IMAGE_AUTORISES));
            }

            // Vérification du contenu
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IllegalArgumentException("Le fichier n'est pas une image valide");
            }

            // Vérification des dimensions
            if (image.getWidth() > MAX_IMAGE_SIZE || image.getHeight() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("Les dimensions de l'image dépassent la limite de " + 
                    MAX_IMAGE_SIZE + "x" + MAX_IMAGE_SIZE + " pixels");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la validation de l'image", e);
            throw new IllegalArgumentException("Image invalide: " + e.getMessage());
        }
    }

    private static String getFileExtension(String filename) {
        return Optional.ofNullable(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(filename.lastIndexOf(".") + 1))
            .orElse("");
    }

    private static void checkFileSize(String cheminFichier) {
        File file = new File(cheminFichier);
        if (file.exists() && file.length() > MAX_FILE_SIZE_BYTES) {
            throw new SecurityException("La taille du fichier PDF dépasse la limite autorisée de " + MAX_FILE_SIZE_MB + "MB");
        }
    }

    // Main methods for generating tickets and reports
    public static void genererTicket(Vente vente, String cheminFichier) {
        LOGGER.info("Début de la génération du ticket pour la vente " + vente.getId());

        if (vente == null) {
            throw new IllegalArgumentException("La vente ne peut pas être null");
        }
        validateFilePath(cheminFichier);

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
            validateImage(logoPath); // Validate the logo image
            if (logoPath != null && !logoPath.isEmpty()) {
                try {
                    Image logo = Image.getInstance(logoPath);
                    float maxWidth = document.getPageSize().getWidth() - (2 * MARGIN);
                    float maxHeight = 50f;
                    if (logo.getWidth() > maxWidth || logo.getHeight() > maxHeight) {
                        logo.scaleToFit(maxWidth, maxHeight);
                    }
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                    document.add(new Paragraph(" "));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Erreur lors du chargement du logo: " + e.getMessage());
                }
            }

            addEntrepriseParagraph(document, configController, titleFont);
            addVenteInfos(document, vente, normalFont, configController);

            if (configController.getValeur(ConfigurationParam.CLE_FORMAT_RECU).equals("DETAILLE")) {
                addDetailedArticles(document, vente, normalFont, boldFont, configController);
            } else {
                addCompactArticles(document, vente, normalFont, boldFont, configController);
            }

            addFooter(document, configController, normalFont);

            document.close();
            checkFileSize(cheminFichier);
            LOGGER.info("Ticket généré avec succès: " + cheminFichier);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du ticket", e);
            throw new RuntimeException("Erreur lors de la génération du ticket: " + e.getMessage());
        }
    }

    private static void addEntrepriseParagraph(Document document, ConfigurationController config, Font titleFont) throws DocumentException {
        try {
            Paragraph entrepriseInfo = new Paragraph();
            entrepriseInfo.setAlignment(Element.ALIGN_CENTER);

            String nomEntreprise = sanitizeInput(config.getValeur(ConfigurationParam.CLE_NOM_ENTREPRISE));
            if (!nomEntreprise.isEmpty()) {
                Chunk nomChunk = new Chunk(nomEntreprise.toUpperCase() + "\n", titleFont);
                entrepriseInfo.add(nomChunk);
            }

            String adresse = sanitizeInput(config.getValeur(ConfigurationParam.CLE_ADRESSE_ENTREPRISE));
            if (!adresse.isEmpty()) {
                entrepriseInfo.add(new Chunk(adresse + "\n", titleFont));
            }

            String telephone = sanitizeInput(config.getValeur(ConfigurationParam.CLE_TELEPHONE_ENTREPRISE));
            if (!telephone.isEmpty()) {
                entrepriseInfo.add(new Chunk("Tél : " + telephone + "\n", titleFont));
            }

            String siret = sanitizeInput(config.getValeur(ConfigurationParam.CLE_SIRET_ENTREPRISE));
            if (!siret.isEmpty()) {
                entrepriseInfo.add(new Chunk("SIRET : " + siret + "\n", titleFont));
            }

            String enTete = sanitizeInput(config.getValeur(ConfigurationParam.CLE_EN_TETE_RECU));
            if (!enTete.isEmpty()) {
                entrepriseInfo.add(new Chunk(enTete + "\n", titleFont));
            }

            if (entrepriseInfo.size() > 0) {
                document.add(entrepriseInfo);
                document.add(new Paragraph("----------------------------------------", titleFont));
                document.add(new Paragraph(" "));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout des informations de l'entreprise", e);
            throw e;
        }
    }

    private static void addVenteInfos(Document document, Vente vente, Font normalFont, ConfigurationController config) throws DocumentException {
        try {
            Paragraph venteInfo = new Paragraph();
            venteInfo.add(new Chunk("Ticket N°: " + vente.getId() + "\n", normalFont));
            venteInfo.add(new Chunk("Date: " + vente.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n", normalFont));

            if (vente.getClient() != null) {
                venteInfo.add(new Chunk("Client: " + sanitizeInput(vente.getClient().getNom()) + "\n", normalFont));
                if (vente.getClient().getTelephone() != null && !vente.getClient().getTelephone().isEmpty()) {
                    venteInfo.add(new Chunk("Tél: " + sanitizeInput(vente.getClient().getTelephone()) + "\n", normalFont));
                }
            }

            document.add(venteInfo);
            document.add(new Paragraph("----------------------------------------", normalFont));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout des informations de vente", e);
            throw e;
        }
    }

    private static void addDetailedArticles(Document document, Vente vente, Font normalFont, Font boldFont, ConfigurationController config) throws DocumentException {
        try {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 6, 3, 3});

            Stream.of("Qté", "Article", "P.U.", "Total")
                .forEach(title -> {
                    PdfPCell cell = new PdfPCell(new Phrase(sanitizeInput(title), boldFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                });

            double totalHT = 0;
            for (Vente.LigneVente ligne : vente.getLignes()) {
                if (ligne == null || ligne.getProduit() == null) {
                    LOGGER.warning("Ligne de vente ou produit null détecté, ignoré");
                    continue;
                }

                table.addCell(new Phrase(String.valueOf(ligne.getQuantite()), normalFont));
                table.addCell(new Phrase(sanitizeInput(ligne.getProduit().getNom()), normalFont));
                table.addCell(new Phrase(String.format("%.2f€", ligne.getPrixUnitaire()), normalFont));
                double total = ligne.getQuantite() * ligne.getPrixUnitaire();
                table.addCell(new Phrase(String.format("%.2f€", total), normalFont));
                totalHT += total;
            }

            document.add(table);
            document.add(new Paragraph(" "));
            addTotaux(document, totalHT, config, boldFont);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout des articles détaillés", e);
            throw e;
        }
    }

    private static void addCompactArticles(Document document, Vente vente, Font normalFont, Font boldFont, ConfigurationController config) throws DocumentException {
        try {
            double totalHT = 0;
            for (Vente.LigneVente ligne : vente.getLignes()) {
                if (ligne == null || ligne.getProduit() == null) {
                    LOGGER.warning("Ligne de vente ou produit null détecté, ignoré");
                    continue;
                }

                Paragraph ligneParagraph = new Paragraph();
                double total = ligne.getQuantite() * ligne.getPrixUnitaire();
                String nom = sanitizeInput(ligne.getProduit().getNom());
                ligneParagraph.add(new Chunk(String.format("%3d %-20s %7.2f€\n",
                    ligne.getQuantite(),
                    truncateString(nom, 20),
                    total), normalFont));
                document.add(ligneParagraph);
                totalHT += total;
            }

            document.add(new Paragraph(" "));
            addTotaux(document, totalHT, config, boldFont);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout des articles en format compact", e);
            throw e;
        }
    }

    private static void addTotaux(Document document, double totalHT, ConfigurationController config, Font boldFont) throws DocumentException {
        try {
            Paragraph totauxParagraph = new Paragraph();
            totauxParagraph.setAlignment(Element.ALIGN_RIGHT);

            boolean tvaEnabled = Boolean.parseBoolean(config.getValeur(ConfigurationParam.CLE_TVA_ENABLED));
            if (tvaEnabled) {
                double tauxTVA = Double.parseDouble(config.getValeur(ConfigurationParam.CLE_TAUX_TVA));
                if (tauxTVA < 0 || tauxTVA > 100) {
                    LOGGER.warning("Taux de TVA invalide détecté: " + tauxTVA);
                    tauxTVA = 20.0; // Valeur par défaut
                }

                double montantTVA = totalHT * (tauxTVA / 100);

                totauxParagraph.add(new Chunk(String.format("Total HT: %8.2f€\n", totalHT), boldFont));
                totauxParagraph.add(new Chunk(String.format("TVA %s%%: %8.2f€\n", tauxTVA, montantTVA), boldFont));
                totauxParagraph.add(new Chunk(String.format("Total TTC: %8.2f€\n", totalHT + montantTVA), boldFont));
            } else {
                totauxParagraph.add(new Chunk(String.format("Total: %8.2f€\n", totalHT), boldFont));
            }

            document.add(new Paragraph("----------------------------------------", boldFont));
            document.add(totauxParagraph);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout des totaux", e);
            throw e;
        }
    }

    private static void addFooter(Document document, ConfigurationController config, Font normalFont) throws DocumentException {
        try {
            document.add(new Paragraph("----------------------------------------", normalFont));

            String piedPage = sanitizeInput(config.getValeur(ConfigurationParam.CLE_PIED_PAGE_RECU));
            if (!piedPage.isEmpty()) {
                Paragraph footer = new Paragraph(piedPage, normalFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                document.add(footer);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du pied de page", e);
            throw e;
        }
    }

    public static String genererPreviewTicket(Vente vente) {
        LOGGER.info("Début de la génération de l'aperçu du ticket");
        StringBuilder preview = new StringBuilder();
        ConfigurationController configController = new ConfigurationController();
        configController.chargerConfigurations();

        // En-tête
        String nomEntreprise = sanitizeInput(configController.getValeur("NOM_ENTREPRISE"));
        String adresse = sanitizeInput(configController.getValeur("ADRESSE_ENTREPRISE"));
        String telephone = sanitizeInput(configController.getValeur("TELEPHONE_ENTREPRISE"));
        String siret = sanitizeInput(configController.getValeur("SIRET_ENTREPRISE"));
        String enTete = sanitizeInput(configController.getValeur("EN_TETE_RECU"));

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
            preview.append(String.format("Client: %s\n", sanitizeInput(vente.getClient().getNom())));
            if (vente.getClient().getTelephone() != null && !vente.getClient().getTelephone().isEmpty()) {
                preview.append(String.format("Tél: %s\n", sanitizeInput(vente.getClient().getTelephone())));
            }
        }
        preview.append(repeatChar('-', 40)).append("\n");

        // Articles
        preview.append(String.format("%-3s %-20s %7s %7s\n", "Qté", "Article", "P.U.", "Total"));
        preview.append(repeatChar('-', 40)).append("\n");

        double totalHT = 0;
        int totalArticles = 0;

        for (Vente.LigneVente ligne : vente.getLignes()) {
            String nom = sanitizeInput(ligne.getProduit().getNom());
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
        String piedPage = sanitizeInput(configController.getValeur("PIED_PAGE_RECU"));
        preview.append(centerText(piedPage, 40)).append("\n");
        preview.append(centerText("Merci de votre visite", 40)).append("\n");
        preview.append(centerText("* * *", 40)).append("\n\n");
        LOGGER.info("Aperçu du ticket généré avec succès");
        return preview.toString();
    }

    public static String genererPreviewReglementCreance(Client client, double montant, double nouveauSolde) {
        LOGGER.info("Début de la génération de l'aperçu du règlement de créance");
        StringBuilder preview = new StringBuilder();
        ConfigurationController configController = new ConfigurationController();
        configController.chargerConfigurations();

        // En-tête
        String nomEntreprise = sanitizeInput(configController.getValeur("NOM_ENTREPRISE"));
        String adresse = sanitizeInput(configController.getValeur("ADRESSE_ENTREPRISE"));
        String telephone = sanitizeInput(configController.getValeur("TELEPHONE_ENTREPRISE"));

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
        preview.append(String.format("Client: %s\n", sanitizeInput(client.getNom())));
        if (client.getTelephone() != null && !client.getTelephone().isEmpty()) {
            preview.append(String.format("Tél: %s\n", sanitizeInput(client.getTelephone())));
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
        String piedPage = sanitizeInput(configController.getValeur("PIED_PAGE_RECU"));
        preview.append(centerText(piedPage, 40)).append("\n");
        preview.append(centerText("* * *", 40)).append("\n");
        preview.append("\n");  // Espace final
        LOGGER.info("Aperçu du règlement de créance généré avec succès");
        return preview.toString();
    }

    public static void genererRapportVentes(List<Vente> ventes, String cheminFichier) {
        LOGGER.info("Début de la génération du rapport des ventes");
        validateFilePath(cheminFichier);
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
                table.addCell(vente.getClient() != null ? sanitizeInput(vente.getClient().getNom()) : "Vente comptant");
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
            checkFileSize(cheminFichier);
            LOGGER.info("Rapport des ventes généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des ventes: " + e.getMessage());
        }
    }

    public static void genererReglementCreance(Client client, double montant, double nouveauSolde, String cheminFichier) {
        LOGGER.info("Début de la génération du reçu de règlement de créance");
        validateFilePath(cheminFichier);
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
            checkFileSize(cheminFichier);
            LOGGER.info("Reçu de règlement de créance généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du reçu de règlement", e);
            throw new RuntimeException("Erreur lors de la génération du reçu: " + e.getMessage());
        }
    }
    public static void genererRapportStocks(List<Produit> produits, String cheminFichier) {
        LOGGER.info("Début de la génération du rapport des stocks");
        validateFilePath(cheminFichier);
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
                table.addCell(sanitizeInput(produit.getNom()));
                table.addCell(sanitizeInput(produit.getCategorie()));
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
            checkFileSize(cheminFichier);
            LOGGER.info("Rapport des stocks généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des stocks: " + e.getMessage());
        }
    }

    public static void genererRapportCreances(List<Client> clients, String cheminFichier) {
        LOGGER.info("Début de la génération du rapport des créances");
        validateFilePath(cheminFichier);
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
                    table.addCell(new Phrase(sanitizeInput(client.getNom()), normalFont));
                    table.addCell(new Phrase(client.getTelephone() != null ? sanitizeInput(client.getTelephone()) : "-", normalFont));

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
            checkFileSize(cheminFichier);
            LOGGER.info("Rapport des créances généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des créances: " + e.getMessage());
        }
    }

    public static void genererRapportCaisse(List<MouvementCaisse> mouvements, String cheminFichier) {
        LOGGER.info("Début de la génération du journal de caisse");
        validateFilePath(cheminFichier);
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
                table.addCell(sanitizeInput(mouvement.getDescription()));
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
            checkFileSize(cheminFichier);
            LOGGER.info("Journal de caisse généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du journal de caisse", e);
            throw new RuntimeException("Erreur lors de la génération du journal de caisse: " + e.getMessage());
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, String cheminFichier) {
        LOGGER.info("Début de la génération du rapport des fournisseurs");
        validateFilePath(cheminFichier);
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
                table.addCell(sanitizeInput(fournisseur.getNom()));
                table.addCell(fournisseur.getTelephone() != null ? sanitizeInput(fournisseur.getTelephone()) : "");
                table.addCell(fournisseur.getAdresse() != null ? sanitizeInput(fournisseur.getAdresse()) : "");
                table.addCell(sanitizeInput(fournisseur.getStatut()));
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
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du résumé des statuts des fournisseurs", e);
                }
            });

            document.close();
            checkFileSize(cheminFichier);
            LOGGER.info("Rapport des fournisseurs généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des fournisseurs: " +
                e.getMessage());
        }
    }
}