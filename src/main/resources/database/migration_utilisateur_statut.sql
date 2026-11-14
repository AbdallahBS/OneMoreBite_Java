-- Migration : statut de compte utilisateur pour la supervision administrateur
-- (BF-15).
--
-- Ajoute la colonne utilisateur.statut, distincte du statut métier
-- commercant.statut (workflow de validation). Les comptes existants sont
-- ACTIF par défaut, puis le statut des commerçants est aligné sur leur statut
-- métier existant (EN_ATTENTE, ACTIF ou REJETE).
--
-- Ce script est exécutable sur une base héritée (utilisateur.statut =
-- INACTIF/ACTIF/REFUSE) ou sur une base fraîche sans colonnes : il ajoute la
-- colonne si elle est absente, étend l'enum si besoin, aligne les valeurs
-- historiques puis restreint l'enum à la nouvelle convention du projet.

USE onemorebite_db;

-- 1. Ajout de la colonne si la base n'en possède pas encore.
SET @colonne_statut_existe := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'utilisateur'
      AND COLUMN_NAME = 'statut'
);
SET @sql_colonne := IF(
    @colonne_statut_existe = 0,
    'ALTER TABLE utilisateur ADD COLUMN statut ENUM(''ACTIF'',''EN_ATTENTE'',''REJETE'',''DESACTIVE'') NOT NULL DEFAULT ''ACTIF'' AFTER mot_de_passe',
    'SELECT 1'
);
PREPARE stmt_statut FROM @sql_colonne;
EXECUTE stmt_statut;
DEALLOCATE PREPARE stmt_statut;

-- 2. Extension temporaire de l'enum pour supporter les valeurs historiques.
ALTER TABLE utilisateur
    MODIFY COLUMN statut ENUM('INACTIF', 'ACTIF', 'REFUSE', 'EN_ATTENTE', 'REJETE', 'DESACTIVE')
    NOT NULL DEFAULT 'ACTIF';

-- 3. Alignement : pour un commerçant, le statut métier (commerant.statut)
--    est la référence du workflow de validation.
UPDATE utilisateur u
INNER JOIN commercant c ON c.id = u.id
SET u.statut = CASE c.statut
                    WHEN 'EN_ATTENTE' THEN 'EN_ATTENTE'
                    WHEN 'REJETE' THEN 'REJETE'
                    WHEN 'ACTIF' THEN 'ACTIF'
                    ELSE u.statut
                END
WHERE u.statut IN ('INACTIF', 'REFUSE');

-- 4. Pour les autres comptes (clients, administrateurs), INACTIF/REFUSE
--    correspondent à des valeurs par défaut d'un flux non encore finalisé :
--    on les considère comme actifs. Les vrais refus/utilisateurs désactivés
--    sont gérés via le statut administrateur (DESACTIVE).
UPDATE utilisateur SET statut = 'ACTIF' WHERE statut = 'INACTIF';
UPDATE utilisateur SET statut = 'REJETE' WHERE statut = 'REFUSE';

-- 5. Restauration de l'enum de référence.
ALTER TABLE utilisateur
    MODIFY COLUMN statut ENUM('ACTIF', 'EN_ATTENTE', 'REJETE', 'DESACTIVE') NOT NULL DEFAULT 'ACTIF';

-- 6. Le statut métier d'un commerçant accepté devient "ACTIF", rejeté devient
--    "REJETE" et toute désactivation récente est représentée par "DESACTIVE".
ALTER TABLE commercant
    MODIFY COLUMN statut ENUM('EN_ATTENTE', 'ACTIF', 'REJETE', 'DESACTIVE') NOT NULL DEFAULT 'EN_ATTENTE';
