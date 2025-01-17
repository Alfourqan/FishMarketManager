package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import com.poissonnerie.controller.*;
import com.poissonnerie.model.*;
import com.poissonnerie.util.PDFGenerator;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

public class MainViewSwing {
    private final JPanel mainPanel;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final ClientController clientController;
    private final CaisseController caisseController;

    public MainViewSwing() {
        mainPanel = new JPanel(new BorderLayout());
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        venteController = new VenteController();
        produitController = new ProduitController();
        clientController = new ClientController();
        caisseController = new CaisseController();

        initializeComponents();
    }

    private void initializeComponents() {
        // Menu Bar
        JMenuBar menuBar = createMenuBar();
        mainPanel.add(menuBar, BorderLayout.NORTH);

        // Panel de navigation vertical à gauche avec plus d'espace
        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBackground(new Color(220, 224, 228));
        navigationPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        navigationPanel.setPreferredSize(new Dimension(160, 0));

        JPanel[] views = {
            new ProduitViewSwing().getMainPanel(),
            new VenteViewSwing().getMainPanel(),
            new ClientViewSwing().getMainPanel(),
            new CaisseViewSwing().getMainPanel(),
            new InventaireViewSwing().getMainPanel()
        };

        String[] viewNames = {"Produits", "Ventes", "Clients", "Caisse", "Inventaire"};
        MaterialDesign[] icons = {
            MaterialDesign.MDI_PACKAGE_VARIANT,
            MaterialDesign.MDI_CART,
            MaterialDesign.MDI_ACCOUNT_MULTIPLE,
            MaterialDesign.MDI_CASH_MULTIPLE,
            MaterialDesign.MDI_CLIPBOARD_TEXT
        };

        ButtonGroup buttonGroup = new ButtonGroup();
        navigationPanel.add(Box.createVerticalStrut(5));

        for (int i = 0; i < viewNames.length; i++) {
            JToggleButton navButton = createNavigationButton(viewNames[i], icons[i]);
            final String cardName = viewNames[i];
            final int index = i;

            navButton.addActionListener(e -> cardLayout.show(contentPanel, cardName));
            buttonGroup.add(navButton);
            navigationPanel.add(navButton);
            navigationPanel.add(Box.createVerticalStrut(5));

            contentPanel.add(views[index], cardName);

            if (i == 0) {
                navButton.setSelected(true);
            }
        }

        navigationPanel.add(Box.createVerticalStrut(5));

        mainPanel.add(navigationPanel, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Fichier avec icônes
        JMenu fichierMenu = new JMenu("Fichier");
        JMenuItem parametresMenuItem = new JMenuItem("Paramètres", createLargeIcon(MaterialDesign.MDI_SETTINGS));
        JMenuItem exporterMenuItem = new JMenuItem("Exporter les données", createLargeIcon(MaterialDesign.MDI_EXPORT));
        JMenuItem quitterMenuItem = new JMenuItem("Quitter", createLargeIcon(MaterialDesign.MDI_EXIT_TO_APP));

        parametresMenuItem.addActionListener(e -> showParametres());
        quitterMenuItem.addActionListener(e -> System.exit(0));

        fichierMenu.add(parametresMenuItem);
        fichierMenu.add(exporterMenuItem);
        fichierMenu.addSeparator();
        fichierMenu.add(quitterMenuItem);

        // Menu Rapports avec icônes
        JMenu rapportsMenu = new JMenu("Rapports");
        JMenuItem ventesJourMenuItem = new JMenuItem("Ventes du jour", createLargeIcon(MaterialDesign.MDI_CHART_BAR));
        JMenuItem stocksMenuItem = new JMenuItem("État des stocks", createLargeIcon(MaterialDesign.MDI_CLIPBOARD_TEXT));
        JMenuItem creancesMenuItem = new JMenuItem("État des créances", createLargeIcon(MaterialDesign.MDI_WALLET_MEMBERSHIP));
        JMenuItem caisseMenuItem = new JMenuItem("Journal de caisse", createLargeIcon(MaterialDesign.MDI_CASH));

        // Ajout des gestionnaires d'événements pour les rapports
        ventesJourMenuItem.addActionListener(e -> genererRapportVentesJour());
        stocksMenuItem.addActionListener(e -> genererRapportStocks());
        creancesMenuItem.addActionListener(e -> genererRapportCreances());
        caisseMenuItem.addActionListener(e -> genererRapportCaisse());

        rapportsMenu.add(ventesJourMenuItem);
        rapportsMenu.add(stocksMenuItem);
        rapportsMenu.add(creancesMenuItem);
        rapportsMenu.add(caisseMenuItem);

        menuBar.add(fichierMenu);
        menuBar.add(rapportsMenu);

        fichierMenu.setFont(new Font(fichierMenu.getFont().getName(), Font.BOLD, 13));
        rapportsMenu.setFont(new Font(rapportsMenu.getFont().getName(), Font.BOLD, 13));

        return menuBar;
    }

    private void genererRapportVentesJour() {
        try {
            venteController.chargerVentes();
            LocalDate aujourd'hui = LocalDate.now();

            List<Vente> ventesJour = venteController.getVentes().stream()
                .filter(v -> v.getDate().toLocalDate().equals(aujourd'hui))
                .collect(Collectors.toList());

            String nomFichier = "rapport_ventes_" + aujourd'hui + ".pdf";
            PDFGenerator.genererRapportVentes(ventesJour, nomFichier);

            afficherMessageSuccess("Rapport généré avec succès", 
                "Le rapport des ventes a été généré dans le fichier : " + nomFichier);

            // Ouvrir le fichier
            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la génération du rapport", e.getMessage());
        }
    }

    private void genererRapportStocks() {
        try {
            produitController.chargerProduits();
            String nomFichier = "rapport_stocks_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportStocks(produitController.getProduits(), nomFichier);

            afficherMessageSuccess("Rapport généré avec succès", 
                "Le rapport des stocks a été généré dans le fichier : " + nomFichier);

            // Ouvrir le fichier
            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la génération du rapport", e.getMessage());
        }
    }

    private void genererRapportCreances() {
        try {
            clientController.chargerClients();
            String nomFichier = "rapport_creances_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportCreances(clientController.getClients(), nomFichier);

            afficherMessageSuccess("Rapport généré avec succès", 
                "Le rapport des créances a été généré dans le fichier : " + nomFichier);

            // Ouvrir le fichier
            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la génération du rapport", e.getMessage());
        }
    }

    private void genererRapportCaisse() {
        try {
            caisseController.chargerMouvements();
            String nomFichier = "rapport_caisse_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportCaisse(caisseController.getMouvements(), nomFichier);

            afficherMessageSuccess("Rapport généré avec succès", 
                "Le rapport de caisse a été généré dans le fichier : " + nomFichier);

            // Ouvrir le fichier
            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la génération du rapport", e.getMessage());
        }
    }

    private void ouvrirFichierPDF(String nomFichier) {
        try {
            File file = new File(nomFichier);
            if (file.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
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

    private JToggleButton createNavigationButton(String text, MaterialDesign iconCode) {
        JToggleButton button = new JToggleButton(text);

        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        button.setIcon(icon);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);
        button.setMargin(new Insets(6, 10, 6, 10));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(130, 32));
        button.setMaximumSize(new Dimension(130, 32));
        button.setMinimumSize(new Dimension(130, 32));
        button.setFont(new Font(button.getFont().getName(), Font.BOLD, 12));
        button.setForeground(new Color(50, 50, 50));

        return button;
    }

    private FontIcon createLargeIcon(MaterialDesign iconCode) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(20);
        return icon;
    }

    private void showParametres() {
        Window window = SwingUtilities.getWindowAncestor(mainPanel);
        JDialog dialog;
        if (window instanceof Frame) {
            dialog = new JDialog((Frame) window, "Paramètres", true);
        } else if (window instanceof Dialog) {
            dialog = new JDialog((Dialog) window, "Paramètres", true);
        } else {
            dialog = new JDialog();
            dialog.setTitle("Paramètres");
            dialog.setModal(true);
        }

        dialog.setContentPane(new ConfigurationViewSwing().getMainPanel());
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}