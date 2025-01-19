package com.poissonnerie;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainViewSwing;
import com.poissonnerie.view.SplashScreen;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicReference;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static SplashScreen splash;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private static final AtomicReference<JFrame> mainFrame = new AtomicReference<>();

    public static void main(String[] args) {
        // Configuration du gestionnaire d'exceptions non gérées
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Exception non gérée dans le thread " + thread.getName(), throwable);
            handleError(throwable, "Erreur inattendue");
        });

        // Démarrage sécurisé de l'application
        SwingUtilities.invokeLater(() -> {
            try {
                LOGGER.info("Démarrage de l'application...");
                initializeApplication();
            } catch (Exception e) {
                handleError(e, "Erreur lors du démarrage");
            }
        });
    }

    private static void initializeApplication() {
        try {
            // Affichage du splash screen
            splash = new SplashScreen();
            splash.setVisible(true);

            // Démarrage du processus d'initialisation
            new Thread(() -> {
                try {
                    initializeComponents();
                } catch (Exception e) {
                    handleError(e, "Erreur lors de l'initialisation");
                }
            }, "InitThread").start();

        } catch (Exception e) {
            handleError(e, "Erreur lors de la création du splash screen");
        }
    }

    private static void initializeComponents() throws Exception {
        updateProgress(10, "Configuration du thème...");
        Thread.sleep(500);

        // Configuration du thème
        SwingUtilities.invokeAndWait(() -> {
            try {
                FlatMaterialLighterIJTheme.setup();
                configureUI();
                LOGGER.info("Thème configuré avec succès");
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la configuration du thème", e);
            }
        });

        // Initialisation de la base de données avec retry et validation SIRET désactivée
        updateProgress(30, "Initialisation de la base de données...");
        System.setProperty("SKIP_SIRET_VALIDATION", "true");
        try {
            initializeDatabaseWithRetry();
            LOGGER.info("Base de données initialisée avec succès");
        } finally {
            System.clearProperty("SKIP_SIRET_VALIDATION");
        }

        updateProgress(50, "Vérification de la base de données...");
        DatabaseManager.checkDatabaseHealth();

        updateProgress(80, "Chargement de l'interface principale...");
        Thread.sleep(500);

        // Création et affichage de la fenêtre principale
        SwingUtilities.invokeAndWait(() -> {
            try {
                createAndShowMainWindow();
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la création de la fenêtre principale", e);
            }
        });

        finishStartup();
    }

    private static void initializeDatabaseWithRetry() throws Exception {
        Exception lastException = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                DatabaseManager.initDatabase();
                LOGGER.info("Base de données initialisée avec succès");
                return;
            } catch (Exception e) {
                lastException = e;
                LOGGER.warning("Tentative " + (i + 1) + " échouée: " + e.getMessage());
                if (i < MAX_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }
        throw new Exception("Échec de l'initialisation de la base de données après " + MAX_RETRIES + " tentatives", lastException);
    }

    private static void createAndShowMainWindow() {
        JFrame frame = new JFrame("Gestion Poissonnerie");
        mainFrame.set(frame);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupAndExit(frame);
            }
        });

        frame.setSize(1200, 800);
        LOGGER.info("Fenêtre principale créée");

        MainViewSwing mainView = new MainViewSwing();
        frame.setContentPane(mainView.getMainPanel());
        LOGGER.info("Vue principale configurée");

        frame.setLocationRelativeTo(null);
    }

    private static void cleanupAndExit(JFrame frame) {
        try {
            LOGGER.info("Fermeture de l'application...");
            DatabaseManager.closeConnections();
            frame.dispose();
            System.exit(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture de l'application", e);
            System.exit(1);
        }
    }

    private static void updateProgress(int progress, String message) {
        if (splash != null && splash.isVisible()) {
            splash.setProgress(progress, message);
            LOGGER.info(message + " - " + progress + "%");
        }
    }

    private static void finishStartup() {
        SwingUtilities.invokeLater(() -> {
            try {
                updateProgress(100, "Démarrage terminé");
                Thread.sleep(500);

                JFrame frame = mainFrame.get();
                if (frame != null) {
                    frame.setVisible(true);
                    LOGGER.info("Application démarrée avec succès");
                } else {
                    throw new IllegalStateException("La fenêtre principale n'a pas été initialisée");
                }
            } catch (Exception e) {
                handleError(e, "Erreur lors de la finalisation du démarrage");
            } finally {
                disposeSplashScreen();
            }
        });
    }

    private static void disposeSplashScreen() {
        if (splash != null) {
            splash.dispose();
            splash = null;
        }
    }

    private static void handleError(Throwable e, String message) {
        String fullMessage = String.format("%s: %s\nType: %s",
            message,
            e.getMessage(),
            e.getClass().getSimpleName());

        LOGGER.log(Level.SEVERE, fullMessage, e);

        SwingUtilities.invokeLater(() -> {
            disposeSplashScreen();

            JOptionPane.showMessageDialog(null,
                fullMessage,
                "Erreur",
                JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        });
    }

    private static void configureUI() {
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

        // Configuration des champs de texte
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
    }
}