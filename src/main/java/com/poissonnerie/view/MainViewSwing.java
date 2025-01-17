package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

public class MainViewSwing {
    private final JPanel mainPanel;
    private final CardLayout cardLayout;
    private final JPanel contentPanel;

    public MainViewSwing() {
        mainPanel = new JPanel(new BorderLayout());
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        initializeComponents();
    }

    private void initializeComponents() {
        // Menu Bar
        JMenuBar menuBar = createMenuBar();
        mainPanel.add(menuBar, BorderLayout.NORTH);

        // Panel de navigation vertical à gauche avec plus d'espace
        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBackground(new Color(220, 224, 228)); // Gris clair
        navigationPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        navigationPanel.setPreferredSize(new Dimension(160, 0));

        // Création des boutons de navigation avec icônes
        JPanel[] views = {
            new ProduitViewSwing().getMainPanel(),
            new VenteViewSwing().getMainPanel(),
            new ClientViewSwing().getMainPanel(),
            new CaisseViewSwing().getMainPanel(),
            new InventaireViewSwing().getMainPanel()  // Ajout de la vue inventaire
        };

        String[] viewNames = {"Produits", "Ventes", "Clients", "Caisse", "Inventaire"};  // Ajout du nom
        MaterialDesign[] icons = {
            MaterialDesign.MDI_PACKAGE_VARIANT,
            MaterialDesign.MDI_CART,
            MaterialDesign.MDI_ACCOUNT_MULTIPLE,
            MaterialDesign.MDI_CASH_MULTIPLE,
            MaterialDesign.MDI_CLIPBOARD_TEXT  // Changed from MDI_CLIPBOARD_CHECK_OUTLINE to MDI_CLIPBOARD_TEXT
        };

        ButtonGroup buttonGroup = new ButtonGroup();

        // Ajouter un padding en haut
        navigationPanel.add(Box.createVerticalStrut(5));

        for (int i = 0; i < viewNames.length; i++) {
            JToggleButton navButton = createNavigationButton(viewNames[i], icons[i]);
            final String cardName = viewNames[i];
            final int index = i;

            navButton.addActionListener(e -> cardLayout.show(contentPanel, cardName));
            buttonGroup.add(navButton);
            navigationPanel.add(navButton);
            navigationPanel.add(Box.createVerticalStrut(5)); // Moins d'espace entre les boutons

            contentPanel.add(views[index], cardName);

            // Sélectionner le premier bouton par défaut
            if (i == 0) {
                navButton.setSelected(true);
            }
        }

        // Ajouter un padding en bas
        navigationPanel.add(Box.createVerticalStrut(5));

        mainPanel.add(navigationPanel, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
    }

    private JToggleButton createNavigationButton(String text, MaterialDesign iconCode) {
        JToggleButton button = new JToggleButton(text);

        // Configuration de l'icône avec une taille plus petite
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(18);
        button.setIcon(icon);

        // Style du bouton amélioré
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10); // Espace entre l'icône et le texte
        button.setMargin(new Insets(6, 10, 6, 10)); // Marges réduites
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(130, 32)); // Dimensions réduites
        button.setMaximumSize(new Dimension(130, 32));
        button.setMinimumSize(new Dimension(130, 32));
        button.setFont(new Font(button.getFont().getName(), Font.BOLD, 12)); // Police plus petite

        // Meilleur contraste pour le texte
        button.setForeground(new Color(50, 50, 50));

        return button;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Fichier avec icônes plus grandes
        JMenu fichierMenu = new JMenu("Fichier");
        JMenuItem parametresMenuItem = new JMenuItem("Paramètres", 
            createLargeIcon(MaterialDesign.MDI_SETTINGS));
        JMenuItem exporterMenuItem = new JMenuItem("Exporter les données", 
            createLargeIcon(MaterialDesign.MDI_EXPORT));
        JMenuItem quitterMenuItem = new JMenuItem("Quitter", 
            createLargeIcon(MaterialDesign.MDI_EXIT_TO_APP));

        parametresMenuItem.addActionListener(e -> showParametres());
        quitterMenuItem.addActionListener(e -> System.exit(0));

        fichierMenu.add(parametresMenuItem);
        fichierMenu.add(exporterMenuItem);
        fichierMenu.addSeparator();
        fichierMenu.add(quitterMenuItem);

        // Menu Rapports avec icônes plus grandes
        JMenu rapportsMenu = new JMenu("Rapports");
        JMenuItem ventesJourMenuItem = new JMenuItem("Ventes du jour", 
            createLargeIcon(MaterialDesign.MDI_CHART_BAR));
        JMenuItem stocksMenuItem = new JMenuItem("État des stocks", 
            createLargeIcon(MaterialDesign.MDI_CLIPBOARD_TEXT));
        JMenuItem creancesMenuItem = new JMenuItem("État des créances", 
            createLargeIcon(MaterialDesign.MDI_WALLET_MEMBERSHIP));
        JMenuItem caisseMenuItem = new JMenuItem("Journal de caisse", 
            createLargeIcon(MaterialDesign.MDI_CASH));

        rapportsMenu.add(ventesJourMenuItem);
        rapportsMenu.add(stocksMenuItem);
        rapportsMenu.add(creancesMenuItem);
        rapportsMenu.add(caisseMenuItem);

        menuBar.add(fichierMenu);
        menuBar.add(rapportsMenu);

        // Style du menu
        fichierMenu.setFont(new Font(fichierMenu.getFont().getName(), Font.BOLD, 13));
        rapportsMenu.setFont(new Font(rapportsMenu.getFont().getName(), Font.BOLD, 13));

        return menuBar;
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