-- Migration : ajout du statut REFUSEE aux réservations (table commande).
--
-- Nécessaire pour distinguer un refus par le commerçant (REFUSEE) d'une
-- annulation par le client propriétaire (ANNULEE), conformément aux règles
-- métier de réservation. Aucune autre modification de schéma n'est requise :
-- la table commande, ses clés étrangères et ses colonnes existantes sont
-- réutilisées.

USE onemorebite_db;

ALTER TABLE commande
    MODIFY COLUMN statut ENUM('EN_ATTENTE', 'CONFIRMEE', 'REFUSEE', 'ANNULEE')
    DEFAULT 'EN_ATTENTE';
