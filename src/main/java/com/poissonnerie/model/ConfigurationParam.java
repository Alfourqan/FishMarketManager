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
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    // Mise à jour du pattern téléphone pour accepter plus de formats
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,4}[-\\s.]?[0-9]{1,4}$"
    );
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
        setValeur(valeur); // Validation immédiate de la valeur
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

        // Vérification de la longueur avant tout traitement
        if (valeur.trim().length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("La valeur ne peut pas dépasser " + MAX_VALUE_LENGTH + " caractères");
        }

        String cleanValue = valeur.trim();

        // Si la validation SIRET est désactivée, retourner la valeur pour le SIRET
        if (cle.equals(CLE_SIRET_ENTREPRISE) && System.getProperty("SKIP_SIRET_VALIDATION") != null) {
            return cleanValue;
        }

        // Validation spécifique selon le type de configuration
        switch (cle) {
            case CLE_EMAIL:
                if (!EMAIL_PATTERN.matcher(cleanValue).matches()) {
                    throw new IllegalArgumentException("Format d'email invalide");
                }
                return cleanValue;

            case CLE_TAUX_TVA:
                try {
                    // Extraction des chiffres et du point décimal uniquement
                    String sanitizedValue = cleanValue.replaceAll("[^0-9.]", "");

                    if (sanitizedValue.isEmpty()) {
                        throw new IllegalArgumentException("Le taux de TVA doit contenir au moins un chiffre");
                    }

                    // Vérification du format numérique
                    if (!sanitizedValue.matches("^\\d*\\.?\\d+$")) {
                        throw new IllegalArgumentException("Format de taux TVA invalide");
                    }

                    double taux = Double.parseDouble(sanitizedValue);
                    if (taux < 0 || taux > 100) {
                        throw new IllegalArgumentException("Le taux de TVA doit être entre 0 et 100");
                    }

                    return String.format("%.2f", taux);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Le taux de TVA doit être un nombre valide");
                }

            case CLE_TVA_ENABLED:
                String lowercaseValue = cleanValue.toLowerCase();
                if (!lowercaseValue.equals("true") && !lowercaseValue.equals("false")) {
                    throw new IllegalArgumentException("La valeur doit être 'true' ou 'false'");
                }
                return lowercaseValue;

            case CLE_TELEPHONE_ENTREPRISE:
                if (!PHONE_PATTERN.matcher(cleanValue).matches()) {
                    throw new IllegalArgumentException("Format de téléphone invalide");
                }
                return cleanValue;

            case CLE_SIRET_ENTREPRISE:
                if (!SIRET_PATTERN.matcher(cleanValue).matches()) {
                    throw new IllegalArgumentException("Format de SIRET invalide");
                }
                return cleanValue;

            case CLE_FORMAT_RECU:
                if (!cleanValue.equals("COMPACT") && !cleanValue.equals("DETAILLE")) {
                    throw new IllegalArgumentException("Le format du reçu doit être 'COMPACT' ou 'DETAILLE'");
                }
                return cleanValue;
        }

        // Pour les autres types de configuration, appliquer le nettoyage standard
        return sanitizeInput(cleanValue);
    }

    private static String validateDescription(String description) {
        if (description == null) {
            return "";
        }
        String cleanDesc = description.trim();
        if (cleanDesc.length() > 200) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 200 caractères");
        }
        return sanitizeInput(cleanDesc);
    }

    private static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        // Nettoyage des caractères spéciaux et les caractères de contrôle
        String cleaned = input.replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", "")
                            .replaceAll("[<>\"'%;)(&+\\[\\]{}]", "")
                            .trim()
                            .replaceAll("\\s+", " ");

        // Limiter la longueur
        return cleaned.length() > MAX_VALUE_LENGTH ? cleaned.substring(0, MAX_VALUE_LENGTH) : cleaned;
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
        try {
            if (estCrypte && valeur != null && !valeur.isEmpty()) {
                return decryptValue(valeur);
            }
            return valeur;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du décryptage de la valeur", e);
            throw new IllegalStateException("Impossible de décrypter la valeur: " + e.getMessage(), e);
        }
    }

    public void setValeur(String valeur) {
        try {
            String validatedValue = validateValeur(valeur, this.cle);
            if (estCrypte && validatedValue != null && !validatedValue.isEmpty() && 
                System.getenv().get("CONFIG_SECRET_KEY") != null) {
                this.valeur = encryptValue(validatedValue);
            } else {
                this.valeur = validatedValue;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur lors du traitement de la valeur", e);
            if (this.cle.equals(CLE_SIRET_ENTREPRISE) && 
                System.getProperty("SKIP_SIRET_VALIDATION") != null) {
                // Si la validation SIRET est désactivée, accepter la valeur telle quelle
                this.valeur = valeur;
                LOGGER.info("Validation SIRET désactivée, valeur acceptée: " + valeur);
            } else {
                throw new IllegalStateException("Impossible de traiter la valeur: " + e.getMessage(), e);
            }
        }
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = validateDescription(description);
    }

    // Constantes pour les clés de configuration
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

    // Nouvelles constantes pour la personnalisation des reçus
    public static final String CLE_POLICE_TITRE_RECU = "POLICE_TITRE_RECU";
    public static final String CLE_POLICE_TEXTE_RECU = "POLICE_TEXTE_RECU";
    public static final String CLE_ALIGNEMENT_TITRE_RECU = "ALIGNEMENT_TITRE_RECU";
    public static final String CLE_ALIGNEMENT_TEXTE_RECU = "ALIGNEMENT_TEXTE_RECU";
    public static final String CLE_STYLE_BORDURE_RECU = "STYLE_BORDURE_RECU";
    public static final String CLE_MESSAGE_COMMERCIAL_RECU = "MESSAGE_COMMERCIAL_RECU";
    public static final String CLE_AFFICHER_TVA_DETAILS = "AFFICHER_TVA_DETAILS";
    public static final String CLE_INFO_SUPPLEMENTAIRE_RECU = "INFO_SUPPLEMENTAIRE_RECU";

    //Theme constants
    public static final String CLE_THEME = "THEME_ACTIF";
    public static final String CLE_THEME_PERSONNALISE = "THEME_PERSONNALISE";
    public static final String CLE_THEME_PRIMAIRE = "THEME_COULEUR_PRIMAIRE";
    public static final String CLE_THEME_SECONDAIRE = "THEME_COULEUR_SECONDAIRE";
    public static final String CLE_THEME_FOND = "THEME_COULEUR_FOND";
    public static final String CLE_THEME_TEXTE = "THEME_COULEUR_TEXTE";
    public static final String CLE_THEME_ACCENT = "THEME_COULEUR_ACCENT";
    public static final String CLE_THEME_POLICE_TITRE = "THEME_POLICE_TITRE";
    public static final String CLE_THEME_POLICE_CORPS = "THEME_POLICE_CORPS";


    private static final java.util.Set<String> CLES_SENSIBLES = new java.util.HashSet<>(java.util.Arrays.asList(
        CLE_EMAIL,
        CLE_TELEPHONE_ENTREPRISE,
        CLE_SIRET_ENTREPRISE
    ));

    // Méthodes de cryptage/décryptage
    private static final String ALGORITHM = "AES";

    private static String getSecretKey() {
        String key = System.getenv().getOrDefault("CONFIG_SECRET_KEY", null);
        if (key == null || key.trim().isEmpty()) {
            key = "poissonnerie_secure_key_" + java.time.LocalDate.now().getYear();
            LOGGER.warning("Utilisation de la clé de cryptage par défaut");
        }
        return key;
    }

    private static SecretKeySpec generateKey() throws Exception {
        try {
            String secretKey = getSecretKey();
            byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = java.util.Arrays.copyOf(key, 16);
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération de la clé: " + e.getMessage(), e);
            throw new IllegalStateException("Erreur de génération de clé: " + e.getMessage(), e);
        }
    }

    private static String encryptValue(String value) throws Exception {
        if (value == null || value.isEmpty() || System.getenv().get("CONFIG_SECRET_KEY") == null) {
            LOGGER.warning("Valeur non cryptée car clé de cryptage non configurée ou valeur vide");
            return value;
        }

        try {
            SecretKeySpec key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur de cryptage, retour de la valeur non cryptée", e);
            return value;
        }
    }

    private static String decryptValue(String encrypted) throws Exception {
        if (encrypted == null || encrypted.isEmpty() || System.getenv().get("CONFIG_SECRET_KEY") == null) {
            LOGGER.warning("Valeur non décryptée car clé de cryptage non configurée ou valeur vide");
            return encrypted;
        }

        try {
            SecretKeySpec key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur de décryptage, retour de la valeur cryptée", e);
            return encrypted;
        }
    }
}