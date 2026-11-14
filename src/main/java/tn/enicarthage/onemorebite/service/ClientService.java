package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.ClientDAO;
import tn.enicarthage.onemorebite.dao.StatutCompteUtilisateur;
import tn.enicarthage.onemorebite.dto.ClientRegisterRequest;
import tn.enicarthage.onemorebite.dto.ClientResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.model.Client;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.security.PasswordUtil;

import java.util.Locale;
import java.util.regex.Pattern;

public class ClientService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ClientDAO clientDAO = new ClientDAO();
    private final JwtUtil jwtUtil = new JwtUtil();

    public ClientResponse inscrire(ClientRegisterRequest req) {
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

        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Le format de l'email est invalide.");
        }

        if (clientDAO.emailExiste(email)) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }

        Client client = new Client();
        client.setNom(req.nom().trim());
        client.setEmail(email);
        client.setMotDePasse(PasswordUtil.hash(req.motDePasse()));
        client.setTelephone(req.telephone() == null ? null : req.telephone().trim());

        Client cree = clientDAO.creer(client);
        return mapToResponse(cree);
    }

    public LoginResponse login(LoginRequest req) {
        if (req == null || req.email() == null || req.email().isBlank()
                || req.motDePasse() == null || req.motDePasse().isBlank()) {
            throw new IllegalArgumentException("L'email et le mot de passe sont obligatoires.");
        }

        String email = req.email().trim();
        String password = req.motDePasse();

        Client client = clientDAO.trouverParEmail(email)
                .orElseThrow(() -> new AuthenticationException(
                        "Identifiants invalides : Client introuvable ou accès non autorisé."));

        if (!PasswordUtil.verifier(password, client.getMotDePasse())) {
            throw new AuthenticationException("Identifiants invalides : Mot de passe incorrect.");
        }

        if (StatutCompteUtilisateur.estDesactive(client.getStatutCompte())) {
            throw new AuthenticationException(
                    "Votre compte a été désactivé par l'administrateur. Veuillez le contacter.");
        }

        // Migration transparente des anciens mots de passe stockés en clair.
        if (!PasswordUtil.estHashBCrypt(client.getMotDePasse())) {
            clientDAO.mettreAJourMotDePasse(client.getId(), PasswordUtil.hash(password));
        }

        String token = jwtUtil.genererToken(client);

        return new LoginResponse(
                token,
                "Bearer",
                client.getId(),
                client.getNom(),
                client.getEmail(),
                "CLIENT"
        );
    }

    public ClientResponse mapToResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getNom(),
                client.getEmail(),
                client.getTelephone(),
                "CLIENT"
        );
    }
}
