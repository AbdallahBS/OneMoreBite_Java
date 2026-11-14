package tn.enicarthage.onemorebite.dto;

import java.time.LocalDateTime;

/**
 * Vue d'un avis avec les noms du client et du commerçant, obtenus par jointure
 * SQL afin d'éviter des appels supplémentaires côté frontend.
 */
public record AvisResponse(
        int id,
        int note,
        String commentaire,
        LocalDateTime dateAvis,
        int clientId,
        String clientNom,
        int commercantId,
        String commercantNom
) {}
