package com.poissonnerie;

import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Initialisation de la base de données
        DatabaseManager.initDatabase();
        
        // Configuration de la fenêtre principale
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView.getRoot(), 1200, 800);
        primaryStage.setTitle("Gestion Poissonnerie");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
