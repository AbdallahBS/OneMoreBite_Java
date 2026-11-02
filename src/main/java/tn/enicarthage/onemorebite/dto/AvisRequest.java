package tn.enicarthage.onemorebite.dto;

public record AvisRequest(
        int note,
        String commentaire,
        int clientId,
        Integer produitId
) {}
