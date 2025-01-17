package com.poissonnerie.controller;

import com.poissonnerie.model.Client;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientController {
    private final List<Client> clients = new ArrayList<>();

    public List<Client> getClients() {
        return clients;
    }

    public void chargerClients() {
        clients.clear();
        String sql = "SELECT * FROM clients";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Client client = new Client(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("telephone"),
                    rs.getString("adresse"),
                    rs.getDouble("solde")
                );
                clients.add(client);
            }
            System.out.println("Clients chargés avec succès: " + clients.size() + " clients");
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des clients: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void ajouterClient(Client client) {
        String sql = "INSERT INTO clients (nom, telephone, adresse, solde) VALUES (?, ?, ?, ?)";
        String getIdSql = "SELECT last_insert_rowid() as id";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, client.getNom());
                pstmt.setString(2, client.getTelephone());
                pstmt.setString(3, client.getAdresse());
                pstmt.setDouble(4, client.getSolde());
                pstmt.executeUpdate();

                // Récupérer l'ID généré
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(getIdSql)) {
                    if (rs.next()) {
                        client.setId(rs.getInt("id"));
                        clients.add(client);
                    }
                }
                conn.commit();
                System.out.println("Client ajouté avec succès: " + client.getNom());
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erreur lors de l'ajout du client: " + e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur fatale lors de l'ajout du client: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout du client", e);
        }
    }

    public void mettreAJourClient(Client client) {
        String sql = "UPDATE clients SET nom = ?, telephone = ?, adresse = ?, solde = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, client.getNom());
                pstmt.setString(2, client.getTelephone());
                pstmt.setString(3, client.getAdresse());
                pstmt.setDouble(4, client.getSolde());
                pstmt.setInt(5, client.getId());

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    conn.commit();
                    System.out.println("Client mis à jour avec succès: " + client.getNom());
                } else {
                    conn.rollback();
                    throw new SQLException("Aucun client mis à jour avec l'ID: " + client.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erreur lors de la mise à jour du client: " + e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur fatale lors de la mise à jour du client: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour du client", e);
        }
    }

    public void supprimerClient(Client client) {
        String sql = "DELETE FROM clients WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, client.getId());
                int rowsDeleted = pstmt.executeUpdate();

                if (rowsDeleted > 0) {
                    clients.remove(client);
                    conn.commit();
                    System.out.println("Client supprimé avec succès: " + client.getNom());
                } else {
                    conn.rollback();
                    throw new SQLException("Aucun client supprimé avec l'ID: " + client.getId());
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erreur lors de la suppression du client: " + e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur fatale lors de la suppression du client: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du client", e);
        }
    }

    public void reglerCreance(Client client, double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant du règlement doit être positif");
        }

        if (montant > client.getSolde()) {
            throw new IllegalArgumentException("Le montant du règlement ne peut pas être supérieur au solde dû");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE clients SET solde = solde - ? WHERE id = ? AND solde >= ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setDouble(1, montant);
                    pstmt.setInt(2, client.getId());
                    pstmt.setDouble(3, montant);

                    int rowsUpdated = pstmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        // Mise à jour du modèle local
                        client.setSolde(client.getSolde() - montant);

                        // Enregistrer le mouvement de caisse
                        String sqlMouvement = "INSERT INTO mouvements_caisse (type, montant, description) VALUES ('ENTREE', ?, ?)";
                        try (PreparedStatement pstmtMvt = conn.prepareStatement(sqlMouvement)) {
                            pstmtMvt.setDouble(1, montant);
                            pstmtMvt.setString(2, "Règlement créance - Client: " + client.getNom());
                            pstmtMvt.executeUpdate();
                        }

                        conn.commit();
                        System.out.println("Créance réglée avec succès pour le client " + client.getNom() + 
                                         " - Montant: " + montant + "€");
                    } else {
                        conn.rollback();
                        throw new SQLException("Impossible de régler la créance. Vérifiez le solde du client.");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erreur lors du règlement de la créance: " + e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur fatale lors du règlement de la créance: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du règlement de la créance", e);
        }
    }

    public void ajouterCreance(Client client, double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant de la créance doit être positif");
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE clients SET solde = solde + ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setDouble(1, montant);
                    pstmt.setInt(2, client.getId());

                    int rowsUpdated = pstmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        client.setSolde(client.getSolde() + montant);
                        conn.commit();
                        System.out.println("Créance ajoutée avec succès pour le client " + client.getNom() + 
                                         " - Montant: " + montant + "€");
                    } else {
                        conn.rollback();
                        throw new SQLException("Impossible d'ajouter la créance. Client non trouvé.");
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erreur lors de l'ajout de la créance: " + e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erreur fatale lors de l'ajout de la créance: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout de la créance", e);
        }
    }
}