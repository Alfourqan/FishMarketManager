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
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[0-9+\\-\\s]*$"
    );

    public FournisseurController() {
        this.fournisseurs = new ArrayList<>();
    }

    public void chargerFournisseurs() {
        LOGGER.info("Chargement des fournisseurs...");
        fournisseurs.clear();
        String sql = "SELECT * FROM fournisseurs WHERE supprime = false ORDER BY nom";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Fournisseur fournisseur = creerFournisseurDepuisResultSet(rs);
                    fournisseurs.add(fournisseur);
                }
                conn.commit();
                LOGGER.info("Fournisseurs chargés avec succès: " + fournisseurs.size() + " enregistrements");
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors du chargement des fournisseurs", e);
                throw new RuntimeException("Erreur lors du chargement des fournisseurs", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion à la base de données", e);
            throw new RuntimeException("Erreur de connexion à la base de données", e);
        }
    }

    public List<Fournisseur> getFournisseurs() {
        return new ArrayList<>(fournisseurs); // Retourne une copie pour éviter les modifications externes
    }

    public void ajouterFournisseur(Fournisseur fournisseur) {
        LOGGER.info("Tentative d'ajout d'un nouveau fournisseur: " + fournisseur.getNom());
        validateFournisseur(fournisseur);

        String sql = "INSERT INTO fournisseurs (nom, contact, telephone, email, adresse, statut) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            conn.setAutoCommit(false);
            try {
                preparerStatementFournisseur(pstmt, fournisseur);
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        fournisseur.setId(generatedKeys.getInt(1));
                        fournisseurs.add(fournisseur);
                        LOGGER.info("Fournisseur ajouté avec succès, ID: " + fournisseur.getId());
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout du fournisseur", e);
                throw new RuntimeException("Erreur lors de l'ajout du fournisseur", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de l'ajout du fournisseur", e);
            throw new RuntimeException("Erreur de connexion lors de l'ajout du fournisseur", e);
        }
    }

    public void mettreAJourFournisseur(Fournisseur fournisseur) {
        LOGGER.info("Tentative de mise à jour du fournisseur ID: " + fournisseur.getId());
        validateFournisseur(fournisseur);

        String sql = "UPDATE fournisseurs SET nom = ?, contact = ?, telephone = ?, email = ?, adresse = ?, statut = ? WHERE id = ? AND supprime = false";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                preparerStatementFournisseur(pstmt, fournisseur);
                pstmt.setInt(7, fournisseur.getId());

                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new IllegalStateException("Fournisseur non trouvé ou déjà supprimé: " + fournisseur.getId());
                }

                // Mettre à jour la liste en mémoire
                int index = fournisseurs.indexOf(fournisseur);
                if (index != -1) {
                    fournisseurs.set(index, fournisseur);
                }

                conn.commit();
                LOGGER.info("Fournisseur mis à jour avec succès, ID: " + fournisseur.getId());
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour du fournisseur", e);
                throw new RuntimeException("Erreur lors de la mise à jour du fournisseur", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de la mise à jour du fournisseur", e);
            throw new RuntimeException("Erreur de connexion lors de la mise à jour du fournisseur", e);
        }
    }

    public void supprimerFournisseur(Fournisseur fournisseur) {
        if (fournisseur == null || fournisseur.getId() <= 0) {
            LOGGER.warning("Tentative de suppression d'un fournisseur invalide");
            throw new IllegalArgumentException("Fournisseur invalide");
        }

        LOGGER.info("Tentative de suppression du fournisseur ID: " + fournisseur.getId());
        String sql = "UPDATE fournisseurs SET supprime = true WHERE id = ? AND supprime = false";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                pstmt.setInt(1, fournisseur.getId());
                int rowsUpdated = pstmt.executeUpdate();

                if (rowsUpdated == 0) {
                    throw new IllegalStateException("Fournisseur non trouvé ou déjà supprimé: " + fournisseur.getId());
                }

                fournisseurs.remove(fournisseur);
                conn.commit();
                LOGGER.info("Fournisseur supprimé avec succès, ID: " + fournisseur.getId());
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la suppression du fournisseur", e);
                throw new RuntimeException("Erreur lors de la suppression du fournisseur", e);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur de connexion lors de la suppression du fournisseur", e);
            throw new RuntimeException("Erreur de connexion lors de la suppression du fournisseur", e);
        }
    }

    public List<Fournisseur> rechercherFournisseurs(String terme) {
        if (terme == null || terme.trim().isEmpty()) {
            LOGGER.warning("Tentative de recherche avec un terme vide");
            return new ArrayList<>();
        }

        LOGGER.info("Recherche de fournisseurs avec le terme: " + terme);
        List<Fournisseur> resultats = new ArrayList<>();
        String sql = "SELECT * FROM fournisseurs WHERE supprime = false AND " +
                    "(LOWER(nom) LIKE LOWER(?) OR LOWER(contact) LIKE LOWER(?) OR " +
                    "LOWER(telephone) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?))";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchTerm = "%" + terme.toLowerCase() + "%";
            for (int i = 1; i <= 4; i++) {
                pstmt.setString(i, searchTerm);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    resultats.add(creerFournisseurDepuisResultSet(rs));
                }
            }
            LOGGER.info("Recherche terminée, " + resultats.size() + " résultats trouvés");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des fournisseurs", e);
            throw new RuntimeException("Erreur lors de la recherche des fournisseurs", e);
        }
        return resultats;
    }

    private void validateFournisseur(Fournisseur fournisseur) {
        if (fournisseur == null) {
            throw new IllegalArgumentException("Le fournisseur ne peut pas être null");
        }
        if (fournisseur.getNom() == null || fournisseur.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du fournisseur est obligatoire");
        }
        if (fournisseur.getEmail() != null && !fournisseur.getEmail().isEmpty() && 
            !EMAIL_PATTERN.matcher(fournisseur.getEmail()).matches()) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
        if (fournisseur.getTelephone() != null && !fournisseur.getTelephone().isEmpty() && 
            !PHONE_PATTERN.matcher(fournisseur.getTelephone()).matches()) {
            throw new IllegalArgumentException("Format de téléphone invalide");
        }
        // Validation de la longueur des champs
        if (fournisseur.getNom().length() > 100) {
            throw new IllegalArgumentException("Le nom du fournisseur est trop long (max 100 caractères)");
        }
        if (fournisseur.getContact() != null && fournisseur.getContact().length() > 100) {
            throw new IllegalArgumentException("Le nom du contact est trop long (max 100 caractères)");
        }
        if (fournisseur.getEmail() != null && fournisseur.getEmail().length() > 100) {
            throw new IllegalArgumentException("L'email est trop long (max 100 caractères)");
        }
        if (fournisseur.getAdresse() != null && fournisseur.getAdresse().length() > 255) {
            throw new IllegalArgumentException("L'adresse est trop longue (max 255 caractères)");
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

    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Échapper les caractères spéciaux HTML et SQL
        return input.replaceAll("[<>\"'%;)(&+]", "");
    }
}