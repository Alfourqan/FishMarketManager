package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

public class MainViewSwing {
    private final JPanel mainPanel;
    private final JTabbedPane tabbedPane;

    public MainViewSwing() {
        mainPanel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();

        initializeComponents();
    }

    private void initializeComponents() {
        // Menu Bar
        JMenuBar menuBar = createMenuBar();
        mainPanel.add(menuBar, BorderLayout.NORTH);

        // Tabs avec icônes
        FontIcon productsIcon = FontIcon.of(MaterialDesign.MDI_PACKAGE);
        productsIcon.setIconSize(20);
        FontIcon salesIcon = FontIcon.of(MaterialDesign.MDI_CART);
        salesIcon.setIconSize(20);
        FontIcon clientsIcon = FontIcon.of(MaterialDesign.MDI_ACCOUNT_MULTIPLE);
        clientsIcon.setIconSize(20);
        FontIcon cashIcon = FontIcon.of(MaterialDesign.MDI_CASH_100);
        cashIcon.setIconSize(20);

        tabbedPane.addTab("Produits", productsIcon, new ProduitViewSwing().getMainPanel());
        tabbedPane.addTab("Ventes", salesIcon, new VenteViewSwing().getMainPanel());
        tabbedPane.addTab("Clients", clientsIcon, new ClientViewSwing().getMainPanel());
        tabbedPane.addTab("Caisse", cashIcon, new CaisseViewSwing().getMainPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
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

        // Gestionnaires d'événements pour les rapports
        caisseMenuItem.addActionListener(e -> {
            tabbedPane.setSelectedComponent(
                tabbedPane.getComponentAt(tabbedPane.indexOfTab("Caisse"))
            );
        });

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