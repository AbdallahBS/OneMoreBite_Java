package tn.enicarthage.onemorebite.dto;

import java.time.LocalDate;

/**
 * Vue détaillée d'une réservation (table commande) avec les informations du
 * produit, du commerçant et du client associés. Les statuts exposés sont les
 * valeurs existantes de l'enum StatutCommande : EN_ATTENTE, CONFIRMEE,
 * REFUSEE, ANNULEE.
 */
public record ReservationResponse(
        int id,
        String dateCommande,
        int quantite,
        String statut,
        int produitId,
        String produitNom,
        String produitDescription,
        double produitPrix,
        String produitImageUrl,
        LocalDate produitDateExpiration,
        int produitQuantite,
        boolean produitDisponibilite,
        int commercantId,
        String commercantNom,
        String commercantLocalisation,
        Integer clientId,
        String clientNom,
        String clientEmail,
        String clientTelephone,
        String nomInvite,
        String emailInvite,
        String telephoneInvite
) {}
