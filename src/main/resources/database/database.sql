DROP DATABASE IF EXISTS onemorebite_db;
CREATE DATABASE onemorebite_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE onemorebite_db;

-- 1. Tables

CREATE TABLE utilisateur (
                             id INT AUTO_INCREMENT PRIMARY KEY,
                             nom VARCHAR(100) NOT NULL,
                             email VARCHAR(255) NOT NULL UNIQUE,
                             mot_de_passe VARCHAR(255) NOT NULL,
                             telephone VARCHAR(30),
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE administrateur (
                                id INT PRIMARY KEY,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                CONSTRAINT fk_admin_utilisateur FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE commercant (
                            id INT PRIMARY KEY,
                            localisation VARCHAR(255) NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            CONSTRAINT fk_commercant_utilisateur FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE client (
                        id INT PRIMARY KEY,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_client_utilisateur FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE produit (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         nom VARCHAR(150) NOT NULL,
                         description TEXT,
                         prix DECIMAL(10, 3) NOT NULL,
                         quantite INT NOT NULL DEFAULT 0,
                         image_url VARCHAR(255),
                         date_expiration DATE,
                         disponibilite BOOLEAN DEFAULT TRUE,
                         commercant_id INT NOT NULL,
                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         CONSTRAINT fk_produit_commercant FOREIGN KEY (commercant_id) REFERENCES commercant(id) ON DELETE CASCADE
);

CREATE TABLE commande (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          date_commande DATETIME DEFAULT CURRENT_TIMESTAMP,
                          quantite INT NOT NULL DEFAULT 1,
                          statut ENUM('EN_ATTENTE', 'CONFIRMEE', 'ANNULEE') DEFAULT 'EN_ATTENTE',
                          produit_id INT NOT NULL,
                          client_id INT NULL,
                          nom_invite VARCHAR(100),
                          email_invite VARCHAR(255),
                          telephone_invite VARCHAR(30),
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT fk_commande_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE,
                          CONSTRAINT fk_commande_client FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE SET NULL
);

CREATE TABLE avis (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      note INT NOT NULL CHECK (note BETWEEN 1 AND 5),
                      commentaire TEXT,
                      date_avis DATETIME DEFAULT CURRENT_TIMESTAMP,
                      client_id INT NOT NULL,
                      produit_id INT NOT NULL,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      CONSTRAINT fk_avis_client FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE,
                      CONSTRAINT fk_avis_produit FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
);

-- 2. Insertions ordonnées

INSERT INTO utilisateur (id, nom, email, mot_de_passe, telephone) VALUES
                                                                      (1, 'Admin System', 'admin@onemorebite.tn', 'admin123', '71000000'),
                                                                      (2, 'Boulangerie Ben Ali', 'contact@benali.tn', 'pass123', '71111111'),
                                                                      (3, 'Sami Client', 'sami@gmail.com', 'client123', '20222222');

INSERT INTO administrateur (id) VALUES (1);
INSERT INTO commercant (id, localisation) VALUES (2, 'Tunis, Centre Ville');
INSERT INTO client (id) VALUES (3);

INSERT INTO produit (id, nom, description, prix, quantite, image_url, date_expiration, disponibilite, commercant_id) VALUES
                                                                                                                         (1, 'Panier Viennoiseries', '4 croissants et 2 pains au chocolat du jour', 5.500, 10, 'https://example.com/images/viennoiseries.jpg', '2026-10-15', TRUE, 2),
                                                                                                                         (2, 'Panier Sandwichs', '2 sandwichs thon et fromage', 7.000, 5, 'https://example.com/images/sandwichs.jpg', '2026-10-12', TRUE, 2);

INSERT INTO commande (id, quantite, statut, produit_id, client_id) VALUES
    (1, 1, 'CONFIRMEE', 1, 3);

INSERT INTO commande (id, quantite, statut, produit_id, client_id, nom_invite, email_invite, telephone_invite) VALUES
    (2, 2, 'EN_ATTENTE', 2, NULL, 'Mohamed Invité', 'mohamed@gmail.com', '98888888');

INSERT INTO avis (note, commentaire, client_id, produit_id) VALUES
    (5, 'Excellent panier, viennoiseries encore très fraîches !', 3, 1);