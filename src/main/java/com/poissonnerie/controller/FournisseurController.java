package com.poissonnerie.controller;

import com.poissonnerie.model.Fournisseur;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FournisseurController {
    private static final Logger LOGGER = Logger.getLogger(FournisseurController.class.getName());
    private List<Fournisseur> fournisseurs;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$"
    );
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public FournisseurController() {
        this.fournisseurs = new ArrayList<>();
    }

    public void chargerFournisseurs() {
        LOGGER.info("Chargement des fournisseurs...");
        fournisseurs.clear();
        String sql = "SELECT * FROM fournisseurs WHERE supprime = false ORDER BY nom";

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        Fournisseur fournisseur = creerFournisseurDepuisResultSet(rs);
                        fournisseurs.add(fournisseur);
                    }
                    conn.commit();
                    LOGGER.info("Fournisseurs chargés avec succès: " + fournisseurs.size() + " enregistrements");
                    return;
                } catch (SQLException e) {
                    conn.rollback();
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        LOGGER.log(Level.SEVERE, "Échec définitif du chargement des fournisseurs après " + MAX_RETRY_ATTEMPTS + " tentatives", e);
                        throw new RuntimeException("Erreur lors du chargement des fournisseurs", e);
                    }
                    LOGGER.warning("Tentative " + attempt + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (SQLException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Erreur fatale de connexion à la base de données", e);
                    throw new RuntimeException("Erreur de connexion à la base de données", e);
                }
                LOGGER.warning("Tentative de connexion " + attempt + " échouée");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.severe("Interruption pendant la tentative de reconnexion");
                throw new RuntimeException("Opération interrompue", e);
            }
        }
    }

    public List<Fournisseur> getFournisseurs() {
        return new ArrayList<>(fournisseurs);
    }

    public void ajouterFournisseur(Fournisseur fournisseur) {
        LOGGER.info("Tentative d'ajout d'un nouveau fournisseur: " + fournisseur.getNom());
        validateFournisseur(fournisseur);

        String sql = "INSERT INTO fournisseurs (nom, contact, telephone, email, adresse, statut) VALUES (?, ?, ?, ?, ?, ?)";

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    preparerStatementFournisseur(pstmt, fournisseur);
                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected == 0) {
                        throw new SQLException("L'insertion du fournisseur a échoué, aucune ligne affectée.");
                    }

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            fournisseur.setId(generatedKeys.getInt(1));
                            fournisseurs.add(fournisseur);
                            LOGGER.info("Fournisseur ajouté avec succès, ID: " + fournisseur.getId());
                        } else {
                            throw new SQLException("L'insertion du fournisseur a échoué, aucun ID généré.");
                        }
                    }
                    conn.commit();
                    return;
                } catch (SQLException e) {
                    conn.rollback();
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        LOGGER.log(Level.SEVERE, "Échec définitif de l'ajout du fournisseur après " + MAX_RETRY_ATTEMPTS + " tentatives", e);
                        throw new RuntimeException("Erreur lors de l'ajout du fournisseur", e);
                    }
                    LOGGER.warning("Tentative " + attempt + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (SQLException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Opération interrompue", e);
                }
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Erreur fatale lors de l'ajout du fournisseur", e);
                    throw new RuntimeException("Erreur lors de l'ajout du fournisseur", e);
                }
            }
        }
    }

    public void mettreAJourFournisseur(Fournisseur fournisseur) {
        LOGGER.info("Tentative de mise à jour du fournisseur ID: " + fournisseur.getId());
        validateFournisseur(fournisseur);

        String sql = "UPDATE fournisseurs SET nom = ?, contact = ?, telephone = ?, email = ?, adresse = ?, statut = ? " +
                    "WHERE id = ? AND supprime = false";

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    preparerStatementFournisseur(pstmt, fournisseur);
                    pstmt.setInt(7, fournisseur.getId());

                    int rowsUpdated = pstmt.executeUpdate();
                    if (rowsUpdated == 0) {
                        throw new IllegalStateException("Fournisseur non trouvé ou déjà supprimé: " + fournisseur.getId());
                    }

                    // Mise à jour de la liste en mémoire
                    int index = fournisseurs.indexOf(fournisseur);
                    if (index != -1) {
                        fournisseurs.set(index, fournisseur);
                    }

                    conn.commit();
                    LOGGER.info("Fournisseur mis à jour avec succès, ID: " + fournisseur.getId());
                    return;
                } catch (SQLException e) {
                    conn.rollback();
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        LOGGER.log(Level.SEVERE, "Échec définitif de la mise à jour du fournisseur après " + MAX_RETRY_ATTEMPTS + " tentatives", e);
                        throw new RuntimeException("Erreur lors de la mise à jour du fournisseur", e);
                    }
                    LOGGER.warning("Tentative " + attempt + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (SQLException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Opération interrompue", e);
                }
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Erreur fatale lors de la mise à jour du fournisseur", e);
                    throw new RuntimeException("Erreur lors de la mise à jour du fournisseur", e);
                }
            }
        }
    }

    public void supprimerFournisseur(Fournisseur fournisseur) {
        if (fournisseur == null || fournisseur.getId() <= 0) {
            LOGGER.warning("Tentative de suppression d'un fournisseur invalide");
            throw new IllegalArgumentException("Fournisseur invalide");
        }

        LOGGER.info("Tentative de suppression du fournisseur ID: " + fournisseur.getId());
        String sql = "UPDATE fournisseurs SET supprime = true WHERE id = ? AND supprime = false";

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
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
                    return;
                } catch (SQLException e) {
                    conn.rollback();
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        LOGGER.log(Level.SEVERE, "Échec définitif de la suppression du fournisseur après " + MAX_RETRY_ATTEMPTS + " tentatives", e);
                        throw new RuntimeException("Erreur lors de la suppression du fournisseur", e);
                    }
                    LOGGER.warning("Tentative " + attempt + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (SQLException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Opération interrompue", e);
                }
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Erreur fatale lors de la suppression du fournisseur", e);
                    throw new RuntimeException("Erreur lors de la suppression du fournisseur", e);
                }
            }
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

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
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
                return resultats;
            } catch (SQLException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la recherche des fournisseurs", e);
                    throw new RuntimeException("Erreur lors de la recherche des fournisseurs", e);
                }
                LOGGER.warning("Tentative " + attempt + " échouée, nouvelle tentative dans " + RETRY_DELAY_MS + "ms");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    LOGGER.severe("Interruption pendant la tentative de reconnexion");
                    throw new RuntimeException("Opération interrompue", ex);
                }

            }
        }
        return resultats;
    }

    private void validateFournisseur(Fournisseur fournisseur) {
        List<String> errors = new ArrayList<>();

        if (fournisseur == null) {
            throw new IllegalArgumentException("Le fournisseur ne peut pas être null");
        }

        // Validation du nom
        if (fournisseur.getNom() == null || fournisseur.getNom().trim().isEmpty()) {
            errors.add("Le nom du fournisseur est obligatoire");
        } else if (fournisseur.getNom().length() > 100) {
            errors.add("Le nom du fournisseur est trop long (max 100 caractères)");
        }

        // Validation du contact
        if (fournisseur.getContact() != null && fournisseur.getContact().length() > 100) {
            errors.add("Le nom du contact est trop long (max 100 caractères)");
        }

        // Validation de l'email
        if (fournisseur.getEmail() != null && !fournisseur.getEmail().isEmpty()) {
            if (fournisseur.getEmail().length() > 100) {
                errors.add("L'email est trop long (max 100 caractères)");
            } else if (!EMAIL_PATTERN.matcher(fournisseur.getEmail()).matches()) {
                errors.add("Format d'email invalide");
            }
        }

        // Validation du téléphone
        if (fournisseur.getTelephone() != null && !fournisseur.getTelephone().isEmpty() && 
            !PHONE_PATTERN.matcher(fournisseur.getTelephone()).matches()) {
            errors.add("Format de téléphone invalide");
        }

        // Validation de l'adresse
        if (fournisseur.getAdresse() != null && fournisseur.getAdresse().length() > 255) {
            errors.add("L'adresse est trop longue (max 255 caractères)");
        }

        // Si des erreurs sont présentes, les regrouper dans un message
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
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
        if (input == null) {
            return "";
        }
        // Échapper les caractères spéciaux HTML et SQL
        return input.replaceAll("[<>\"'%;)(&+]", "")
                   .trim()
                   .replaceAll("\\s+", " ");
    }

    private Fournisseur creerFournisseurDepuisResultSet(ResultSet rs) throws SQLException {
        try {
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
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création d'un fournisseur depuis le ResultSet", e);
            throw e;
        }
    }
}