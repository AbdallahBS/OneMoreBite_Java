package tn.enicarthage.onemorebite.dto;

import java.util.List;

/**
 * Synthèse des avis d'un commerçant : liste complète, note moyenne et nombre
 * total d'avis. Utilisée par l'endpoint public /commercants/{id}/avis.
 */
public record AvisCommercantResponse(
        int commercantId,
        String commercantNom,
        double noteMoyenne,
        int nombreAvis,
        List<AvisResponse> avis
) {}
