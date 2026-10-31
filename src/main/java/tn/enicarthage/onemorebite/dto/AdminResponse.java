package tn.enicarthage.onemorebite.dto;

public record AdminResponse(
        int id,
        String nom,
        String email,
        String telephone,
        String role
) {}
