PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS configurations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cle TEXT NOT NULL UNIQUE,
    valeur TEXT,
    description TEXT
);

CREATE TABLE IF NOT EXISTS produits (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nom TEXT NOT NULL,
    categorie TEXT NOT NULL,
    prix_achat REAL NOT NULL,
    prix_vente REAL NOT NULL,
    stock INTEGER NOT NULL,
    seuil_alerte INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS clients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nom TEXT NOT NULL,
    telephone TEXT,
    adresse TEXT,
    solde REAL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fournisseurs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nom TEXT NOT NULL,
    contact TEXT,
    telephone TEXT,
    email TEXT,
    adresse TEXT,
    statut TEXT DEFAULT 'Actif',
    supprime BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS ventes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
    client_id INTEGER,
    credit INTEGER DEFAULT 0,
    total REAL NOT NULL,
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE IF NOT EXISTS lignes_vente (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vente_id INTEGER,
    produit_id INTEGER,
    quantite INTEGER NOT NULL,
    prix_unitaire REAL NOT NULL,
    FOREIGN KEY (vente_id) REFERENCES ventes(id),
    FOREIGN KEY (produit_id) REFERENCES produits(id)
);

CREATE TABLE IF NOT EXISTS mouvements_caisse (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT DEFAULT (datetime('now', 'localtime')),
    type TEXT NOT NULL CHECK (type IN ('ENTREE', 'SORTIE', 'OUVERTURE', 'CLOTURE')),
    montant REAL NOT NULL,
    description TEXT
);

-- Insertion des configurations par défaut si elles n'existent pas
INSERT OR IGNORE INTO configurations (cle, valeur, description) VALUES
('TAUX_TVA', '20.0', 'Taux de TVA en pourcentage'),
('TVA_ENABLED', 'true', 'Activation/désactivation de la TVA'),
('NOM_ENTREPRISE', '', 'Nom de l''entreprise'),
('ADRESSE_ENTREPRISE', '', 'Adresse de l''entreprise'),
('TELEPHONE_ENTREPRISE', '', 'Numéro de téléphone de l''entreprise'),
('EMAIL_ENTREPRISE', '', 'Adresse email de l''entreprise'),
('SIRET_ENTREPRISE', '', 'Numéro SIRET de l''entreprise'),
('LOGO_PATH', '', 'Chemin vers le logo de l''entreprise'),
('FORMAT_RECU', 'COMPACT', 'Format des reçus (COMPACT ou DETAILLE)'),
('PIED_PAGE_RECU', 'Merci de votre visite !', 'Message en pied de page des reçus'),
('EN_TETE_RECU', '', 'Message en en-tête des reçus');