package tn.enicarthage.onemorebite.dto;

public record UpdateProfilRequest(
        String nom,
        String email,
        String telephone,
        String motDePasse,
        String ancienMotDePasse,
        String localisation
) {}
