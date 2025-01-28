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
        // Configuration système pour VNC et X11
        try {
            System.setProperty("java.awt.headless", "false");
            System.setProperty("sun.java2d.opengl", "true");
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            System.setProperty("sun.java2d.xrender", "true");

            // Configuration du logging
            LOGGER.setLevel(Level.ALL);
            LOGGER.info("Démarrage de l'application...");

            // Vérification de l'environnement graphique
            if (GraphicsEnvironment.isHeadless()) {
                LOGGER.severe("Environnement graphique non disponible");
                showError("Erreur critique", new Exception("Environnement graphique non disponible"));
                System.exit(1);
            }

            // Initialisation dans l'EDT avec gestion d'erreurs robuste
            SwingUtilities.invokeAndWait(() -> {
                try {
                    // Installation du thème avant la création des composants
                    UIManager.setLookAndFeel(new FlatMaterialLighterIJTheme());
                    LOGGER.info("Thème installé");

                    // Configuration UI
                    configureUI();
                    LOGGER.info("Interface configurée");

                    // Création et affichage du SplashScreen avec position explicite
                    splash = new SplashScreen();
                    splash.setLocationRelativeTo(null);
                    splash.setProgress(0, "Démarrage...");
                    splash.setVisible(true);
                    splash.toFront();
                    LOGGER.info("SplashScreen créé et affiché");

                    // Initialisation en arrière-plan
                    initializeApplication();

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur d'initialisation de l'interface", e);
                    showError("Erreur d'initialisation", e);
                }
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale", e);
            showError("Erreur fatale", e);
            System.exit(1);
        }
    }

    private static void initializeApplication() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    splash.setProgress(30, "Initialisation de la base de données...");
                    DatabaseManager.initDatabase();
                    LOGGER.info("Base de données initialisée");

                    splash.setProgress(60, "Chargement des données...");
                    ClientController clientController = new ClientController();
                    clientController.ajouterClientTest();
                    LOGGER.info("Données chargées");

                    return null;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur d'initialisation", e);
                    throw e;
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Vérifier les exceptions
                    splash.setProgress(100, "Prêt !");

                    SwingUtilities.invokeLater(() -> {
                        try {
                            // Création de la vue de login
                            loginView = new LoginView();
                            loginView.addLoginSuccessListener(() -> createAndShowMainFrame());

                            // Transition SplashScreen -> Login après délai
                            Timer timer = new Timer(1500, e -> {
                                splash.dispose();
                                loginView.setLocationRelativeTo(null);
                                loginView.setVisible(true);
                                loginView.toFront();
                                LOGGER.info("Transition vers login effectuée");
                            });
                            timer.setRepeats(false);
                            timer.start();

                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du login", e);
                            showError("Erreur d'initialisation du login", e);
                        }
                    });

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur fatale", e);
                    showError("Erreur d'initialisation", e);
                }
            }
        }.execute();
    }

    private static void createAndShowMainFrame() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Création de la fenêtre principale
                mainFrame = new JFrame("Gestion Poissonnerie");
                mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                // Configuration de la taille avec vérification de l'écran
                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                int screenWidth = gd.getDisplayMode().getWidth();
                int screenHeight = gd.getDisplayMode().getHeight();

                mainFrame.setSize(
                    Math.min(1200, screenWidth - 100),
                    Math.min(800, screenHeight - 100)
                );
                mainFrame.setLocationRelativeTo(null);

                // Configuration du contenu
                mainFrame.setContentPane(new MainViewSwing().getMainPanel());

                // Gestion de la fermeture
                mainFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        try {
                            // Nettoyage des ressources
                            DatabaseManager.checkDatabaseHealth();
                            mainFrame.dispose();
                            System.exit(0);
                        } catch (Exception ex) {
                            LOGGER.log(Level.SEVERE, "Erreur de fermeture", ex);
                            System.exit(1);
                        }
                    }
                });

                mainFrame.setVisible(true);
                mainFrame.toFront();
                LOGGER.info("Fenêtre principale affichée");

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la création de la fenêtre principale", e);
                showError("Erreur de création de la fenêtre principale", e);
            }
        });
    }

    private static void configureUI() {
        // Configuration de l'interface utilisateur
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 14);
        Color primaryColor = new Color(33, 150, 243);
        Color backgroundColor = new Color(245, 245, 245);
        Color textColor = new Color(33, 33, 33);

        UIManager.put("defaultFont", defaultFont);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Button.background", primaryColor);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", defaultFont.deriveFont(Font.BOLD));
        UIManager.put("Button.margin", new Insets(6, 14, 6, 14));
        UIManager.put("Button.focusPainted", false);
        UIManager.put("Panel.background", backgroundColor);
        UIManager.put("Panel.font", defaultFont);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", textColor);
        UIManager.put("Table.selectionBackground", primaryColor.brighter());
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.gridColor", new Color(224, 224, 224));
        UIManager.put("Table.font", defaultFont);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("TableHeader.background", primaryColor);
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("TableHeader.font", defaultFont.deriveFont(Font.BOLD));
    }

    private static void showError(String title, Exception e) {
        String message = String.format("%s: %s", title, e.getMessage());
        LOGGER.severe(message);
        if (splash != null) {
            splash.dispose();
        }
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, "Erreur", JOptionPane.ERROR_MESSAGE);
        });
        System.exit(1);
    }
}