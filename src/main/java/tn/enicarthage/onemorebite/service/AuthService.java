package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.AdministrateurDAO;
import tn.enicarthage.onemorebite.dao.CommercantDAO;
import tn.enicarthage.onemorebite.dto.AdminResponse;
import tn.enicarthage.onemorebite.dto.CommercantResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.model.Administrateur;
import tn.enicarthage.onemorebite.model.Commercant;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.security.PasswordUtil;

import java.util.Optional;

public class AuthService {

    public static final String DEFAULT_ADMIN_EMAIL = "admin@enicar.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin";

    private final AdministrateurDAO administrateurDAO = new AdministrateurDAO();
    private final CommercantDAO commercantDAO = new CommercantDAO();
    private final JwtUtil jwtUtil = new JwtUtil();

    public AuthService() {
    }

    public void initialiserDefaultAdmin() {
        administrateurDAO.initialiserAdminParDefaut(DEFAULT_ADMIN_EMAIL, DEFAULT_ADMIN_PASSWORD);
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.motDePasse() == null || request.motDePasse().isBlank()) {
            throw new IllegalArgumentException("L'email et le mot de passe sont obligatoires.");
        }

        String email = request.email().trim();
        String password = request.motDePasse();

        // 1. Tenter la connexion Administrateur
        Optional<Administrateur> adminOpt = administrateurDAO.trouverParEmail(email);
        if (adminOpt.isPresent()) {
            Administrateur admin = adminOpt.get();
            if (!PasswordUtil.verifier(password, admin.getMotDePasse())) {
                throw new AuthenticationException("Identifiants invalides : Mot de passe incorrect.");
            }

            if (!PasswordUtil.estHashBCrypt(admin.getMotDePasse())) {
                administrateurDAO.mettreAJourMotDePasse(admin.getId(), PasswordUtil.hash(password));
            }

            String token = jwtUtil.genererToken(admin);
            return new LoginResponse(
                    token,
                    "Bearer",
                    admin.getId(),
                    admin.getNom(),
                    admin.getEmail(),
                    "ADMIN"
            );
        }

        Optional<Commercant> commercantOpt = commercantDAO.trouverParEmail(email);
        if (commercantOpt.isPresent()) {
            Commercant c = commercantOpt.get();
            if (!PasswordUtil.verifier(password, c.getMotDePasse())) {
                throw new AuthenticationException("Identifiants invalides : Mot de passe incorrect.");
            }

            // Contrôle du statut du compte commerçant
            if ("EN_ATTENTE".equalsIgnoreCase(c.getStatut())) {
                throw new AuthenticationException("Votre compte est en attente de validation par l'administrateur.");
            }
            if ("REJETE".equalsIgnoreCase(c.getStatut())) {
                throw new AuthenticationException("Votre compte a été refusé par l'administrateur.");
            }
            if (!"ACTIF".equalsIgnoreCase(c.getStatut())) {
                throw new AuthenticationException("Compte commerçant non actif (statut: " + c.getStatut() + ").");
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

        throw new AuthenticationException("Identifiants invalides : Utilisateur introuvable ou accès non autorisé.");
    }

    public Object validerSession(String authHeader) {
        String token = extraireToken(authHeader);
        JwtUtil.JwtClaims claims = jwtUtil.validerToken(token);

        if ("COMMERCANT".equalsIgnoreCase(claims.role())) {
            Commercant c = commercantDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new AuthenticationException("Session invalide : Commerçant non trouvé."));
            return new CommercantResponse(
                    c.getId(),
                    c.getNom(),
                    c.getEmail(),
                    c.getTelephone(),
                    c.getLocalisation(),
                    c.getStatut()
            );
        }

        Administrateur admin = administrateurDAO.trouverParId(claims.id())
                .orElseThrow(() -> new AuthenticationException("Session invalide : Administrateur non trouvé."));

        return new AdminResponse(
                admin.getId(),
                admin.getNom(),
                admin.getEmail(),
                admin.getTelephone(),
                "ADMIN"
        );
    }

    public void logout(String authHeader) {
        String token = extraireToken(authHeader);
        jwtUtil.validerToken(token);
        jwtUtil.revoquerToken(token);
    }

    public String extraireToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new AuthenticationException("En-tête Authorization manquant ou invalide.");
        }
        if (authHeader.startsWith("Bearer ") || authHeader.startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader.trim();
    }
}
