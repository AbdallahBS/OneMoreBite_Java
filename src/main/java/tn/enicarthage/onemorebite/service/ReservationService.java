package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.ProduitDAO;
import tn.enicarthage.onemorebite.dao.ReservationDAO;
import tn.enicarthage.onemorebite.dto.ReservationRequest;
import tn.enicarthage.onemorebite.dto.ReservationResponse;
import tn.enicarthage.onemorebite.dto.ReservationUpdateRequest;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.exception.ForbiddenException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Commande;
import tn.enicarthage.onemorebite.model.Produit;
import tn.enicarthage.onemorebite.model.StatutCommande;
import tn.enicarthage.onemorebite.security.JwtUtil;

import java.util.Date;
import java.util.List;

/**
 * Règles métier des réservations. L'identité du client ou du commerçant est
 * toujours déduite du token JWT ; aucune information d'appartenance fournie
 * par le frontend n'est utilisée.
 */
public class ReservationService {

    private static final String STATUT_EN_ATTENTE = StatutCommande.EN_ATTENTE.name();

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final ProduitDAO produitDAO = new ProduitDAO();

    public ReservationResponse creer(ReservationRequest req, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerClient(claims);

        if (req == null) {
            throw new IllegalArgumentException("La requête de réservation est obligatoire.");
        }
        if (req.produitId() == null || req.produitId() <= 0) {
            throw new IllegalArgumentException("Le champ 'produitId' est obligatoire et doit être positif.");
        }
        if (req.quantite() == null || req.quantite() <= 0) {
            throw new IllegalArgumentException("Le champ 'quantite' est obligatoire et doit être strictement positif.");
        }

        Produit produit = produitDAO.trouverParId(req.produitId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit non trouvé avec id: " + req.produitId()));

        if (!produit.isDisponibilite()) {
            throw new IllegalArgumentException("Ce produit n'est pas disponible à la réservation.");
        }
        if (produit.getQuantite() < req.quantite()) {
            throw new IllegalArgumentException(
                    "Stock insuffisant : " + produit.getQuantite() + " unité(s) disponible(s).");
        }

        Commande commande = new Commande();
        commande.setDateCommande(new Date());
        commande.setQuantite(req.quantite());
        commande.setStatut(StatutCommande.EN_ATTENTE);
        commande.setProduitId(req.produitId());
        commande.setClientId(utilisateur.id());
        commande.setNomInvite(null);
        commande.setEmailInvite(null);
        commande.setTelephoneInvite(null);

        return reservationDAO.creer(commande);
    }

    public List<ReservationResponse> listerMesReservations(JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerClient(claims);
        return reservationDAO.listerParClient(utilisateur.id());
    }

    public List<ReservationResponse> listerRecues(JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerCommercant(claims);
        return reservationDAO.listerParCommercant(utilisateur.id());
    }

    public ReservationResponse obtenir(int id, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerClientOuCommercant(claims);
        ReservationResponse reservation = trouverReservation(id);

        if (estClient(utilisateur)) {
            verifierProprietaireClient(reservation, utilisateur.id());
        } else {
            verifierProprietaireCommercant(reservation, utilisateur.id());
        }
        return reservation;
    }

    public ReservationResponse modifier(int id, ReservationUpdateRequest req, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerClient(claims);

        if (req == null || req.quantite() == null) {
            throw new IllegalArgumentException("Le champ 'quantite' est obligatoire.");
        }
        if (req.quantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive.");
        }

        ReservationResponse reservation = trouverReservation(id);
        verifierProprietaireClient(reservation, utilisateur.id());
        exigerStatutEnAttente(reservation, "modifiées");

        if (!reservation.produitDisponibilite()) {
            throw new IllegalArgumentException("Ce produit n'est plus disponible.");
        }
        if (reservation.produitQuantite() < req.quantite()) {
            throw new IllegalArgumentException(
                    "Stock insuffisant : " + reservation.produitQuantite() + " unité(s) disponible(s).");
        }

        if (!reservationDAO.modifierQuantite(id, utilisateur.id(), req.quantite())) {
            verifierEchecClient(id, utilisateur.id(), "modifiées");
        }
        return trouverReservation(id);
    }

    public ReservationResponse annuler(int id, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerClient(claims);

        ReservationResponse reservation = trouverReservation(id);
        verifierProprietaireClient(reservation, utilisateur.id());
        exigerStatutEnAttente(reservation, "annulées");

        if (!reservationDAO.annuler(id, utilisateur.id())) {
            verifierEchecClient(id, utilisateur.id(), "annulées");
        }
        return trouverReservation(id);
    }

    public ReservationResponse confirmer(int id, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerCommercant(claims);

        ReservationResponse reservation = trouverReservation(id);
        verifierProprietaireCommercant(reservation, utilisateur.id());
        exigerStatutEnAttente(reservation, "confirmées");

        if (!reservationDAO.changerStatutParCommercant(id, utilisateur.id(), StatutCommande.CONFIRMEE)) {
            verifierEchecCommercant(id, utilisateur.id(), "confirmées");
        }
        return trouverReservation(id);
    }

    public ReservationResponse refuser(int id, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerCommercant(claims);

        ReservationResponse reservation = trouverReservation(id);
        verifierProprietaireCommercant(reservation, utilisateur.id());
        exigerStatutEnAttente(reservation, "refusées");

        if (!reservationDAO.changerStatutParCommercant(id, utilisateur.id(), StatutCommande.REFUSEE)) {
            verifierEchecCommercant(id, utilisateur.id(), "refusées");
        }
        return trouverReservation(id);
    }

    private ReservationResponse trouverReservation(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identifiant de réservation invalide: " + id);
        }
        return reservationDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'id : " + id));
    }

