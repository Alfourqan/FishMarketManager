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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ajouterClient(Client client) {
        String sql = "INSERT INTO clients (nom, telephone, adresse, solde) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, client.getNom());
            pstmt.setString(2, client.getTelephone());
            pstmt.setString(3, client.getAdresse());
            pstmt.setDouble(4, client.getSolde());

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    client.setId(generatedKeys.getInt(1));
                }
            }

            clients.add(client);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void mettreAJourClient(Client client) {
        String sql = "UPDATE clients SET nom = ?, telephone = ?, adresse = ?, solde = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, client.getNom());
            pstmt.setString(2, client.getTelephone());
            pstmt.setString(3, client.getAdresse());
            pstmt.setDouble(4, client.getSolde());
            pstmt.setInt(5, client.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void supprimerClient(Client client) {
        String sql = "DELETE FROM clients WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, client.getId());
            pstmt.executeUpdate();
            clients.remove(client);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ajouterCreance(Client client, double montant) {
        client.setSolde(client.getSolde() + montant);
        mettreAJourClient(client);
    }

    public void reglerCreance(Client client, double montant) {
        client.setSolde(client.getSolde() - montant);
        mettreAJourClient(client);
    }
}