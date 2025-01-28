-- Insertion de produits de test
INSERT INTO produits (nom, categorie, prix_achat, prix_vente, stock, seuil_alerte) VALUES
('Saumon frais', 'Poissons frais', 15.50, 25.90, 50, 10),
('Thon rouge', 'Poissons frais', 18.75, 32.50, 30, 5),
('Crevettes', 'Crustacés', 12.00, 19.90, 100, 20),
('Huîtres', 'Coquillages', 8.50, 14.90, 200, 30);

-- Insertion de clients de test
INSERT INTO clients (nom, telephone, adresse, solde) VALUES
('Jean Dupont', '0123456789', '123 Rue de la Mer', 0),
('Marie Martin', '0234567890', '456 Avenue des Poissons', 0);

-- Insertion de ventes de test
INSERT INTO ventes (date, client_id, credit, total) VALUES
(strftime('%s','now') * 1000, 1, 0, 77.70),
(strftime('%s','now', '-1 day') * 1000, 2, 0, 32.50),
(strftime('%s','now', '-2 day') * 1000, NULL, 0, 44.80);

-- Insertion de lignes de vente de test
INSERT INTO lignes_vente (vente_id, produit_id, quantite, prix_unitaire) VALUES
(1, 1, 2, 25.90),
(1, 3, 1, 25.90),
(2, 2, 1, 32.50),
(3, 3, 1, 19.90),
(3, 4, 2, 14.90);
