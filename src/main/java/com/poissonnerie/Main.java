package com.poissonnerie;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainViewSwing;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("Démarrage de l'application...");

                // Set Look and Feel
                FlatMaterialLighterIJTheme.setup();

                // Configuration du style global
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("ProgressBar.arc", 8);
                UIManager.put("TextComponent.arc", 8);

                // Couleurs modernes selon le design
                Color primaryColor = new Color(0, 135, 136); // Vert pour l'en-tête
                Color sidebarColor = new Color(51, 51, 75); // Couleur sombre pour la barre latérale
                Color primaryTextColor = Color.WHITE;
                Color darkTextColor = new Color(33, 33, 33);
                Color backgroundColor = new Color(245, 245, 245);

                // Configuration des boutons de navigation avec style moderne
                UIManager.put("ToggleButton.background", sidebarColor);
                UIManager.put("ToggleButton.foreground", Color.WHITE);
                UIManager.put("ToggleButton.select", primaryColor);
                UIManager.put("ToggleButton.selectedForeground", Color.WHITE);
                UIManager.put("ToggleButton.hoverBackground", sidebarColor.brighter());
                UIManager.put("ToggleButton.font", new Font("Segoe UI", Font.PLAIN, 14));
                UIManager.put("ToggleButton.margin", new Insets(12, 20, 12, 20));
                UIManager.put("ToggleButton.focusable", false);

                // Configuration des boutons standards
                UIManager.put("Button.background", primaryColor);
                UIManager.put("Button.foreground", primaryTextColor);
                UIManager.put("Button.hoverBackground", primaryColor.darker());
                UIManager.put("Button.focusedBackground", primaryColor.darker());
                UIManager.put("Button.pressedBackground", primaryColor.darker());
                UIManager.put("Button.selectedBackground", primaryColor.darker());

                // Configuration des menus et en-tête
                UIManager.put("MenuBar.background", primaryColor);
                UIManager.put("MenuBar.foreground", primaryTextColor);
                UIManager.put("Menu.foreground", primaryTextColor);
                UIManager.put("Menu.selectionBackground", primaryColor.darker());
                UIManager.put("Menu.selectionForeground", Color.WHITE);
                UIManager.put("Menu.font", new Font("Segoe UI", Font.BOLD, 14));

                // Style des panneaux et composants
                UIManager.put("Panel.background", backgroundColor);
                UIManager.put("Label.foreground", darkTextColor);
                UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("TextField.foreground", darkTextColor);
                UIManager.put("TextField.background", new Color(255, 255, 255));
                UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("ComboBox.foreground", darkTextColor);
                UIManager.put("ComboBox.background", new Color(255, 255, 255));
                UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("TextArea.foreground", darkTextColor);
                UIManager.put("TextArea.background", new Color(255, 255, 255));
                UIManager.put("TextArea.font", new Font("Segoe UI", Font.PLAIN, 13));


                // Marges et padding
                UIManager.put("Button.margin", new Insets(8, 16, 8, 16));
                UIManager.put("TabbedPane.contentBorderInsets", new Insets(10, 10, 10, 10));

                // Style des tableaux
                UIManager.put("Table.showHorizontalLines", true);
                UIManager.put("Table.showVerticalLines", true);
                UIManager.put("Table.gridColor", new Color(220, 220, 220));
                UIManager.put("Table.selectionBackground", new Color(207, 216, 233));
                UIManager.put("Table.selectionForeground", new Color(33, 33, 33));
                UIManager.put("Table.background", new Color(250, 250, 250));
                UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("TableHeader.background", new Color(235, 238, 241));
                UIManager.put("TableHeader.foreground", darkTextColor);
                UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));

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