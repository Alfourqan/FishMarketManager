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
import java.time.LocalDate;
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
import java.util.*;
import java.security.MessageDigest;
import java.util.UUID;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.DoubleSummaryStatistics;

public class PDFGenerator {
    private static final Logger LOGGER = Logger.getLogger(PDFGenerator.class.getName());
    private static final float TICKET_WIDTH = 170.079f;
    private static final float MARGIN = 14.17f;
    private static final int MAX_FILE_SIZE_MB = 10;
    private static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
    private static final Set<String> FORMATS_IMAGE_AUTORISES = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
    private static final int MAX_IMAGE_SIZE = 1000;
    private static final String OUTPUT_DIR = "generated_pdfs";
    private static final int MAX_FILENAME_LENGTH = 255;

    // Nouvelles constantes pour l'analyse
    private static final int SEUIL_ALERTE_STOCK = 10;
    private static final double SEUIL_ALERTE_CA = 1000.0;

    static {
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

    public static void genererRapportVentes(List<Vente> ventes, String cheminFichier, 
            LocalDate dateDebut, LocalDate dateFin) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // En-tête avec période
            ajouterEnTeteRapport(document, "Rapport des Ventes", dateDebut, dateFin);

            // Statistiques globales
            Map<String, Object> stats = ReportStatisticsManager.analyserVentes(ventes, dateDebut, dateFin, null);
            ajouterStatistiquesVentes(document, stats);

            // Analyse des tendances
            ajouterAnalyseTendances(document, ventes, dateDebut, dateFin);

            // Tableau détaillé des ventes
            ajouterTableauVentes(document, ventes);

            // Graphiques et visualisations
            ajouterVisualisationsVentes(document, stats);

            document.close();
            LOGGER.info("Rapport des ventes généré avec succès: " + cheminFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport des ventes", e);
            throw new RuntimeException("Erreur lors de la génération du rapport des ventes", e);
        }
    }

