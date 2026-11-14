package tn.enicarthage.onemorebite.dto;

/**
 * Données publiques d'un commerçant, exposées au frontend sans information
 * sensible (email, mot de passe, statut interne).
 */
public record CommercantPublicResponse(
        int id,
        String nom,
        String localisation,
        String telephone
) {
    public CommercantPublicResponse {
        // Le téléphone est optionnel en base. On garantit toutefois la présence
        // de la clé JSON (Gson omet les champs null par défaut).
        telephone = telephone == null ? "" : telephone;
    }
}
