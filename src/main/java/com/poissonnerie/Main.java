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
                UIManager.put("Button.arc", 10);
                UIManager.put("Component.arc", 10);
                UIManager.put("ProgressBar.arc", 10);
                UIManager.put("TextComponent.arc", 10);

                // Couleurs modernes avec meilleur contraste
                Color primaryColor = new Color(25, 118, 210); // Bleu plus foncé
                Color primaryLightColor = new Color(64, 196, 255); // Bleu clair pour hover
                Color primaryTextColor = Color.WHITE;
                Color darkTextColor = new Color(33, 33, 33); // Texte foncé pour meilleur contraste

                // Configuration des boutons de navigation
                UIManager.put("ToggleButton.background", new Color(245, 246, 247));
                UIManager.put("ToggleButton.foreground", darkTextColor);
                UIManager.put("ToggleButton.select", primaryColor);
                UIManager.put("ToggleButton.selectedForeground", Color.WHITE);
                UIManager.put("ToggleButton.font", new Font(UIManager.getFont("ToggleButton.font").getName(), Font.BOLD, 14));
                UIManager.put("ToggleButton.margin", new Insets(10, 15, 10, 15));
                UIManager.put("ToggleButton.focusable", false);

                // Configuration des boutons standards
                UIManager.put("Button.background", primaryColor);
                UIManager.put("Button.foreground", primaryTextColor);
                UIManager.put("Button.hoverBackground", primaryLightColor);
                UIManager.put("Button.focusedBackground", primaryLightColor);
                UIManager.put("Button.pressedBackground", primaryColor.darker());
                UIManager.put("Button.selectedBackground", primaryColor.darker());
                UIManager.put("Button.default.background", primaryColor);
                UIManager.put("Button.default.foreground", primaryTextColor);


                // Configuration des onglets
                UIManager.put("TabbedPane.foreground", darkTextColor);
                UIManager.put("TabbedPane.selectedForeground", primaryColor);
                UIManager.put("TabbedPane.selectedBackground", new Color(232, 240, 254));
                UIManager.put("TabbedPane.unselectedBackground", new Color(245, 246, 247));
                UIManager.put("TabbedPane.contentAreaColor", Color.WHITE);
                UIManager.put("TabbedPane.focus", primaryColor);
                UIManager.put("TabbedPane.font", new Font(UIManager.getFont("TabbedPane.font").getName(), Font.BOLD, 13));
                UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(3, 3, 3, 3));
                UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16));

                // Configuration des menus
                UIManager.put("MenuBar.foreground", darkTextColor);
                UIManager.put("MenuBar.background", Color.WHITE);
                UIManager.put("Menu.foreground", darkTextColor);
                UIManager.put("Menu.selectionBackground", primaryColor);
                UIManager.put("Menu.selectionForeground", Color.WHITE);
                UIManager.put("Menu.font", new Font(UIManager.getFont("Menu.font").getName(), Font.PLAIN, 13));
                UIManager.put("MenuItem.foreground", darkTextColor);
                UIManager.put("MenuItem.selectionBackground", primaryColor);
                UIManager.put("MenuItem.selectionForeground", Color.WHITE);
                UIManager.put("MenuItem.font", new Font(UIManager.getFont("MenuItem.font").getName(), Font.PLAIN, 13));

                // Couleurs des textes et composants
                UIManager.put("Label.foreground", darkTextColor);
                UIManager.put("Label.font", new Font(UIManager.getFont("Label.font").getName(), Font.PLAIN, 13));
                UIManager.put("TextField.foreground", darkTextColor);
                UIManager.put("TextField.font", new Font(UIManager.getFont("TextField.font").getName(), Font.PLAIN, 13));
                UIManager.put("ComboBox.foreground", darkTextColor);
                UIManager.put("ComboBox.font", new Font(UIManager.getFont("ComboBox.font").getName(), Font.PLAIN, 13));
                UIManager.put("TextArea.foreground", darkTextColor);
                UIManager.put("TextArea.font", new Font(UIManager.getFont("TextArea.font").getName(), Font.PLAIN, 13));

                // Marges et padding
                UIManager.put("Button.margin", new Insets(8, 16, 8, 16));
                UIManager.put("TabbedPane.contentBorderInsets", new Insets(10, 10, 10, 10));

                // Style des tableaux
                UIManager.put("Table.showHorizontalLines", true);
                UIManager.put("Table.showVerticalLines", true);
                UIManager.put("Table.gridColor", new Color(224, 224, 224));
                UIManager.put("Table.selectionBackground", new Color(232, 240, 254));
                UIManager.put("Table.selectionForeground", darkTextColor);
                UIManager.put("Table.font", new Font(UIManager.getFont("Table.font").getName(), Font.PLAIN, 13));
                UIManager.put("TableHeader.background", new Color(245, 246, 247));
                UIManager.put("TableHeader.foreground", darkTextColor);
                UIManager.put("TableHeader.font", new Font(UIManager.getFont("TableHeader.font").getName(), Font.BOLD, 13));

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