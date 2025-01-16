package com.poissonnerie.view;

import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView {
    private final BorderPane root;
    private final TabPane tabPane;

    public MainView() {
        root = new BorderPane();
        tabPane = new TabPane();
        
        initializeComponents();
    }

    private void initializeComponents() {
        // Onglet Produits
        Tab produitsTab = new Tab("Produits");
        produitsTab.setContent(new ProduitView().getRoot());
        produitsTab.setClosable(false);

        // Onglet Ventes
        Tab ventesTab = new Tab("Ventes");
        ventesTab.setContent(new VenteView().getRoot());
        ventesTab.setClosable(false);

        // Onglet Clients
        Tab clientsTab = new Tab("Clients");
        clientsTab.setContent(new ClientView().getRoot());
        clientsTab.setClosable(false);

        tabPane.getTabs().addAll(produitsTab, ventesTab, clientsTab);
        
        // Menu
        MenuBar menuBar = createMenuBar();
        
        root.setTop(menuBar);
        root.setCenter(tabPane);
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Menu Fichier
        Menu fichierMenu = new Menu("Fichier");
        MenuItem exporterMenuItem = new MenuItem("Exporter les données");
        MenuItem quitterMenuItem = new MenuItem("Quitter");
        quitterMenuItem.setOnAction(e -> System.exit(0));
        fichierMenu.getItems().addAll(exporterMenuItem, new SeparatorMenuItem(), quitterMenuItem);
        
        // Menu Rapports
        Menu rapportsMenu = new Menu("Rapports");
        MenuItem ventesJourMenuItem = new MenuItem("Ventes du jour");
        MenuItem stocksMenuItem = new MenuItem("État des stocks");
        MenuItem creancesMenuItem = new MenuItem("État des créances");
        rapportsMenu.getItems().addAll(ventesJourMenuItem, stocksMenuItem, creancesMenuItem);
        
        menuBar.getMenus().addAll(fichierMenu, rapportsMenu);
        
        return menuBar;
    }

    public BorderPane getRoot() {
        return root;
    }
}
