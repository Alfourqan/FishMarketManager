package com.poissonnerie.view;

import com.poissonnerie.controller.VenteController;
import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.*;
import com.poissonnerie.util.PDFGenerator;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class VenteView {
    private final VBox root;
    private final VenteController venteController;
    private final ProduitController produitController;
    private final ClientController clientController;
    private final TableView<Vente> tableVentes;
    private final TableView<Vente.LigneVente> tablePanier;
    private final ObservableList<Vente.LigneVente> panier;
    private ComboBox<Client> clientCombo;
    private CheckBox creditCheck;
    private Label totalLabel;

    public VenteView() {
        root = new VBox(10);
        venteController = new VenteController();
        produitController = new ProduitController();
        clientController = new ClientController();
        tableVentes = new TableView<>();
        tablePanier = new TableView<>();
        panier = FXCollections.observableArrayList();
        
        initializeComponents();
        loadData();
    }

    private void loadData() {
        produitController.chargerProduits();
        clientController.chargerClients();
        venteController.chargerVentes();
    }

    private void initializeComponents() {
        root.setPadding(new Insets(10));

        // Section nouvelle vente
        TitledPane nouvelleVentePane = createNouvelleVentePane();
        
        // Section historique des ventes
        TitledPane historiquePane = createHistoriquePane();

        root.getChildren().addAll(nouvelleVentePane, historiquePane);
    }

    private TitledPane createNouvelleVentePane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // En-tête de vente
        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);

        clientCombo = new ComboBox<>(clientController.getClients());
        clientCombo.setPromptText("Sélectionner un client");
        creditCheck = new CheckBox("Vente à crédit");
        
        creditCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            clientCombo.setDisable(!newVal);
            if (!newVal) {
                clientCombo.getSelectionModel().clearSelection();
            }
        });
        
        clientCombo.setDisable(true);

        header.add(new Label("Client:"), 0, 0);
        header.add(clientCombo, 1, 0);
        header.add(creditCheck, 2, 0);

        // Sélection des produits
        GridPane produitSelection = new GridPane();
        produitSelection.setHgap(10);
        produitSelection.setVgap(10);

        ComboBox<Produit> produitCombo = new ComboBox<>(produitController.getProduits());
        produitCombo.setPromptText("Sélectionner un produit");
        TextField quantiteField = new TextField();
        quantiteField.setPromptText("Quantité");
        Button ajouterBtn = new Button("Ajouter au panier");

        produitSelection.add(new Label("Produit:"), 0, 0);
        produitSelection.add(produitCombo, 1, 0);
        produitSelection.add(new Label("Quantité:"), 2, 0);
        produitSelection.add(quantiteField, 3, 0);
        produitSelection.add(ajouterBtn, 4, 0);

        // Table du panier
        TableColumn<Vente.LigneVente, String> produitCol = new TableColumn<>("Produit");
        produitCol.setCellValueFactory(cellData -> 
            cellData.getValue().getProduit().nomProperty());

        TableColumn<Vente.LigneVente, Number> quantiteCol = new TableColumn<>("Quantité");
        quantiteCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getQuantite()));

        TableColumn<Vente.LigneVente, Number> prixCol = new TableColumn<>("Prix unitaire");
        prixCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getPrixUnitaire()));

        TableColumn<Vente.LigneVente, Number> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleDoubleProperty(
                cellData.getValue().getQuantite() * cellData.getValue().getPrixUnitaire()));

        tablePanier.getColumns().addAll(produitCol, quantiteCol, prixCol, totalCol);
        tablePanier.setItems(panier);

        // Total et boutons de validation
        HBox footer = new HBox(10);
        totalLabel = new Label("Total: 0.00 €");
        Button validerBtn = new Button("Valider la vente");
        Button annulerBtn = new Button("Annuler");

        footer.getChildren().addAll(totalLabel, validerBtn, annulerBtn);

        // Gestionnaires d'événements
        ajouterBtn.setOnAction(e -> {
            Produit produit = produitCombo.getValue();
            if (produit != null) {
                try {
                    int quantite = Integer.parseInt(quantiteField.getText());
                    if (quantite > 0 && quantite <= produit.getStock()) {
                        Vente.LigneVente ligne = new Vente.LigneVente(
                            produit,
                            quantite,
                            produit.getPrix()
                        );
                        panier.add(ligne);
                        updateTotal();
                        produitCombo.getSelectionModel().clearSelection();
                        quantiteField.clear();
                    }
                } catch (NumberFormatException ex) {
                    // Ignorer les entrées non numériques
                }
            }
        });

        validerBtn.setOnAction(e -> {
            if (!panier.isEmpty()) {
                Vente vente = new Vente(
                    0,
                    LocalDateTime.now(),
                    clientCombo.getValue(),
                    creditCheck.isSelected(),
                    calculateTotal()
                );
                vente.setLignes(new ArrayList<>(panier));
                
                venteController.enregistrerVente(vente);
                
                // Génération du PDF
                PDFGenerator.genererFacture(vente, "facture_" + vente.getId() + ".pdf");
                
                // Réinitialisation du formulaire
                resetForm();
            }
        });

        annulerBtn.setOnAction(e -> resetForm());

        content.getChildren().addAll(header, produitSelection, tablePanier, footer);
        
        TitledPane pane = new TitledPane("Nouvelle Vente", content);
        pane.setCollapsible(false);
        return pane;
    }

    private TitledPane createHistoriquePane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        // Configuration de la table des ventes
        TableColumn<Vente, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        TableColumn<Vente, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getClient() != null ? cellData.getValue().getClient().getNom() : "Vente comptant"));

        TableColumn<Vente, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().isCredit() ? "Crédit" : "Comptant"));

        TableColumn<Vente, Number> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cellData -> cellData.getValue().totalProperty());

        tableVentes.getColumns().addAll(dateCol, clientCol, typeCol, totalCol);
        tableVentes.setItems(venteController.getVentes());

        content.getChildren().add(tableVentes);
        
        TitledPane pane = new TitledPane("Historique des Ventes", content);
        pane.setCollapsible(false);
        return pane;
    }

    private void updateTotal() {
        totalLabel.setText(String.format("Total: %.2f €", calculateTotal()));
    }

    private double calculateTotal() {
        return panier.stream()
            .mapToDouble(ligne -> ligne.getQuantite() * ligne.getPrixUnitaire())
            .sum();
    }

    private void resetForm() {
        clientCombo.getSelectionModel().clearSelection();
        creditCheck.setSelected(false);
        panier.clear();
        updateTotal();
    }

    public VBox getRoot() {
        return root;
    }
}
