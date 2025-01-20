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
    private static final AtomicReference<JFrame> mainFrame = new AtomicReference<>();
    private static final int CACHE_WARMUP_CONNECTIONS = 2;

    static {
        try {
            // Configuration initiale pour améliorer les performances
            System.setProperty("sun.java2d.opengl", "true");
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

            // Désactiver le mode headless pour Replit
            System.setProperty("java.awt.headless", "false");

            // Configuration de la recherche de polices
            System.setProperty("swing.defaultlaf", FlatMaterialLighterIJTheme.class.getName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation des propriétés système", e);
        }
    }

    public static void main(String[] args) {
        // Gestionnaire d'exceptions non gérées
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.log(Level.SEVERE, "Exception non gérée dans le thread " + thread.getName(), throwable);
            handleError(throwable, "Erreur inattendue");
        });

        // Préchauffage de la JVM
        warmupJVM();

        // Initialisation de l'interface graphique dans l'EDT
        SwingUtilities.invokeLater(() -> {
            try {
                LOGGER.info("Démarrage de l'application...");
                UIManager.setLookAndFeel(new FlatMaterialLighterIJTheme());
                JFrame.setDefaultLookAndFeelDecorated(true);
                initializeApplication();
            } catch (Exception e) {
                handleError(e, "Erreur lors du démarrage");
            }
        });
    }

    private static void warmupJVM() {
        // Préchauffage des connexions de base de données
        try {
            DatabaseManager.initDatabase(); // S'assurer que la base est initialisée avant tout
            for (int i = 0; i < CACHE_WARMUP_CONNECTIONS; i++) {
                DatabaseManager.getConnection().close();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur lors du préchauffage des connexions", e);
        }

        // Préchauffage des composants Swing
        SwingUtilities.invokeLater(() -> {
            new JPanel();
            new JButton();
            new JTextField();
            new JTable();
        });
    }

    private static void initializeApplication() {
        try {
            // Création et affichage du splash screen
            splash = new SplashScreen();
            splash.setVisible(true);

            SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Configuration du thème
                    publish(10);
                    SwingUtilities.invokeAndWait(() -> configureUI());

                    // Création et affichage de la fenêtre principale
                    publish(80);
                    SwingUtilities.invokeAndWait(() -> createAndShowMainWindow());

                    publish(100);
                    return null;
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    int progress = chunks.get(chunks.size() - 1);
                    splash.setProgress(progress, getProgressMessage(progress));
                }

                @Override
                protected void done() {
                    try {
                        get();
                        disposeSplashScreen();
                    } catch (Exception e) {
                        handleError(e, "Erreur lors de l'initialisation");
                    }
                }
            };
            worker.execute();

        } catch (Exception e) {
            handleError(e, "Erreur lors de la création du splash screen");
        }
    }

    private static String getProgressMessage(int progress) {
        return switch (progress) {
            case 10 -> "Configuration de l'interface...";
            case 80 -> "Chargement de l'interface principale...";
            case 100 -> "Démarrage terminé";
            default -> "Chargement en cours...";
        };
    }

    private static void createAndShowMainWindow() {
        try {
            JFrame frame = new JFrame("Gestion Poissonnerie");
            mainFrame.set(frame);

            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cleanupAndExit(frame);
                }
            });

            // Configuration de la taille et position
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = Math.min(1200, screenSize.width - 100);
            int height = Math.min(800, screenSize.height - 100);
            frame.setSize(width, height);
            frame.setLocationRelativeTo(null);

            // Création et configuration optimisée de la vue principale
            MainViewSwing mainView = new MainViewSwing();
            frame.setContentPane(mainView.getMainPanel());

            // Activation du double buffering pour une meilleure performance
            frame.createBufferStrategy(2);

            frame.setVisible(true);
            frame.requestFocus();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création de la fenêtre principale", e);
            throw e;
        }
    }

    private static void cleanupAndExit(JFrame frame) {
        try {
            DatabaseManager.closeConnections();
            frame.dispose();
            System.exit(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la fermeture", e);
            System.exit(1);
        }
    }

    private static void disposeSplashScreen() {
        if (splash != null) {
            splash.dispose();
            splash = null;
        }
    }

    private static void handleError(Throwable e, String message) {
        String fullMessage = String.format("%s: %s%nType: %s",
                message,
                e.getMessage(),
                e.getClass().getSimpleName());

        LOGGER.log(Level.SEVERE, fullMessage, e);

        SwingUtilities.invokeLater(() -> {
            disposeSplashScreen();
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println(fullMessage);
            } else {
                JOptionPane.showMessageDialog(null,
                        fullMessage,
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        });
    }

    private static void configureUI() {
        try {
            // Configuration optimisée de l'interface utilisateur
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);

            // Optimisation des polices
            Font defaultFont = new Font("Segoe UI", Font.PLAIN, 14);
            UIManager.put("defaultFont", defaultFont);

            // Configuration des couleurs avec mise en cache
            Color primaryColor = new Color(0, 120, 212);
            Color backgroundColor = new Color(248, 250, 252);
            Color textColor = new Color(30, 41, 59);

            configureTableUI(defaultFont, primaryColor, backgroundColor, textColor);
            configureButtonsUI(defaultFont, primaryColor);
            configurePanelsUI(defaultFont, backgroundColor);
            configureTextFieldsUI(textColor, primaryColor);
            configureScrollBarsUI(backgroundColor);
            configureTabsUI(defaultFont, backgroundColor, primaryColor);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la configuration de l'interface", e);
            throw e;
        }
    }

    private static void configureTableUI(Font defaultFont, Color primaryColor, Color backgroundColor, Color textColor) {
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", textColor);
        UIManager.put("Table.selectionBackground", new Color(219, 234, 254));
        UIManager.put("Table.selectionForeground", textColor);
        UIManager.put("Table.gridColor", new Color(226, 232, 240));
        UIManager.put("Table.font", defaultFont);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showGrid", true);
        UIManager.put("Table.intercellSpacing", new Dimension(1, 1));

        UIManager.put("TableHeader.background", new Color(33, 33, 33));
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("TableHeader.font", defaultFont.deriveFont(Font.BOLD));
        UIManager.put("TableHeader.height", 35);
    }

    private static void configureButtonsUI(Font defaultFont, Color primaryColor) {
        UIManager.put("Button.background", primaryColor);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", defaultFont.deriveFont(Font.BOLD));
        UIManager.put("Button.margin", new Insets(6, 14, 6, 14));
        UIManager.put("Button.focusPainted", false);
    }

    private static void configurePanelsUI(Font defaultFont, Color backgroundColor) {
        UIManager.put("Panel.background", backgroundColor);
        UIManager.put("Panel.font", defaultFont);
    }

    private static void configureTextFieldsUI(Color textColor, Color primaryColor) {
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", textColor);
        UIManager.put("TextField.caretForeground", primaryColor);
    }

    private static void configureScrollBarsUI(Color backgroundColor) {
        UIManager.put("ScrollBar.thumb", new Color(203, 213, 225));
        UIManager.put("ScrollBar.track", backgroundColor);
        UIManager.put("ScrollBar.width", 10);
    }

    private static void configureTabsUI(Font defaultFont, Color backgroundColor, Color primaryColor) {
        UIManager.put("TabbedPane.background", backgroundColor);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.selectedForeground", primaryColor);
        UIManager.put("TabbedPane.font", defaultFont.deriveFont(Font.BOLD));
    }
}