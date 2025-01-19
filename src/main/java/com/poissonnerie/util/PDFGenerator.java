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
import java.security.MessageDigest;
import java.util.UUID;
import java.io.IOException;
import java.io.ByteArrayOutputStream;


public class PDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(PDFGenerator.class.getName());
    private static final float TICKET_WIDTH = 170.079f; // 6 cm en points
    private static final float MARGIN = 14.17f; // 5mm en points
    private static final int MAX_FILE_SIZE_MB = 10;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
    private static final Set<String> FORMATS_IMAGE_AUTORISES = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
    private static final int MAX_IMAGE_SIZE = 1000; // pixels
    private static final String OUTPUT_DIR = "generated_pdfs";
    private static final int MAX_FILENAME_LENGTH = 255;
    
    static {
        // Création du répertoire de sortie s'il n'existe pas
        try {
            Path outputPath = Paths.get(OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du répertoire de sortie", e);
        }
    }

    // Utility methods avec améliorations de sécurité
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
        return input.replaceAll("[<>\"'%;)(&+\\[\\]{}]", "")
                   .replaceAll("\\\\", "/")
                   .replaceAll("\\.\\.", "")
                   .trim()
                   .replaceAll("\\s+", " ");
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de fichier ne peut pas être vide");
        }

        // Remplacer les caractères non autorisés
        String sanitized = filename.replaceAll("[^a-zA-Z0-9.-]", "_")
                                 .replaceAll("\\.\\.", "_")
                                 .replaceAll("\\s+", "_");

        // Tronquer si trop long
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            String extension = getFileExtension(sanitized);
            int baseLength = MAX_FILENAME_LENGTH - extension.length() - 1;
            sanitized = sanitized.substring(0, baseLength) + "." + extension;
        }

        return sanitized;
    }

    private static void validateFilePath(String cheminFichier) {
        if (cheminFichier == null || cheminFichier.trim().isEmpty()) {
            throw new IllegalArgumentException("Le chemin du fichier ne peut pas être vide");
        }

        try {
            // Générer un nom de fichier unique basé sur UUID
            String originalName = new File(cheminFichier).getName();
            String sanitizedName = sanitizeFilename(UUID.randomUUID().toString() + "_" + originalName);
            Path path = Paths.get(OUTPUT_DIR, sanitizedName).normalize();
            
            // Vérification que le chemin est dans le répertoire de sortie autorisé
            Path baseDir = Paths.get(OUTPUT_DIR).normalize();
            if (!path.startsWith(baseDir)) {
                throw new SecurityException("Accès non autorisé en dehors du répertoire de sortie");
            }

            // Création du répertoire parent si nécessaire
            Files.createDirectories(path.getParent());

            // Vérification des permissions
            if (Files.exists(path) && !Files.isWritable(path)) {
                throw new SecurityException("Permissions insuffisantes pour écrire le fichier PDF");
            }

            // Vérification de la taille si le fichier existe déjà
            if (Files.exists(path)) {
                checkFileSize(path.toString());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la validation du chemin", e);
            throw new IllegalArgumentException("Erreur lors de la création du chemin de fichier: " + e.getMessage());
        }
    }

    private static void validateImage(String logoPath) {
        if (logoPath == null || logoPath.trim().isEmpty()) {
            return;
        }

        try {
            Path path = Paths.get(logoPath).normalize();
            if (!path.isAbsolute()) {
                path = path.toAbsolutePath();
            }

            // Vérification du chemin
            Path baseDir = Paths.get(System.getProperty("user.dir")).normalize();
            if (!path.startsWith(baseDir)) {
                throw new SecurityException("Accès non autorisé en dehors du répertoire de l'application");
            }

            // Vérification de l'existence et du type de fichier
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Le fichier image n'existe pas ou n'est pas un fichier régulier");
            }

            // Calcul et vérification du hash du fichier
            byte[] fileContent = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileContent);

            // Vérification de la taille
            if (fileContent.length > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("La taille du fichier image dépasse la limite de " + MAX_FILE_SIZE_MB + "MB");
            }

            // Vérification du format
            String extension = getFileExtension(path.getFileName().toString()).toLowerCase();
            if (!FORMATS_IMAGE_AUTORISES.contains(extension)) {
                throw new IllegalArgumentException("Format d'image non autorisé. Formats acceptés : " + 
                    String.join(", ", FORMATS_IMAGE_AUTORISES));
            }

            // Vérification du contenu de l'image
            BufferedImage image = ImageIO.read(path.toFile());
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
        try {
            Path path = Paths.get(cheminFichier);
            if (Files.exists(path) && Files.size(path) > MAX_FILE_SIZE_BYTES) {
                throw new SecurityException("La taille du fichier PDF dépasse la limite autorisée de " + MAX_FILE_SIZE_MB + "MB");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la vérification de la taille du fichier", e);
            throw new SecurityException("Impossible de vérifier la taille du fichier: " + e.getMessage());
        }
    }

    public static void genererReglementCreance(Client client, double montantRegle, double resteAPayer, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête
            Paragraph header = new Paragraph("Reçu de Règlement de Créance", 
                new Font(BaseFont.createFont(), 16, Font.BOLD));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            // Informations client
            document.add(new Paragraph("Client: " + sanitizeInput(client.getNom())));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            document.add(new Paragraph("Montant réglé: " + String.format("%.2f €", montantRegle)));
            document.add(new Paragraph("Reste à payer: " + String.format("%.2f €", resteAPayer)));

            document.close();
            LOGGER.info("Reçu de règlement généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du reçu de règlement", e);
            throw new RuntimeException("Erreur lors de la génération du reçu de règlement", e);
        }
    }

    public static void genererRapportFournisseurs(List<Fournisseur> fournisseurs, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // Contenu du rapport
            Paragraph title = new Paragraph("Rapport des Fournisseurs", 
                new Font(BaseFont.createFont(), 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Tableau des fournisseurs
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            // En-têtes
            Stream.of("Nom", "Contact", "Téléphone", "Adresse")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });

            // Données
            for (Fournisseur f : fournisseurs) {
                table.addCell(sanitizeInput(f.getNom()));
                table.addCell(sanitizeInput(f.getContact()));
                table.addCell(sanitizeInput(f.getTelephone()));
                table.addCell(sanitizeInput(f.getAdresse()));
            }

            document.add(table);
            document.close();
            LOGGER.info("Rapport fournisseurs généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport fournisseurs", e);
            throw new RuntimeException("Erreur lors de la génération du rapport fournisseurs", e);
        }
    }

    public static void genererRapportVentes(List<Vente> ventes, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            Paragraph title = new Paragraph("Rapport des Ventes", 
                new Font(BaseFont.createFont(), 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            Stream.of("Date", "Client", "Produits", "Total", "Statut")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });

            for (Vente v : ventes) {
                table.addCell(v.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                table.addCell(sanitizeInput(v.getClient().getNom()));
                table.addCell(String.valueOf(v.getProduits().size()));
                table.addCell(String.format("%.2f €", v.getTotal()));
                table.addCell(v.getStatut().toString());
            }

            document.add(table);
            document.close();
            LOGGER.info("Rapport des ventes généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des ventes", e);
        }
    }

    public static void genererRapportStocks(List<Produit> produits, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            Paragraph title = new Paragraph("État des Stocks", 
                new Font(BaseFont.createFont(), 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            Stream.of("Référence", "Nom", "Prix", "Quantité", "Seuil Alerte")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });

            for (Produit p : produits) {
                table.addCell(sanitizeInput(p.getReference()));
                table.addCell(sanitizeInput(p.getNom()));
                table.addCell(String.format("%.2f €", p.getPrix()));
                table.addCell(String.valueOf(p.getQuantite()));
                table.addCell(String.valueOf(p.getSeuilAlerte()));
            }

            document.add(table);
            document.close();
            LOGGER.info("Rapport des stocks généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des stocks", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des stocks", e);
        }
    }

    public static void genererRapportCreances(List<Client> clients, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            Paragraph title = new Paragraph("Rapport des Créances", 
                new Font(BaseFont.createFont(), 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            Stream.of("Client", "Total Créances", "Dernière Vente", "Statut")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });

            for (Client c : clients) {
                table.addCell(sanitizeInput(c.getNom()));
                table.addCell(String.format("%.2f €", c.getTotalCreances()));
                table.addCell(c.getDerniereVente() != null ? 
                    c.getDerniereVente().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
                table.addCell(c.getStatutCreances().toString());
            }

            document.add(table);
            document.close();
            LOGGER.info("Rapport des créances généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des créances", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des créances", e);
        }
    }

    public static void genererRapportCaisse(List<MouvementCaisse> mouvements, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            Paragraph title = new Paragraph("Rapport de Caisse", 
                new Font(BaseFont.createFont(), 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            Stream.of("Date", "Type", "Montant", "Description")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });

            for (MouvementCaisse m : mouvements) {
                table.addCell(m.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                table.addCell(m.getType().toString());
                table.addCell(String.format("%.2f €", m.getMontant()));
                table.addCell(sanitizeInput(m.getDescription()));
            }

            document.add(table);
            document.close();
            LOGGER.info("Rapport de caisse généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport de caisse", e);
            throw new RuntimeException("Erreur lors de la génération du rapport de caisse", e);
        }
    }

    public static void genererPreviewTicket(Vente vente) {
        try {
            Document document = new Document(new Rectangle(TICKET_WIDTH, PageSize.A4.getHeight()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            // En-tête du ticket
            Paragraph header = new Paragraph("TICKET DE CAISSE", 
                new Font(BaseFont.createFont(), 12, Font.BOLD));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            // Détails de la vente
            document.add(new Paragraph("Date: " + vente.getDate().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            document.add(new Paragraph("Client: " + sanitizeInput(vente.getClient().getNom())));

            // Tableau des produits
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            for (Map.Entry<Produit, Integer> entry : vente.getProduits().entrySet()) {
                table.addCell(sanitizeInput(entry.getKey().getNom()));
                table.addCell(String.valueOf(entry.getValue()));
                table.addCell(String.format("%.2f €", 
                    entry.getKey().getPrix() * entry.getValue()));
            }

            document.add(table);

            // Total
            Paragraph total = new Paragraph("Total: " + String.format("%.2f €", vente.getTotal()),
                new Font(BaseFont.createFont(), 12, Font.BOLD));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
            LOGGER.info("Preview du ticket généré avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du preview du ticket", e);
            throw new RuntimeException("Erreur lors de la génération du preview du ticket", e);
        }
    }

    public static void genererTicket(Vente vente, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document(new Rectangle(TICKET_WIDTH, PageSize.A4.getHeight()));
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête du ticket
            Paragraph header = new Paragraph("TICKET DE CAISSE", 
                new Font(BaseFont.createFont(), 12, Font.BOLD));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            // Informations de l'entreprise
            document.add(new Paragraph("Date: " + vente.getDate().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            document.add(new Paragraph("N° Ticket: " + vente.getId()));
            document.add(new Paragraph("Client: " + sanitizeInput(vente.getClient().getNom())));

            // Tableau des produits
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            Stream.of("Produit", "Qté", "P.U.", "Total")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(1);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });

            for (Map.Entry<Produit, Integer> entry : vente.getProduits().entrySet()) {
                table.addCell(sanitizeInput(entry.getKey().getNom()));
                table.addCell(String.valueOf(entry.getValue()));
                table.addCell(String.format("%.2f €", entry.getKey().getPrix()));
                table.addCell(String.format("%.2f €", 
                    entry.getKey().getPrix() * entry.getValue()));
            }

            document.add(table);

            // Total et TVA
            document.add(new Paragraph("Total HT: " + String.format("%.2f €", vente.getTotalHT())));
            document.add(new Paragraph("TVA (" + vente.getTauxTVA() + "%): " + 
                String.format("%.2f €", vente.getMontantTVA())));
            Paragraph total = new Paragraph("Total TTC: " + String.format("%.2f €", vente.getTotal()),
                new Font(BaseFont.createFont(), 12, Font.BOLD));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            // Pied de page
            document.add(new Paragraph("Merci de votre visite !"));

            document.close();
            LOGGER.info("Ticket généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du ticket", e);
            throw new RuntimeException("Erreur lors de la génération du ticket", e);
        }
    }
}