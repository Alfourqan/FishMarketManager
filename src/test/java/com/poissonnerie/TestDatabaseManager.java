package com.poissonnerie;

import com.poissonnerie.util.DatabaseManager;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDatabaseManager {
    
    @BeforeAll
    public static void setUp() {
        System.setProperty("SKIP_SIRET_VALIDATION", "true");
    }
    
    @AfterAll
    public static void tearDown() {
        System.clearProperty("SKIP_SIRET_VALIDATION");
    }
    
    @Test
    public void testInitDatabase() {
        Assertions.assertDoesNotThrow(() -> {
            DatabaseManager.initDatabase();
        }, "L'initialisation de la base de données devrait réussir");
    }
    
    @Test
    public void testGetConnection() {
        Assertions.assertDoesNotThrow(() -> {
            try (Connection conn = DatabaseManager.getConnection()) {
                Assertions.assertNotNull(conn, "La connexion ne devrait pas être null");
                Assertions.assertFalse(conn.isClosed(), "La connexion ne devrait pas être fermée");
            }
        }, "La récupération de la connexion devrait réussir");
    }
    
    @Test
    public void testCheckDatabaseHealth() {
        Assertions.assertDoesNotThrow(() -> {
            DatabaseManager.checkDatabaseHealth();
        }, "La vérification de la santé de la base de données devrait réussir");
    }
    
    @Test
    public void testForeignKeyConstraints() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Vérifier que les contraintes de clé étrangère sont activées
            var rs = stmt.executeQuery("PRAGMA foreign_keys");
            Assertions.assertTrue(rs.next(), "Le résultat de PRAGMA foreign_keys devrait avoir une ligne");
            Assertions.assertEquals(1, rs.getInt(1), "Les contraintes de clé étrangère devraient être activées");
        }
    }
    
    @Test
    public void testDatabaseReset() {
        Assertions.assertDoesNotThrow(() -> {
            DatabaseManager.resetConnection();
            try (Connection conn = DatabaseManager.getConnection()) {
                Assertions.assertNotNull(conn, "La connexion après réinitialisation ne devrait pas être null");
                Assertions.assertFalse(conn.isClosed(), "La connexion après réinitialisation ne devrait pas être fermée");
            }
        }, "La réinitialisation de la connexion devrait réussir");
    }
}
