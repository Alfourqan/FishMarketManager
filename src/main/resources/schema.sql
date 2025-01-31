-- Configuration optimisée SQLite
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA cache_size = 2000;
PRAGMA page_size = 4096;
PRAGMA temp_store = MEMORY;
PRAGMA busy_timeout = 30000;
PRAGMA foreign_keys = OFF;  -- Désactivé temporairement pour la migration

-- Tables principales dans l'ordre de dépendance
DROP TABLE IF EXISTS mouvements_caisse;
DROP TABLE IF EXISTS user_actions;
DROP TABLE IF EXISTS ventes;
DROP TABLE IF EXISTS lignes_vente;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER',
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
    last_login INTEGER,
    active BOOLEAN DEFAULT true,
    CONSTRAINT username_min_length CHECK (length(username) >= 3)
);

CREATE TABLE mouvements_caisse (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT DEFAULT (datetime('now', 'localtime')),
    type TEXT NOT NULL CHECK (type IN ('ENTREE', 'SORTIE', 'OUVERTURE', 'CLOTURE')),
    montant REAL NOT NULL CHECK (montant > 0),
    description TEXT,
    user_id INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_actions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    action_type TEXT NOT NULL,
    username TEXT NOT NULL,
    date_time TEXT NOT NULL,
    description TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id INTEGER NOT NULL,
    user_id INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS produits (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nom TEXT NOT NULL,
    categorie TEXT NOT NULL,
    prix_achat REAL NOT NULL,
    prix_vente REAL NOT NULL,
    stock INTEGER NOT NULL,
    seuil_alerte INTEGER NOT NULL,
    supprime BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS clients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nom TEXT NOT NULL,
    telephone TEXT,
    adresse TEXT,
    solde REAL DEFAULT 0,
    supprime BOOLEAN DEFAULT false
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

CREATE TABLE ventes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
    client_id INTEGER,
    credit INTEGER DEFAULT 0,
    total REAL NOT NULL,
    supprime BOOLEAN DEFAULT false,
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE lignes_vente (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vente_id INTEGER,
    produit_id INTEGER,
    quantite INTEGER NOT NULL,
    prix_unitaire REAL NOT NULL,
    FOREIGN KEY (vente_id) REFERENCES ventes(id),
    FOREIGN KEY (produit_id) REFERENCES produits(id)
);

CREATE TABLE IF NOT EXISTS configurations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cle TEXT NOT NULL UNIQUE,
    valeur TEXT,
    description TEXT
);

-- Index optimisés
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_produits_nom ON produits(nom);
CREATE INDEX IF NOT EXISTS idx_produits_categorie ON produits(categorie);
CREATE INDEX IF NOT EXISTS idx_clients_nom ON clients(nom);
CREATE INDEX IF NOT EXISTS idx_ventes_date ON ventes(date);
CREATE INDEX IF NOT EXISTS idx_ventes_client ON ventes(client_id);
CREATE INDEX IF NOT EXISTS idx_lignes_vente_vente ON lignes_vente(vente_id);
CREATE INDEX IF NOT EXISTS idx_lignes_vente_produit ON lignes_vente(produit_id);
CREATE INDEX IF NOT EXISTS idx_mouvements_caisse_date ON mouvements_caisse(date);
CREATE INDEX IF NOT EXISTS idx_mouvements_caisse_type ON mouvements_caisse(type);
CREATE INDEX IF NOT EXISTS idx_mouvements_caisse_user ON mouvements_caisse(user_id);
CREATE INDEX IF NOT EXISTS idx_user_actions_date ON user_actions(date_time);
CREATE INDEX IF NOT EXISTS idx_user_actions_type ON user_actions(action_type);
CREATE INDEX IF NOT EXISTS idx_user_actions_entity ON user_actions(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_user_actions_user ON user_actions(user_id);

-- Configurations par défaut
INSERT OR IGNORE INTO configurations (cle, valeur, description) VALUES
('TAUX_TVA', '20.0', 'Taux de TVA en pourcentage'),
('TVA_ENABLED', 'true', 'Activation/désactivation de la TVA'),
('NOM_ENTREPRISE', '', 'Nom de l''entreprise'),
('ADRESSE_ENTREPRISE', '', 'Adresse de l''entreprise'),
('TELEPHONE_ENTREPRISE', '', 'Numéro de téléphone de l''entreprise'),
('EMAIL_ENTREPRISE', '', 'Adresse email de l''entreprise'),
('SIRET_ENTREPRISE', '12345678901234', 'Numéro SIRET de l''entreprise');

-- Réactivation des foreign keys après la migration
PRAGMA foreign_keys = ON;