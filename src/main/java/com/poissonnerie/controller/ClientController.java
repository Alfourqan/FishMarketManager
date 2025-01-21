package com.poissonnerie.controller;

import com.poissonnerie.model.Client;
import com.poissonnerie.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ClientController {
    private static final Logger LOGGER = Logger.getLogger(ClientController.class.getName());
    private final List<Client> clients;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s]*$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 20;

    public ClientController() {
        this.clients = new ArrayList<>();
        LOGGER.setLevel(Level.ALL);
    }

    public List<Client> getClients() {
        if (clients.isEmpty()) {
            chargerClients();
        }
        LOGGER.info("Retour de " + clients.size() + " clients, dont " +
            clients.stream().filter(c -> c.getSolde() > 0).count() + " avec solde positif");
        return new ArrayList<>(clients);
    }

    public void chargerClients() {
        LOGGER.info("Chargement des clients...");
        clients.clear();
        String sql = "SELECT id, nom, telephone, adresse, solde FROM clients ORDER BY nom";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try (ResultSet rs = stmt.executeQuery()) {
                int count = 0;
                double totalSoldes = 0.0;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nom = rs.getString("nom");
                    String telephone = rs.getString("telephone");
                    String adresse = rs.getString("adresse");
                    double solde = rs.getDouble("solde");

                    Client client = new Client(id, nom, telephone, adresse, solde);
                    clients.add(client);
                    count++;
                    totalSoldes += solde;

                    LOGGER.fine("Client chargé: ID=" + id + ", Nom=" + nom + ", Solde=" + solde);
                }
                conn.commit();
                LOGGER.info("Clients chargés avec succès: " + count + " clients, Solde total: " + totalSoldes);

                // Log des clients avec solde positif
                clients.stream()
                    .filter(c -> c.getSolde() > 0)
                    .forEach(c -> LOGGER.info("Client avec créance: " + c.getNom() + ", Solde: " + c.getSolde()));
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors du chargement des clients", e);
                throw new RuntimeException("Erreur lors du chargement des clients", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion à la base de données", e);
            throw new RuntimeException("Erreur de connexion à la base de données", e);
        }
    }

    private void validateClient(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Le client ne peut pas être null");
        }
        if (client.getNom() == null || client.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire");
        }
        if (client.getNom().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Le nom du client est trop long (max " + MAX_NAME_LENGTH + " caractères)");
        }
        if (client.getTelephone() != null && !client.getTelephone().isEmpty()) {
            if (client.getTelephone().length() > MAX_PHONE_LENGTH) {
                throw new IllegalArgumentException("Le numéro de téléphone est trop long (max " + MAX_PHONE_LENGTH + " caractères)");
            }
            if (!PHONE_PATTERN.matcher(client.getTelephone()).matches()) {
                throw new IllegalArgumentException("Format de téléphone invalide");
            }
        }
        if (client.getAdresse() != null && client.getAdresse().length() > MAX_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("L'adresse est trop longue (max " + MAX_ADDRESS_LENGTH + " caractères)");
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Échapper les caractères spéciaux HTML et SQL
        return input.replaceAll("[<>\"'%;)(&+]", "");
    }

    public void ajouterClient(Client client) {
        validateClient(client); // Validation d'abord
        LOGGER.info("Tentative d'ajout d'un nouveau client: " + client.getNom());

        String sql = "INSERT INTO clients (nom, telephone, adresse, solde) VALUES (?, ?, ?, ?)";
        String getIdSql = "SELECT last_insert_rowid() as id";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, sanitizeInput(client.getNom()));
                pstmt.setString(2, sanitizeInput(client.getTelephone()));
                pstmt.setString(3, sanitizeInput(client.getAdresse()));
                pstmt.setDouble(4, client.getSolde());
                pstmt.executeUpdate();

                // Récupérer l'ID généré
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(getIdSql)) {
                    if (rs.next()) {
                        client.setId(rs.getInt("id"));
                        clients.add(client);
                        LOGGER.info("Client ajouté avec succès, ID: " + client.getId());
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du client", e);
                throw new RuntimeException("Erreur lors de l'ajout du client", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de l'ajout du client", e);
            throw new RuntimeException("Erreur de connexion lors de l'ajout du client", e);
        }
    }

    public void mettreAJourClient(Client client) {
        LOGGER.info("Tentative de mise à jour du client ID: " + client.getId());
        validateClient(client);

        String sql = "UPDATE clients SET nom = ?, telephone = ?, adresse = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, sanitizeInput(client.getNom()));
                pstmt.setString(2, sanitizeInput(client.getTelephone()));
                pstmt.setString(3, sanitizeInput(client.getAdresse()));
                pstmt.setInt(4, client.getId());

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new IllegalStateException("Client non trouvé: " + client.getId());
                }

                // Mettre à jour la liste en mémoire
                int index = clients.indexOf(client);
                if (index != -1) {
                    clients.set(index, client);
                }

                conn.commit();
                LOGGER.info("Client mis à jour avec succès, ID: " + client.getId());
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du client", e);
                throw new RuntimeException("Erreur lors de la mise à jour du client", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de la mise à jour du client", e);
            throw new RuntimeException("Erreur de connexion lors de la mise à jour du client", e);
        }
    }

    public void supprimerClient(Client client) {
        if (client == null || client.getId() <= 0) {
            LOGGER.warning("Tentative de suppression d'un client invalide");
            throw new IllegalArgumentException("Client invalide");
        }

        LOGGER.info("Tentative de suppression du client ID: " + client.getId());
        String sql = "DELETE FROM clients WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, client.getId());

                int rowsDeleted = pstmt.executeUpdate();
                if (rowsDeleted == 0) {
                    throw new IllegalStateException("Client non trouvé: " + client.getId());
                }

                clients.remove(client);
                conn.commit();
                LOGGER.info("Client supprimé avec succès, ID: " + client.getId());
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du client", e);
                throw new RuntimeException("Erreur lors de la suppression du client", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de la suppression du client", e);
            throw new RuntimeException("Erreur de connexion lors de la suppression du client", e);
        }
    }

    public void reglerCreance(Client client, double montant) {
        if (client == null || client.getId() <= 0) {
            LOGGER.warning("Tentative de règlement de créance pour un client invalide");
            throw new IllegalArgumentException("Client invalide");
        }
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant du règlement doit être positif");
        }
        if (montant > client.getSolde()) {
            throw new IllegalArgumentException("Le montant du règlement ne peut pas être supérieur au solde dû");
        }

        LOGGER.info("Tentative de règlement de créance pour le client ID: " + client.getId() + ", montant: " + montant);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlUpdateClient = "UPDATE clients SET solde = solde - ? WHERE id = ? AND solde >= ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateClient)) {
                    pstmt.setDouble(1, montant);
                    pstmt.setInt(2, client.getId());
                    pstmt.setDouble(3, montant);

                    int rowsUpdated = pstmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        String sqlMouvement = "INSERT INTO mouvements_caisse (type, montant, description) VALUES (?, ?, ?)";
                        try (PreparedStatement pstmtMvt = conn.prepareStatement(sqlMouvement)) {
                            pstmtMvt.setString(1, "ENTREE");
                            pstmtMvt.setDouble(2, montant);
                            pstmtMvt.setString(3, "Règlement créance - Client: " + sanitizeInput(client.getNom()));
                            pstmtMvt.executeUpdate();
                        }

                        client.setSolde(client.getSolde() - montant);
                        conn.commit();
                        LOGGER.info("Créance réglée avec succès pour le client " + client.getNom() +
                            " - Montant: " + montant + "€");
                    } else {
                        conn.rollback();
                        throw new IllegalStateException("Impossible de régler la créance. Vérifiez le solde du client.");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors du règlement de la créance", e);
                throw new RuntimeException("Erreur lors du règlement de la créance", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors du règlement de la créance", e);
            throw new RuntimeException("Erreur de connexion lors du règlement de la créance", e);
        }
    }

    public void ajouterClientTest() {
        LOGGER.info("Ajout d'un client test avec créance...");
        try {
            Client clientTest = new Client(0, "Client Test Créance", "0123456789", "1 Rue Test", 150.50);
            ajouterClient(clientTest);
            LOGGER.info("Client test ajouté avec succès: " + clientTest.getNom() + ", Solde: " + clientTest.getSolde());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du client test", e);
            throw new RuntimeException("Erreur lors de l'ajout du client test: " + e.getMessage(), e);
        }
    }
}