package com.poissonnerie;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainViewSwing;
import com.poissonnerie.view.SplashScreen;
import com.poissonnerie.view.LoginView;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.poissonnerie.controller.ClientController;
import javax.swing.Timer;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static JFrame mainFrame;
    private static SplashScreen splash;
    private static LoginView loginView;

    public static void main(String[] args) {
        // Configuration système
        System.setProperty("java.awt.headless", "false");
        System.setProperty("sun.java2d.opengl", "true");

        // Configuration du logging
        LOGGER.setLevel(Level.ALL);
        LOGGER.info("Démarrage de l'application...");

        try {
            // Affichage du splash screen en premier
            SwingUtilities.invokeLater(() -> {
                splash = new SplashScreen();
                splash.setVisible(true);
                updateSplashProgress(5, "Initialisation de l'application...");
            });

            // Installation du thème
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    FlatMaterialLighterIJTheme.setup();
                    updateSplashProgress(15, "Thème installé");
                    LOGGER.info("Thème installé avec succès");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'installation du thème", e);
                }
            });

            // Configuration de l'interface graphique
            SwingUtilities.invokeAndWait(() -> {
                try {
                    configureUI();
                    updateSplashProgress(25, "Interface configurée");

                    // Création de la fenêtre principale (mais pas encore affichée)
                    mainFrame = new JFrame("Gestion Poissonnerie");
                    mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                    // Configuration de la taille
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    int width = Math.min(1200, screenSize.width - 100);
                    int height = Math.min(800, screenSize.height - 100);
                    mainFrame.setSize(width, height);
                    mainFrame.setLocationRelativeTo(null);

                    // Ajout du contenu
                    MainViewSwing mainView = new MainViewSwing();
                    mainFrame.setContentPane(mainView.getMainPanel());
                    updateSplashProgress(35, "Interface principale préparée");

                    // Gestion de la fermeture
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
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de l'interface", e);
                    showError("Erreur d'initialisation", e);
                }
            });

            // Initialisation de la base de données
            updateSplashProgress(45, "Initialisation de la base de données...");
            DatabaseManager.initDatabase();
            updateSplashProgress(60, "Base de données initialisée");

            // Ajout d'un client test avec créance pour le développement
            updateSplashProgress(70, "Chargement des données de test...");
            ClientController clientController = new ClientController();
            clientController.ajouterClientTest();
            updateSplashProgress(80, "Données de test chargées");

            // Préparation de l'authentification
            updateSplashProgress(90, "Préparation de l'authentification...");
            SwingUtilities.invokeLater(() -> {
                try {
                    // Création de l'écran de login (mais pas encore affiché)
                    loginView = new LoginView();
                    loginView.addLoginSuccessListener(() -> {
                        // Quand le login réussit, on affiche la fenêtre principale
                        mainFrame.setVisible(true);
                        mainFrame.toFront();
                        mainFrame.requestFocus();
                        LOGGER.info("Interface principale affichée après authentification réussie");
                    });

                    updateSplashProgress(100, "Application prête !");

                    // Attendre un peu pour montrer le 100%
                    Timer timer = new Timer(500, e -> {
                        // Fermer le splash screen et afficher le login
                        if (splash != null) {
                            splash.dispose();
                        }
                        loginView.setVisible(true);
                        LOGGER.info("Écran de login affiché avec succès");
                    });
                    timer.setRepeats(false);
                    timer.start();
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

    private static void updateSplashProgress(int progress, String message) {
        SwingUtilities.invokeLater(() -> {
            if (splash != null) {
                splash.setProgress(progress, message);
            }
        });
    }

    private static void showError(String title, Exception e) {
        String message = String.format("%s: %s", title, e.getMessage());
        System.err.println(message);
        if (splash != null) {
            splash.dispose();
        }
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message, "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        System.exit(1);
    }

    private static void configureUI() {
        try {
            // Configuration optimisée de l'interface utilisateur
            Font defaultFont = new Font("Segoe UI", Font.PLAIN, 14);
            Color primaryColor = new Color(33, 150, 243);
            Color backgroundColor = new Color(245, 245, 245);
            Color textColor = new Color(33, 33, 33);

            // Configuration générale
            UIManager.put("defaultFont", defaultFont);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);

            // Boutons
            UIManager.put("Button.background", primaryColor);
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.font", defaultFont.deriveFont(Font.BOLD));
            UIManager.put("Button.margin", new Insets(6, 14, 6, 14));
            UIManager.put("Button.focusPainted", false);

            // Panneaux
            UIManager.put("Panel.background", backgroundColor);
            UIManager.put("Panel.font", defaultFont);

            // Tables
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("Table.foreground", textColor);
            UIManager.put("Table.selectionBackground", primaryColor.brighter());
            UIManager.put("Table.selectionForeground", Color.WHITE);
            UIManager.put("Table.gridColor", new Color(224, 224, 224));
            UIManager.put("Table.font", defaultFont);
            UIManager.put("Table.rowHeight", 30);

            // En-têtes de table
            UIManager.put("TableHeader.background", primaryColor);
            UIManager.put("TableHeader.foreground", Color.WHITE);
            UIManager.put("TableHeader.font", defaultFont.deriveFont(Font.BOLD));

            LOGGER.info("Configuration de l'interface terminée");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la configuration de l'interface", e);
            throw e;
        }
    }
}