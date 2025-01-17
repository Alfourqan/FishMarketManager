-- Creation of tables with PostgreSQL syntax
CREATE TABLE IF NOT EXISTS configurations (
    id SERIAL PRIMARY KEY,
    cle VARCHAR(255) NOT NULL UNIQUE,
    valeur TEXT,
    description TEXT
);

CREATE TABLE IF NOT EXISTS produits (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    categorie VARCHAR(50) NOT NULL,
    prix DECIMAL(10,2) NOT NULL,
    stock INTEGER NOT NULL,
    seuil_alerte INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    telephone VARCHAR(20),
    adresse TEXT,
    solde DECIMAL(10,2) DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ventes (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_id INTEGER REFERENCES clients(id),
    credit BOOLEAN DEFAULT FALSE,
    total DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS lignes_vente (
    id SERIAL PRIMARY KEY,
    vente_id INTEGER REFERENCES ventes(id),
    produit_id INTEGER REFERENCES produits(id),
    quantite INTEGER NOT NULL,
    prix_unitaire DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS mouvements_caisse (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(10) CHECK (type IN ('ENTREE', 'SORTIE')),
    montant DECIMAL(10,2) NOT NULL,
    description TEXT
);

-- Insertion des configurations par défaut si elles n'existent pas
INSERT INTO configurations (cle, valeur, description) VALUES
('TAUX_TVA', '20.0', 'Taux de TVA en pourcentage'),
('NOM_ENTREPRISE', '', 'Nom de l''entreprise'),
('ADRESSE_ENTREPRISE', '', 'Adresse de l''entreprise'),
('TELEPHONE_ENTREPRISE', '', 'Numéro de téléphone de l''entreprise'),
('PIED_PAGE_RECU', 'Merci de votre visite !', 'Message en pied de page des reçus');