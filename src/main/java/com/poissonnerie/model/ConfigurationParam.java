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

/**
 * Classe de gestion des paramètres de configuration de l'application
 * avec amélioration de la sécurité et validation des données
 */
public class ConfigurationParam {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationParam.class.getName());
    private static final int MAX_VALUE_LENGTH = 1000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9. ()-]{10,}$");
    private static final Pattern SIRET_PATTERN = Pattern.compile("^[0-9]{14}$");

    private int id;
    private String cle;
    private String valeur;
    private String description;
    private boolean estCrypte;

    /**
     * Constructeur avec validation des paramètres
     */
    public ConfigurationParam(int id, String cle, String valeur, String description) {
        this.id = id;
        this.cle = validateCle(cle);
        this.valeur = validateValeur(valeur, cle);
        this.description = validateDescription(description);
        this.estCrypte = needsEncryption(cle);
    }

    private static String validateCle(String cle) {
        cle = Objects.requireNonNull(cle, "La clé ne peut pas être null").trim();
        if (cle.isEmpty()) {
            throw new IllegalArgumentException("La clé ne peut pas être vide");
        }
        if (cle.length() > 50) {
            throw new IllegalArgumentException("La clé ne peut pas dépasser 50 caractères");
        }
        if (!Pattern.matches("^[A-Z_]+$", cle)) {
            throw new IllegalArgumentException("La clé doit être en majuscules et ne contenir que des lettres et des underscores");
        }
        return cle;
    }

    private static String validateValeur(String valeur, String cle) {
        valeur = Objects.requireNonNull(valeur, "La valeur ne peut pas être null").trim();
        if (valeur.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("La valeur ne peut pas dépasser " + MAX_VALUE_LENGTH + " caractères");
        }

        // Validation spécifique selon le type de configuration
        switch (cle) {
            case CLE_EMAIL:
                if (!EMAIL_PATTERN.matcher(valeur).matches()) {
                    throw new IllegalArgumentException("Format d'email invalide");
                }
                break;
            case CLE_TELEPHONE_ENTREPRISE:
                if (!PHONE_PATTERN.matcher(valeur).matches()) {
                    throw new IllegalArgumentException("Format de téléphone invalide");
                }
                break;
            case CLE_SIRET_ENTREPRISE:
                if (!SIRET_PATTERN.matcher(valeur).matches()) {
                    throw new IllegalArgumentException("Format de SIRET invalide");
                }
                break;
            case CLE_TAUX_TVA:
                try {
                    double taux = Double.parseDouble(valeur);
                    if (taux < 0 || taux > 100) {
                        throw new IllegalArgumentException("Le taux de TVA doit être entre 0 et 100");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Format de taux de TVA invalide");
                }
                break;
        }

        return valeur;
    }

    private static String validateDescription(String description) {
        description = Objects.requireNonNull(description, "La description ne peut pas être null").trim();
        if (description.length() > 200) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 200 caractères");
        }
        return description;
    }

    private static boolean needsEncryption(String cle) {
        return CLES_SENSIBLES.contains(cle);
    }

    // Getters et setters avec validation
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCle() { return cle; }
    public void setCle(String cle) {
        this.cle = validateCle(cle);
        this.estCrypte = needsEncryption(cle);
    }

    public String getValeur() {
        if (estCrypte) {
            try {
                return decryptValue(valeur);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors du décryptage de la valeur", e);
                return null;
            }
        }
        return valeur;
    }

    public void setValeur(String valeur) {
        String validatedValue = validateValeur(valeur, this.cle);
        if (estCrypte) {
            try {
                this.valeur = encryptValue(validatedValue);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Erreur lors du cryptage de la valeur", e);
                throw new IllegalStateException("Impossible de crypter la valeur", e);
            }
        } else {
            this.valeur = validatedValue;
        }
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = validateDescription(description);
    }

    // Constantes pour les clés de configuration avec documentation
    /** Taux de TVA appliqué aux ventes */
    public static final String CLE_TAUX_TVA = "TAUX_TVA";
    /** Activation/désactivation de la TVA */
    public static final String CLE_TVA_ENABLED = "TVA_ENABLED";
    /** Email de contact de l'entreprise */
    public static final String CLE_EMAIL = "EMAIL_ENTREPRISE";

    /** Nom officiel de l'entreprise */
    public static final String CLE_NOM_ENTREPRISE = "NOM_ENTREPRISE";
    /** Adresse complète de l'entreprise */
    public static final String CLE_ADRESSE_ENTREPRISE = "ADRESSE_ENTREPRISE";
    /** Numéro de téléphone de contact */
    public static final String CLE_TELEPHONE_ENTREPRISE = "TELEPHONE_ENTREPRISE";
    /** Numéro SIRET de l'entreprise */
    public static final String CLE_SIRET_ENTREPRISE = "SIRET_ENTREPRISE";

    /** Chemin vers le logo de l'entreprise */
    public static final String CLE_LOGO_PATH = "LOGO_PATH";
    /** Format des reçus (A4, A5, etc.) */
    public static final String CLE_FORMAT_RECU = "FORMAT_RECU";
    /** Texte personnalisé en pied de page des reçus */
    public static final String CLE_PIED_PAGE_RECU = "PIED_PAGE_RECU";
    /** En-tête personnalisé des reçus */
    public static final String CLE_EN_TETE_RECU = "EN_TETE_RECU";

    // Ensemble des clés sensibles nécessitant un cryptage
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