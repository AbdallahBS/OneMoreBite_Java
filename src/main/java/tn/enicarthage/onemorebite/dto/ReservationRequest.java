package tn.enicarthage.onemorebite.dto;

/**
 * Requête de création d'une réservation par un client authentifié.
 * Le clientId n'est volontairement pas accepté : il est déduit du token JWT.
 */
public record ReservationRequest(
        Integer produitId,
        Integer quantite
) {}
