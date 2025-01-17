package com.poissonnerie;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainViewSwing;

public class Main {
    public static void main(String[] args) {
        try {
            // Test explicite de la connexion à la base de données
            System.out.println("Test de la connexion à la base de données...");
            DatabaseManager.testConnection();

            // Initialisation de la base de données
            System.out.println("Démarrage de l'application...");
            DatabaseManager.initDatabase();
            System.out.println("Base de données initialisée avec succès");

            // Pour garantir que l'interface graphique s'exécute dans l'EDT
            SwingUtilities.invokeLater(() -> {
                try {
                    // Set Look and Feel
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    System.out.println("Look and Feel configuré");

                    // Création et affichage de la fenêtre principale
                    JFrame frame = new JFrame("Gestion Poissonnerie");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setSize(1200, 800);

                    MainViewSwing mainView = new MainViewSwing();
                    frame.setContentPane(mainView.getMainPanel());

                    // Centrer la fenêtre
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    System.out.println("Interface graphique lancée avec succès");
                } catch (Exception e) {
                    String errorMessage = "Erreur lors du démarrage de l'interface: " + e.getMessage();
                    System.err.println(errorMessage);
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, 
                        errorMessage,
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            });
        } catch (Exception e) {
            String errorMessage = "Erreur fatale lors de l'initialisation: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                errorMessage,
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}