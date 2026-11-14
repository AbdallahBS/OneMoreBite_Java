package tn.enicarthage.onemorebite.dto;

/**
 * Requête de modification d'un avis par son client propriétaire.
 * Seuls la note et le commentaire sont modifiables ; le commerçant ciblé
 * reste inchangé.
 */
public record AvisUpdateRequest(
        Integer note,
        String commentaire
) {}
