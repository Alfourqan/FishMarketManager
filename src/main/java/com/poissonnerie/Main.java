package com.poissonnerie;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.util.DatabaseManager;
import com.poissonnerie.view.MainViewSwing;

public class Main {
    public static void main(String[] args) {
        // Pour garantir que l'interface graphique s'exécute dans l'EDT
        SwingUtilities.invokeLater(() -> {
            try {
                // Set Look and Feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // Initialisation de la base de données
                DatabaseManager.initDatabase();

                // Création et affichage de la fenêtre principale
                JFrame frame = new JFrame("Gestion Poissonnerie");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1200, 800);

                MainViewSwing mainView = new MainViewSwing();
                frame.setContentPane(mainView.getMainPanel());

                // Centrer la fenêtre
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Erreur lors du démarrage: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}