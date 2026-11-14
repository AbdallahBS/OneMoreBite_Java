package tn.enicarthage.onemorebite.controller;

import tn.enicarthage.onemorebite.dto.AdminResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.dto.UpdateProfilRequest;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.service.AuthService;

import java.util.Map;

public class AuthController {

    private final AuthService authService = new AuthService();

    public LoginResponse login(LoginRequest request) {
        return authService.login(request);
    }

    public Object me(String authHeader) {
        return authService.validerSession(authHeader);
    }

    public Object modifierProfil(UpdateProfilRequest request, JwtUtil.JwtClaims claims) {
        return authService.modifierProfil(request, claims);
    }

    public void supprimerCompte(JwtUtil.JwtClaims claims, String authHeader) {
        authService.supprimerCompte(claims, authHeader);
    }

    public Map<String, String> logout(String authHeader) {
        authService.logout(authHeader);
        return Map.of("message", "Déconnexion réussie. La session a été invalidée.");
    }

    public void initialiserDefaultAdmin() {
        authService.initialiserDefaultAdmin();
    }
}
