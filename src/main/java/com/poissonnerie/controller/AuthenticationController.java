package com.poissonnerie.controller;

import java.util.Map;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AuthenticationController {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class.getName());
    private static final Map<String, String> users = new HashMap<>();
    private static AuthenticationController instance;

    private AuthenticationController() {
        // Initialisation avec un utilisateur par défaut (à des fins de test)
        try {
            addUser("admin", "admin");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation des utilisateurs", e);
        }
    }

    public static AuthenticationController getInstance() {
        if (instance == null) {
            instance = new AuthenticationController();
        }
        return instance;
    }

    public void addUser(String username, String password) throws NoSuchAlgorithmException {
        String hashedPassword = hashPassword(password);
        users.put(username, hashedPassword);
        LOGGER.log(Level.INFO, "Utilisateur ajouté: {0}", username);
    }

    public boolean authenticate(String username, String password) {
        try {
            String hashedPassword = hashPassword(password);
            String storedPassword = users.get(username);
            boolean isAuthenticated = storedPassword != null && storedPassword.equals(hashedPassword);
            
            LOGGER.log(Level.INFO, "Tentative d'authentification pour {0}: {1}", 
                new Object[]{username, isAuthenticated ? "réussie" : "échouée"});
            
            return isAuthenticated;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'authentification", e);
            return false;
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes());
        StringBuilder hexString = new StringBuilder();
        
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}
