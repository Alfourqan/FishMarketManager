package com.poissonnerie;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainViewSwing;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static JFrame mainFrame;

    public static void main(String[] args) {
        // Configuration système
        System.setProperty("java.awt.headless", "false");
        System.setProperty("sun.java2d.opengl", "true");

        LOGGER.info("Démarrage de l'application...");

        try {
            // Initialisation de la base de données
            DatabaseManager.initDatabase();
            LOGGER.info("Base de données initialisée");

            // Configuration de l'interface graphique
            SwingUtilities.invokeAndWait(() -> {
                try {
                    // Installation du thème
                    FlatMaterialLighterIJTheme.setup();
                    LOGGER.info("Thème configuré");

                    // Création de la fenêtre principale
                    mainFrame = new JFrame("Gestion Poissonnerie");
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    // Configuration de la taille
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    int width = Math.min(1200, screenSize.width - 100);
                    int height = Math.min(800, screenSize.height - 100);
                    mainFrame.setSize(width, height);
                    mainFrame.setLocationRelativeTo(null);

                    // Ajout du contenu
                    MainViewSwing mainView = new MainViewSwing();
                    mainFrame.setContentPane(mainView.getMainPanel());

                    LOGGER.info("Interface principale créée");

                    // Affichage
                    mainFrame.setVisible(true);
                    mainFrame.toFront();
                    mainFrame.requestFocus();

                    mainFrame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            try {
                                DatabaseManager.closeConnections();
                                mainFrame.dispose();
                                System.exit(0);
                            } catch (Exception ex) {
                                LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture", ex);
                                System.exit(1);
                            }
                        }
                    });


                    LOGGER.info("Interface affichée");

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de l'interface", e);
                    showError("Erreur d'initialisation", e);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale", e);
            showError("Erreur fatale", e);
        }
    }

    private static void showError(String title, Exception e) {
        String message = String.format("%s: %s", title, e.getMessage());
        System.err.println(message);
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message, "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }
}