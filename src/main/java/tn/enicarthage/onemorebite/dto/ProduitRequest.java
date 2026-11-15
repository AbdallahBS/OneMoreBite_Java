package tn.enicarthage.onemorebite.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Requête de création / mise à jour d'un produit.
 * Les types "boxés" permettent de distinguer un champ absent d'un champ volontairement mis à zéro.
 */
public record ProduitRequest(
        String nom,
        String description,
        Double prix,
        Integer quantite,
        String imageUrl,
        List<String> images,
        LocalDate dateExpiration,
        Boolean disponibilite,
        Integer commercantId
) {}
