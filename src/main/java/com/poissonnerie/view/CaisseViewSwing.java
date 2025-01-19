package com.poissonnerie.view;

import com.poissonnerie.controller.CaisseController;
import com.poissonnerie.model.MouvementCaisse;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CaisseViewSwing {
    private static final Logger LOGGER = Logger.getLogger(CaisseViewSwing.class.getName());
    private static final String MSG_ERREUR_MONTANT = "Le montant doit être un nombre positif";
    private static final String MSG_ERREUR_DESCRIPTION = "La description est obligatoire";
    private static final String MSG_ERREUR_CAISSE_FERMEE = "La caisse doit être ouverte pour effectuer des mouvements";
    private static final double MONTANT_MAX = 99999.99;

    private final JPanel mainPanel;
    private final CaisseController controller;
    private final JTable tableMouvements;
    private final DefaultTableModel tableModel;
    private final JLabel soldeLabel;
    private JButton ouvrirBtn;
    private JButton cloturerBtn;
    private JButton ajouterBtn;
    private JButton exporterBtn;
    private final AtomicBoolean caisseOuverte = new AtomicBoolean(false);

    public CaisseViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(248, 250, 252));
        controller = new CaisseController();

        // Initialisation du label de solde
        soldeLabel = new JLabel("Solde: 0.00 €");
        soldeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        soldeLabel.setForeground(new Color(30, 41, 59));
        soldeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        String[] columnNames = {"Date", "Type", "Montant", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableMouvements = new JTable(tableModel);
        setupTableStyle(); // Appel de notre nouvelle méthode de style

        initializeComponents();
        loadData();
        updateCaisseState();
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode, Color color) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(20);
        icon.setIconColor(Color.WHITE);

        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Effet de survol avec animation
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
                // Animation de mise à l'échelle
                Timer timer = new Timer(50, e -> {
                    button.setMargin(new Insets(9, 19, 9, 19));
                    button.repaint();
                });
                timer.setRepeats(false);
                timer.start();
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                // Animation de retour à la normale
                Timer timer = new Timer(50, e -> {
                    button.setMargin(new Insets(10, 20, 10, 20));
                    button.repaint();
                });
                timer.setRepeats(false);
                timer.start();
            }
        });

        return button;
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel du haut avec style moderne
        JPanel topPanel = new JPanel(new BorderLayout(20, 0));
        topPanel.setBackground(new Color(248, 250, 252));
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        actionPanel.setBackground(new Color(248, 250, 252));

        // Création des boutons avec nouveaux styles et animations
        ouvrirBtn = createStyledButton("Ouvrir la caisse", MaterialDesign.MDI_CASH, new Color(34, 197, 94));
        cloturerBtn = createStyledButton("Clôturer la caisse", MaterialDesign.MDI_CLOSE_CIRCLE, new Color(239, 68, 68));
        ajouterBtn = createStyledButton("Nouveau mouvement", MaterialDesign.MDI_PLUS_CIRCLE, new Color(0, 120, 212));
        exporterBtn = createStyledButton("Exporter (CSV)", MaterialDesign.MDI_EXPORT, new Color(0, 183, 195));

        actionPanel.add(ouvrirBtn);
        actionPanel.add(cloturerBtn);
        actionPanel.add(ajouterBtn);
        actionPanel.add(exporterBtn);

        // Panel pour le solde avec style moderne
        JPanel soldePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        soldePanel.setBackground(new Color(248, 250, 252));
        soldePanel.add(soldeLabel);

        topPanel.add(soldePanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.CENTER);

        // Style moderne pour la table (already set in constructor)

        JScrollPane scrollPane = new JScrollPane(tableMouvements);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);


        // Event handlers
        ouvrirBtn.addActionListener(e -> ouvrirCaisse());
        cloturerBtn.addActionListener(e -> cloturerCaisse());
        ajouterBtn.addActionListener(e -> showMouvementDialog());
        exporterBtn.addActionListener(e -> exporterMouvements());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void setupTableStyle() {
        // Configuration de base du tableau
        tableMouvements.setShowGrid(false);
        tableMouvements.setGridColor(new Color(230, 230, 230));
        tableMouvements.setBackground(Color.WHITE);
        tableMouvements.setSelectionBackground(new Color(232, 240, 254));
        tableMouvements.setSelectionForeground(new Color(33, 33, 33));
        tableMouvements.setRowHeight(35);
        tableMouvements.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableMouvements.setIntercellSpacing(new Dimension(0, 0));

        // Configuration des cellules avec alternance de couleurs
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                }
                // Padding des cellules ajusté
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                ((JLabel) c).setHorizontalAlignment(JLabel.LEFT);
                return c;
            }
        };

        // Appliquer le renderer à toutes les colonnes
        for (int i = 0; i < tableMouvements.getColumnCount(); i++) {
            tableMouvements.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // Style de l'en-tête simplifié
        JTableHeader header = tableMouvements.getTableHeader();
        header.setBackground(new Color(33, 33, 33));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

        // Configuration du rendu de l'en-tête
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                label.setBackground(new Color(33, 33, 33));
                label.setForeground(Color.WHITE);
                label.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                label.setHorizontalAlignment(JLabel.LEFT);
                return label;
            }
        };

        header.setDefaultRenderer(headerRenderer);
    }

    private void ouvrirCaisse() {
        if (!caisseOuverte.get()) {
            try {
                double montantInitial = getMontantInitial();
                if (montantInitial > 0 && montantInitial <= MONTANT_MAX) {
                    MouvementCaisse mouvement = new MouvementCaisse(
                        0,
                        LocalDateTime.now(),
                        MouvementCaisse.TypeMouvement.OUVERTURE,
                        montantInitial,
                        "Ouverture de caisse"
                    );
                    controller.ajouterMouvement(mouvement);
                    caisseOuverte.set(true);
                    LOGGER.log(Level.INFO, "Ouverture de caisse avec montant initial: {0}", montantInitial);
                    updateCaisseState();
                    refreshTable();
                    JOptionPane.showMessageDialog(mainPanel,
                        "La caisse a été ouverte avec succès",
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    LOGGER.log(Level.WARNING, "Tentative d'ouverture avec montant invalide: {0}", montantInitial);
                    JOptionPane.showMessageDialog(mainPanel,
                        "Le montant initial doit être compris entre 0 et " + MONTANT_MAX,
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de la caisse", e);
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
            double montant = Double.parseDouble(montantStr.replace(",", ".").trim());
            if (montant <= 0 || montant > MONTANT_MAX) {
                throw new IllegalArgumentException("Le montant doit être compris entre 0 et " + MONTANT_MAX);
            }
            return montant;
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Tentative de saisie d'un montant invalide: {0}", montantStr);
            JOptionPane.showMessageDialog(mainPanel,
                MSG_ERREUR_MONTANT,
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
            return 0;
        }
    }

    private void cloturerCaisse() {
        if (caisseOuverte.get()) {
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
                    caisseOuverte.set(false);
                    LOGGER.log(Level.INFO, "Clôture de caisse avec solde final: {0}", soldeFinal);
                    updateCaisseState();
                    refreshTable();

                    // Afficher le récapitulatif de clôture
                    JOptionPane.showMessageDialog(mainPanel,
                        String.format("Clôture de caisse effectuée\nSolde final: %.2f €", soldeFinal),
                        "Succès",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la clôture de la caisse", e);
                    JOptionPane.showMessageDialog(mainPanel,
                        "Erreur lors de la clôture de la caisse : " + e.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private synchronized void updateCaisseState() {
        boolean etatPrecedent = caisseOuverte.get();
        if (controller.getMouvements().size() > 0) {
            MouvementCaisse dernierMouvement = controller.getMouvements().get(controller.getMouvements().size() - 1);
            caisseOuverte.set(dernierMouvement.getType() != MouvementCaisse.TypeMouvement.CLOTURE);
        }

        // Mettre à jour l'état des boutons de manière thread-safe
        SwingUtilities.invokeLater(() -> {
            ouvrirBtn.setEnabled(!caisseOuverte.get());
            cloturerBtn.setEnabled(caisseOuverte.get());
            ajouterBtn.setEnabled(caisseOuverte.get());
        });

        if (etatPrecedent != caisseOuverte.get()) {
            LOGGER.log(Level.INFO, "État de la caisse modifié: {0}",
                caisseOuverte.get() ? "ouverte" : "fermée");
        }
    }

    private void showMouvementDialog() {
        if (!caisseOuverte.get()) {
            JOptionPane.showMessageDialog(mainPanel,
                MSG_ERREUR_CAISSE_FERMEE,
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

        // Champs du formulaire avec validation renforcée
        JComboBox<MouvementCaisse.TypeMouvement> typeCombo = new JComboBox<>(new MouvementCaisse.TypeMouvement[]{
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

        // Validation renforcée lors de la soumission
        JButton okButton = new JButton("OK");
        okButton.addActionListener(evt -> {
            try {
                String montantText = montantField.getText().trim().replace(",", ".");
                String description = descriptionField.getText().trim();

                if (montantText.isEmpty()) {
                    throw new IllegalArgumentException(MSG_ERREUR_MONTANT);
                }
                if (description.isEmpty()) {
                    throw new IllegalArgumentException(MSG_ERREUR_DESCRIPTION);
                }

                double montant;
                try {
                    montant = Double.parseDouble(montantText);
                    if (montant <= 0 || montant > MONTANT_MAX) {
                        throw new IllegalArgumentException("Le montant doit être compris entre 0 et " + MONTANT_MAX);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(MSG_ERREUR_MONTANT);
                }

                MouvementCaisse mouvement = new MouvementCaisse(
                    0,
                    LocalDateTime.now(),
                    (MouvementCaisse.TypeMouvement) typeCombo.getSelectedItem(),
                    montant,
                    description
                );

                controller.ajouterMouvement(mouvement);
                LOGGER.log(Level.INFO, "Nouveau mouvement de caisse ajouté: {0}", mouvement);
                refreshTable();
                dialog.dispose();

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur lors de l'ajout d'un mouvement", e);
                JOptionPane.showMessageDialog(dialog,
                    e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Annuler");
        cancelButton.addActionListener(evt -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
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
                LOGGER.log(Level.SEVERE, "Erreur lors de l'export", e);
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
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des mouvements", e);
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