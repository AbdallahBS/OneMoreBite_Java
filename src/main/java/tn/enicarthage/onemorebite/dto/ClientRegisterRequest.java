package tn.enicarthage.onemorebite.dto;

public record ClientRegisterRequest(
        String nom,
        String email,
        String motDePasse,
        String telephone
) {}
