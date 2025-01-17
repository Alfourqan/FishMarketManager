package com.poissonnerie.controller;

import com.poissonnerie.model.MouvementCaisse;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CaisseController {
    private List<MouvementCaisse> mouvements;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public CaisseController() {
        this.mouvements = new ArrayList<>();
        // Récupération des variables d'environnement
        this.dbUrl = System.getenv("DATABASE_URL");
        this.dbUser = System.getenv("PGUSER");
        this.dbPassword = System.getenv("PGPASSWORD");
    }

    public void chargerMouvements() throws SQLException {
        mouvements.clear();
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            System.out.println("Connexion à la base de données établie");
            String sql = "SELECT * FROM mouvements_caisse ORDER BY date DESC";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    MouvementCaisse mouvement = new MouvementCaisse(
                        rs.getInt("id"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        MouvementCaisse.TypeMouvement.valueOf(rs.getString("type")),
                        rs.getDouble("montant"),
                        rs.getString("description")
                    );
                    mouvements.add(mouvement);
                }
            }
        }
        System.out.println("Mouvements de caisse chargés : " + mouvements.size() + " mouvements");
    }

    public void enregistrerMouvement(MouvementCaisse mouvement) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO mouvements_caisse (date, type, montant, description) VALUES (?, ?, ?, ?) RETURNING id";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(2, mouvement.getType().toString());
                pstmt.setDouble(3, mouvement.getMontant());
                pstmt.setString(4, mouvement.getDescription());

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        mouvement.setId(rs.getInt(1));
                    }
                }
            }
            chargerMouvements(); // Recharger la liste
        }
    }

    public double calculerSoldeCaisse() {
        return mouvements.stream()
            .mapToDouble(m -> m.getType() == MouvementCaisse.TypeMouvement.ENTREE ?
                m.getMontant() : -m.getMontant())
            .sum();
    }

    public List<MouvementCaisse> getMouvements() {
        return new ArrayList<>(mouvements);
    }

    public List<MouvementCaisse> getMouvementsJour(LocalDateTime date) {
        return mouvements.stream()
            .filter(m -> m.getDate().toLocalDate().equals(date.toLocalDate()))
            .toList();
    }

    public String exporterMouvementsCSV(List<MouvementCaisse> mouvements) {
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Montant,Description\n");

        for (MouvementCaisse m : mouvements) {
            csv.append(String.format("%s,%s,%.2f,\"%s\"\n",
                m.getDate().toString(),
                m.getType().toString(),
                m.getMontant(),
                m.getDescription().replace("\"", "\"\"")));
        }

        return csv.toString();
    }
}