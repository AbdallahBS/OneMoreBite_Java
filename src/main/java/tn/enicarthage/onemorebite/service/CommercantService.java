package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.CommercantDAO;
import tn.enicarthage.onemorebite.dao.StatutCompteUtilisateur;
import tn.enicarthage.onemorebite.dto.CommercantPublicResponse;
import tn.enicarthage.onemorebite.dto.CommercantRegisterRequest;
import tn.enicarthage.onemorebite.dto.CommercantResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Commercant;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.security.PasswordUtil;

import java.util.List;

public class CommercantService {

    private final CommercantDAO commercantDAO = new CommercantDAO();
    private final JwtUtil jwtUtil = new JwtUtil();

    public CommercantResponse inscrire(CommercantRegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("La requête d'inscription est requise.");
        }
        if (req.nom() == null || req.nom().isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        if (req.email() == null || req.email().isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire.");
        }
        if (req.motDePasse() == null || req.motDePasse().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }
        if (req.localisation() == null || req.localisation().isBlank()) {
            throw new IllegalArgumentException("La localisation est obligatoire.");
        }

        String email = req.email().trim();
        if (commercantDAO.emailExiste(email)) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }

        String motDePasseHache = PasswordUtil.hash(req.motDePasse());

        Commercant c = new Commercant();
        c.setNom(req.nom().trim());
        c.setEmail(email);
        c.setMotDePasse(motDePasseHache);
        c.setTelephone(req.telephone());
        c.setLocalisation(req.localisation().trim());
        c.setStatut("EN_ATTENTE"); 

        Commercant cree = commercantDAO.creer(c);
        return mapToResponse(cree);
    }

    public LoginResponse login(LoginRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()
                || req.motDePasse() == null || req.motDePasse().isBlank()) {
            throw new IllegalArgumentException("L'email et le mot de passe sont obligatoires.");
        }

        String email = req.email().trim();
        String password = req.motDePasse();

        Commercant c = commercantDAO.trouverParEmail(email)
                .orElseThrow(() -> new AuthenticationException("Identifiants invalides : Commerçant introuvable ou accès non autorisé."));

        if (!PasswordUtil.verifier(password, c.getMotDePasse())) {
            throw new AuthenticationException("Identifiants invalides : Mot de passe incorrect.");
        }

        if (StatutCompteUtilisateur.estDesactive(c.getStatutCompte())) {
            throw new AuthenticationException(
                    "Votre compte a été désactivé par l'administrateur. Veuillez le contacter.");
        }

        if ("EN_ATTENTE".equalsIgnoreCase(c.getStatut())) {
            throw new AuthenticationException("Votre compte est en attente de validation par l'administrateur.");
        }
        if ("REJETE".equalsIgnoreCase(c.getStatut())) {
            throw new AuthenticationException("Votre compte a été refusé par l'administrateur.");
        }
        if (!"ACTIF".equalsIgnoreCase(c.getStatut())) {
            throw new AuthenticationException("Compte commerçant inactif ou statut invalide : " + c.getStatut());
        }

        String token = jwtUtil.genererToken(c);

        return new LoginResponse(
                token,
                "Bearer",
                c.getId(),
                c.getNom(),
                c.getEmail(),
                "COMMERCANT"
        );
    }

    public List<CommercantResponse> lister(String statutFiltre) {
        List<Commercant> liste;
        if (statutFiltre != null && !statutFiltre.isBlank()) {
            liste = commercantDAO.listerParStatut(statutFiltre.trim().toUpperCase());
        } else {
            liste = commercantDAO.listerTous();
        }
        return liste.stream().map(this::mapToResponse).toList();
    }

    /**
     * Liste publique destinée au frontend : uniquement les commerçants dont le
     * compte et le statut métier sont ACTIF, avec des données non sensibles.
     */
    public List<CommercantPublicResponse> listerActifsPublics(String recherche) {
        return commercantDAO.listerActifsAvecRecherche(recherche)
                .stream()
                .map(this::mapToPublicResponse)
                .toList();
    }

    public CommercantResponse obtenir(int id) {
        Commercant c = commercantDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commerçant non trouvé avec l'id : " + id));
        return mapToResponse(c);
    }

    public CommercantResponse accepter(int id) {
        Commercant c = commercantDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commerçant non trouvé avec l'id : " + id));

        commercantDAO.mettreAJourStatut(id, "ACTIF");
        c.setStatut("ACTIF");
        return mapToResponse(c);
    }

    public CommercantResponse rejeter(int id) {
        Commercant c = commercantDAO.trouverParId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commerçant non trouvé avec l'id : " + id));

        commercantDAO.mettreAJourStatut(id, "REJETE");
        c.setStatut("REJETE");
        return mapToResponse(c);
    }

    public CommercantResponse mapToResponse(Commercant c) {
        return new CommercantResponse(
                c.getId(),
                c.getNom(),
                c.getEmail(),
                c.getTelephone(),
                c.getLocalisation(),
                c.getStatut()
        );
    }

    public CommercantPublicResponse mapToPublicResponse(Commercant c) {
        return new CommercantPublicResponse(
                c.getId(),
                c.getNom(),
                c.getLocalisation(),
                c.getTelephone()
        );
    }
}
