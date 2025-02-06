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
                // Désactiver temporairement les contraintes de clé étrangère
                stmt.execute("PRAGMA foreign_keys = OFF");

                // Table des rôles
                stmt.execute("CREATE TABLE IF NOT EXISTS " + ROLES_TABLE + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nom TEXT NOT NULL UNIQUE," +
                    "description TEXT" +
                    ")");

                // Table des permissions
                stmt.execute("CREATE TABLE IF NOT EXISTS " + PERMISSIONS_TABLE + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "code TEXT NOT NULL UNIQUE," +
                    "description TEXT," +
                    "module TEXT NOT NULL" +
                    ")");

                // Table de liaison users-roles
                stmt.execute("CREATE TABLE IF NOT EXISTS " + USERS_ROLES_TABLE + " (" +
                    "user_id INTEGER," +
                    "role_id INTEGER," +
                    "PRIMARY KEY (user_id, role_id)," +
                    "FOREIGN KEY (role_id) REFERENCES " + ROLES_TABLE + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")");

                // Table de liaison roles-permissions
                stmt.execute("CREATE TABLE IF NOT EXISTS " + ROLES_PERMISSIONS_TABLE + " (" +
                    "role_id INTEGER," +
                    "permission_id INTEGER," +
                    "PRIMARY KEY (role_id, permission_id)," +
                    "FOREIGN KEY (role_id) REFERENCES " + ROLES_TABLE + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (permission_id) REFERENCES " + PERMISSIONS_TABLE + "(id) ON DELETE CASCADE" +
                    ")");

                // Réactiver les contraintes de clé étrangère
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
        String sql = "INSERT INTO " + ROLES_TABLE + " (nom, description) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            conn.setAutoCommit(false);
            try {
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

                conn.commit();
                LOGGER.info("Rôle créé avec succès: " + role);
                return role;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void attribuerRoleUtilisateur(Integer userId, Integer roleId) throws SQLException {
        String sql = "INSERT INTO " + USERS_ROLES_TABLE + " (user_id, role_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, roleId);

                pstmt.executeUpdate();
                conn.commit();
                LOGGER.info("Rôle " + roleId + " attribué à l'utilisateur " + userId);
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'attribution du rôle: " + e.getMessage(), e);
                throw e;
            }
        }
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
    public void attribuerRoleUtilisateur(Integer userId, Integer roleId) throws SQLException {
        String sql = "INSERT INTO " + USERS_ROLES_TABLE + " (user_id, role_id) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            try {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, roleId);

                pstmt.executeUpdate();
                conn.commit();
                LOGGER.info("Rôle " + roleId + " attribué à l'utilisateur " + userId);
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Erreur lors de l'attribution du rôle: " + e.getMessage(), e);
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