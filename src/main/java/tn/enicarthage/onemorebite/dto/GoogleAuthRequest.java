package tn.enicarthage.onemorebite.dto;

public record GoogleAuthRequest(
        String credential,
        String email,
        String nom,
        String photoUrl,
        String role
) {}
