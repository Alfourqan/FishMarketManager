package com.poissonnerie.controller;

import com.poissonnerie.model.Role;
import com.poissonnerie.model.Permission;
import com.poissonnerie.util.DatabaseManager;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class RoleController {
    private static final Logger LOGGER = Logger.getLogger(RoleController.class.getName());
    private static RoleController instance;
    private static final String ROLES_TABLE = "roles";
    private static final String PERMISSIONS_TABLE = "permissions";
    private static final String ROLES_PERMISSIONS_TABLE = "roles_permissions";
    private static final String USERS_ROLES_TABLE = "users_roles";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private RoleController() {
        try {
            initializeTables();
            LOGGER.info("RoleController initialisé avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation de RoleController", e);
            throw new RuntimeException("Erreur lors de l'initialisation de RoleController", e);
        }
    }

    public static RoleController getInstance() {
        if (instance == null) {
            synchronized (RoleController.class) {
                if (instance == null) {
                    instance = new RoleController();
                }
            }
        }
        return instance;
    }

    private void initializeTables() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF");

                stmt.execute("CREATE TABLE IF NOT EXISTS " + ROLES_TABLE + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nom TEXT NOT NULL UNIQUE," +
                    "description TEXT" +
                    ")");

                stmt.execute("CREATE TABLE IF NOT EXISTS " + PERMISSIONS_TABLE + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "code TEXT NOT NULL UNIQUE," +
                    "description TEXT," +
                    "module TEXT NOT NULL" +
                    ")");

                stmt.execute("CREATE TABLE IF NOT EXISTS " + USERS_ROLES_TABLE + " (" +
                    "user_id INTEGER," +
                    "role_id INTEGER," +
                    "PRIMARY KEY (user_id, role_id)," +
                    "FOREIGN KEY (role_id) REFERENCES " + ROLES_TABLE + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")");

                stmt.execute("CREATE TABLE IF NOT EXISTS " + ROLES_PERMISSIONS_TABLE + " (" +
                    "role_id INTEGER," +
                    "permission_id INTEGER," +
                    "PRIMARY KEY (role_id, permission_id)," +
                    "FOREIGN KEY (role_id) REFERENCES " + ROLES_TABLE + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (permission_id) REFERENCES " + PERMISSIONS_TABLE + "(id) ON DELETE CASCADE" +
                    ")");

                stmt.execute("PRAGMA foreign_keys = ON");

                conn.commit();
                LOGGER.info("Tables de gestion des rôles créées avec succès");
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de la création des tables: " + e.getMessage(), e);
                throw e;
            }
        }
    }

    public Role creerRole(Role role) throws SQLException {
        // First try to find if the role already exists
        String selectSql = "SELECT id FROM " + ROLES_TABLE + " WHERE nom = ?";
        String insertSql = "INSERT INTO " + ROLES_TABLE + " (nom, description) VALUES (?, ?)";

        int retries = 0;
        while (retries < MAX_RETRIES) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // Check if role exists
                    try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                        pstmt.setString(1, role.getNom());
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            role.setId(rs.getInt("id"));
                            LOGGER.info("Rôle existant trouvé: " + role.getNom());
                            return role;
                        }
                    }

                    // If not exists, create new role
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setString(1, role.getNom());
                        pstmt.setString(2, role.getDescription());

                        int affectedRows = pstmt.executeUpdate();
                        if (affectedRows == 0) {
                            throw new SQLException("La création du rôle a échoué");
                        }

                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                role.setId(generatedKeys.getInt(1));
                            } else {
                                throw new SQLException("La création du rôle a échoué, aucun ID obtenu");
                            }
                        }
                    }

                    conn.commit();
                    LOGGER.info("Rôle créé avec succès: " + role);
                    return role;

                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                    }

                    if (e.getMessage().contains("UNIQUE constraint failed") && retries < MAX_RETRIES - 1) {
                        LOGGER.warning("Conflit de création de rôle, nouvelle tentative " + (retries + 1));
                        retries++;
                        Thread.sleep(RETRY_DELAY_MS);
                        continue;
                    }
                    throw e;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interruption pendant la création du rôle", ie);
            }
        }
        throw new SQLException("Échec de la création du rôle après " + MAX_RETRIES + " tentatives");
    }

    public void attribuerRoleUtilisateur(Integer userId, Integer roleId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM " + USERS_ROLES_TABLE + " WHERE user_id = ? AND role_id = ?";
        String insertSql = "INSERT INTO " + USERS_ROLES_TABLE + " (user_id, role_id) VALUES (?, ?)";
        int retries = 0;

        while (retries < MAX_RETRIES) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Vérifier si l'association existe déjà
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setInt(1, userId);
                        checkStmt.setInt(2, roleId);
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next() && rs.getInt(1) > 0) {
                            LOGGER.info("L'association user_id=" + userId + " et role_id=" + roleId + " existe déjà");
                            conn.commit();
                            return;
                        }
                    }

                    // Si l'association n'existe pas, la créer
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, userId);
                        insertStmt.setInt(2, roleId);
                        insertStmt.executeUpdate();
                        conn.commit();
                        LOGGER.info("Rôle " + roleId + " attribué à l'utilisateur " + userId);
                        return;
                    }

                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException re) {
                        LOGGER.log(Level.SEVERE, "Erreur lors du rollback", re);
                    }

                    if (e.getMessage().contains("UNIQUE constraint failed") && retries < MAX_RETRIES - 1) {
                        LOGGER.warning("Conflit lors de l'attribution du rôle, nouvelle tentative " + (retries + 1));
                        retries++;
                        Thread.sleep(RETRY_DELAY_MS);
                        continue;
                    }
                    throw e;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interruption pendant l'attribution du rôle", ie);
            }
        }
        throw new SQLException("Échec de l'attribution du rôle après " + MAX_RETRIES + " tentatives");
    }

    public List<Role> getRoles() throws SQLException {
        String sql = "SELECT * FROM " + ROLES_TABLE;
        List<Role> roles = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Role role = new Role();
                role.setId(rs.getInt("id"));
                role.setNom(rs.getString("nom"));
                role.setDescription(rs.getString("description"));

                // Charger les permissions associées
                role.getPermissions().addAll(getPermissionsForRole(role.getId()));

                roles.add(role);
            }
        }

        return roles;
    }

    private Set<Permission> getPermissionsForRole(Integer roleId) throws SQLException {
        String sql = "SELECT p.* FROM " + PERMISSIONS_TABLE + " p " +
                    "JOIN " + ROLES_PERMISSIONS_TABLE + " rp ON p.id = rp.permission_id " +
                    "WHERE rp.role_id = ?";

        Set<Permission> permissions = new HashSet<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roleId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Permission permission = new Permission();
                    permission.setId(rs.getInt("id"));
                    permission.setCode(rs.getString("code"));
                    permission.setDescription(rs.getString("description"));
                    permission.setModule(rs.getString("module"));
                    permissions.add(permission);
                }
            }
        }

        return permissions;
    }

    private void ajouterPermissionsRole(Integer roleId, Set<Permission> permissions) throws SQLException {
        if (permissions.isEmpty()) return;

        String sql = "INSERT INTO " + ROLES_PERMISSIONS_TABLE + " (role_id, permission_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                for (Permission permission : permissions) {
                    pstmt.setInt(1, roleId);
                    pstmt.setInt(2, permission.getId());
                    pstmt.addBatch();
                }

                pstmt.executeBatch();
                conn.commit();

                LOGGER.info("Permissions ajoutées au rôle " + roleId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public Set<Role> getRolesUtilisateur(Integer userId) throws SQLException {
        String sql = "SELECT r.* FROM " + ROLES_TABLE + " r " +
                    "JOIN " + USERS_ROLES_TABLE + " ur ON r.id = ur.role_id " +
                    "WHERE ur.user_id = ?";

        Set<Role> roles = new HashSet<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Role role = new Role();
                    role.setId(rs.getInt("id"));
                    role.setNom(rs.getString("nom"));
                    role.setDescription(rs.getString("description"));

                    // Charger les permissions associées
                    role.getPermissions().addAll(getPermissionsForRole(role.getId()));

                    roles.add(role);
                }
            }
        }

        return roles;
    }
}