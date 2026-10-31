package tn.enicarthage.onemorebite.dto;

public record CommercantResponse(
        int id,
        String nom,
        String email,
        String telephone,
        String localisation,
        String statut
) {}
