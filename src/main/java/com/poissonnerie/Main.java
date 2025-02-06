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
import java.util.concurrent.*;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static JFrame mainFrame;
    private static SplashScreen splash;
    private static LoginView loginView;
    private static final int TIMEOUT_SECONDS = 60;

    public static void main(String[] args) {
        try {
            // Configuration système pour VNC et X11
            configureSystemProperties();
            LOGGER.info("Configuration système initialisée");

            // Initialisation dans l'EDT
            SwingUtilities.invokeAndWait(() -> {
                try {
                    LOGGER.info("Démarrage de l'application dans l'EDT...");
                    initializeApplication();
                } catch (Exception e) {
                    handleFatalError("Erreur d'initialisation dans l'EDT", e);
                }
            });

        } catch (Exception e) {
            handleFatalError("Erreur fatale au démarrage", e);
        }
    }

    private static void configureSystemProperties() {
        System.setProperty("java.awt.headless", "false");
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.xrender", "true");
        LOGGER.setLevel(Level.ALL);
    }

    private static void initializeApplication() throws Exception {
        // Vérification de l'environnement graphique
        if (GraphicsEnvironment.isHeadless()) {
            throw new Exception("Environnement graphique non disponible");
        }

        // Installation du thème avec timeout
        CompletableFuture<Void> themeSetup = CompletableFuture.runAsync(() -> {
            try {
                UIManager.setLookAndFeel(new FlatMaterialLighterIJTheme());
                configureUI();
                LOGGER.info("Thème et UI configurés avec succès");
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });

        try {
            themeSetup.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER.severe("Timeout lors de l'installation du thème");
            throw e;
        }

        // Affichage du SplashScreen
        SwingUtilities.invokeLater(() -> {
            try {
                splash = new SplashScreen();
                splash.setLocationRelativeTo(null);
                splash.setProgress(0, "Démarrage...");
                splash.setVisible(true);
                splash.toFront();
                LOGGER.info("SplashScreen créé et affiché");

                // Initialisation en arrière-plan
                startBackgroundInitialization();
            } catch (Exception e) {
                handleFatalError("Erreur lors de l'affichage du SplashScreen", e);
            }
        });
    }

    private static void startBackgroundInitialization() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> initTask = executor.submit(() -> {
            try {
                LOGGER.info("Début de l'initialisation en arrière-plan");
                initializeDatabase();
                loadInitialData();
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation en arrière-plan", e);
                throw new CompletionException(e);
            }
        });

        // Surveillance du timeout
        try {
            initTask.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            LOGGER.info("Initialisation en arrière-plan terminée avec succès");
            showLoginScreen();
        } catch (TimeoutException e) {
            LOGGER.severe("Timeout lors de l'initialisation");
            handleFatalError("L'initialisation a pris trop de temps", e);
        } catch (Exception e) {
            handleFatalError("Erreur lors de l'initialisation", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void initializeDatabase() throws Exception {
        splash.setProgress(30, "Initialisation de la base de données...");
        LOGGER.info("Début de l'initialisation de la base de données");
        DatabaseManager.initializeDatabase();  // Using the public method instead
        LOGGER.info("Base de données initialisée avec succès");
    }

    private static void loadInitialData() throws Exception {
        splash.setProgress(60, "Chargement des données...");
        LOGGER.info("Début du chargement des données initiales");
        ClientController clientController = new ClientController();
        clientController.ajouterClientTest();
        LOGGER.info("Données initiales chargées avec succès");
    }

    private static void showLoginScreen() {
        SwingUtilities.invokeLater(() -> {
            try {
                splash.setProgress(100, "Prêt !");
                loginView = new LoginView();
                loginView.addLoginSuccessListener(username -> createAndShowMainFrame(username));

                // Transition SplashScreen -> Login
                Timer timer = new Timer(1500, e -> {
                    if (splash != null && splash.isDisplayable()) {
                        splash.dispose();
                    }
                    loginView.setLocationRelativeTo(null);
                    loginView.setVisible(true);
                    loginView.toFront();
                    LOGGER.info("Transition vers login effectuée");
                });
                timer.setRepeats(false);
                timer.start();

            } catch (Exception e) {
                handleFatalError("Erreur lors de l'initialisation du login", e);
            }
        });
    }

    private static void createAndShowMainFrame(String username) {
        SwingUtilities.invokeLater(() -> {
            try {
                LOGGER.info("Création de la fenêtre principale pour l'utilisateur: " + username);
                mainFrame = new JFrame("Gestion Poissonnerie");
                mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                configureMainFrameSize();
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setContentPane(new MainViewSwing(username).getMainPanel());
                configureMainFrameClose();

                mainFrame.setVisible(true);
                mainFrame.toFront();
                LOGGER.info("Fenêtre principale affichée avec succès");

            } catch (Exception e) {
                handleFatalError("Erreur lors de la création de la fenêtre principale", e);
            }
        });
    }

    private static void configureMainFrameSize() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();

        mainFrame.setSize(
            Math.min(1200, screenWidth - 100),
            Math.min(800, screenHeight - 100)
        );
    }

    private static void configureMainFrameClose() {
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    DatabaseManager.checkDatabaseHealth();
                    mainFrame.dispose();
                    System.exit(0);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture", ex);
                    System.exit(1);
                }
            }
        });
    }

    private static void handleFatalError(String message, Exception e) {
        String fullMessage = String.format("%s: %s", message, e.getMessage());
        LOGGER.severe(fullMessage);
        e.printStackTrace();

        if (splash != null && splash.isDisplayable()) {
            splash.dispose();
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                fullMessage,
                "Erreur fatale",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }

    private static void configureUI() {
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
}