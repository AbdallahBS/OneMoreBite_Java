package tn.enicarthage.onemorebite.dto;

/**
 * Vue sécurisée d'un utilisateur pour la supervision administrateur.
 * Le mot de passe n'est jamais exposé.
 */
public record UtilisateurResponse(
        int id,
        String nom,
        String email,
        String telephone,
        String role,
        String statut,
        String localisation
) {}
