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
        // Configuration du style global avec des coins arrondis modernes
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("ProgressBar.arc", 12);
        UIManager.put("TextComponent.arc", 12);

        // Palette de couleurs moderne et professionnelle
        Color primaryColor = new Color(0, 120, 212);     // Bleu professionnel
        Color secondaryColor = new Color(0, 153, 188);   // Bleu clair
        Color accentColor = new Color(0, 183, 195);      // Turquoise
        Color warningColor = new Color(255, 140, 0);     // Orange
        Color successColor = new Color(34, 197, 94);     // Vert
        Color errorColor = new Color(239, 68, 68);       // Rouge
        Color backgroundColor = new Color(248, 250, 252); // Gris très clair
        Color textColor = new Color(30, 41, 59);         // Gris foncé
        Color lightTextColor = new Color(148, 163, 184); // Gris clair

        // Style des tableaux amélioré
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", textColor);
        UIManager.put("Table.selectionBackground", new Color(219, 234, 254));
        UIManager.put("Table.selectionForeground", textColor);
        UIManager.put("Table.gridColor", new Color(226, 232, 240));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Table.rowHeight", 35); // Réduit de 45 à 35
        UIManager.put("Table.showGrid", false); // Désactive la grille par défaut
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0)); // Réduit l'espacement

        // Style amélioré des en-têtes de tableau avec fond sombre
        UIManager.put("TableHeader.background", new Color(33, 33, 33));  // Noir plus profond
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 14));
        UIManager.put("TableHeader.height", 40); // Réduit de 50 à 40
        UIManager.put("TableHeader.border", BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(51, 51, 51)),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        // Configuration des boutons avec style moderne
        UIManager.put("Button.background", primaryColor);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 14));
        UIManager.put("Button.margin", new Insets(8, 16, 8, 16));
        UIManager.put("Button.focusPainted", false);
        UIManager.put("Button.select", primaryColor.darker());
        UIManager.put("Button.border", BorderFactory.createEmptyBorder(8, 16, 8, 16));

        // Style de la barre latérale
        UIManager.put("SidePanel.background", new Color(17, 24, 39));
        UIManager.put("SidePanel.foreground", Color.WHITE);
        UIManager.put("SidePanel.font", new Font("Segoe UI", Font.BOLD, 14));
        UIManager.put("SidePanel.border", BorderFactory.createEmptyBorder());

        // Style des panneaux
        UIManager.put("Panel.background", backgroundColor);
        UIManager.put("Panel.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Panel.border", BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Style des labels et textes
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Label.foreground", textColor);

        // Style des champs de texte
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", textColor);
        UIManager.put("TextField.caretForeground", primaryColor);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        UIManager.put("TextField.focusedBorder", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(primaryColor, 2),
            BorderFactory.createEmptyBorder(7, 11, 7, 11)
        ));


        // Style des scrollbars
        UIManager.put("ScrollBar.thumb", new Color(203, 213, 225));
        UIManager.put("ScrollBar.thumbDarkShadow", new Color(203, 213, 225));
        UIManager.put("ScrollBar.thumbHighlight", new Color(203, 213, 225));
        UIManager.put("ScrollBar.thumbShadow", new Color(203, 213, 225));
        UIManager.put("ScrollBar.track", backgroundColor);
        UIManager.put("ScrollBar.width", 12);

        // Style des menus
        UIManager.put("Menu.background", Color.WHITE);
        UIManager.put("Menu.foreground", textColor);
        UIManager.put("Menu.selectionBackground", primaryColor);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("Menu.border", BorderFactory.createEmptyBorder(5, 5, 5, 5));
        UIManager.put("Menu.font", new Font("Segoe UI", Font.PLAIN, 14));

        // Style des onglets
        UIManager.put("TabbedPane.background", backgroundColor);
        UIManager.put("TabbedPane.foreground", textColor);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.selectedForeground", primaryColor);
        UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.BOLD, 14));
    }
}