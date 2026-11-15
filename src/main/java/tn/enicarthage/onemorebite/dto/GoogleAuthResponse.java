package tn.enicarthage.onemorebite.dto;

public record GoogleAuthResponse(
        String token,
        String type,
        int id,
        String nom,
        String email,
        String role,
        String photoUrl,
        boolean profilComplet,
        boolean nouveauCompte,
        String message
) {}
