package tn.enicarthage.onemorebite.dto;

public record LoginResponse(
        String token,
        String type,
        int id,
        String nom,
        String email,
        String role
) {}
