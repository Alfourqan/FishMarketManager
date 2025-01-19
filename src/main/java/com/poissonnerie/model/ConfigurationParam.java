package com.poissonnerie.model;

import java.util.Objects;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ConfigurationParam {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationParam.class.getName());
    private static final int MAX_VALUE_LENGTH = 1000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9. ()-]{10,}$");
    private static final Pattern SIRET_PATTERN = Pattern.compile("^[0-9]{14}$");

    private int id;
    private String cle;
    private String valeur;
    private String description;
    private boolean estCrypte;

    public ConfigurationParam(int id, String cle, String valeur, String description) {
        this.id = id;
        this.cle = validateCle(cle);
        this.description = validateDescription(description);
        this.estCrypte = needsEncryption(cle);
        setValeur(valeur); // Utilise setValeur pour appliquer la validation immédiate
    }

    private static String validateCle(String cle) {
        if (cle == null || cle.trim().isEmpty()) {
            throw new IllegalArgumentException("La clé ne peut pas être null ou vide");
        }
        if (cle.length() > 50) {
            throw new IllegalArgumentException("La clé ne peut pas dépasser 50 caractères");
        }
        if (!Pattern.matches("^[A-Z_]+$", cle)) {
            throw new IllegalArgumentException("La clé doit être en majuscules et ne contenir que des lettres et des underscores");
        }
        return cle;
    }

    public static String validateValeur(String valeur, String cle) {
        // Validation de base
        if (valeur == null) {
            throw new IllegalArgumentException("La valeur ne peut pas être null");
        }

        String cleanValue = valeur.trim();

        // Validation de la longueur avant tout autre traitement
        if (cleanValue.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("La valeur ne peut pas dépasser " + MAX_VALUE_LENGTH + " caractères");
        }

        // Validation spécifique selon le type de configuration
        switch (cle) {
            case CLE_EMAIL:
                if (!EMAIL_PATTERN.matcher(cleanValue).matches()) {
                    throw new IllegalArgumentException("Format d'email invalide");
                }
                return cleanValue; // Pas de sanitization pour l'email

            case CLE_TELEPHONE_ENTREPRISE:
                if (!PHONE_PATTERN.matcher(cleanValue).matches()) {
                    throw new IllegalArgumentException("Format de téléphone invalide");
                }
                break;

            case CLE_SIRET_ENTREPRISE:
                if (!SIRET_PATTERN.matcher(cleanValue).matches()) {
                    throw new IllegalArgumentException("Format de SIRET invalide");
                }
                break;

            case CLE_TAUX_TVA:
                try {
                    // Nettoyage plus permissif pour les caractères spéciaux
                    String numericValue = cleanValue
                        .replaceAll("[^0-9.,]", "") // Garde uniquement les chiffres et les séparateurs décimaux
                        .replace(",", ".")          // Normalise le séparateur décimal
                        .replaceAll("\\.+", ".");   // Évite les points multiples

                    if (numericValue.isEmpty()) {
                        throw new IllegalArgumentException("Le taux de TVA doit contenir au moins un chiffre");
                    }

                    double taux = Double.parseDouble(numericValue);
                    if (taux < 0 || taux > 100) {
                        throw new IllegalArgumentException("Le taux de TVA doit être entre 0 et 100");
                    }

                    return String.format("%.2f", taux); // Retourne directement la valeur normalisée
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Le taux de TVA doit être un nombre valide");
                }

            case CLE_TVA_ENABLED:
                if (!cleanValue.equalsIgnoreCase("true") && !cleanValue.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("La valeur doit être 'true' ou 'false'");
                }
                break;

            case CLE_FORMAT_RECU:
                if (!cleanValue.equals("COMPACT") && !cleanValue.equals("DETAILLE")) {
                    throw new IllegalArgumentException("Le format du reçu doit être 'COMPACT' ou 'DETAILLE'");
                }
                break;
        }

        return sanitizeInput(cleanValue);
    }

    private static String validateDescription(String description) {
        if (description == null) {
            return "";
        }
        description = description.trim();
        if (description.length() > 200) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 200 caractères");
        }
        return sanitizeInput(description);
    }

    private static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Échappement des caractères spéciaux
        return input
            .replaceAll("[<>'\"]", "_")           // Caractères HTML basiques
            .replaceAll("&(?![a-zA-Z0-9#]+;)", "_") // & non suivi d'une entité HTML
            .replaceAll("(?i)javascript:", "")     // Protection XSS
            .replaceAll("(?i)data:", "")          // Protection contre les données embarquées
            .replaceAll("(?i)vbscript:", "")      // Protection supplémentaire
            .replaceAll("\\\\", "/");             // Normalisation des chemins
    }

    private static boolean needsEncryption(String cle) {
        return CLES_SENSIBLES.contains(cle);
    }

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCle() { return cle; }
    public void setCle(String cle) { 
        this.cle = validateCle(cle);
        this.estCrypte = needsEncryption(cle);
    }

    public String getValeur() {
        try {
            if (estCrypte && valeur != null && !valeur.isEmpty()) {
                return decryptValue(valeur);
            }
            return valeur;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du décryptage de la valeur", e);
            throw new IllegalStateException("Impossible de décrypter la valeur", e);
        }
    }

    public void setValeur(String valeur) {
        try {
            String validatedValue = validateValeur(valeur, this.cle);
            if (estCrypte && !validatedValue.isEmpty()) {
                this.valeur = encryptValue(validatedValue);
            } else {
                this.valeur = validatedValue;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du cryptage de la valeur", e);
            throw new IllegalStateException("Impossible de crypter la valeur", e);
        }
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = validateDescription(description);
    }

    // Constantes
    public static final String CLE_TAUX_TVA = "TAUX_TVA";
    public static final String CLE_TVA_ENABLED = "TVA_ENABLED";
    public static final String CLE_EMAIL = "EMAIL_ENTREPRISE";
    public static final String CLE_NOM_ENTREPRISE = "NOM_ENTREPRISE";
    public static final String CLE_ADRESSE_ENTREPRISE = "ADRESSE_ENTREPRISE";
    public static final String CLE_TELEPHONE_ENTREPRISE = "TELEPHONE_ENTREPRISE";
    public static final String CLE_SIRET_ENTREPRISE = "SIRET_ENTREPRISE";
    public static final String CLE_LOGO_PATH = "LOGO_PATH";
    public static final String CLE_FORMAT_RECU = "FORMAT_RECU";
    public static final String CLE_PIED_PAGE_RECU = "PIED_PAGE_RECU";
    public static final String CLE_EN_TETE_RECU = "EN_TETE_RECU";

    private static final java.util.Set<String> CLES_SENSIBLES = new java.util.HashSet<>(java.util.Arrays.asList(
        CLE_EMAIL,
        CLE_TELEPHONE_ENTREPRISE,
        CLE_SIRET_ENTREPRISE
    ));

    // Méthodes de cryptage/décryptage
    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = System.getenv().getOrDefault("CONFIG_SECRET_KEY", "defaultSecretKey12345");

    private static String encryptValue(String value) throws Exception {
        SecretKeySpec key = generateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private static String decryptValue(String encrypted) throws Exception {
        SecretKeySpec key = generateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        return new String(decryptedBytes);
    }

    private static SecretKeySpec generateKey() throws Exception {
        byte[] key = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = java.util.Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, ALGORITHM);
    }
}