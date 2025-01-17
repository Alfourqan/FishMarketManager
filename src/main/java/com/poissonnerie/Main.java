package com.poissonnerie;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainViewSwing;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;

public class Main {
    public static void main(String[] args) {
        // Pour garantir que l'interface graphique s'exécute dans l'EDT
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("Démarrage de l'application...");

                // Set Look and Feel
                FlatMaterialLighterIJTheme.setup();

                // Configuration du style global
                UIManager.put("Button.arc", 10);
                UIManager.put("Component.arc", 10);
                UIManager.put("ProgressBar.arc", 10);
                UIManager.put("TextComponent.arc", 10);

                // Couleurs modernes
                UIManager.put("Button.background", new Color(63, 81, 181));
                UIManager.put("Button.foreground", Color.WHITE);
                UIManager.put("Button.hoverBackground", new Color(92, 107, 192));
                UIManager.put("Button.pressedBackground", new Color(48, 63, 159));

                // Marges et padding
                UIManager.put("Button.margin", new Insets(8, 16, 8, 16));
                UIManager.put("TabbedPane.contentBorderInsets", new Insets(10, 10, 10, 10));
                UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16));

                // Style des tableaux
                UIManager.put("Table.showHorizontalLines", true);
                UIManager.put("Table.showVerticalLines", true);
                UIManager.put("Table.gridColor", new Color(224, 224, 224));
                UIManager.put("Table.selectionBackground", new Color(197, 202, 233));
                UIManager.put("Table.selectionForeground", new Color(33, 33, 33));

                System.out.println("Look and Feel configuré.");

                // Initialisation de la base de données
                System.out.println("Initialisation de la base de données...");
                DatabaseManager.initDatabase();
                System.out.println("Base de données initialisée avec succès.");

                // Création et affichage de la fenêtre principale
                JFrame frame = new JFrame("Gestion Poissonnerie");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 800);
                System.out.println("Fenêtre principale créée.");

                MainViewSwing mainView = new MainViewSwing();
                frame.setContentPane(mainView.getMainPanel());
                System.out.println("Vue principale configurée.");

                // Centrer la fenêtre
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                System.out.println("Application démarrée avec succès.");

                // Ajouter un hook pour fermer proprement la connexion à la base de données
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Fermeture de l'application...");
                    DatabaseManager.closeConnection();
                    System.out.println("Connexion à la base de données fermée.");
                }));

            } catch (Exception e) {
                e.printStackTrace();
                String message = "Erreur lors du démarrage: " + e.getMessage() + 
                               "\nType: " + e.getClass().getSimpleName();
                System.err.println(message);
                JOptionPane.showMessageDialog(null, 
                    message,
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}