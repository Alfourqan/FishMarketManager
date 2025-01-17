package com.poissonnerie.view;

import com.poissonnerie.controller.CaisseController;
import com.poissonnerie.model.MouvementCaisse;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CaisseViewSwing {
    private final JPanel mainPanel;
    private final CaisseController controller;
    private final JTable tableMouvements;
    private final DefaultTableModel tableModel;
    private final JLabel soldeLabel;
    private JComboBox<MouvementCaisse.TypeMouvement> typeCombo;
    private JButton ouvrirBtn;
    private JButton cloturerBtn;
    private JButton ajouterBtn;
    private JButton exporterBtn;
    private boolean caisseOuverte = false;

    public CaisseViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new CaisseController();

        // Création du modèle de table
        String[] columnNames = {"Date", "Type", "Montant", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableMouvements = new JTable(tableModel);
        soldeLabel = new JLabel("Solde: 0.00 €");
        soldeLabel.setFont(soldeLabel.getFont().deriveFont(Font.BOLD, 16));

        initializeComponents();
        loadData();
        updateCaisseState();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel du haut avec le solde et les boutons d'action
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Boutons de gestion de la caisse
        ouvrirBtn = new JButton("Ouvrir la caisse");
        cloturerBtn = new JButton("Clôturer la caisse");
        ajouterBtn = new JButton("Nouveau mouvement");
        exporterBtn = new JButton("Exporter (CSV)");

        actionPanel.add(ouvrirBtn);
        actionPanel.add(cloturerBtn);
        actionPanel.add(ajouterBtn);
        actionPanel.add(exporterBtn);
        topPanel.add(soldeLabel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.CENTER);

        // Table avec scroll
        JScrollPane scrollPane = new JScrollPane(tableMouvements);
        tableMouvements.setFillsViewportHeight(true);

        // Event handlers
        ouvrirBtn.addActionListener(e -> ouvrirCaisse());
        cloturerBtn.addActionListener(e -> cloturerCaisse());
        ajouterBtn.addActionListener(e -> showMouvementDialog());
        exporterBtn.addActionListener(e -> exporterMouvements());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void ouvrirCaisse() {
        if (!caisseOuverte) {
            try {
                double montantInitial = getMontantInitial();
                if (montantInitial > 0) {
                    MouvementCaisse mouvement = new MouvementCaisse(
                        0,
                        LocalDateTime.now(),
                        MouvementCaisse.TypeMouvement.OUVERTURE,
                        montantInitial,
                        "Ouverture de caisse"
                    );
                    controller.ajouterMouvement(mouvement);
                    caisseOuverte = true;
                    updateCaisseState();
                    refreshTable();
                    JOptionPane.showMessageDialog(mainPanel,
                        "La caisse a été ouverte avec succès",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de l'ouverture de la caisse : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private double getMontantInitial() {
        String montantStr = JOptionPane.showInputDialog(mainPanel,
            "Entrez le montant initial de la caisse:",
            "Ouverture de caisse",
            JOptionPane.QUESTION_MESSAGE);

        if (montantStr == null || montantStr.trim().isEmpty()) {
            return 0;
        }

        try {
            double montant = Double.parseDouble(montantStr.replace(",", "."));
            if (montant <= 0) {
                throw new IllegalArgumentException("Le montant doit être positif");
            }
            return montant;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Montant invalide",
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    private void cloturerCaisse() {
        if (caisseOuverte) {
            int confirmation = JOptionPane.showConfirmDialog(mainPanel,
                "Êtes-vous sûr de vouloir clôturer la caisse ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION);

            if (confirmation == JOptionPane.YES_OPTION) {
                try {
                    double soldeFinal = controller.getSoldeCaisse();
                    MouvementCaisse mouvement = new MouvementCaisse(
                        0,
                        LocalDateTime.now(),
                        MouvementCaisse.TypeMouvement.CLOTURE,
                        soldeFinal,
                        "Clôture de caisse"
                    );
                    controller.ajouterMouvement(mouvement);
                    caisseOuverte = false;
                    updateCaisseState();
                    refreshTable();

                    // Afficher le récapitulatif de clôture
                    JOptionPane.showMessageDialog(mainPanel,
                        String.format("Clôture de caisse effectuée\nSolde final: %.2f €", soldeFinal),
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Erreur lors de la clôture de la caisse : " + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void updateCaisseState() {
        // Mettre à jour l'état des boutons en fonction de l'état de la caisse
        ouvrirBtn.setEnabled(!caisseOuverte);
        cloturerBtn.setEnabled(caisseOuverte);
        ajouterBtn.setEnabled(caisseOuverte);

        // Vérifier le dernier mouvement pour déterminer l'état de la caisse
        if (controller.getMouvements().size() > 0) {
            MouvementCaisse dernierMouvement = controller.getMouvements().get(controller.getMouvements().size() - 1);
            caisseOuverte = dernierMouvement.getType() != MouvementCaisse.TypeMouvement.CLOTURE;
        }
    }

    private void showMouvementDialog() {
        if (!caisseOuverte) {
            JOptionPane.showMessageDialog(mainPanel,
                "La caisse doit être ouverte pour effectuer des mouvements",
                "Caisse fermée",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
            "Nouveau mouvement de caisse", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Champs du formulaire
        typeCombo = new JComboBox<>(new MouvementCaisse.TypeMouvement[]{
            MouvementCaisse.TypeMouvement.ENTREE,
            MouvementCaisse.TypeMouvement.SORTIE
        });
        JTextField montantField = new JTextField(10);
        JTextField descriptionField = new JTextField(20);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        panel.add(typeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Montant:"), gbc);
        gbc.gridx = 1;
        panel.add(montantField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
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
                    (MouvementCaisse.TypeMouvement) typeCombo.getSelectedItem(),
                    montant,
                    description
                );

                controller.ajouterMouvement(mouvement);
                refreshTable();
                dialog.dispose();

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

    private void exporterMouvements() {
        // Créer un sélecteur de fichier
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exporter les mouvements");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fichiers CSV", "csv"));
        fileChooser.setSelectedFile(new File("mouvements_caisse.csv"));

        if (fileChooser.showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                String filePath = file.getPath();
                if (!filePath.toLowerCase().endsWith(".csv")) {
                    filePath += ".csv";
                }

                // Exporter les données
                String csvContent = controller.exporterMouvementsCSV(
                    LocalDateTime.now().minusDays(30), // Par défaut, exporter les 30 derniers jours
                    LocalDateTime.now()
                );

                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write(csvContent);
                }

                JOptionPane.showMessageDialog(mainPanel,
                    "Export réussi: " + filePath,
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainPanel,
                    "Erreur lors de l'export: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadData() {
        try {
            controller.chargerMouvements();
            refreshTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des mouvements : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (MouvementCaisse mouvement : controller.getMouvements()) {
            tableModel.addRow(new Object[]{
                mouvement.getDate().format(formatter),
                mouvement.getType(),
                String.format("%.2f €", mouvement.getMontant()),
                mouvement.getDescription()
            });
        }

        // Mettre à jour le solde
        soldeLabel.setText(String.format("Solde: %.2f €", controller.getSoldeCaisse()));
        updateCaisseState();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}