package com.poissonnerie.view;

import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.Client;
import com.poissonnerie.util.PDFGenerator;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;

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

        initializeComponents();
        loadData();
    }

    private void initializeComponents() {
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Boutons d'action
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton ajouterBtn = new JButton("Ajouter");
        JButton modifierBtn = new JButton("Modifier");
        JButton supprimerBtn = new JButton("Supprimer");
        JButton reglerCreanceBtn = new JButton("Régler créance");
        JButton actualiserBtn = new JButton("Actualiser");
        actualiserBtn.setIcon(UIManager.getIcon("Table.refreshIcon"));

        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);
        buttonPanel.add(reglerCreanceBtn);
        buttonPanel.add(actualiserBtn);

        // Table avec scroll
        JScrollPane scrollPane = new JScrollPane(tableClients);
        tableClients.setFillsViewportHeight(true);

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

        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
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

    public JPanel getMainPanel() {
        return mainPanel;
    }
}