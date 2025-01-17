package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;

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

        // Tabs
        tabbedPane.addTab("Produits", new ProduitViewSwing().getMainPanel());
        tabbedPane.addTab("Ventes", new VenteViewSwing().getMainPanel());
        tabbedPane.addTab("Clients", new ClientViewSwing().getMainPanel());
        tabbedPane.addTab("Caisse", new CaisseViewSwing().getMainPanel()); //Added line

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Fichier
        JMenu fichierMenu = new JMenu("Fichier");
        JMenuItem parametresMenuItem = new JMenuItem("Paramètres");
        JMenuItem exporterMenuItem = new JMenuItem("Exporter les données");
        JMenuItem quitterMenuItem = new JMenuItem("Quitter");

        parametresMenuItem.addActionListener(e -> showParametres());
        quitterMenuItem.addActionListener(e -> System.exit(0));

        fichierMenu.add(parametresMenuItem);
        fichierMenu.add(exporterMenuItem);
        fichierMenu.addSeparator();
        fichierMenu.add(quitterMenuItem);

        // Menu Rapports
        JMenu rapportsMenu = new JMenu("Rapports");
        JMenuItem ventesJourMenuItem = new JMenuItem("Ventes du jour");
        JMenuItem stocksMenuItem = new JMenuItem("État des stocks");
        JMenuItem creancesMenuItem = new JMenuItem("État des créances");
        rapportsMenu.add(ventesJourMenuItem);
        rapportsMenu.add(stocksMenuItem);
        rapportsMenu.add(creancesMenuItem);

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