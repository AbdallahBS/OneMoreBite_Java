package tn.enicarthage.onemorebite.dto;

/**
 * Requête de modification d'une réservation par son client propriétaire.
 * Seule la quantité est modifiable ; le produit et le statut restent maîtrisés
 * par le serveur.
 */
public record ReservationUpdateRequest(
        Integer quantite
) {}
