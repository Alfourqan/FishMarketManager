package com.poissonnerie.view;

import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.Client;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class ClientView {
    private final VBox root;
    private final ClientController controller;
    private final TableView<Client> tableClients;

    public ClientView() {
        root = new VBox(10);
        controller = new ClientController();
        tableClients = new TableView<>();
        
        initializeComponents();
        controller.chargerClients();
    }

    private void initializeComponents() {
        root.setPadding(new Insets(10));

        // Boutons d'action
        HBox buttonBox = new HBox(10);
        Button ajouterBtn = new Button("Ajouter");
        Button modifierBtn = new Button("Modifier");
        Button supprimerBtn = new Button("Supprimer");
        Button reglerCreanceBtn = new Button("Régler créance");
        buttonBox.getChildren().addAll(ajouterBtn, modifierBtn, supprimerBtn, reglerCreanceBtn);

        // Configuration de la table
        TableColumn<Client, String> nomCol = new TableColumn<>("Nom");
        nomCol.setCellValueFactory(cellData -> cellData.getValue().nomProperty());

        TableColumn<Client, String> telephoneCol = new TableColumn<>("Téléphone");
        telephoneCol.setCellValueFactory(cellData -> cellData.getValue().telephoneProperty());

        TableColumn<Client, String> adresseCol = new TableColumn<>("Adresse");
        adresseCol.setCellValueFactory(cellData -> cellData.getValue().adresseProperty());

        TableColumn<Client, Number> soldeCol = new TableColumn<>("Solde");
        soldeCol.setCellValueFactory(cellData -> cellData.getValue().soldeProperty());
        soldeCol.setCellFactory(col -> new TableCell<Client, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f €", item.doubleValue()));
                    if (item.doubleValue() > 0) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        tableClients.getColumns().addAll(nomCol, telephoneCol, adresseCol, soldeCol);
        tableClients.setItems(controller.getClients());

        // Gestionnaires d'événements
        ajouterBtn.setOnAction(e -> showClientDialog(null));
        modifierBtn.setOnAction(e -> {
            Client selected = tableClients.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showClientDialog(selected);
            }
        });
        supprimerBtn.setOnAction(e -> {
            Client selected = tableClients.getSelectionModel().getSelectedItem();
            if (selected != null) {
                controller.supprimerClient(selected);
            }
        });
        reglerCreanceBtn.setOnAction(e -> {
            Client selected = tableClients.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getSolde() > 0) {
                showReglerCreanceDialog(selected);
            }
        });

        root.getChildren().addAll(buttonBox, tableClients);
    }

    private void showClientDialog(Client client) {
        Dialog<Client> dialog = new Dialog<>();
        dialog.setTitle(client == null ? "Nouveau client" : "Modifier client");

        // Champs du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomField = new TextField();
        TextField telephoneField = new TextField();
        TextArea adresseArea = new TextArea();
        adresseArea.setPrefRowCount(3);

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Téléphone:"), 0, 1);
        grid.add(telephoneField, 1, 1);
        grid.add(new Label("Adresse:"), 0, 2);
        grid.add(adresseArea, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Pré-remplissage si modification
        if (client != null) {
            nomField.setText(client.getNom());
            telephoneField.setText(client.getTelephone());
            adresseArea.setText(client.getAdresse());
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (client == null) {
                    return new Client(
                        0,
                        nomField.getText(),
                        telephoneField.getText(),
                        adresseArea.getText(),
                        0.0
                    );
                } else {
                    client.setNom(nomField.getText());
                    client.setTelephone(telephoneField.getText());
                    client.setAdresse(adresseArea.getText());
                    return client;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (client == null) {
                controller.ajouterClient(result);
            } else {
                controller.mettreAJourClient(result);
            }
        });
    }

    private void showReglerCreanceDialog(Client client) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Régler créance");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField montantField = new TextField();
        Label soldeLabel = new Label(String.format("Solde actuel: %.2f €", client.getSolde()));

        grid.add(soldeLabel, 0, 0, 2, 1);
        grid.add(new Label("Montant à régler:"), 0, 1);
        grid.add(montantField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    return Double.parseDouble(montantField.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(montant -> {
            if (montant != null && montant > 0 && montant <= client.getSolde()) {
                controller.reglerCreance(client, montant);
            }
        });
    }

    public VBox getRoot() {
        return root;
    }
}
