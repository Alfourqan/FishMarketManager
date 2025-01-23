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

        SwingUtilities.invokeLater(() -> {
            try {
                // Créer et afficher le SplashScreen immédiatement
                splash = new SplashScreen();
                splash.setVisible(true);
                splash.toFront();
                splash.requestFocus();
                updateSplashProgress(0, "Démarrage de l'application...");

                // Utiliser un SwingWorker pour les initialisations lourdes
                SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            // Installation du thème
                            publish(20);
                            updateSplashProgress(20, "Installation du thème...");
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                            FlatMaterialLighterIJTheme.setup();
                            LOGGER.info("Thème installé avec succès");

                            // Configuration de l'interface
                            publish(40);
                            updateSplashProgress(40, "Configuration de l'interface...");
                            configureUI();
                            LOGGER.info("Interface configurée avec succès");

                            // Initialisation de la base de données
                            publish(60);
                            updateSplashProgress(60, "Initialisation de la base de données...");
                            DatabaseManager.initDatabase();
                            LOGGER.info("Base de données initialisée avec succès");

                            // Chargement des données de test
                            publish(80);
                            updateSplashProgress(80, "Chargement des données...");
                            ClientController clientController = new ClientController();
                            clientController.ajouterClientTest();
                            LOGGER.info("Données de test chargées avec succès");

                            return null;
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation", e);
                            throw e;
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            get(); // Vérifier les exceptions
                            updateSplashProgress(100, "Application prête !");

                            // Créer l'écran de login
                            loginView = new LoginView();
                            loginView.addLoginSuccessListener(() -> {
                                // Création de la fenêtre principale
                                mainFrame = new JFrame("Gestion Poissonnerie");
                                mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                                // Configuration de la taille
                                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                                int width = Math.min(1200, screenSize.width - 100);
                                int height = Math.min(800, screenSize.height - 100);
                                mainFrame.setSize(width, height);
                                mainFrame.setLocationRelativeTo(null);

                                // Configuration du contenu
                                MainViewSwing mainView = new MainViewSwing();
                                mainFrame.setContentPane(mainView.getMainPanel());

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

                                mainFrame.setVisible(true);
                                mainFrame.toFront();
                                mainFrame.requestFocus();
                                LOGGER.info("Interface principale affichée après authentification réussie");
                            });

                            // Attendre un moment avant de fermer le splash et afficher le login
                            Timer timer = new Timer(1500, e -> {
                                if (splash != null) {
                                    splash.dispose();
                                    splash = null;
                                }
                                loginView.setVisible(true);
                                loginView.toFront();
                                loginView.requestFocus();
                                LOGGER.info("Écran de login affiché avec succès");
                            });
                            timer.setRepeats(false);
                            timer.start();

                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation", e);
                            showError("Erreur d'initialisation", e);
                        }
                    }
                };

                worker.execute();

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur fatale", e);
                showError("Erreur fatale", e);
            }
        });
    }

    private static void updateSplashProgress(int progress, String message) {
        if (splash != null) {
            splash.setProgress(progress, message);
        }
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