
package com.poissonnerie.view;

import javax.swing.*;
import java.awt.*;
import com.poissonnerie.controller.ReportController;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

public class ReportViewSwing {
    private final JPanel mainPanel;
    private final ReportController reportController;
    private final String username;

    public ReportViewSwing(String username) {
        this.username = username;
        this.mainPanel = new JPanel(new BorderLayout());
        this.reportController = new ReportController();
        initializeComponents();
    }

    private void initializeComponents() {
        mainPanel.add(createReportPanel(), BorderLayout.CENTER);
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Boutons pour les différents rapports
        JButton stockButton = new JButton("Rapport des Stocks");
        JButton ventesButton = new JButton("Rapport des Ventes");
        JButton fournisseursButton = new JButton("Rapport Fournisseurs");

        stockButton.addActionListener(e -> genererRapportStocks());
        ventesButton.addActionListener(e -> genererRapportVentes());
        fournisseursButton.addActionListener(e -> genererRapportFournisseurs());

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(stockButton, gbc);

        gbc.gridy = 1;
        panel.add(ventesButton, gbc);

        gbc.gridy = 2;
        panel.add(fournisseursButton, gbc);

        return panel;
    }

    private void genererRapportStocks() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "rapport_stocks_" + timestamp + ".pdf";
            reportController.genererRapportStocksPDF(username, fileName);
            JOptionPane.showMessageDialog(mainPanel, 
                "Rapport des stocks généré avec succès : " + fileName,
                "Succès", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors de la génération du rapport : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void genererRapportVentes() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "rapport_ventes_" + timestamp + ".pdf";
            reportController.genererRapportVentesPDF(username, fileName);
            JOptionPane.showMessageDialog(mainPanel,
                "Rapport des ventes généré avec succès : " + fileName,
                "Succès", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors de la génération du rapport : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void genererRapportFournisseurs() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "rapport_fournisseurs_" + timestamp + ".pdf";
            reportController.genererRapportFournisseursPDF(username, fileName);
            JOptionPane.showMessageDialog(mainPanel,
                "Rapport des fournisseurs généré avec succès : " + fileName,
                "Succès", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors de la génération du rapport : " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
