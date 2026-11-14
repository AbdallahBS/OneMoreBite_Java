package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.ReservationDAO;
import tn.enicarthage.onemorebite.dao.UtilisateurDAO;
import tn.enicarthage.onemorebite.dto.ReservationResponse;
import tn.enicarthage.onemorebite.dto.UtilisateurResponse;
import tn.enicarthage.onemorebite.exception.ForbiddenException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.StatutCommande;

import java.util.List;
import java.util.Locale;

/**
 * Supervision administrateur : gestion des comptes utilisateurs et
 * consultation transverse des réservations.
 */
public class AdminService {

    public static final String STATUT_ACTIF = "ACTIF";
    public static final String STATUT_DESACTIVE = "DESACTIVE";

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    public List<UtilisateurResponse> listerUtilisateurs(String role, String statut) {
        return utilisateurDAO.lister(normaliserRole(role), normaliserStatutUtilisateur(statut));
    }

    public UtilisateurResponse activerUtilisateur(int id) {
        trouverUtilisateur(id);
        if (!utilisateurDAO.mettreAJourStatut(id, STATUT_ACTIF)) {
            throw new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id);
        }
        return trouverUtilisateur(id);
    }

    public UtilisateurResponse desactiverUtilisateur(int id, int adminConnecteId) {
        UtilisateurResponse utilisateur = trouverUtilisateur(id);

        if (id == adminConnecteId) {
            throw new ForbiddenException(
                    "Vous ne pouvez pas désactiver votre propre compte administrateur.");
        }

        if ("ADMIN".equalsIgnoreCase(utilisateur.role())
                && AuthService.DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(utilisateur.email())) {
            throw new ForbiddenException(
                    "Le compte administrateur principal ne peut pas être désactivé.");
        }

        if (!utilisateurDAO.mettreAJourStatut(id, STATUT_DESACTIVE)) {
            throw new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id);
        }
        return trouverUtilisateur(id);
    }

    public List<ReservationResponse> listerReservations(String statut, Integer clientId, Integer commercantId) {
        if (clientId != null && clientId <= 0) {
            throw new IllegalArgumentException("Le paramètre 'clientId' doit être positif.");
        }
        if (commercantId != null && commercantId <= 0) {
            throw new IllegalArgumentException("Le paramètre 'commercantId' doit être positif.");
        }
        return reservationDAO.listerAvecFiltres(
                normaliserStatutReservation(statut), clientId, commercantId);
    }

    public ReservationResponse obtenirReservation(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identifiant de réservation invalide: " + id);
        }
        return reservationDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réservation non trouvée avec l'id : " + id));
    }

    private UtilisateurResponse trouverUtilisateur(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identifiant utilisateur invalide: " + id);
        }
        return utilisateurDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouvé avec l'id : " + id));
    }

    private String normaliserRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String valeur = role.trim().toUpperCase(Locale.ROOT);
        return switch (valeur) {
            case "ADMIN", "COMMERCANT", "CLIENT" -> valeur;
            default -> throw new IllegalArgumentException(
                    "Rôle invalide: " + role + ". Valeurs acceptées : CLIENT, COMMERCANT, ADMIN.");
        };
    }

    private String normaliserStatutUtilisateur(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        String valeur = statut.trim().toUpperCase(Locale.ROOT);
        return switch (valeur) {
            case "ACTIF" -> "ACTIF";
            case "EN_ATTENTE" -> "EN_ATTENTE";
            case "REJETE", "REFUSE" -> "REJETE";
            case "DESACTIVE", "INACTIF" -> "DESACTIVE";
            default -> throw new IllegalArgumentException(
                    "Statut invalide: " + statut
                            + ". Valeurs acceptées : ACTIF, EN_ATTENTE, REJETE, DESACTIVE.");
        };
    }

    private String normaliserStatutReservation(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        String valeur = statut.trim().toUpperCase(Locale.ROOT);
        try {
            return StatutCommande.valueOf(valeur).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Statut de réservation invalide: " + statut
                            + ". Valeurs acceptées : EN_ATTENTE, CONFIRMEE, REFUSEE, ANNULEE.");
        }
    }
}
