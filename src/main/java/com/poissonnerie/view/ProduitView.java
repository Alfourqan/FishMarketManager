package com.poissonnerie.view;

import com.poissonnerie.controller.ProduitController;
import com.poissonnerie.model.Produit;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class ProduitView {
    private final VBox root;
    private final ProduitController controller;
    private final TableView<Produit> tableProduits;

    public ProduitView() {
        root = new VBox(10);
        controller = new ProduitController();
        tableProduits = new TableView<>();
        
        initializeComponents();
        controller.chargerProduits();
    }

    private void initializeComponents() {
        root.setPadding(new Insets(10));

        // Boutons d'action
        HBox buttonBox = new HBox(10);
        Button ajouterBtn = new Button("Ajouter");
        Button modifierBtn = new Button("Modifier");
        Button supprimerBtn = new Button("Supprimer");
        buttonBox.getChildren().addAll(ajouterBtn, modifierBtn, supprimerBtn);

        // Configuration de la table
        TableColumn<Produit, String> nomCol = new TableColumn<>("Nom");
        nomCol.setCellValueFactory(cellData -> cellData.getValue().nomProperty());

        TableColumn<Produit, String> categorieCol = new TableColumn<>("Catégorie");
        categorieCol.setCellValueFactory(cellData -> cellData.getValue().categorieProperty());

        TableColumn<Produit, Number> prixCol = new TableColumn<>("Prix");
        prixCol.setCellValueFactory(cellData -> cellData.getValue().prixProperty());

        TableColumn<Produit, Number> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(cellData -> cellData.getValue().stockProperty());
        stockCol.setCellFactory(col -> new TableCell<Produit, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    Produit produit = getTableView().getItems().get(getIndex());
                    if (item.intValue() <= produit.getSeuilAlerte()) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        tableProduits.getColumns().addAll(nomCol, categorieCol, prixCol, stockCol);
        tableProduits.setItems(controller.getProduits());

        // Gestionnaires d'événements
        ajouterBtn.setOnAction(e -> showProduitDialog(null));
        modifierBtn.setOnAction(e -> {
            Produit selected = tableProduits.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showProduitDialog(selected);
            }
        });
        supprimerBtn.setOnAction(e -> {
            Produit selected = tableProduits.getSelectionModel().getSelectedItem();
            if (selected != null) {
                controller.supprimerProduit(selected);
            }
        });

        root.getChildren().addAll(buttonBox, tableProduits);
    }

    private void showProduitDialog(Produit produit) {
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle(produit == null ? "Nouveau produit" : "Modifier produit");

        // Champs du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomField = new TextField();
        ComboBox<String> categorieCombo = new ComboBox<>();
        categorieCombo.getItems().addAll("Frais", "Surgelé", "Transformé");
        TextField prixField = new TextField();
        TextField stockField = new TextField();
        TextField seuilField = new TextField();

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Catégorie:"), 0, 1);
        grid.add(categorieCombo, 1, 1);
        grid.add(new Label("Prix:"), 0, 2);
        grid.add(prixField, 1, 2);
        grid.add(new Label("Stock:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("Seuil d'alerte:"), 0, 4);
        grid.add(seuilField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Pré-remplissage si modification
        if (produit != null) {
            nomField.setText(produit.getNom());
            categorieCombo.setValue(produit.getCategorie());
            prixField.setText(String.valueOf(produit.getPrix()));
            stockField.setText(String.valueOf(produit.getStock()));
            seuilField.setText(String.valueOf(produit.getSeuilAlerte()));
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (produit == null) {
                    return new Produit(
                        0,
                        nomField.getText(),
                        categorieCombo.getValue(),
                        Double.parseDouble(prixField.getText()),
                        Integer.parseInt(stockField.getText()),
                        Integer.parseInt(seuilField.getText())
                    );
                } else {
                    produit.setNom(nomField.getText());
                    produit.setCategorie(categorieCombo.getValue());
                    produit.setPrix(Double.parseDouble(prixField.getText()));
                    produit.setStock(Integer.parseInt(stockField.getText()));
                    produit.setSeuilAlerte(Integer.parseInt(seuilField.getText()));
                    return produit;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (produit == null) {
                controller.ajouterProduit(result);
            } else {
                controller.mettreAJourProduit(result);
            }
        });
    }

    public VBox getRoot() {
        return root;
    }
}
