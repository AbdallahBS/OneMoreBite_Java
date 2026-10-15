package tn.enicarthage.onemorebite.dto;


import java.time.LocalDate;

public record ProduitRequest(
        String nom,
        String description,
        double prix,
        int quantite,
        String imageUrl,
        LocalDate dateExpiration,
        boolean disponibilite,
        int commercantId
) {}