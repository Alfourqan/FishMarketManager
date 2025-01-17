package com.poissonnerie.view;

import com.poissonnerie.controller.ClientController;
import com.poissonnerie.model.Client;
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

        buttonPanel.add(ajouterBtn);
        buttonPanel.add(modifierBtn);
        buttonPanel.add(supprimerBtn);
        buttonPanel.add(reglerCreanceBtn);

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
                        controller.supprimerClient(controller.getClients().get(selectedRow));
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

        // Affichage des informations du client
        JLabel clientLabel = new JLabel("Client: " + client.getNom());
        JLabel soldeLabel = new JLabel(String.format("Solde actuel: %.2f €", client.getSolde()));
        JTextField montantField = new JTextField(10);

        gbc.gridwidth = 2;
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(clientLabel, gbc);

        gbc.gridy = 1;
        panel.add(soldeLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Montant à régler:"), gbc);
        gbc.gridx = 1;
        panel.add(montantField, gbc);

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Annuler");

        okButton.addActionListener(e -> {
            try {
                String montantText = montantField.getText().trim().replace(",", ".");
                if (montantText.isEmpty()) {
                    throw new NumberFormatException("Veuillez entrer un montant");
                }

                double montant = Double.parseDouble(montantText);

                if (montant <= 0) {
                    throw new IllegalArgumentException("Le montant doit être positif");
                }

                if (montant > client.getSolde()) {
                    throw new IllegalArgumentException("Le montant ne peut pas être supérieur au solde dû");
                }

                controller.reglerCreance(client, montant);
                refreshTable();
                dialog.dispose();

                JOptionPane.showMessageDialog(mainPanel,
                    String.format("Règlement de %.2f € effectué avec succès", montant),
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Veuillez entrer un montant valide",
                    "Erreur de saisie",
                    JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog,
                    ex.getMessage(),
                    "Erreur de validation",
                    JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Erreur lors du règlement : " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
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