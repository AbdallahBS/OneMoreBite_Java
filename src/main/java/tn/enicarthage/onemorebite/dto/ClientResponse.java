package tn.enicarthage.onemorebite.dto;

public record ClientResponse(
        int id,
        String nom,
        String email,
        String telephone,
        String role
) {}
