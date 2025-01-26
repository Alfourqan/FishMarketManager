package com.poissonnerie.view;

import com.poissonnerie.controller.CaisseController;
import com.poissonnerie.model.MouvementCaisse;
import org.jdesktop.swingx.JXDatePicker;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        controller = new CaisseController();

        // Initialisation du label de solde avec style moderne
        soldeLabel = new JLabel("Solde: 0.00 €");
        soldeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        soldeLabel.setForeground(new Color(33, 33, 33));
        soldeLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        // Création du modèle de table
        String[] columnNames = {"Date", "Type", "Montant", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableMouvements = new JTable(tableModel);
        setupTableStyle();
        initializeComponents();
        loadData();
        updateCaisseState();
    }

    private void setupTableStyle() {
        // Configuration de base du tableau
        tableMouvements.setShowGrid(false);
        tableMouvements.setGridColor(new Color(230, 230, 230));
        tableMouvements.setBackground(Color.WHITE);
        tableMouvements.setSelectionBackground(new Color(232, 240, 254));
        tableMouvements.setSelectionForeground(new Color(33, 33, 33));
        tableMouvements.setRowHeight(35); // Hauteur ajustée pour correspondre au style produits
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
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                ((JLabel) c).setHorizontalAlignment(column == 2 ? JLabel.RIGHT : JLabel.LEFT);
                return c;
            }
        };

        // Appliquer le renderer à toutes les colonnes
        for (int i = 0; i < tableMouvements.getColumnCount(); i++) {
            tableMouvements.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // Style de l'en-tête
        JTableHeader header = tableMouvements.getTableHeader();
        header.setBackground(new Color(33, 33, 33));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

        // Configuration des colonnes
        tableMouvements.getColumnModel().getColumn(0).setPreferredWidth(160); // Date
        tableMouvements.getColumnModel().getColumn(1).setPreferredWidth(120); // Type
        tableMouvements.getColumnModel().getColumn(2).setPreferredWidth(140); // Montant
        tableMouvements.getColumnModel().getColumn(3).setPreferredWidth(280); // Description

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
                label.setHorizontalAlignment(column == 2 ? JLabel.RIGHT : JLabel.LEFT);
                return label;
            }
        };

        header.setDefaultRenderer(headerRenderer);
    }

    private JButton createStyledButton(String text, MaterialDesign iconCode, Color color) {
        FontIcon icon = FontIcon.of(iconCode);
        icon.setIconSize(16);  // Réduction de la taille de l'icône
        icon.setIconColor(Color.WHITE);

        JButton button = new JButton(text);
        button.setIcon(icon);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 12, 8, 12));  // Marges réduites
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void initializeComponents() {
        // Panel d'en-tête avec titre et recherche
        JPanel headerPanel = createHeaderPanel();

        // Panel des boutons d'action
        JPanel actionPanel = createActionPanel();

        // Conteneur principal
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setOpaque(false);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(actionPanel, BorderLayout.CENTER);

        // ScrollPane pour la table
        JScrollPane scrollPane = new JScrollPane(tableMouvements);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 225)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        mainPanel.add(contentPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Panel gauche pour le titre
        JPanel leftPanel = new JPanel(new BorderLayout(10, 0));
        leftPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Gestion de la Caisse");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(33, 33, 33));

        // Panel de recherche par date
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);

        // Création des composants de recherche
        JXDatePicker dateDebutPicker = new JXDatePicker();
        JXDatePicker dateFinPicker = new JXDatePicker();
        dateDebutPicker.setDate(new Date());
        dateFinPicker.setDate(new Date());

        // Personnalisation des date pickers
        dateDebutPicker.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateFinPicker.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton searchButton = createStyledButton("Rechercher", MaterialDesign.MDI_MAGNIFY, new Color(33, 150, 243));
        searchButton.addActionListener(e -> {
            LocalDateTime dateDebut = dateDebutPicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime dateFin = dateFinPicker.getDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

            List<MouvementCaisse> mouvementsFiltres = controller.rechercherMouvementsParDate(dateDebut, dateFin);
            refreshTableWithMovements(mouvementsFiltres);
            updateTotalInfo(mouvementsFiltres);
        });

        JButton resetButton = createStyledButton("Réinitialiser", MaterialDesign.MDI_REFRESH, new Color(158, 158, 158));
        resetButton.addActionListener(e -> {
            dateDebutPicker.setDate(new Date());
            dateFinPicker.setDate(new Date());
            refreshTable(); // Recharge les mouvements du jour
        });

        // Ajout des composants au panel de recherche
        searchPanel.add(new JLabel("Du:"));
        searchPanel.add(dateDebutPicker);
        searchPanel.add(new JLabel("Au:"));
        searchPanel.add(dateFinPicker);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        leftPanel.add(titleLabel, BorderLayout.NORTH);
        leftPanel.add(searchPanel, BorderLayout.SOUTH);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(soldeLabel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));  // Espacement réduit
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Création des boutons avec styles harmonisés
        ouvrirBtn = createStyledButton("Ouvrir la caisse", MaterialDesign.MDI_CASH, new Color(76, 175, 80));
        cloturerBtn = createStyledButton("Clôturer la caisse", MaterialDesign.MDI_CLOSE_CIRCLE, new Color(244, 67, 54));
        ajouterBtn = createStyledButton("Nouveau mouvement", MaterialDesign.MDI_PLUS_CIRCLE, new Color(33, 150, 243));
        exporterBtn = createStyledButton("Exporter (CSV)", MaterialDesign.MDI_EXPORT, new Color(156, 39, 176));

        // Configuration des event handlers
        ouvrirBtn.addActionListener(e -> ouvrirCaisse());
        cloturerBtn.addActionListener(e -> cloturerCaisse());
        ajouterBtn.addActionListener(e -> showMouvementDialog());
        exporterBtn.addActionListener(e -> exporterMouvements());

        // Ajout des boutons avec espacement approprié
        actionPanel.add(Box.createHorizontalStrut(4));  // Espacement initial réduit
        actionPanel.add(ouvrirBtn);
        actionPanel.add(Box.createHorizontalStrut(8));  // Espacement entre boutons réduit
        actionPanel.add(cloturerBtn);
        actionPanel.add(Box.createHorizontalStrut(8));
        actionPanel.add(ajouterBtn);
        actionPanel.add(Box.createHorizontalStrut(8));
        actionPanel.add(exporterBtn);

        return actionPanel;
    }

    private void ouvrirCaisse() {
        if (controller.isCaisseOuverte()) {
            JOptionPane.showMessageDialog(mainPanel,
                "La caisse est déjà ouverte",
                "Information",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

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
        caisseOuverte.set(controller.isCaisseOuverte());

        // Mettre à jour l'état des boutons de manière thread-safe
        SwingUtilities.invokeLater(() -> {
            ouvrirBtn.setEnabled(!caisseOuverte.get());
            cloturerBtn.setEnabled(caisseOuverte.get());
            ajouterBtn.setEnabled(caisseOuverte.get());
            exporterBtn.setEnabled(true); // Toujours actif pour permettre l'export historique
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
            updateCurrentDayInfo();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des mouvements", e);
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des mouvements : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCurrentDayInfo() {
        LocalDateTime now = LocalDateTime.now();
        List<MouvementCaisse> mouvementsDuJour = controller.getMouvementsDuJour(now);

        // Calcul des totaux du jour
        double totalEntrees = mouvementsDuJour.stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.ENTREE)
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();

        double totalSorties = mouvementsDuJour.stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.SORTIE)
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();

        // Mise à jour du titre pour inclure les informations du jour
        String date = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String infoJour = String.format("Caisse du %s - Entrées: %.2f € - Sorties: %.2f €",
            date, totalEntrees, totalSorties);

        // Mise à jour du label de titre avec les informations du jour
        JLabel titleLabel = (JLabel) SwingUtilities.getDeepestComponentAt(
            mainPanel, 10, 10); // Récupérer le label de titre
        if (titleLabel != null) {
            titleLabel.setText(infoJour);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        LocalDateTime now = LocalDateTime.now();

        // Filtrer pour n'afficher que les mouvements du jour
        List<MouvementCaisse> mouvementsDuJour = controller.getMouvementsDuJour(now);

        for (MouvementCaisse mouvement : mouvementsDuJour) {
            // Style conditionnel selon le type de mouvement
            String montantFormate = String.format("%,.2f €", mouvement.getMontant());
            if (mouvement.getType() == MouvementCaisse.TypeMouvement.SORTIE) {
                montantFormate = "-" + montantFormate;
            }

            tableModel.addRow(new Object[]{
                mouvement.getDate().format(formatter),
                mouvement.getType().toString(),
                montantFormate,
                mouvement.getDescription()
            });
        }

        // Mettre à jour le solde avec le même format monétaire
        soldeLabel.setText(String.format("Solde: %,.2f €", controller.getSoldeCaisse()));
        updateCaisseState();
    }
    private void refreshTableWithMovements(List<MouvementCaisse> mouvements) {
        tableModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (MouvementCaisse mouvement : mouvements) {
            String montantFormate = String.format("%,.2f €", mouvement.getMontant());
            if (mouvement.getType() == MouvementCaisse.TypeMouvement.SORTIE) {
                montantFormate = "-" + montantFormate;
            }

            tableModel.addRow(new Object[]{
                mouvement.getDate().format(formatter),
                mouvement.getType().toString(),
                montantFormate,
                mouvement.getDescription()
            });
        }
    }

    private void updateTotalInfo(List<MouvementCaisse> mouvements) {
        double totalEntrees = mouvements.stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.ENTREE)
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();

        double totalSorties = mouvements.stream()
            .filter(m -> m.getType() == MouvementCaisse.TypeMouvement.SORTIE)
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();

        String periode = "Période sélectionnée";
        String infoTotal = String.format("%s - Entrées: %.2f € - Sorties: %.2f €",
            periode, totalEntrees, totalSorties);

        // Met à jour le label de titre avec les informations de la période
        JLabel titleLabel = (JLabel) SwingUtilities.getDeepestComponentAt(mainPanel, 10, 10);
        if (titleLabel != null) {
            titleLabel.setText(infoTotal);
        }
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }
}