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
        return new ArrayList<>(clients);
    }

    public void chargerClients() {
        LOGGER.info("Chargement des clients...");
        clients.clear();
        String sql = "SELECT id, nom, telephone, adresse, solde FROM clients ORDER BY nom";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Client client = new Client(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("telephone"),
                    rs.getString("adresse"),
                    rs.getDouble("solde")
                );
                clients.add(client);
                LOGGER.fine("Client chargé: ID=" + client.getId() + ", Nom=" + client.getNom());
            }

            LOGGER.info("Clients chargés avec succès: " + clients.size() + " clients");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des clients", e);
            throw new RuntimeException("Erreur lors du chargement des clients", e);
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
        if (input == null) return "";
        return input.replaceAll("[<>\"'%;)(&+]", "");
    }

    public void ajouterClient(Client client) {
        validateClient(client);
        LOGGER.info("Tentative d'ajout d'un nouveau client: " + client.getNom());

        String sql = "INSERT INTO clients (nom, telephone, adresse, solde) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, sanitizeInput(client.getNom()));
                pstmt.setString(2, sanitizeInput(client.getTelephone()));
                pstmt.setString(3, sanitizeInput(client.getAdresse()));
                pstmt.setDouble(4, client.getSolde());

                int rows = pstmt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("L'ajout du client a échoué");
                }

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        client.setId(rs.getInt(1));
                        clients.add(client);
                        conn.commit();
                        LOGGER.info("Client ajouté avec succès, ID: " + client.getId());
                    } else {
                        throw new SQLException("Impossible d'obtenir l'ID du client");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du client", e);
                throw new RuntimeException("Erreur lors de l'ajout du client: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de l'ajout du client", e);
            throw new RuntimeException("Erreur de connexion lors de l'ajout du client: " + e.getMessage(), e);
        }
    }

    public void mettreAJourClient(Client client) {
        validateClient(client);
        LOGGER.info("Tentative de mise à jour du client ID: " + client.getId());

        String sql = "UPDATE clients SET nom = ?, telephone = ?, adresse = ? WHERE id = ? AND solde >= 0";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, sanitizeInput(client.getNom()));
                pstmt.setString(2, sanitizeInput(client.getTelephone()));
                pstmt.setString(3, sanitizeInput(client.getAdresse()));
                pstmt.setInt(4, client.getId());

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new IllegalStateException("Client non trouvé ou mise à jour impossible: " + client.getId());
                }

                int index = clients.indexOf(client);
                if (index != -1) {
                    clients.set(index, client);
                }

                conn.commit();
                LOGGER.info("Client mis à jour avec succès, ID: " + client.getId());
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du client", e);
                throw new RuntimeException("Erreur lors de la mise à jour du client: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de la mise à jour du client", e);
            throw new RuntimeException("Erreur de connexion lors de la mise à jour du client: " + e.getMessage(), e);
        }
    }

    public void supprimerClient(Client client) {
        if (client == null || client.getId() <= 0) {
            throw new IllegalArgumentException("Client invalide");
        }

        LOGGER.info("Tentative de suppression du client ID: " + client.getId());

        // Vérifier d'abord si le client a des ventes associées
        String checkVentesSql = "SELECT COUNT(*) FROM ventes WHERE client_id = ?";
        String deleteClientSql = "DELETE FROM clients WHERE id = ? AND solde = 0";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Vérifier les ventes
                try (PreparedStatement checkStmt = conn.prepareStatement(checkVentesSql)) {
                    checkStmt.setInt(1, client.getId());
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new IllegalStateException("Impossible de supprimer le client car il a des ventes associées");
                    }
                }

                // Supprimer le client
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteClientSql)) {
                    deleteStmt.setInt(1, client.getId());
                    int rowsDeleted = deleteStmt.executeUpdate();

                    if (rowsDeleted == 0) {
                        throw new IllegalStateException("Client non trouvé ou solde non nul");
                    }

                    clients.remove(client);
                    conn.commit();
                    LOGGER.info("Client supprimé avec succès, ID: " + client.getId());
                }
            } catch (SQLException | IllegalStateException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du client", e);
                throw new RuntimeException("Erreur lors de la suppression du client: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de la suppression du client", e);
            throw new RuntimeException("Erreur de connexion lors de la suppression du client: " + e.getMessage(), e);
        }
    }

    public void reglerCreance(Client client, double montant) {
        if (client == null || client.getId() <= 0) {
            throw new IllegalArgumentException("Client invalide");
        }
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant du règlement doit être positif");
        }
        if (montant > client.getSolde()) {
            throw new IllegalArgumentException("Le montant du règlement ne peut pas être supérieur au solde dû");
        }

        LOGGER.info("Tentative de règlement de créance pour le client ID: " + client.getId() + ", montant: " + montant);

        String updateClientSql = "UPDATE clients SET solde = solde - ? WHERE id = ? AND solde >= ?";
        String insertReglementSql = "INSERT INTO reglements_clients (client_id, montant, type_paiement, commentaire) VALUES (?, ?, ?, ?)";
        String insertMouvementSql = "INSERT INTO mouvements_caisse (type, montant, description) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Mise à jour du solde client
                try (PreparedStatement updateStmt = conn.prepareStatement(updateClientSql)) {
                    updateStmt.setDouble(1, montant);
                    updateStmt.setInt(2, client.getId());
                    updateStmt.setDouble(3, montant);

                    int rowsUpdated = updateStmt.executeUpdate();
                    if (rowsUpdated == 0) {
                        throw new IllegalStateException("Impossible de mettre à jour le solde du client");
                    }

                    // Enregistrement du règlement client
                    try (PreparedStatement reglementStmt = conn.prepareStatement(insertReglementSql)) {
                        reglementStmt.setInt(1, client.getId());
                        reglementStmt.setDouble(2, montant);
                        reglementStmt.setString(3, "ESPECES"); // Par défaut en espèces
                        reglementStmt.setString(4, "Règlement de créance");
                        reglementStmt.executeUpdate();
                    }

                    // Enregistrement du mouvement de caisse
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertMouvementSql)) {
                        insertStmt.setString(1, "ENTREE");
                        insertStmt.setDouble(2, montant);
                        insertStmt.setString(3, "Règlement créance - Client: " + sanitizeInput(client.getNom()));
                        insertStmt.executeUpdate();
                    }

                    client.setSolde(client.getSolde() - montant);
                    conn.commit();
                    LOGGER.info("Créance réglée avec succès pour le client " + client.getNom() +
                            " - Montant: " + montant + "€");
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors du règlement de la créance", e);
                throw new RuntimeException("Erreur lors du règlement de la créance: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors du règlement de la créance", e);
            throw new RuntimeException("Erreur de connexion lors du règlement de la créance: " + e.getMessage(), e);
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