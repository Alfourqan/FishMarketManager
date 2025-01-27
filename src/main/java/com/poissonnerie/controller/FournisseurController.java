package com.poissonnerie.controller;

import com.poissonnerie.model.Fournisseur;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class FournisseurController {
    private static final Logger LOGGER = Logger.getLogger(FournisseurController.class.getName());
    private List<Fournisseur> fournisseurs;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$");

    public FournisseurController() {
        this.fournisseurs = new ArrayList<>();
    }

    public void chargerFournisseurs() {
        LOGGER.info("Chargement des fournisseurs...");
        fournisseurs.clear();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM fournisseurs WHERE supprime = false ORDER BY nom");
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                fournisseurs.add(creerFournisseurDepuisResultSet(rs));
            }
            LOGGER.info("Fournisseurs chargés: " + fournisseurs.size());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de chargement des fournisseurs", e);
            throw new RuntimeException("Erreur de chargement des fournisseurs", e);
        }
    }

    public List<Fournisseur> getFournisseurs() {
        return new ArrayList<>(fournisseurs);
    }

    public void ajouterFournisseur(Fournisseur fournisseur) {
        validateFournisseur(fournisseur);
        LOGGER.info("Ajout fournisseur: " + fournisseur.getNom());

        String sql = "INSERT INTO fournisseurs (nom, contact, telephone, email, adresse, statut, supprime) " +
                    "VALUES (?, ?, ?, ?, ?, ?, false)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preparerStatementFournisseur(pstmt, fournisseur);
                if (pstmt.executeUpdate() == 0) {
                    throw new SQLException("Échec de l'insertion");
                }

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        fournisseur.setId(rs.getInt(1));
                        fournisseurs.add(fournisseur);
                        conn.commit();
                        LOGGER.info("Fournisseur ajouté, ID: " + fournisseur.getId());
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur d'ajout du fournisseur", e);
            throw new RuntimeException("Erreur d'ajout du fournisseur", e);
        }
    }

    public void mettreAJourFournisseur(Fournisseur fournisseur) {
        validateFournisseur(fournisseur);
        LOGGER.info("Mise à jour fournisseur ID: " + fournisseur.getId());

        String sql = "UPDATE fournisseurs SET nom = ?, contact = ?, telephone = ?, email = ?, " +
                    "adresse = ?, statut = ? WHERE id = ? AND supprime = false";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                preparerStatementFournisseur(pstmt, fournisseur);
                pstmt.setInt(7, fournisseur.getId());

                if (pstmt.executeUpdate() == 0) {
                    throw new IllegalStateException("Fournisseur non trouvé: " + fournisseur.getId());
                }

                int index = fournisseurs.indexOf(fournisseur);
                if (index != -1) {
                    fournisseurs.set(index, fournisseur);
                }

                conn.commit();
                LOGGER.info("Fournisseur mis à jour, ID: " + fournisseur.getId());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de mise à jour du fournisseur", e);
            throw new RuntimeException("Erreur de mise à jour du fournisseur", e);
        }
    }

    public void supprimerFournisseur(Fournisseur fournisseur) {
        if (fournisseur == null || fournisseur.getId() <= 0) {
            throw new IllegalArgumentException("Fournisseur invalide");
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE fournisseurs SET supprime = true WHERE id = ? AND supprime = false")) {

            pstmt.setInt(1, fournisseur.getId());
            if (pstmt.executeUpdate() > 0) {
                fournisseurs.remove(fournisseur);
                LOGGER.info("Fournisseur supprimé, ID: " + fournisseur.getId());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de suppression du fournisseur", e);
            throw new RuntimeException("Erreur de suppression du fournisseur", e);
        }
    }

    public List<Fournisseur> rechercherFournisseurs(String terme) {
        if (terme == null || terme.trim().isEmpty()) return new ArrayList<>();

        String sql = "SELECT * FROM fournisseurs WHERE supprime = false AND " +
                    "(LOWER(nom) LIKE LOWER(?) OR LOWER(contact) LIKE LOWER(?) OR " +
                    "LOWER(telephone) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?))";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchTerm = "%" + terme.toLowerCase() + "%";
            for (int i = 1; i <= 4; i++) {
                pstmt.setString(i, searchTerm);
            }

            List<Fournisseur> resultats = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultats.add(creerFournisseurDepuisResultSet(rs));
                }
            }
            return resultats;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de recherche des fournisseurs", e);
            throw new RuntimeException("Erreur de recherche des fournisseurs", e);
        }
    }

    private void validateFournisseur(Fournisseur fournisseur) {
        List<String> errors = new ArrayList<>();

        if (fournisseur == null) {
            throw new IllegalArgumentException("Fournisseur invalide");
        }

        if (fournisseur.getNom() == null || fournisseur.getNom().trim().isEmpty()) {
            errors.add("Nom obligatoire");
        }

        if (fournisseur.getTelephone() == null || !PHONE_PATTERN.matcher(fournisseur.getTelephone()).matches()) {
            errors.add("Téléphone invalide");
        }

        String email = fournisseur.getEmail();
        if (email != null && !email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Email invalide");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    private void preparerStatementFournisseur(PreparedStatement pstmt, Fournisseur fournisseur) throws SQLException {
        pstmt.setString(1, sanitizeInput(fournisseur.getNom()));
        pstmt.setString(2, sanitizeInput(fournisseur.getContact()));
        pstmt.setString(3, sanitizeInput(fournisseur.getTelephone()));
        pstmt.setString(4, sanitizeInput(fournisseur.getEmail()));
        pstmt.setString(5, sanitizeInput(fournisseur.getAdresse()));
        pstmt.setString(6, sanitizeInput(fournisseur.getStatut()));
    }

    private String sanitizeInput(String input) {
        return input == null ? "" : input.trim().replaceAll("[<>\"'%;)(&+]", "").replaceAll("\\s+", " ");
    }

    private Fournisseur creerFournisseurDepuisResultSet(ResultSet rs) throws SQLException {
        Fournisseur fournisseur = new Fournisseur(
            rs.getInt("id"),
            rs.getString("nom"),
            rs.getString("contact"),
            rs.getString("telephone"),
            rs.getString("email"),
            rs.getString("adresse")
        );
        fournisseur.setStatut(rs.getString("statut"));
        return fournisseur;
    }
}