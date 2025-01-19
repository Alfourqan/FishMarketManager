package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import com.poissonnerie.util.PDFGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import java.util.ArrayList;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import javax.swing.border.TitledBorder;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.SpinnerNumberModel;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ReportViewSwing {
    private static final Logger LOGGER = Logger.getLogger(ReportViewSwing.class.getName());

    // Constantes UI
    private static final Color PRIMARY_COLOR = new Color(0, 135, 136);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);
    private static final Color INFO_COLOR = new Color(33, 150, 243);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    // Messages d'erreur
    private static final String MSG_ERREUR_GENERATION = "Erreur lors de la génération du rapport : ";
    private static final String MSG_ERREUR_CHARGEMENT = "Erreur lors du chargement des données : ";
    private static final String MSG_SUCCES_GENERATION = "Le rapport a été généré dans le fichier : ";

    private final JPanel mainPanel;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final FournisseurController fournisseurController;
    private JPanel chartPanel;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    public ReportViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        venteController = new VenteController();
        produitController = new ProduitController();
        fournisseurController = new FournisseurController();

        dateDebut = LocalDate.now().minusMonths(1);
        dateFin = LocalDate.now();

        initializeComponents();
        LOGGER.info("ReportViewSwing initialisé avec succès");
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode, Color color) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);

        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setFont(REGULAR_FONT);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private JButton createReportButton(String text, MaterialDesign iconCode) {
        return createStyledButton(text, iconCode, PRIMARY_COLOR);
    }

    private void initializeComponents() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(236, 239, 241));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Sélecteur de période
        JPanel periodPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        periodPanel.setBackground(panel.getBackground());
        periodPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            "Période",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            PRIMARY_COLOR
        ));

        JButton aujourdhuiBtn = createPeriodButton("Aujourd'hui", () -> {
            dateDebut = LocalDate.now();
            dateFin = LocalDate.now();
            updateCharts();
        });

        JButton semaineBtn = createPeriodButton("Cette semaine", () -> {
            dateDebut = LocalDate.now().minusWeeks(1);
            dateFin = LocalDate.now();
            updateCharts();
        });

        JButton moisBtn = createPeriodButton("Ce mois", () -> {
            dateDebut = LocalDate.now().minusMonths(1);
            dateFin = LocalDate.now();
            updateCharts();
        });

        periodPanel.add(aujourdhuiBtn);
        periodPanel.add(semaineBtn);
        periodPanel.add(moisBtn);

        // Boutons de génération de rapports
        JPanel reportButtonsPanel = new JPanel();
        reportButtonsPanel.setLayout(new BoxLayout(reportButtonsPanel, BoxLayout.Y_AXIS));
        reportButtonsPanel.setBackground(panel.getBackground());
        reportButtonsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            "Rapports",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            PRIMARY_COLOR
        ));

        JButton ventesBtn = createReportButton("Rapport des ventes", MaterialDesign.MDI_CART);
        JButton stocksBtn = createStyledButton("Rapport des stocks", MaterialDesign.MDI_PACKAGE_VARIANT, SUCCESS_COLOR);
        JButton fournisseursBtn = createStyledButton("Rapport fournisseurs", MaterialDesign.MDI_TRUCK_DELIVERY, WARNING_COLOR);
        JButton statistiquesBtn = createStyledButton("Statistiques", MaterialDesign.MDI_CHART_BAR, new Color(156, 39, 176));
        JButton creancesBtn = createStyledButton("État des créances", MaterialDesign.MDI_CASH_MULTIPLE, new Color(233, 30, 99));

        // Gestionnaires d'événements
        ventesBtn.addActionListener(e -> genererRapport("ventes", venteController.getVentes().stream()
                .filter(v -> !v.getDate().toLocalDate().isBefore(dateDebut) &&
                        !v.getDate().toLocalDate().isAfter(dateFin))
                .collect(Collectors.toList()), "rapport_ventes_" + LocalDate.now() + ".pdf"));
        stocksBtn.addActionListener(e -> genererRapport("stocks", produitController.getProduits(), "rapport_stocks_" + LocalDate.now() + ".pdf"));
        fournisseursBtn.addActionListener(e -> genererRapport("fournisseurs", fournisseurController.getFournisseurs(), "rapport_fournisseurs_" + LocalDate.now() + ".pdf"));
        statistiquesBtn.addActionListener(e -> afficherStatistiques());
        creancesBtn.addActionListener(e -> afficherEtatCreances());

        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(ventesBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(stocksBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(fournisseursBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(statistiquesBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));
        reportButtonsPanel.add(creancesBtn);
        reportButtonsPanel.add(Box.createVerticalStrut(5));

        panel.add(periodPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(reportButtonsPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel titleLabel = new JLabel("Tableau de bord", SwingConstants.LEFT);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(new Color(33, 33, 33));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        headerPanel.add(titleLabel, BorderLayout.CENTER);

        chartPanel = new JPanel();
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }


    private JButton createPeriodButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(REGULAR_FONT);
        button.setBackground(Color.WHITE);
        button.setForeground(new Color(33, 33, 33));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        // Effet de survol
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(245, 245, 245));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
            }
        });

        button.addActionListener(e -> action.run());
        return button;
    }

    /* Ajout des vérifications de type */
    private void genererRapport(String type, List<?> donnees, String nomFichier) {
        if (donnees == null) {
            LOGGER.severe("Les données ne peuvent pas être null");
            afficherMessageErreur("Erreur", "Données invalides pour la génération du rapport");
            return;
        }

        try {
            switch (type) {
                case "ventes":
                    if (!(donnees.stream().allMatch(d -> d instanceof Vente))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des ventes");
                    }
                    PDFGenerator.genererRapportVentes((List<Vente>) donnees, nomFichier);
                    break;
                case "stocks":
                    if (!(donnees.stream().allMatch(d -> d instanceof Produit))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des stocks");
                    }
                    PDFGenerator.genererRapportStocks((List<Produit>) donnees, nomFichier);
                    break;
                case "fournisseurs":
                    if (!(donnees.stream().allMatch(d -> d instanceof Fournisseur))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des fournisseurs");
                    }
                    PDFGenerator.genererRapportFournisseurs((List<Fournisseur>) donnees, nomFichier);
                    break;
                case "creances":
                    if (!(donnees.stream().allMatch(d -> d instanceof Client))) {
                        throw new IllegalArgumentException("Type de données incorrect pour le rapport des créances");
                    }
                    PDFGenerator.genererRapportCreances((List<Client>) donnees, nomFichier);
                    break;
                default:
                    throw new IllegalArgumentException("Type de rapport inconnu: " + type);
            }
            LOGGER.info("Rapport généré avec succès: " + nomFichier);
            afficherMessageSuccess("Rapport généré", MSG_SUCCES_GENERATION + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du rapport", e);
            afficherMessageErreur("Erreur", MSG_ERREUR_GENERATION + e.getMessage());
        }
    }

    private void afficherStatistiques() {
        chartPanel.removeAll();
        chartPanel.setLayout(new GridLayout(2, 2, 15, 15));

        addStatPanel("Ventes", String.format(
            "Aujourd'hui: %.2f €\nCette semaine: %.2f €\nCe mois: %.2f €",
            calculerVentesTotal(LocalDate.now(), LocalDate.now()),
            calculerVentesTotal(LocalDate.now().minusWeeks(1), LocalDate.now()),
            calculerVentesTotal(LocalDate.now().minusMonths(1), LocalDate.now())
        ));

        addStatPanel("Stock", String.format(
            "Total produits: %d\nEn alerte: %d\nValeur totale: %.2f €",
            getNombreProduits(),
            getNombreProduitsEnAlerte(),
            getValeurTotaleStock()
        ));

        addStatPanel("Fournisseurs", String.format(
            "Total: %d\nCommandes en cours: %d",
            getNombreFournisseurs(),
            getCommandesEnCours()
        ));

        addStatPanel("Performance", String.format(
            "Marge brute: %.2f %%\nRotation stock: %.1f jours",
            calculerMargeBrute(),
            calculerRotationStock()
        ));

        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private void addStatPanel(String title, String content) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SUBTITLE_FONT);
        titleLabel.setForeground(new Color(33, 33, 33));

        JTextArea contentArea = new JTextArea(content);
        contentArea.setFont(REGULAR_FONT);
        contentArea.setEditable(false);
        contentArea.setBackground(panel.getBackground());
        contentArea.setBorder(null);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(contentArea, BorderLayout.CENTER);

        chartPanel.add(panel);
    }

    // Méthodes utilitaires pour les statistiques
    private double calculerVentesTotal(LocalDate debut, LocalDate fin) {
        try {
            venteController.chargerVentes();
            return venteController.getVentes().stream()
                .filter(v -> !v.getDate().toLocalDate().isBefore(debut) && 
                           !v.getDate().toLocalDate().isAfter(fin))
                .mapToDouble(Vente::getMontantTotal)
                .sum();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du calcul du total des ventes", e);
            return 0.0;
        }
    }

    private int getNombreProduits() {
        try {
            produitController.chargerProduits();
            return produitController.getProduits().size();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des produits", e);
            return 0;
        }
    }

    private int getNombreProduitsEnAlerte() {
        try {
            produitController.chargerProduits();
            return (int) produitController.getProduits().stream()
                .filter(p -> p.getQuantite() <= p.getSeuilAlerte())
                .count();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du calcul des produits en alerte", e);
            return 0;
        }
    }

    private double getValeurTotaleStock() {
        try {
            produitController.chargerProduits();
            return produitController.getProduits().stream()
                .mapToDouble(p -> p.getPrixAchat() * p.getQuantite())
                .sum();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du calcul de la valeur totale du stock", e);
            return 0.0;
        }
    }

    private int getNombreFournisseurs() {
        try {
            fournisseurController.chargerFournisseurs();
            return fournisseurController.getFournisseurs().size();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des fournisseurs", e);
            return 0;
        }
    }

    private int getCommandesEnCours() {
        // À implémenter selon la logique métier
        return 0;
    }

    private double calculerMargeBrute() {
        // À implémenter selon la logique métier
        return 25.5; // Exemple
    }

    private double calculerRotationStock() {
        // À implémenter selon la logique métier
        return 15.3; // Exemple
    }

    private void updateCharts() {
        afficherStatistiques();
    }

    private void ouvrirFichierPDF(String nomFichier) {
        try {
            File file = new File(nomFichier);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Impossible d'ouvrir le fichier PDF", e);
            System.err.println("Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    private void afficherMessageSuccess(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherMessageErreur(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.ERROR_MESSAGE);
    }

    private void afficherEtatCreances() {
        try {
            ClientController clientController = new ClientController();
            clientController.chargerClients();
            List<Client> clients = clientController.getClients();

            List<Client> clientsAvecCreances = clients.stream()
                .filter(c -> c.getSolde() > 0)
                .collect(Collectors.toList());

            if (clientsAvecCreances.isEmpty()) {
                showInfoMessage("État des créances", "Aucune créance en cours.");
                return;
            }

            JDialog dialog = createCreancesDialog(clientsAvecCreances);
            dialog.setVisible(true);

        } catch (Exception e) {
            handleError("affichage des créances", e);
        }
    }

    private JDialog createCreancesDialog(List<Client> clientsAvecCreances) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), 
            "État des créances", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        JTextField searchField = new JTextField(20);
        searchPanel.add(new JLabel("Rechercher: "));
        searchPanel.add(searchField);


        String[] colonnes = {"Client", "Téléphone", "Solde", "Actions"};
        Object[][] donnees = new Object[clientsAvecCreances.size()][4];

        for (int i = 0; i < clientsAvecCreances.size(); i++) {
            Client client = clientsAvecCreances.get(i);
            donnees[i][0] = client.getNom();
            donnees[i][1] = client.getTelephone();
            donnees[i][2] = String.format("%.2f €", client.getSolde());
            donnees[i][3] = client; 
        }

        JTable table = new JTable(donnees, colonnes) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }

            private void filter() {
                String text = searchField.getText();
                if (text.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        table.setRowHeight(30);
        table.setIntercellSpacing(new Dimension(10, 5));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(230, 230, 230));

        table.getTableHeader().setBackground(new Color(240, 240, 240));
        table.getTableHeader().setForeground(new Color(33, 33, 33));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        table.getColumnModel().getColumn(3).setCellRenderer((TableCellRenderer) (table1, value, isSelected, hasFocus, row, column) -> {
            JButton button = new JButton("Détails");
            button.setBackground(INFO_COLOR);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            return button;
        });

        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int col) {
                JButton button = new JButton("Détails");
                button.setBackground(INFO_COLOR);
                button.setForeground(Color.WHITE);
                button.setFocusPainted(false);
                button.setBorderPainted(false);

                button.addActionListener(e -> {
                    Client client = (Client) value;
                    afficherDetailCreancesClient(client);
                    fireEditingStopped();
                });

                return button;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        JButton rapportGlobalBtn = createStyledButton("Rapport global des créances",
            MaterialDesign.MDI_FILE_DOCUMENT, INFO_COLOR);
        rapportGlobalBtn.addActionListener(e -> {
            genererRapport("creances", clientsAvecCreances,
                "rapport_creances_global_" + LocalDate.now() + ".pdf");
            dialog.dispose();
        });
        buttonPanel.add(rapportGlobalBtn);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(searchPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        dialog.add(contentPanel, BorderLayout.CENTER);
        return dialog;
    }

    private void genererRapportCreanceClient(Client client) {
        try {
            String nomFichier = "rapport_creance_" + client.getNom().toLowerCase().replace(" ", "_") 
                + "_" + LocalDate.now() + ".pdf";

            List<Client> clientSeul = new ArrayList<>();
            clientSeul.add(client);

            genererRapport("creances", clientSeul, nomFichier);
        } catch (Exception e) {
            handleError("génération du rapport de créance client", e);
        }
    }

    private void genererRapportCreances(List<Client> clients, String nomFichier) {
        try {
            genererRapport("creances", clients, nomFichier);
        } catch (Exception e) {
            handleError("génération du rapport global de créances", e);
        }
    }

    private void showSuccessMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showErrorMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoMessage(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }
    
    /* Amélioration de la gestion des erreurs dans createDetailDialog */
    private JDialog createDetailDialog(Client client) {
        if (client == null) {
            LOGGER.severe("Client null passé à createDetailDialog");
            throw new IllegalArgumentException("Le client ne peut pas être null");
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), 
            "Détail des créances - " + client.getNom(), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);

        try {
            JPanel mainPanel = createDetailMainPanel(client);
            dialog.add(mainPanel);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création du panel de détails", e);
            throw new RuntimeException("Impossible de créer le dialogue de détails", e);
        }

        return dialog;
    }

    private JPanel createDetailMainPanel(Client client) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(createInfoPanel(client), BorderLayout.NORTH);
        panel.add(createHistoriquePanel(), BorderLayout.CENTER);
        panel.add(createButtonPanel(client), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createInfoPanel(Client client) {
        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(SUCCESS_COLOR),
            "Informations client",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            SUCCESS_COLOR
        ));

        infoPanel.add(new JLabel("Nom:"));
        infoPanel.add(new JLabel(client.getNom()));
        infoPanel.add(new JLabel("Téléphone:"));
        infoPanel.add(new JLabel(client.getTelephone()));
        infoPanel.add(new JLabel("Solde actuel:"));
        infoPanel.add(new JLabel(String.format("%.2f €", client.getSolde())));
        return infoPanel;
    }

    private JPanel createHistoriquePanel() {
        JPanel historiquePanel = new JPanel(new BorderLayout());
        historiquePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(INFO_COLOR),
            "Historique des transactions",
            TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            INFO_COLOR
        ));

        String[] columns = {"Date", "Type", "Montant", "Solde après"};
        Object[][] data = new Object[0][4]; 

        JTable table = new JTable(data, columns);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        historiquePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        return historiquePanel;
    }

    private JPanel createButtonPanel(Client client) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton genererRapportBtn = createStyledButton("Générer rapport détaillé",
            MaterialDesign.MDI_FILE_DOCUMENT, INFO_COLOR);
        JButton reglerCreanceBtn = createStyledButton("Régler créance",
            MaterialDesign.MDI_CASH, SUCCESS_COLOR);

        genererRapportBtn.addActionListener(e -> {
            genererRapportCreanceClient(client);
            ((JDialog)((JButton)e.getSource()).getTopLevelAncestor()).dispose();
        });

        reglerCreanceBtn.addActionListener(e -> {
            afficherDialogueReglement(client);
            ((JDialog)((JButton)e.getSource()).getTopLevelAncestor()).dispose();
        });

        buttonPanel.add(genererRapportBtn);
        buttonPanel.add(reglerCreanceBtn);
        return buttonPanel;
    }

    /* Amélioration de la méthode afficherDialogueReglement */
    private void afficherDialogueReglement(Client client) {
        if (client == null || client.getSolde() <= 0) {
            LOGGER.warning("Tentative d'afficher le dialogue de règlement pour un client invalide");
            showErrorMessage("Erreur", "Client invalide ou sans créance");
            return;
        }

        try {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), 
                "Règlement de créance - " + client.getNom(), true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(400, 250);
            dialog.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            configureReglementDialogComponents(dialog, panel, gbc, client);
            dialog.setVisible(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage du dialogue de règlement", e);
            showErrorMessage("Erreur", "Impossible d'afficher le dialogue de règlement: " + e.getMessage());
        }
    }

    /* Nouvelle méthode pour la configuration des composants du dialogue de règlement */
    private void configureReglementDialogComponents(JDialog dialog, JPanel panel, GridBagConstraints gbc, Client client) {
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Solde actuel:"), gbc);

        gbc.gridx = 1;
        panel.add(new JLabel(String.format("%.2f €", client.getSolde())), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Montant à régler:"), gbc);

        gbc.gridx = 1;
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0.0, 0.0, client.getSolde(), 0.01);
        JSpinner montantSpinner = new JSpinner(spinnerModel);
        panel.add(montantSpinner, gbc);

        JPanel buttonPanel = createReglementButtonPanel(dialog, montantSpinner, client);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
    }

    /* Nouvelle méthode pour créer le panel des boutons de règlement */
    private JPanel createReglementButtonPanel(JDialog dialog, JSpinner montantSpinner, Client client) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton validerBtn = createStyledButton("Valider", MaterialDesign.MDI_CHECK, SUCCESS_COLOR);
        JButton annulerBtn = createStyledButton("Annuler", MaterialDesign.MDI_CLOSE, ERROR_COLOR);

        validerBtn.addActionListener(e -> handleReglementValidation(dialog, montantSpinner, client));
        annulerBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(validerBtn);
        buttonPanel.add(annulerBtn);
        return buttonPanel;
    }

    /* Nouvelle méthode pour gérer la validation du règlement */
    private void handleReglementValidation(JDialog dialog, JSpinner montantSpinner, Client client) {
        try {
            double montant = (double) montantSpinner.getValue();
            validateReglementAmount(montant, client.getSolde());

            ClientController clientController = new ClientController();
            clientController.reglerCreance(client, montant);

            showSuccessMessage("Succès", String.format("Règlement de %.2f € effectué avec succès", montant));
            dialog.dispose();
            afficherEtatCreances();
        } catch (IllegalArgumentException ex) {
            showErrorMessage("Erreur", ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Erreur lors du règlement de la créance", ex);
            showErrorMessage("Erreur", "Une erreur est survenue lors du règlement: " + ex.getMessage());
        }
    }

    /* Nouvelle méthode pour valider le montant du règlement */
    private void validateReglementAmount(double montant, double solde) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à 0");
        }
        if (montant > solde) {
            throw new IllegalArgumentException("Le montant ne peut pas être supérieur au solde");
        }
    }

    private void handleError(String operation, Exception e) {
        LOGGER.log(Level.SEVERE, "Erreur lors de " + operation, e);
        showErrorMessage("Erreur", MSG_ERREUR_GENERATION + e.getMessage());
    }

    public JPanel getMainPanel() {
        return mainPanel;    }
    private void afficherDetailCreancesClient(Client client) {
        try {
            JDialog dialog = createDetailDialog(client);
            dialog.setVisible(true);
            LOGGER.info("Affichage des détails des créances pour le client: " + client.getNom());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage des détails du client", e);
            showErrorMessage("Erreur", "Impossible d'afficher les détails : " + e.getMessage());
        }
    }
}