    private static void ajouterEnTeteRapport(Document document, String titre, LocalDate dateDebut, LocalDate dateFin) 
            throws DocumentException, IOException {
        Font titleFont = new Font(BaseFont.createFont(), 16, Font.BOLD);
        Paragraph header = new Paragraph(titre, titleFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        Paragraph period = new Paragraph("Période: du " + 
            dateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
            " au " + dateFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        period.setAlignment(Element.ALIGN_CENTER);
        document.add(period);
        document.add(Chunk.NEWLINE);
    }

    private static void ajouterStatistiquesVentes(Document document, Map<String, Object> stats) 
            throws DocumentException {
        Font sectionFont = new Font(BaseFont.createFont(), 14, Font.BOLD);

        // Chiffres clés
        document.add(new Paragraph("Chiffres Clés:", sectionFont));
        document.add(new Paragraph("Chiffre d'affaires total: " + 
            String.format("%.2f €", ((Number)stats.get("chiffreAffaires")).doubleValue())));
        document.add(new Paragraph("Nombre total de ventes: " + stats.get("nombreVentes")));
        document.add(new Paragraph("Panier moyen: " + 
            String.format("%.2f €", ((Number)stats.get("panierMoyen")).doubleValue())));

        // Répartition par mode de paiement
        document.add(new Paragraph("Répartition par Mode de Paiement:", sectionFont));
        Map<ModePaiement, DoubleSummaryStatistics> statsParMode = 
            (Map<ModePaiement, DoubleSummaryStatistics>) stats.get("statsParMode");
        for (Map.Entry<ModePaiement, DoubleSummaryStatistics> entry : statsParMode.entrySet()) {
            document.add(new Paragraph(String.format("%s: %.2f € (%d ventes)", 
                entry.getKey(), 
                entry.getValue().getSum(),
                entry.getValue().getCount())));
        }
        document.add(Chunk.NEWLINE);
    }

    private static void ajouterAnalyseTendances(Document document, List<Vente> ventes, 
            LocalDate dateDebut, LocalDate dateFin) throws DocumentException {
        Font sectionFont = new Font(BaseFont.createFont(), 14, Font.BOLD);
        document.add(new Paragraph("Analyse des Tendances:", sectionFont));

        // Analyse par période
        Map<LocalDate, List<Vente>> ventesParJour = ventes.stream()
            .collect(Collectors.groupingBy(v -> v.getDate().toLocalDate()));

        // Calculer les moyennes et tendances
        DoubleSummaryStatistics statsJournalieres = ventesParJour.values().stream()
            .mapToDouble(list -> list.stream()
                .mapToDouble(Vente::getMontantTotal)
                .sum())
            .summaryStatistics();

        document.add(new Paragraph("Statistiques Journalières:"));
        document.add(new Paragraph(String.format("- Moyenne: %.2f €", statsJournalieres.getAverage())));
        document.add(new Paragraph(String.format("- Maximum: %.2f €", statsJournalieres.getMax())));
        document.add(new Paragraph(String.format("- Minimum: %.2f €", statsJournalieres.getMin())));
        document.add(Chunk.NEWLINE);
    }

    private static void ajouterTableauVentes(Document document, List<Vente> ventes) 
            throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);

        // En-têtes
        Stream.of("Date", "Client", "Produits", "Total", "Statut")
            .forEach(columnTitle -> {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                header.setBorderWidth(2);
                header.setPhrase(new Phrase(columnTitle));
                table.addCell(header);
            });

        // Données
        for (Vente v : ventes) {
            table.addCell(v.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            table.addCell(sanitizeInput(v.getClient() != null ? v.getClient().getNom() : "Vente comptant"));
            table.addCell(String.valueOf(v.getLignes().size()));
            table.addCell(String.format("%.2f €", v.getMontantTotal()));
            table.addCell(v.getStatut().toString());
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private static void ajouterVisualisationsVentes(Document document, Map<String, Object> stats) 
            throws DocumentException {
        Font sectionFont = new Font(BaseFont.createFont(), 14, Font.BOLD);
        document.add(new Paragraph("Analyses Supplémentaires:", sectionFont));

        // Analyse de la répartition des ventes
        Map<String, Double> repartition = (Map<String, Double>) stats.get("repartitionVentes");
        if (repartition != null) {
            document.add(new Paragraph("Répartition des Ventes:"));
            for (Map.Entry<String, Double> entry : repartition.entrySet()) {
                document.add(new Paragraph(String.format("- %s: %.1f%%", 
                    entry.getKey(), entry.getValue())));
            }
        }

        // Alertes et recommandations
        List<String> alertes = (List<String>) stats.get("alertes");
        if (alertes != null && !alertes.isEmpty()) {
            document.add(new Paragraph("Alertes et Recommandations:", sectionFont));
            for (String alerte : alertes) {
                document.add(new Paragraph("- " + alerte));
            }
        }
    }

    public static void genererRapportStocks(List<Produit> produits, String cheminFichier) {
        try {
            validateFilePath(cheminFichier);
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
            document.open();

            // Titre principal
            Paragraph title = new Paragraph("État des Stocks", 
                new Font(BaseFont.createFont(), 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Statistiques globales
            Map<String, Object> stats = ReportStatisticsManager.analyserStocks(produits, null); // null pour toutes les catégories
            document.add(new Paragraph("Statistiques Globales:", 
                new Font(BaseFont.createFont(), 14, Font.BOLD)));
            document.add(new Paragraph("Nombre total de produits: " + stats.get("totalProduits")));
            document.add(new Paragraph("Valeur totale du stock: " + 
                String.format("%.2f €", ((Number)stats.get("valeurTotaleStock")).doubleValue())));
            document.add(Chunk.NEWLINE);

            // Répartition par statut
            document.add(new Paragraph("Répartition par Statut:", 
                new Font(BaseFont.createFont(), 14, Font.BOLD)));
            Map<String, List<Produit>> produitsParStatut = (Map<String, List<Produit>>) stats.get("produitsParStatut");
            for (Map.Entry<String, List<Produit>> entry : produitsParStatut.entrySet()) {
                document.add(new Paragraph(entry.getKey() + ": " + entry.getValue().size()));
            }
            document.add(Chunk.NEWLINE);

            // Tableau des produits
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

    public static String genererPreviewTicket(Vente vente) {
        try {
            StringBuilder preview = new StringBuilder();
            preview.append("=== TICKET DE CAISSE ===\n\n");
            preview.append("Date: ").append(vente.getDate().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            preview.append("Client: ").append(vente.getClient() != null ? 
                sanitizeInput(vente.getClient().getNom()) : "Vente comptant").append("\n\n");

            // En-tête du tableau
            preview.append(String.format("%-30s %8s %10s %12s\n", "Produit", "Qté", "P.U.", "Total"));
            preview.append("-".repeat(64)).append("\n");

            // Détails des produits
            for (Vente.LigneVente ligne : vente.getLignes()) {
                preview.append(String.format("%-30s %8d %10.2f€ %12.2f€\n",
                    truncateString(sanitizeInput(ligne.getProduit().getNom()), 30),
                    ligne.getQuantite(),
                    ligne.getPrixUnitaire(),
                    ligne.getQuantite() * ligne.getPrixUnitaire()));
            }

            preview.append("-".repeat(64)).append("\n");
            preview.append(String.format("%52s %10.2f€\n", "Total:", vente.getMontantTotal()));
            preview.append("\nMerci de votre visite !\n");
            preview.append("=".repeat(64));

            LOGGER.info("Preview du ticket généré avec succès");
            return preview.toString();
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
            Paragraph titleHeader = new Paragraph("TICKET DE CAISSE", 
                new Font(BaseFont.createFont(), 12, Font.BOLD));
            titleHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(titleHeader);

            // Informations de l'entreprise
            document.add(new Paragraph("Date: " + vente.getDate().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            document.add(new Paragraph("N° Ticket: " + vente.getId()));
            document.add(new Paragraph("Client: " + (vente.getClient() != null ? 
                sanitizeInput(vente.getClient().getNom()) : "Vente comptant")));

            // Tableau des produits
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            // En-têtes du tableau
            Stream.of("Produit", "Qté", "P.U.", "Total")
                .forEach(columnTitle -> {
                    PdfPCell cell = new PdfPCell();
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    cell.setBorderWidth(1);
                    cell.setPhrase(new Phrase(columnTitle));
                    table.addCell(cell);
                });

            // Détails des produits
            for (Vente.LigneVente ligne : vente.getLignes()) {
                table.addCell(sanitizeInput(ligne.getProduit().getNom()));
                table.addCell(String.valueOf(ligne.getQuantite()));
                table.addCell(String.format("%.2f €", ligne.getPrixUnitaire()));
                table.addCell(String.format("%.2f €", 
                    ligne.getQuantite() * ligne.getPrixUnitaire()));
            }

            document.add(table);

            // Total et TVA
            document.add(new Paragraph("Total HT: " + String.format("%.2f €", vente.getMontantTotalHT())));
            document.add(new Paragraph("TVA (" + vente.getTauxTVA() + "%): " + 
                String.format("%.2f €", vente.getMontantTVA())));
            Paragraph total = new Paragraph("Total TTC: " + String.format("%.2f €", vente.getMontantTotal()),
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