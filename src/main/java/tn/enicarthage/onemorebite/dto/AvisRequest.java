package tn.enicarthage.onemorebite.dto;

/**
 * Requête de création d'un avis par un client authentifié.
 * Le clientId n'est volontairement pas accepté : il est déduit du token JWT.
 */
public record AvisRequest(
        Integer commercantId,
        Integer note,
        String commentaire
) {}
