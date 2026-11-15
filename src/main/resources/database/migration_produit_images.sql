-- Migration: Permettre plusieurs images par produit (URLs séparées par des virgules)
ALTER TABLE produit MODIFY COLUMN image_url TEXT;

