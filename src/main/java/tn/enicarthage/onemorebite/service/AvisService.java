package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.AvisDAO;
import tn.enicarthage.onemorebite.dao.CommercantDAO;
import tn.enicarthage.onemorebite.dto.AvisCommercantResponse;
import tn.enicarthage.onemorebite.dto.AvisRequest;
import tn.enicarthage.onemorebite.dto.AvisResponse;
import tn.enicarthage.onemorebite.dto.AvisUpdateRequest;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.exception.ForbiddenException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Avis;
import tn.enicarthage.onemorebite.model.Commercant;
import tn.enicarthage.onemorebite.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Règles métier des avis. L'identité du client est toujours déduite du token
 * JWT ; aucun clientId fourni par le frontend n'est accepté.
 */
public class AvisService {

    private final AvisDAO avisDAO = new AvisDAO();
    private final CommercantDAO commercantDAO = new CommercantDAO();

    public AvisResponse creer(AvisRequest req, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerClient(claims);

        if (req == null) {
            throw new IllegalArgumentException("La requête d'avis est obligatoire.");
        }
        if (req.commercantId() == null || req.commercantId() <= 0) {
            throw new IllegalArgumentException("Le champ 'commercantId' est obligatoire et doit être positif.");
        }

        Commercant commercant = commercantDAO.trouverParId(req.commercantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commerçant non trouvé avec l'id : " + req.commercantId()));

        if (!"ACTIF".equalsIgnoreCase(commercant.getStatut())) {
            throw new IllegalArgumentException("Le commerçant ciblé n'est pas actif.");
        }

        validerNote(req.note());
        validerCommentaire(req.commentaire());

        Avis avis = new Avis();
        avis.setNote(req.note());
        avis.setCommentaire(req.commentaire().trim());
        avis.setDateAvis(LocalDateTime.now());
        avis.setClientId(utilisateur.id());
        avis.setCommercantId(req.commercantId());

        return avisDAO.creer(avis);
    }

    public List<AvisResponse> lister(Integer commercantId) {
        if (commercantId == null) {
            return avisDAO.listerTous();
        }
        if (commercantId <= 0) {
            throw new IllegalArgumentException("Le paramètre 'commercantId' doit être positif.");
        }
        return avisDAO.listerParCommercant(commercantId);
    }

    public AvisCommercantResponse listerParCommercant(int commercantId) {
        if (commercantId <= 0) {
            throw new IllegalArgumentException("Identifiant de commerçant invalide: " + commercantId);
        }

        Commercant commercant = commercantDAO.trouverParId(commercantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commerçant non trouvé avec l'id : " + commercantId));

        List<AvisResponse> avis = avisDAO.listerParCommercant(commercantId);
        double noteMoyenne = avis.stream()
                .mapToInt(AvisResponse::note)
                .average()
                .orElse(0.0);
        noteMoyenne = Math.round(noteMoyenne * 100.0) / 100.0;

        return new AvisCommercantResponse(
                commercant.getId(),
                commercant.getNom(),
                noteMoyenne,
                avis.size(),
                avis
        );
    }

    public AvisResponse modifier(int id, AvisUpdateRequest req, JwtUtil.JwtClaims claims) {
        JwtUtil.JwtClaims utilisateur = exigerClient(claims);

        if (req == null) {
            throw new IllegalArgumentException("La requête de modification est obligatoire.");
        }

        AvisResponse avis = trouverAvis(id);
        verifierProprietaireClient(avis, utilisateur.id());

        validerNote(req.note());
        validerCommentaire(req.commentaire());

        if (!avisDAO.modifier(id, utilisateur.id(), req.note(), req.commentaire().trim())) {
            verifierEchecModification(id, utilisateur.id());
        }
        return trouverAvis(id);
    }

    public void supprimer(int id, JwtUtil.JwtClaims claims) {
        if (claims == null) {
            throw new AuthenticationException("Authentification requise.");
        }

        AvisResponse avis = trouverAvis(id);
        boolean admin = "ADMIN".equalsIgnoreCase(claims.role());
        boolean proprietaire = "CLIENT".equalsIgnoreCase(claims.role()) && avis.clientId() == claims.id();

        if (!admin && !proprietaire) {
            if ("CLIENT".equalsIgnoreCase(claims.role())) {
                throw new ForbiddenException("Vous ne pouvez supprimer que vos propres avis.");
            }
            throw new ForbiddenException("Seul le client auteur de l'avis ou un administrateur peut le supprimer.");
        }

        if (!avisDAO.supprimer(id)) {
            trouverAvis(id);
            throw new IllegalArgumentException(
                    "L'avis n'a pas pu être supprimé en raison d'une modification concurrente.");
        }
    }

    private AvisResponse trouverAvis(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identifiant d'avis invalide: " + id);
        }
        return avisDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avis non trouvé avec l'id : " + id));
    }

    private void verifierProprietaireClient(AvisResponse avis, int clientId) {
        if (avis.clientId() != clientId) {
            throw new ForbiddenException("Vous ne pouvez modifier que vos propres avis.");
        }
    }

    private void verifierEchecModification(int id, int clientId) {
        AvisResponse actuel = trouverAvis(id);
        verifierProprietaireClient(actuel, clientId);
        throw new IllegalArgumentException(
                "L'avis n'a pas pu être modifié en raison d'une modification concurrente.");
    }

    private void validerNote(Integer note) {
        if (note == null) {
            throw new IllegalArgumentException("Le champ 'note' est obligatoire.");
        }
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5.");
        }
    }

    private void validerCommentaire(String commentaire) {
        if (commentaire == null || commentaire.isBlank()) {
            throw new IllegalArgumentException("Le champ 'commentaire' est obligatoire.");
        }
    }

    private JwtUtil.JwtClaims exigerClient(JwtUtil.JwtClaims claims) {
        if (claims == null) {
            throw new AuthenticationException("Authentification requise.");
        }
        if (!"CLIENT".equalsIgnoreCase(claims.role())) {
            throw new ForbiddenException("Cette action est réservée aux clients.");
        }
        return claims;
    }
}
