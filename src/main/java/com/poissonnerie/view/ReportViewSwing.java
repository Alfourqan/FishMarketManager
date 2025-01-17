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
import javax.swing.table.DefaultCellEditor;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

public class ReportViewSwing {
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
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode, Color color) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);

        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Effet de survol
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
        return createStyledButton(text, iconCode, new Color(0, 135, 136));
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
            BorderFactory.createLineBorder(new Color(0, 135, 136), 1),
            "Période",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(0, 135, 136)
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
            BorderFactory.createLineBorder(new Color(0, 135, 136), 1),
            "Rapports",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(0, 135, 136)
        ));

        JButton ventesBtn = createStyledButton("Rapport des ventes", MaterialDesign.MDI_CART, new Color(33, 150, 243));
        JButton stocksBtn = createStyledButton("Rapport des stocks", MaterialDesign.MDI_PACKAGE_VARIANT, new Color(76, 175, 80));
        JButton fournisseursBtn = createStyledButton("Rapport fournisseurs", MaterialDesign.MDI_TRUCK_DELIVERY, new Color(255, 152, 0));
        JButton statistiquesBtn = createStyledButton("Statistiques", MaterialDesign.MDI_CHART_BAR, new Color(156, 39, 176));
        JButton creancesBtn = createStyledButton("État des créances", MaterialDesign.MDI_ACCOUNT_CASH, new Color(233, 30, 99));

        // Gestionnaires d'événements
        ventesBtn.addActionListener(e -> genererRapportVentes());
        stocksBtn.addActionListener(e -> genererRapportStocks());
        fournisseursBtn.addActionListener(e -> genererRapportFournisseurs());
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
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
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
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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

    private void genererRapportVentes() {
        try {
            venteController.chargerVentes();
            String nomFichier = "rapport_ventes_" + LocalDate.now() + ".pdf";

            List<Vente> ventesFiltered = venteController.getVentes().stream()
                .filter(v -> !v.getDate().toLocalDate().isBefore(dateDebut) && 
                           !v.getDate().toLocalDate().isAfter(dateFin))
                .collect(Collectors.toList());

            PDFGenerator.genererRapportVentes(ventesFiltered, nomFichier);
            afficherMessageSuccess("Rapport généré", 
                "Le rapport des ventes a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            afficherMessageErreur("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
        }
    }

    private void genererRapportStocks() {
        try {
            produitController.chargerProduits();
            String nomFichier = "rapport_stocks_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportStocks(produitController.getProduits(), nomFichier);
            afficherMessageSuccess("Rapport généré", 
                "Le rapport des stocks a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            afficherMessageErreur("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
        }
    }

    private void genererRapportFournisseurs() {
        try {
            fournisseurController.chargerFournisseurs();
            String nomFichier = "rapport_fournisseurs_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportFournisseurs(fournisseurController.getFournisseurs(), nomFichier);
            afficherMessageSuccess("Rapport généré", 
                "Le rapport des fournisseurs a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            afficherMessageErreur("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
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
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(33, 33, 33));

        JTextArea contentArea = new JTextArea(content);
        contentArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentArea.setEditable(false);
        contentArea.setBackground(panel.getBackground());
        contentArea.setBorder(null);

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
            return 0.0;
        }
    }

    private int getNombreProduits() {
        try {
            produitController.chargerProduits();
            return produitController.getProduits().size();
        } catch (Exception e) {
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
            return 0.0;
        }
    }

    private int getNombreFournisseurs() {
        try {
            fournisseurController.chargerFournisseurs();
            return fournisseurController.getFournisseurs().size();
        } catch (Exception e) {
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
            System.err.println("Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    private void afficherMessageSuccess(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherMessageErreur(String titre, String message) {
        JOptionPane.showMessageDialog(mainPanel, message, titre, JOptionPane.ERROR_MESSAGE);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void afficherEtatCreances() {
        try {
            // Charger les clients
            ClientController clientController = new ClientController();
            clientController.chargerClients();
            List<Client> clients = clientController.getClients();

            // Filtrer uniquement les clients avec des créances
            List<Client> clientsAvecCreances = clients.stream()
                .filter(c -> c.getSolde() > 0)
                .collect(Collectors.toList());

            if (clientsAvecCreances.isEmpty()) {
                showInfoMessage("État des créances", "Aucune créance en cours.");
                return;
            }

            // Créer la fenêtre de détail des créances
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), 
                "État des créances", true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(null);

            // Panel de recherche
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
            JTextField searchField = new JTextField(20);
            searchPanel.add(new JLabel("Rechercher: "));
            searchPanel.add(searchField);

            // Tableau des créances
            String[] colonnes = {"Client", "Téléphone", "Solde", "Actions"};
            Object[][] donnees = new Object[clientsAvecCreances.size()][4];

            for (int i = 0; i < clientsAvecCreances.size(); i++) {
                Client client = clientsAvecCreances.get(i);
                donnees[i][0] = client.getNom();
                donnees[i][1] = client.getTelephone();
                donnees[i][2] = String.format("%.2f €", client.getSolde());
                donnees[i][3] = "Générer rapport";
            }

            JTable table = new JTable(donnees, colonnes) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 3;
                }
            };

            // Configuration du tri
            TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);

            // Recherche en temps réel
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

            // Style du tableau
            table.setRowHeight(30);
            table.setIntercellSpacing(new Dimension(10, 5));
            table.setShowGrid(false);
            table.setShowHorizontalLines(true);
            table.setGridColor(new Color(230, 230, 230));

            // En-têtes du tableau
            JTableHeader header = table.getTableHeader();
            header.setBackground(new Color(240, 240, 240));
            header.setForeground(new Color(33, 33, 33));
            header.setFont(new Font("Segoe UI", Font.BOLD, 12));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

            // Renderer personnalisé pour la colonne Actions
            table.getColumnModel().getColumn(3).setCellRenderer((TableCellRenderer) (table1, value, isSelected, hasFocus, row, column) -> {
                JButton button = new JButton("Générer rapport");
                button.setBackground(new Color(33, 150, 243));
                button.setForeground(Color.WHITE);
                button.setFocusPainted(false);
                button.setBorderPainted(false);
                return button;
            });

            // Editor personnalisé pour la colonne Actions
            table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
                @Override
                public Component getTableCellEditorComponent(JTable table, Object value,
                        boolean isSelected, int row, int col) {
                    JButton button = new JButton("Générer rapport");
                    button.setBackground(new Color(33, 150, 243));
                    button.setForeground(Color.WHITE);
                    button.setFocusPainted(false);
                    button.setBorderPainted(false);

                    button.addActionListener(e -> {
                        Client client = clientsAvecCreances.get(table.convertRowIndexToModel(row));
                        genererRapportCreanceClient(client);
                        fireEditingStopped();
                    });

                    return button;
                }
            });

            // Ajustement des colonnes
            table.getColumnModel().getColumn(0).setPreferredWidth(200);
            table.getColumnModel().getColumn(1).setPreferredWidth(150);
            table.getColumnModel().getColumn(2).setPreferredWidth(100);
            table.getColumnModel().getColumn(3).setPreferredWidth(150);

            // Boutons d'action globaux
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
            JButton rapportGlobalBtn = createStyledButton("Rapport global des créances",
                MaterialDesign.MDI_FILE_DOCUMENT, new Color(33, 150, 243));
            rapportGlobalBtn.addActionListener(e -> {
                genererRapportCreances(clientsAvecCreances,
                    "rapport_creances_global_" + LocalDate.now() + ".pdf");
                dialog.dispose();
            });
            buttonPanel.add(rapportGlobalBtn);

            // ScrollPane avec style
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            scrollPane.getViewport().setBackground(Color.WHITE);

            // Assembly
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.add(searchPanel, BorderLayout.NORTH);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            dialog.add(contentPanel);
            dialog.setVisible(true);

        } catch (Exception e) {
            showErrorMessage("Erreur", "Erreur lors de l'affichage des créances : " + e.getMessage());
        }
    }

    private void genererRapportCreanceClient(Client client) {
        try {
            String nomFichier = "rapport_creance_" + client.getNom().toLowerCase().replace(" ", "_") 
                + "_" + LocalDate.now() + ".pdf";

            List<Client> clientSeul = new ArrayList<>();
            clientSeul.add(client);

            PDFGenerator.genererRapportCreances(clientSeul, nomFichier);
            showSuccessMessage("Rapport généré",
                "Le rapport de créance pour " + client.getNom() + " a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            showErrorMessage("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
        }
    }

    private void genererRapportCreances(List<Client> clients, String nomFichier) {
        try {
            PDFGenerator.genererRapportCreances(clients, nomFichier);
            showSuccessMessage("Rapport généré", 
                "Le rapport global des créances a été généré dans le fichier : " + nomFichier);
            ouvrirFichierPDF(nomFichier);
        } catch (Exception e) {
            showErrorMessage("Erreur", "Erreur lors de la génération du rapport : " + e.getMessage());
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
}