    private void verifierProprietaireClient(ReservationResponse reservation, int clientId) {
        if (reservation.clientId() == null || !reservation.clientId().equals(clientId)) {
            throw new ForbiddenException("Vous ne pouvez accéder qu'à vos propres réservations.");
        }
    }

    private void verifierProprietaireCommercant(ReservationResponse reservation, int commercantId) {
        if (reservation.commercantId() != commercantId) {
            throw new ForbiddenException("Vous ne pouvez gérer que les réservations de vos propres produits.");
        }
    }

    private void exigerStatutEnAttente(ReservationResponse reservation, String action) {
        if (!STATUT_EN_ATTENTE.equalsIgnoreCase(reservation.statut())) {
            throw new IllegalArgumentException(
                    "Seules les réservations en attente peuvent être " + action + ".");
        }
    }

    private void verifierEchecClient(int id, int clientId, String action) {
        ReservationResponse actuelle = reservationDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'id : " + id));

        verifierProprietaireClient(actuelle, clientId);
        exigerStatutEnAttente(actuelle, action);
        throw new IllegalArgumentException(
                "La réservation n'a pas pu être " + action + " en raison d'une modification concurrente.");
    }

    private void verifierEchecCommercant(int id, int commercantId, String action) {
        ReservationResponse actuelle = reservationDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'id : " + id));

        verifierProprietaireCommercant(actuelle, commercantId);
        exigerStatutEnAttente(actuelle, action);
        throw new IllegalArgumentException(
                "La réservation n'a pas pu être " + action + " en raison d'une modification concurrente.");
    }

    private JwtUtil.JwtClaims exigerClient(JwtUtil.JwtClaims claims) {
        if (claims == null) {
            throw new AuthenticationException("Authentification requise.");
        }
        if (!estClient(claims)) {
            throw new ForbiddenException("Cette action est réservée aux clients.");
        }
        return claims;
    }

    private JwtUtil.JwtClaims exigerCommercant(JwtUtil.JwtClaims claims) {
        if (claims == null) {
            throw new AuthenticationException("Authentification requise.");
        }
        if (!estCommercant(claims)) {
            throw new ForbiddenException("Cette action est réservée aux commerçants.");
        }
        return claims;
    }

    private JwtUtil.JwtClaims exigerClientOuCommercant(JwtUtil.JwtClaims claims) {
        if (claims == null) {
            throw new AuthenticationException("Authentification requise.");
        }
        if (!estClient(claims) && !estCommercant(claims)) {
            throw new ForbiddenException("Cette action est réservée aux clients et aux commerçants.");
        }
        return claims;
    }

    private boolean estClient(JwtUtil.JwtClaims claims) {
        return claims != null && "CLIENT".equalsIgnoreCase(claims.role());
    }

    private boolean estCommercant(JwtUtil.JwtClaims claims) {
        return claims != null && "COMMERCANT".equalsIgnoreCase(claims.role());
    }
}
