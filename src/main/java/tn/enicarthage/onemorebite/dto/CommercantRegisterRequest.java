package tn.enicarthage.onemorebite.dto;

public record CommercantRegisterRequest(
        String nom,
        String email,
        String motDePasse,
        String telephone,
        String localisation
) {}
