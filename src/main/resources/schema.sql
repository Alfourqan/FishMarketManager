-- Ajout des index pour optimiser les performances
CREATE INDEX IF NOT EXISTS idx_produits_nom ON produits(nom);
CREATE INDEX IF NOT EXISTS idx_produits_categorie ON produits(categorie);
CREATE INDEX IF NOT EXISTS idx_clients_nom ON clients(nom);
CREATE INDEX IF NOT EXISTS idx_ventes_date ON ventes(date);
CREATE INDEX IF NOT EXISTS idx_ventes_client ON ventes(client_id);
CREATE INDEX IF NOT EXISTS idx_lignes_vente_vente ON lignes_vente(vente_id);
CREATE INDEX IF NOT EXISTS idx_lignes_vente_produit ON lignes_vente(produit_id);
CREATE INDEX IF NOT EXISTS idx_mouvements_caisse_date ON mouvements_caisse(date);
CREATE INDEX IF NOT EXISTS idx_mouvements_caisse_type ON mouvements_caisse(type);

CREATE TABLE IF NOT EXISTS historique_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produit_id INTEGER NOT NULL,
    date INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
    ancien_stock INTEGER NOT NULL,
    nouveau_stock INTEGER NOT NULL,
    type_mouvement TEXT NOT NULL CHECK (type_mouvement IN ('VENTE', 'AJUSTEMENT', 'RECEPTION')),
    utilisateur_id INTEGER,
    reference_operation TEXT,
    commentaire TEXT,
    FOREIGN KEY (produit_id) REFERENCES produits(id),
    FOREIGN KEY (utilisateur_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS journal_actions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
    utilisateur_id INTEGER,
    type_action TEXT NOT NULL,
    entite TEXT NOT NULL,
    description TEXT,
    details TEXT,
    FOREIGN KEY (utilisateur_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_historique_stock_date ON historique_stock(date);
CREATE INDEX IF NOT EXISTS idx_historique_stock_produit ON historique_stock(produit_id);
CREATE INDEX IF NOT EXISTS idx_journal_actions_date ON journal_actions(date);
CREATE INDEX IF NOT EXISTS idx_journal_actions_utilisateur ON journal_actions(utilisateur_id);

PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA cache_size = 20000;
PRAGMA page_size = 4096;
PRAGMA temp_store = MEMORY;
PRAGMA mmap_size = 30000000000;
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
('SIRET_ENTREPRISE', '12345678901234', 'Numéro SIRET de l''entreprise'),
('LOGO_PATH', '', 'Chemin vers le logo de l''entreprise'),
('FORMAT_RECU', 'COMPACT', 'Format des reçus (COMPACT ou DETAILLE)'),
('PIED_PAGE_RECU', 'Merci de votre visite !', 'Message en pied de page des reçus'),
('EN_TETE_RECU', '', 'Message en en-tête des reçus'),
('POLICE_TITRE_RECU', '14', 'Taille de la police pour le titre du reçu'),
('POLICE_TEXTE_RECU', '12', 'Taille de la police pour le texte du reçu'),
('ALIGNEMENT_TITRE_RECU', 'CENTRE', 'Alignement du titre (GAUCHE, CENTRE, DROITE)'),
('ALIGNEMENT_TEXTE_RECU', 'GAUCHE', 'Alignement du texte (GAUCHE, CENTRE, DROITE)'),
('STYLE_BORDURE_RECU', 'SIMPLE', 'Style de bordure du reçu (SIMPLE, DOUBLE, POINTILLES)'),
('MESSAGE_COMMERCIAL_RECU', '', 'Message commercial ou promotionnel sur le reçu'),
('AFFICHER_TVA_DETAILS', 'true', 'Afficher les détails de TVA sur le reçu'),
('INFO_SUPPLEMENTAIRE_RECU', '', 'Informations supplémentaires sur le reçu');

-- Ajout de la table pour la gestion des utilisateurs
DROP TABLE IF EXISTS users;
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER',
    created_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000),
    last_login INTEGER,
    active BOOLEAN DEFAULT true,
    CONSTRAINT username_min_length CHECK (length(username) >= 3)
);

-- Index pour optimiser les recherches sur le nom d'utilisateur
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);