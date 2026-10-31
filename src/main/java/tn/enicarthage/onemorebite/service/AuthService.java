package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.AdministrateurDAO;
import tn.enicarthage.onemorebite.dto.AdminResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.model.Administrateur;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.security.PasswordUtil;

public class AuthService {

    public static final String DEFAULT_ADMIN_EMAIL = "admin@enicar.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin";

    private final AdministrateurDAO administrateurDAO = new AdministrateurDAO();
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

        Administrateur admin = administrateurDAO.trouverParEmail(email)
                .orElseThrow(() -> new AuthenticationException("Identifiants invalides : Administrateur introuvable ou accès non autorisé."));

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

    public AdminResponse validerSession(String authHeader) {
        String token = extraireToken(authHeader);
        JwtUtil.JwtClaims claims = jwtUtil.validerToken(token);

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
