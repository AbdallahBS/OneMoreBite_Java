-- Migration: add photo_url to utilisateur table
ALTER TABLE utilisateur ADD COLUMN photo_url VARCHAR(500) DEFAULT NULL AFTER telephone;

