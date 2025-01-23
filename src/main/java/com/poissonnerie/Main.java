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

        try {
            // S'assurer que le SplashScreen est créé et affiché immédiatement
            splash = new SplashScreen();
            splash.setVisible(true);
            updateSplashProgress(5, "Initialisation de l'application...");

            // Thread séparé pour les initialisations lourdes
            SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        // Installation du thème
                        publish(15);
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

                        // Configuration de l'interface
                        publish(25);
                        SwingUtilities.invokeAndWait(() -> {
                            try {
                                configureUI();
                                updateSplashProgress(25, "Interface configurée");

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

                                updateSplashProgress(35, "Interface principale préparée");
                            } catch (Exception e) {
                                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de l'interface", e);
                                showError("Erreur d'initialisation", e);
                            }
                        });

                        // Initialisation de la base de données
                        publish(45);
                        updateSplashProgress(45, "Initialisation de la base de données...");
                        DatabaseManager.initDatabase();
                        updateSplashProgress(60, "Base de données initialisée");

                        // Chargement des données de test
                        publish(70);
                        updateSplashProgress(70, "Chargement des données de test...");
                        ClientController clientController = new ClientController();
                        clientController.ajouterClientTest();
                        updateSplashProgress(80, "Données de test chargées");

                        // Préparation de l'authentification
                        publish(90);
                        updateSplashProgress(90, "Préparation de l'authentification...");
                        SwingUtilities.invokeAndWait(() -> {
                            try {
                                // Création de l'écran de login
                                loginView = new LoginView();
                                loginView.addLoginSuccessListener(() -> {
                                    mainFrame.setVisible(true);
                                    mainFrame.toFront();
                                    mainFrame.requestFocus();
                                    LOGGER.info("Interface principale affichée après authentification réussie");
                                });
                            } catch (Exception e) {
                                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du login", e);
                                showError("Erreur d'initialisation", e);
                            }
                        });

                        return null;
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Erreur fatale", e);
                        showError("Erreur fatale", e);
                        return null;
                    }
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    // Mise à jour de la progression pendant l'exécution
                    if (!chunks.isEmpty()) {
                        int progress = chunks.get(chunks.size() - 1);
                        updateSplashProgress(progress, "Chargement en cours...");
                    }
                }

                @Override
                protected void done() {
                    try {
                        get(); // Vérifier les exceptions
                        updateSplashProgress(100, "Application prête !");

                        // Attendre un peu avant de fermer le splash screen
                        Timer timer = new Timer(2000, e -> {
                            if (splash != null) {
                                splash.dispose();
                                splash = null;
                            }
                            if (loginView != null) {
                                loginView.setVisible(true);
                                LOGGER.info("Écran de login affiché avec succès");
                            }
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