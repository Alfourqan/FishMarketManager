package com.poissonnerie.view;

import com.poissonnerie.controller.CaisseController;
import com.poissonnerie.model.MouvementCaisse;
import com.poissonnerie.model.MouvementCaisse.TypeMouvement;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CaisseViewSwing {
    private final JPanel mainPanel;
    private final CaisseController controller;
    private final JTable tableMouvements;
    private final DefaultTableModel tableModel;
    private final JLabel soldeLabel;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CaisseViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new CaisseController();

        // En-tête avec le solde
        JPanel headerPanel = new JPanel(new BorderLayout());
        soldeLabel = new JLabel("Solde: 0.00 €");
        soldeLabel.setFont(soldeLabel.getFont().deriveFont(Font.BOLD, 16));
        headerPanel.add(soldeLabel, BorderLayout.CENTER);

        // Création du modèle de table
        String[] columnNames = {"Date", "Type", "Montant", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableMouvements = new JTable(tableModel);

        initializeComponents(headerPanel);
        loadData();
    }

    private void initializeComponents(JPanel headerPanel) {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Boutons d'action
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton entreeBtn = new JButton("Nouvelle entrée");
        JButton sortieBtn = new JButton("Nouvelle sortie");
        JButton exporterBtn = new JButton("Exporter (CSV)");
        JButton actualiserBtn = new JButton("Actualiser");
        actualiserBtn.setIcon(UIManager.getIcon("Table.refreshIcon"));

        buttonPanel.add(entreeBtn);
        buttonPanel.add(sortieBtn);
        buttonPanel.add(exporterBtn);
        buttonPanel.add(actualiserBtn);

        // Panel supérieur combinant header et boutons
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.CENTER);

        // Table avec scroll
        JScrollPane scrollPane = new JScrollPane(tableMouvements);
        tableMouvements.setFillsViewportHeight(true);

        // Event handlers
        entreeBtn.addActionListener(e -> showMouvementDialog(TypeMouvement.ENTREE));
        sortieBtn.addActionListener(e -> showMouvementDialog(TypeMouvement.SORTIE));
        
        exporterBtn.addActionListener(e -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Exporter les mouvements");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers CSV", "csv"));
                fileChooser.setSelectedFile(new File("mouvements_caisse.csv"));

                if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    String csv = controller.exporterMouvementsCSV(controller.getMouvements());
                    
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(csv);
                    }

                    JOptionPane.showMessageDialog(mainPanel,
                        "Export réussi vers : " + file.getName(),
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de l'export : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        actualiserBtn.addActionListener(e -> {
            try {
                loadData();
                JOptionPane.showMessageDialog(mainPanel,
                    "Données actualisées avec succès",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de l'actualisation : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void showMouvementDialog(TypeMouvement type) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
            type == TypeMouvement.ENTREE ? "Nouvelle entrée" : "Nouvelle sortie",
            true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Champs du formulaire
        JTextField montantField = new JTextField(15);
        JTextField descriptionField = new JTextField(20);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Montant:"), gbc);
        gbc.gridx = 1;
        panel.add(montantField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        panel.add(descriptionField, gbc);

        // Boutons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annuler");

        okButton.addActionListener(evt -> {
            try {
                String montantText = montantField.getText().trim().replace(",", ".");
                String description = descriptionField.getText().trim();

                if (montantText.isEmpty()) {
                    throw new IllegalArgumentException("Le montant est obligatoire");
                }
                if (description.isEmpty()) {
                    throw new IllegalArgumentException("La description est obligatoire");
                }

                double montant;
                try {
                    montant = Double.parseDouble(montantText);
                    if (montant <= 0) {
                        throw new IllegalArgumentException("Le montant doit être positif");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Montant invalide");
                }

                MouvementCaisse mouvement = new MouvementCaisse(
                    0,
                    LocalDateTime.now(),
                    type,
                    montant,
                    description
                );

                controller.enregistrerMouvement(mouvement);
                refreshTable();
                dialog.dispose();

                JOptionPane.showMessageDialog(mainPanel,
                    String.format("Opération enregistrée avec succès\nNouveau solde: %.2f €",
                        controller.calculerSoldeCaisse()),
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialog,
                    e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(evt -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Finalisation du dialog
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    private void loadData() {
        try {
            System.out.println("Chargement des mouvements de caisse...");
            controller.chargerMouvements();
            refreshTable();
            updateSoldeLabel();
            System.out.println("Mouvements de caisse chargés avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des mouvements: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des mouvements : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<MouvementCaisse> mouvements = controller.getMouvements();
        
        for (MouvementCaisse mouvement : mouvements) {
            tableModel.addRow(new Object[]{
                mouvement.getDate().format(DATE_FORMAT),
                mouvement.getType().toString(),
                String.format("%.2f €", mouvement.getMontant()),
                mouvement.getDescription()
            });
        }
        
        updateSoldeLabel();
    }

    private void updateSoldeLabel() {
        double solde = controller.calculerSoldeCaisse();
        soldeLabel.setText(String.format("Solde: %.2f €", solde));
        soldeLabel.setForeground(solde >= 0 ? new Color(0, 100, 0) : Color.RED);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
