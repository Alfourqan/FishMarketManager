package com.poissonnerie.view;

import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.Client;
import com.poissonnerie.util.PDFGenerator;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.kordamp.ikonli.swing.FontIcon;

public class ClientViewSwing {
    private final JPanel mainPanel;
    private final ClientController controller;
    private final JTable tableClients;
    private final DefaultTableModel tableModel;

    public ClientViewSwing() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        controller = new ClientController();

        // Création du modèle de table
        String[] columnNames = {"Nom", "Téléphone", "Adresse", "Solde"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableClients = new JTable(tableModel);
        tableClients.setRowHeight(30);
        tableClients.setFont(new Font(tableClients.getFont().getName(), Font.PLAIN, 13));

        initializeComponents();
        loadData();
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Effet de survol
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(236, 239, 241));

        // Panel des boutons avec style moderne
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 15, 5)
        ));
        buttonPanel.setBackground(new Color(236, 239, 241));

        // Style moderne pour la table et ses en-têtes
        tableClients.setShowGrid(true);
        tableClients.setGridColor(new Color(226, 232, 240));
        tableClients.setBackground(new Color(255, 255, 255));
        tableClients.setSelectionBackground(new Color(219, 234, 254));
        tableClients.setSelectionForeground(new Color(15, 23, 42));
        tableClients.setRowHeight(45);
        tableClients.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tableClients.setIntercellSpacing(new Dimension(1, 1));

        // Style des lignes alternées
        tableClients.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                // Ajouter un padding aux cellules
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
            }
        });

        // Style amélioré des en-têtes avec icônes
        JTableHeader header = tableClients.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);

                // Configuration du style amélioré
                label.setHorizontalAlignment(JLabel.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(51, 65, 85)),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)
                ));
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                label.setBackground(new Color(31, 41, 55));
                label.setForeground(Color.WHITE);
                label.setOpaque(true);

                // Ajout des icônes avec style amélioré
                FontIcon icon = null;
                switch (column) {
                    case 0: // Nom
                        icon = FontIcon.of(MaterialDesign.MDI_ACCOUNT_CIRCLE);
                        break;
                    case 1: // Téléphone
                        icon = FontIcon.of(MaterialDesign.MDI_PHONE_CLASSIC);
                        break;
                    case 2: // Adresse
                        icon = FontIcon.of(MaterialDesign.MDI_MAP_MARKER_RADIUS);
                        break;
                    case 3: // Solde
                        icon = FontIcon.of(MaterialDesign.MDI_CURRENCY_EUR);
                        break;
                }

                if (icon != null) {
                    icon.setIconSize(18);
                    icon.setIconColor(Color.WHITE);
                    label.setIcon(icon);
                    label.setIconTextGap(12);
                }

                return label;
            }
        });

        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 56));


        // Configuration du scroll pane avec style moderne
        JScrollPane scrollPane = new JScrollPane(tableClients);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Création des boutons avec style moderne
        JButton ajouterBtn = createStyledButton("Ajouter", new Color(76, 175, 80));
        JButton modifierBtn = createStyledButton("Modifier", new Color(33, 150, 243));
        JButton supprimerBtn = createStyledButton("Supprimer", new Color(244, 67, 54));
        JButton reglerCreanceBtn = createStyledButton("Régler créance", new Color(255, 152, 0));
        JButton actualiserBtn = createStyledButton("Actualiser", new Color(156, 39, 176));

        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);
        buttonPanel.add(reglerCreanceBtn);
        buttonPanel.add(actualiserBtn);

        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Gestionnaires d'événements
        ajouterBtn.addActionListener(e -> showClientDialog(null));
        modifierBtn.addActionListener(e -> {
            int selectedRow = tableClients.getSelectedRow();
            if (selectedRow >= 0) {
                showClientDialog(controller.getClients().get(selectedRow));
            } else {
                JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez sélectionner un client à modifier",
                    "Aucune sélection",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        supprimerBtn.addActionListener(e -> {
            int selectedRow = tableClients.getSelectedRow();
            if (selectedRow >= 0) {
                Client client = controller.getClients().get(selectedRow);
                if (client.getSolde() > 0) {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Impossible de supprimer un client ayant une créance en cours",
                        "Suppression impossible",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (JOptionPane.showConfirmDialog(mainPanel,
                    "Êtes-vous sûr de vouloir supprimer ce client ?",
                    "Confirmation de suppression",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    try {
                        controller.supprimerClient(client);
                        refreshTable();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainPanel,
                            "Erreur lors de la suppression du client : " + ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez sélectionner un client à supprimer",
                    "Aucune sélection",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        actualiserBtn.addActionListener(e -> {
            try {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
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
            } finally {
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }
        });

        reglerCreanceBtn.addActionListener(e -> {
            int selectedRow = tableClients.getSelectedRow();
            if (selectedRow >= 0) {
                Client client = controller.getClients().get(selectedRow);
                if (client.getSolde() > 0) {
                    showReglerCreanceDialog(client);
                } else {
                    JOptionPane.showMessageDialog(mainPanel,
                        "Ce client n'a pas de créance à régler",
                        "Aucune créance",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(mainPanel,
                    "Veuillez sélectionner un client",
                    "Aucune sélection",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Client client : controller.getClients()) {
            tableModel.addRow(new Object[]{
                client.getNom(),
                client.getTelephone(),
                client.getAdresse(),
                String.format("%.2f €", client.getSolde())
            });
        }
    }

    private void showClientDialog(Client client) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
                                   client == null ? "Nouveau client" : "Modifier client",
                                   true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Champs du formulaire
        JTextField nomField = new JTextField(20);
        JTextField telephoneField = new JTextField(20);
        JTextArea adresseArea = new JTextArea(3, 20);
        adresseArea.setLineWrap(true);
        adresseArea.setWrapStyleWord(true);
        JScrollPane adresseScroll = new JScrollPane(adresseArea);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nom:"), gbc);
        gbc.gridx = 1;
        panel.add(nomField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Téléphone:"), gbc);
        gbc.gridx = 1;
        panel.add(telephoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Adresse:"), gbc);
        gbc.gridx = 1;
        panel.add(adresseScroll, gbc);

        // Pré-remplissage si modification
        if (client != null) {
            nomField.setText(client.getNom());
            telephoneField.setText(client.getTelephone());
            adresseArea.setText(client.getAdresse());
        }

        // Boutons
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annuler");

        okButton.addActionListener(evt -> {
            try {
                String nom = nomField.getText().trim();
                String telephone = telephoneField.getText().trim();
                String adresse = adresseArea.getText().trim();

                if (nom.isEmpty()) {
                    throw new IllegalArgumentException("Le nom est obligatoire");
                }

                if (!telephone.isEmpty() && !telephone.matches("^[0-9+\\-\\s]*$")) {
                    throw new IllegalArgumentException("Le numéro de téléphone contient des caractères invalides");
                }

                if (client == null) {
                    Client nouveauClient = new Client(0, nom, telephone, adresse, 0.0);
                    controller.ajouterClient(nouveauClient);
                } else {
                    client.setNom(nom);
                    client.setTelephone(telephone);
                    client.setAdresse(adresse);
                    controller.mettreAJourClient(client);
                }
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

    private void showReglerCreanceDialog(Client client) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel),
                                   "Régler créance",
                                   true);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // En-tête avec les informations du client
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(0, 0, 10, 0)
        ));

        JLabel clientLabel = new JLabel("<html><b>Client:</b> " + client.getNom() + "</html>");
        JLabel soldeLabel = new JLabel(String.format("<html><b>Solde actuel:</b> <font color='red'>%.2f €</font></html>",
            client.getSolde()));

        headerPanel.add(clientLabel, BorderLayout.NORTH);
        headerPanel.add(soldeLabel, BorderLayout.CENTER);

        // Panneau de saisie du montant
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel montantLabel = new JLabel("Montant à régler:");
        JTextField montantField = new JTextField(10);
        JButton montantTotalBtn = new JButton("Régler tout");

        inputPanel.add(montantLabel);
        inputPanel.add(montantField);
        inputPanel.add(montantTotalBtn);

        // Configuration des composants
        montantTotalBtn.addActionListener(e -> {
            montantField.setText(String.format("%.2f", client.getSolde()));
            montantField.selectAll();
            montantField.requestFocus();
        });

        // Ajout des composants au panneau principal
        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(headerPanel, gbc);

        gbc.gridy = 1;
        panel.add(inputPanel, gbc);

        // Boutons d'action
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("Valider");
        JButton cancelButton = new JButton("Annuler");

        okButton.addActionListener(e -> {
            try {
                String montantText = montantField.getText().trim().replace(",", ".");
                if (montantText.isEmpty()) {
                    throw new IllegalArgumentException("Veuillez entrer un montant");
                }

                double montant;
                try {
                    montant = Double.parseDouble(montantText);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Montant invalide");
                }

                if (montant <= 0) {
                    throw new IllegalArgumentException("Le montant doit être positif");
                }

                if (montant > client.getSolde()) {
                    throw new IllegalArgumentException(
                        String.format("Le montant ne peut pas dépasser le solde actuel (%.2f €)",
                        client.getSolde())
                    );
                }

                // Tentative de règlement
                controller.reglerCreance(client, montant);

                // Générer le reçu de règlement
                String nomFichier = String.format("reglement_%s_%d.pdf",
                    client.getNom().toLowerCase().replace(" ", "_"),
                    System.currentTimeMillis());
                PDFGenerator.genererReglementCreance(client, montant,
                    client.getSolde(), nomFichier);

                // Rafraîchir l'affichage
                refreshTable();

                // Fermer le dialogue
                dialog.dispose();

                // Afficher confirmation
                JOptionPane.showMessageDialog(mainPanel,
                    String.format("<html>" +
                        "<div style='margin-bottom: 10px'>Règlement effectué avec succès</div>" +
                        "<table>" +
                        "<tr><td><b>Montant réglé:</b></td><td style='padding-left: 10px'>%.2f €</td></tr>" +
                        "<tr><td><b>Nouveau solde:</b></td><td style='padding-left: 10px'>%.2f €</td></tr>" +
                        "<tr><td colspan='2'><br>Reçu généré: <b>%s</b></td></tr>" +
                        "</table></html>",
                        montant, client.getSolde(), nomFichier),
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                    ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // Style des boutons
        okButton.setPreferredSize(cancelButton.getPreferredSize());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Finalisation du dialogue
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setResizable(false);

        // Sélectionner le champ de montant au démarrage
        SwingUtilities.invokeLater(() -> {
            montantField.requestFocusInWindow();
            montantField.selectAll();
        });

        dialog.setVisible(true);
    }

    private void loadData() {
        try {
            System.out.println("Chargement des clients...");
            controller.chargerClients();
            refreshTable();
            System.out.println("Clients chargés avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des clients: " + e.getMessage());
            JOptionPane.showMessageDialog(mainPanel,
                "Erreur lors du chargement des clients : " + e.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}