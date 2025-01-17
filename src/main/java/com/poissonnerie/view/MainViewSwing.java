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

        // Panel de navigation vertical à gauche
        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setBackground(new Color(245, 246, 247));
        navigationPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        navigationPanel.setPreferredSize(new Dimension(200, 0));

        // Création des boutons de navigation avec icônes
        JPanel[] views = {
            new ProduitViewSwing().getMainPanel(),
            new VenteViewSwing().getMainPanel(),
            new ClientViewSwing().getMainPanel(),
            new CaisseViewSwing().getMainPanel()
        };

        String[] viewNames = {"Produits", "Ventes", "Clients", "Caisse"};
        MaterialDesign[] icons = {
            MaterialDesign.MDI_PACKAGE,
            MaterialDesign.MDI_CART,
            MaterialDesign.MDI_ACCOUNT_MULTIPLE,
            MaterialDesign.MDI_CASH_100
        };

        ButtonGroup buttonGroup = new ButtonGroup();

        for (int i = 0; i < viewNames.length; i++) {
            JToggleButton navButton = createNavigationButton(viewNames[i], icons[i]);
            final String cardName = viewNames[i];
            final int index = i;

            navButton.addActionListener(e -> cardLayout.show(contentPanel, cardName));
            buttonGroup.add(navButton);
            navigationPanel.add(navButton);
            navigationPanel.add(Box.createVerticalStrut(10));

            contentPanel.add(views[index], cardName);

            // Sélectionner le premier bouton par défaut
            if (i == 0) {
                navButton.setSelected(true);
            }
        }

        mainPanel.add(navigationPanel, BorderLayout.WEST);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
    }

    private JToggleButton createNavigationButton(String text, MaterialDesign iconCode) {
        JToggleButton button = new JToggleButton(text);

        // Configuration de l'icône
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(24);
        button.setIcon(icon);

        // Style du bouton
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMargin(new Insets(10, 15, 10, 15));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(180, 50));
        button.setMaximumSize(new Dimension(180, 50));
        button.setMinimumSize(new Dimension(180, 50));

        return button;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Fichier
        JMenu fichierMenu = new JMenu("Fichier");
        JMenuItem parametresMenuItem = new JMenuItem("Paramètres", 
            FontIcon.of(MaterialDesign.MDI_SETTINGS, 16));
        JMenuItem exporterMenuItem = new JMenuItem("Exporter les données", 
            FontIcon.of(MaterialDesign.MDI_EXPORT, 16));
        JMenuItem quitterMenuItem = new JMenuItem("Quitter", 
            FontIcon.of(MaterialDesign.MDI_EXIT_TO_APP, 16));

        parametresMenuItem.addActionListener(e -> showParametres());
        quitterMenuItem.addActionListener(e -> System.exit(0));

        fichierMenu.add(parametresMenuItem);
        fichierMenu.add(exporterMenuItem);
        fichierMenu.addSeparator();
        fichierMenu.add(quitterMenuItem);

        // Menu Rapports avec icônes
        JMenu rapportsMenu = new JMenu("Rapports");
        JMenuItem ventesJourMenuItem = new JMenuItem("Ventes du jour", 
            FontIcon.of(MaterialDesign.MDI_CHART_BAR, 16));
        JMenuItem stocksMenuItem = new JMenuItem("État des stocks", 
            FontIcon.of(MaterialDesign.MDI_CLIPBOARD_TEXT, 16));
        JMenuItem creancesMenuItem = new JMenuItem("État des créances", 
            FontIcon.of(MaterialDesign.MDI_WALLET_MEMBERSHIP, 16));
        JMenuItem caisseMenuItem = new JMenuItem("Journal de caisse", 
            FontIcon.of(MaterialDesign.MDI_CASH, 16));

        rapportsMenu.add(ventesJourMenuItem);
        rapportsMenu.add(stocksMenuItem);
        rapportsMenu.add(creancesMenuItem);
        rapportsMenu.add(caisseMenuItem);

        menuBar.add(fichierMenu);
        menuBar.add(rapportsMenu);

        return menuBar;
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