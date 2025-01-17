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
        // En-tête vert avec "HOME"
        JPanel headerPanel = createHeader();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Panel de navigation vertical à gauche (barre latérale sombre)
        JPanel navigationPanel = createNavigationPanel();
        mainPanel.add(navigationPanel, BorderLayout.WEST);

        // Contenu principal
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Ajouter les vues
        addViews();
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(76, 175, 80)); // Vert
        headerPanel.setPreferredSize(new Dimension(0, 50));

        JLabel titleLabel = new JLabel("HOME");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel createNavigationPanel() {
        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBackground(new Color(33, 37, 41)); // Couleur sombre
        navigationPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        navigationPanel.setPreferredSize(new Dimension(200, 0));

        // Définir les éléments de navigation
        String[] viewNames = {
            "Produits", "Ventes", "Clients", "Factures", 
            "Fournisseurs", "Catégories", "Inventaire", "Caisse", 
            "Report", "Réglages", "Déconnexion"
        };

        MaterialDesign[] icons = {
            MaterialDesign.MDI_PACKAGE_VARIANT,
            MaterialDesign.MDI_CART,
            MaterialDesign.MDI_ACCOUNT_MULTIPLE,
            MaterialDesign.MDI_FILE_DOCUMENT,
            MaterialDesign.MDI_TRUCK_DELIVERY,
            MaterialDesign.MDI_TAG_MULTIPLE,
            MaterialDesign.MDI_CLIPBOARD_TEXT,
            MaterialDesign.MDI_CASH_MULTIPLE,
            MaterialDesign.MDI_CHART_BAR,
            MaterialDesign.MDI_SETTINGS,
            MaterialDesign.MDI_LOGOUT
        };

        ButtonGroup buttonGroup = new ButtonGroup();
        navigationPanel.add(Box.createVerticalStrut(10));

        for (int i = 0; i < viewNames.length; i++) {
            JToggleButton navButton = createNavigationButton(viewNames[i], icons[i]);
            final String cardName = viewNames[i];

            if (i < viewNames.length - 1) { // Tous sauf Déconnexion
                navButton.addActionListener(e -> cardLayout.show(contentPanel, cardName));
            } else { // Bouton Déconnexion
                navButton.addActionListener(e -> handleLogout());
            }

            buttonGroup.add(navButton);
            navigationPanel.add(navButton);
            navigationPanel.add(Box.createVerticalStrut(5));

            if (i == 0) {
                navButton.setSelected(true);
            }
        }

        return navigationPanel;
    }

    private void addViews() {
        // Ajouter les vues existantes
        contentPanel.add(new ProduitViewSwing().getMainPanel(), "Produits");
        contentPanel.add(new VenteViewSwing().getMainPanel(), "Ventes");
        contentPanel.add(new ClientViewSwing().getMainPanel(), "Clients");
        contentPanel.add(new CaisseViewSwing().getMainPanel(), "Caisse");
        contentPanel.add(new InventaireViewSwing().getMainPanel(), "Inventaire");

        // Ajouter des panels temporaires pour les nouvelles vues
        contentPanel.add(createTemporaryPanel("Factures"), "Factures");
        contentPanel.add(createTemporaryPanel("Fournisseurs"), "Fournisseurs");
        contentPanel.add(createTemporaryPanel("Catégories"), "Catégories");
        contentPanel.add(createTemporaryPanel("Report"), "Report");
        contentPanel.add(createTemporaryPanel("Réglages"), "Réglages");
    }

    private JPanel createTemporaryPanel(String name) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel("Vue " + name + " en construction", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JToggleButton createNavigationButton(String text, MaterialDesign iconCode) {
        JToggleButton button = new JToggleButton(text);

        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);
        button.setIcon(icon);

        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(15);
        button.setMargin(new Insets(8, 15, 8, 15));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(170, 40));
        button.setMaximumSize(new Dimension(170, 40));
        button.setMinimumSize(new Dimension(170, 40));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(33, 37, 41));
        button.setBorderPainted(false);
        button.setOpaque(true);

        // Style au survol et à la sélection
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.isSelected()) {
                    button.setBackground(new Color(44, 49, 54));
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!button.isSelected()) {
                    button.setBackground(new Color(33, 37, 41));
                }
            }
        });

        return button;
    }

    private void handleLogout() {
        int response = JOptionPane.showConfirmDialog(
            mainPanel,
            "Voulez-vous vraiment vous déconnecter ?",
            "Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.YES_OPTION) {
            Window window = SwingUtilities.getWindowAncestor(mainPanel);
            if (window != null) {
                window.dispose();
            }
            System.exit(0);
        }
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
            LocalDate aujourdhui = LocalDate.now();

            List<Vente> ventesJour = venteController.getVentes().stream()
                .filter(v -> v.getDate().toLocalDate().equals(aujourdhui))
                .collect(Collectors.toList());

            String nomFichier = "rapport_ventes_" + aujourdhui + ".pdf";
            PDFGenerator.genererRapportVentes(ventesJour, nomFichier);

            afficherMessageSuccess("Rapport genere avec succes", 
                "Le rapport des ventes a ete genere dans le fichier : " + nomFichier);

            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la generation du rapport", e.getMessage());
        }
    }

    private void genererRapportStocks() {
        try {
            produitController.chargerProduits();
            String nomFichier = "rapport_stocks_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportStocks(produitController.getProduits(), nomFichier);

            afficherMessageSuccess("Rapport genere avec succes", 
                "Le rapport des stocks a ete genere dans le fichier : " + nomFichier);

            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la generation du rapport", e.getMessage());
        }
    }

    private void genererRapportCreances() {
        try {
            clientController.chargerClients();
            String nomFichier = "rapport_creances_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportCreances(clientController.getClients(), nomFichier);

            afficherMessageSuccess("Rapport genere avec succes", 
                "Le rapport des creances a ete genere dans le fichier : " + nomFichier);

            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la generation du rapport", e.getMessage());
        }
    }

    private void genererRapportCaisse() {
        try {
            caisseController.chargerMouvements();
            String nomFichier = "rapport_caisse_" + LocalDate.now() + ".pdf";
            PDFGenerator.genererRapportCaisse(caisseController.getMouvements(), nomFichier);

            afficherMessageSuccess("Rapport genere avec succes", 
                "Le rapport de caisse a ete genere dans le fichier : " + nomFichier);

            ouvrirFichierPDF(nomFichier);

        } catch (Exception e) {
            afficherMessageErreur("Erreur lors de la generation du rapport", e.getMessage());
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