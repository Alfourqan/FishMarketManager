-- Configuration de la base de données PostgreSQL
SET client_encoding = 'UTF8';

-- Tables principales dans l'ordre de dépendance
DROP TABLE IF EXISTS reglements_clients CASCADE;
DROP TABLE IF EXISTS mouvements_caisse CASCADE;
DROP TABLE IF EXISTS user_actions CASCADE;
DROP TABLE IF EXISTS ventes CASCADE;
DROP TABLE IF EXISTS lignes_vente CASCADE;
DROP TABLE IF EXISTS produits CASCADE;
DROP TABLE IF EXISTS fournisseurs CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS configurations CASCADE;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    active BOOLEAN DEFAULT true,
    force_password_reset BOOLEAN DEFAULT false,
    CONSTRAINT username_min_length CHECK (length(username) >= 3),
    CONSTRAINT password_min_length CHECK (length(password) >= 6)
);

CREATE TABLE configurations (
    id SERIAL PRIMARY KEY,
    cle VARCHAR(50) NOT NULL UNIQUE,
    valeur TEXT,
    description TEXT,
    CONSTRAINT cle_not_empty CHECK (length(trim(cle)) > 0)
);

CREATE TABLE fournisseurs (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    contact VARCHAR(100),
    telephone VARCHAR(20),
    email VARCHAR(100),
    adresse TEXT,
    statut VARCHAR(20) DEFAULT 'Actif',
    supprime BOOLEAN DEFAULT false,
    CONSTRAINT nom_fournisseur_min_length CHECK (length(trim(nom)) >= 2),
    CONSTRAINT contact_min_length CHECK (contact IS NULL OR length(trim(contact)) >= 2),
    CONSTRAINT telephone_format CHECK (telephone IS NULL OR length(trim(telephone)) >= 8),
    CONSTRAINT email_format CHECK (email IS NULL OR email LIKE '%@%.%'),
    CONSTRAINT adresse_min_length CHECK (adresse IS NULL OR length(trim(adresse)) >= 5),
    CONSTRAINT statut_valide CHECK (statut IN ('Actif', 'Inactif', 'En attente'))
);

-- Créer un fournisseur par défaut
INSERT INTO fournisseurs (nom, contact, telephone, email, statut) 
VALUES ('Fournisseur par défaut', 'Contact', '0123456789', 'contact@default.com', 'Actif');

CREATE TABLE produits (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    categorie VARCHAR(50) NOT NULL,
    prix_achat NUMERIC(10,2) NOT NULL,
    prix_vente NUMERIC(10,2) NOT NULL,
    stock INTEGER NOT NULL,
    seuil_alerte INTEGER NOT NULL,
    fournisseur_id INTEGER,
    supprime BOOLEAN DEFAULT false,
    CONSTRAINT nom_produit_min_length CHECK (length(trim(nom)) >= 2),
    CONSTRAINT categorie_valide CHECK (trim(categorie) IN ('Frais', 'Surgelé', 'Transformé')),
    CONSTRAINT prix_achat_positif CHECK (prix_achat > 0),
    CONSTRAINT prix_vente_positif CHECK (prix_vente > 0),
    CONSTRAINT prix_vente_superieur CHECK (prix_vente > prix_achat),
    CONSTRAINT stock_positif CHECK (stock >= 0),
    CONSTRAINT seuil_alerte_positif CHECK (seuil_alerte >= 0),
    FOREIGN KEY (fournisseur_id) REFERENCES fournisseurs(id)
);

CREATE TABLE clients (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    telephone VARCHAR(20),
    adresse TEXT,
    solde NUMERIC(10,2) DEFAULT 0,
    supprime BOOLEAN DEFAULT false,
    CONSTRAINT nom_client_min_length CHECK (length(trim(nom)) >= 2),
    CONSTRAINT telephone_format CHECK (telephone IS NULL OR length(trim(telephone)) >= 8),
    CONSTRAINT adresse_min_length CHECK (adresse IS NULL OR length(trim(adresse)) >= 5)
);

CREATE TABLE mouvements_caisse (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ENTREE', 'SORTIE', 'OUVERTURE', 'CLOTURE')),
    montant NUMERIC(10,2) NOT NULL CHECK (montant > 0),
    description TEXT,
    user_id INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT description_min_length CHECK (description IS NULL OR length(trim(description)) >= 3)
);

CREATE TABLE user_actions (
    id SERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    username VARCHAR(50) NOT NULL,
    date_time TIMESTAMP NOT NULL,
    description TEXT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id INTEGER NOT NULL,
    user_id INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT description_min_length CHECK (length(trim(description)) >= 3)
);

CREATE TABLE ventes (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_id INTEGER,
    credit INTEGER DEFAULT 0,
    total NUMERIC(10,2) NOT NULL,
    supprime BOOLEAN DEFAULT false,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT total_positif CHECK (total >= 0),
    CONSTRAINT credit_positif CHECK (credit >= 0)
);

CREATE TABLE lignes_vente (
    id SERIAL PRIMARY KEY,
    vente_id INTEGER,
    produit_id INTEGER,
    quantite INTEGER NOT NULL,
    prix_unitaire NUMERIC(10,2) NOT NULL,
    FOREIGN KEY (vente_id) REFERENCES ventes(id),
    FOREIGN KEY (produit_id) REFERENCES produits(id),
    CONSTRAINT quantite_positive CHECK (quantite > 0),
    CONSTRAINT prix_unitaire_positif CHECK (prix_unitaire > 0)
);

-- Nouvelle table pour les règlements clients
CREATE TABLE reglements_clients (
    id SERIAL PRIMARY KEY,
    client_id INTEGER NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    montant NUMERIC(10,2) NOT NULL,
    type_paiement VARCHAR(20) NOT NULL,
    commentaire TEXT,
    vente_id INTEGER,
    user_id INTEGER,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (vente_id) REFERENCES ventes(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT montant_positif CHECK (montant > 0),
    CONSTRAINT type_paiement_valide CHECK (type_paiement IN ('ESPECES', 'CARTE', 'CHEQUE', 'VIREMENT')),
    CONSTRAINT commentaire_min_length CHECK (commentaire IS NULL OR length(trim(commentaire)) >= 3)
);

-- Ajout des index pour la nouvelle table
CREATE INDEX IF NOT EXISTS idx_reglements_clients_client ON reglements_clients(client_id);
CREATE INDEX IF NOT EXISTS idx_reglements_clients_date ON reglements_clients(date);
CREATE INDEX IF NOT EXISTS idx_reglements_clients_vente ON reglements_clients(vente_id);
CREATE INDEX IF NOT EXISTS idx_reglements_clients_user ON reglements_clients(user_id);


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
INSERT INTO configurations (cle, valeur, description) VALUES
('TAUX_TVA', '20.0', 'Taux de TVA en pourcentage'),
('TVA_ENABLED', 'true', 'Activation/désactivation de la TVA'),
('NOM_ENTREPRISE', '', 'Nom de l''entreprise'),
('ADRESSE_ENTREPRISE', '', 'Adresse de l''entreprise'),
('TELEPHONE_ENTREPRISE', '', 'Numéro de téléphone de l''entreprise'),
('EMAIL_ENTREPRISE', '', 'Adresse email de l''entreprise'),
('SIRET_ENTREPRISE', '12345678901234', 'Numéro SIRET de l''entreprise');