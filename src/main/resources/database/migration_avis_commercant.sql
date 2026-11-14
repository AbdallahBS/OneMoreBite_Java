-- Migration : rattachement des avis au commerçant (BF-13 / BF-14).
--
-- L'ancienne table avis référençait un produit (avis.produit_id). Le module de
-- gestion des avis note désormais directement un commerçant. Cette migration
-- conserve les avis existants en déduisant le commerçant via
-- produit.commercant_id, puis remplace la table avis.
--
-- À exécuter une seule fois sur une base créée avant ce changement. Le script
-- complet database.sql contient déjà le nouveau schéma.

USE onemorebite_db;

DROP TABLE IF EXISTS avis_migration_tmp;

CREATE TABLE avis_migration_tmp (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    note INT NOT NULL,
                                    commentaire TEXT,
                                    date_avis DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    client_id INT NOT NULL,
                                    commercant_id INT NOT NULL,
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO avis_migration_tmp (id, note, commentaire, date_avis, client_id, commercant_id, created_at, updated_at)
SELECT a.id,
       a.note,
       COALESCE(a.commentaire, ''),
       a.date_avis,
       a.client_id,
       p.commercant_id,
       a.created_at,
       a.updated_at
FROM avis a
INNER JOIN produit p ON p.id = a.produit_id;

DROP TABLE avis;

CREATE TABLE avis (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      note INT NOT NULL CHECK (note BETWEEN 1 AND 5),
                      commentaire TEXT NOT NULL,
                      date_avis DATETIME DEFAULT CURRENT_TIMESTAMP,
                      client_id INT NOT NULL,
                      commercant_id INT NOT NULL,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      CONSTRAINT fk_avis_client FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE,
                      CONSTRAINT fk_avis_commercant FOREIGN KEY (commercant_id) REFERENCES commercant(id) ON DELETE CASCADE
);

INSERT INTO avis (id, note, commentaire, date_avis, client_id, commercant_id, created_at, updated_at)
SELECT id, note, commentaire, date_avis, client_id, commercant_id, created_at, updated_at
FROM avis_migration_tmp;

DROP TABLE avis_migration_tmp;
