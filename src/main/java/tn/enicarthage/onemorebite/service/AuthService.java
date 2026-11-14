package tn.enicarthage.onemorebite.service;

import tn.enicarthage.onemorebite.dao.AdministrateurDAO;
import tn.enicarthage.onemorebite.dao.ClientDAO;
import tn.enicarthage.onemorebite.dao.CommercantDAO;
import tn.enicarthage.onemorebite.dto.AdminResponse;
import tn.enicarthage.onemorebite.dto.ClientResponse;
import tn.enicarthage.onemorebite.dto.CommercantResponse;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.LoginResponse;
import tn.enicarthage.onemorebite.dto.UpdateProfilRequest;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.exception.ForbiddenException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.model.Administrateur;
import tn.enicarthage.onemorebite.model.Client;
import tn.enicarthage.onemorebite.model.Commercant;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.security.PasswordUtil;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {

    public static final String DEFAULT_ADMIN_EMAIL = "admin@enicar.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final AdministrateurDAO administrateurDAO = new AdministrateurDAO();
    private final CommercantDAO commercantDAO = new CommercantDAO();
    private final ClientDAO clientDAO = new ClientDAO();
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

        Optional<Client> clientOpt = clientDAO.trouverParEmail(email);
        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            if (!PasswordUtil.verifier(password, client.getMotDePasse())) {
                throw new AuthenticationException("Identifiants invalides : Mot de passe incorrect.");
            }

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

        if ("CLIENT".equalsIgnoreCase(claims.role())) {
            Client client = clientDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new AuthenticationException("Session invalide : Client non trouvé."));
            return new ClientResponse(
                    client.getId(),
                    client.getNom(),
                    client.getEmail(),
                    client.getTelephone(),
                    "CLIENT"
            );
        }

        if (!"ADMIN".equalsIgnoreCase(claims.role())) {
            throw new AuthenticationException("Session invalide : rôle inconnu.");
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

    /**
     * Mise à jour partielle du profil de l'utilisateur connecté. Seuls les champs
     * non-null de la requête sont appliqués ; le rôle reste celui du token JWT.
     */
    public Object modifierProfil(UpdateProfilRequest req, JwtUtil.JwtClaims claims) {
        if (req == null) {
            throw new IllegalArgumentException("La requête de mise à jour du profil est obligatoire.");
        }

        String role = normaliserRole(claims);

        if ("CLIENT".equals(role)) {
            Client client = clientDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new AuthenticationException("Session invalide : Client non trouvé."));

            String nom = resoudreNom(req, client.getNom());
            String email = resoudreEmail(req, client.getEmail(), claims.id());
            String telephone = resoudreTelephone(req, client.getTelephone());
            String motDePasseHache = resoudreMotDePasse(req, client.getMotDePasse());

            if (!clientDAO.mettreAJourProfil(client.getId(), nom, email, telephone, motDePasseHache)) {
                throw new ResourceNotFoundException("Compte client non trouvé.");
            }
            client.setNom(nom);
            client.setEmail(email);
            client.setTelephone(telephone);

            return new ClientResponse(client.getId(), client.getNom(), client.getEmail(),
                    client.getTelephone(), "CLIENT");
        }

        if ("COMMERCANT".equals(role)) {
            Commercant commercant = commercantDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new AuthenticationException("Session invalide : Commerçant non trouvé."));

            String nom = resoudreNom(req, commercant.getNom());
            String email = resoudreEmail(req, commercant.getEmail(), claims.id());
            String telephone = resoudreTelephone(req, commercant.getTelephone());
            String motDePasseHache = resoudreMotDePasse(req, commercant.getMotDePasse());
            String localisation = commercant.getLocalisation();

            // La localisation n'est prise en compte que pour un COMMERCANT.
            if (req.localisation() != null) {
                if (req.localisation().isBlank()) {
                    throw new IllegalArgumentException("La localisation ne peut pas être vide.");
                }
                localisation = req.localisation().trim();
            }

            if (!commercantDAO.mettreAJourProfil(commercant.getId(), nom, email, telephone,
                    motDePasseHache, localisation)) {
                throw new ResourceNotFoundException("Compte commerçant non trouvé.");
            }
            commercant.setNom(nom);
            commercant.setEmail(email);
            commercant.setTelephone(telephone);
            commercant.setLocalisation(localisation);

            return new CommercantResponse(commercant.getId(), commercant.getNom(), commercant.getEmail(),
                    commercant.getTelephone(), commercant.getLocalisation(), commercant.getStatut());
        }

        if ("ADMIN".equals(role)) {
            Administrateur admin = administrateurDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new AuthenticationException("Session invalide : Administrateur non trouvé."));

            String nom = resoudreNom(req, admin.getNom());
            String email = resoudreEmail(req, admin.getEmail(), claims.id());
            String telephone = resoudreTelephone(req, admin.getTelephone());
            String motDePasseHache = resoudreMotDePasse(req, admin.getMotDePasse());

            if (!administrateurDAO.mettreAJourProfil(admin.getId(), nom, email, telephone, motDePasseHache)) {
                throw new ResourceNotFoundException("Compte administrateur non trouvé.");
            }
            admin.setNom(nom);
            admin.setEmail(email);
            admin.setTelephone(telephone);

            return new AdminResponse(admin.getId(), admin.getNom(), admin.getEmail(),
                    admin.getTelephone(), "ADMIN");
        }

        throw new AuthenticationException("Session invalide : rôle inconnu.");
    }

    /**
     * Supprime le compte connecté. Le token JWT courant est révoqué immédiatement
     * après la suppression, comme pour POST /auth/logout.
     */
    public void supprimerCompte(JwtUtil.JwtClaims claims, String authHeader) {
        String role = normaliserRole(claims);

        if ("CLIENT".equals(role)) {
            clientDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Compte client non trouvé."));
            if (!clientDAO.supprimer(claims.id())) {
                throw new ResourceNotFoundException("Compte client non trouvé.");
            }
        } else if ("COMMERCANT".equals(role)) {
            commercantDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Compte commerçant non trouvé."));
            if (!commercantDAO.supprimer(claims.id())) {
                throw new ResourceNotFoundException("Compte commerçant non trouvé.");
            }
        } else if ("ADMIN".equals(role)) {
            Administrateur admin = administrateurDAO.trouverParId(claims.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Compte administrateur non trouvé."));

            boolean adminPrincipal = admin.getEmail() != null
                    && admin.getEmail().equalsIgnoreCase(DEFAULT_ADMIN_EMAIL);
            if (adminPrincipal || administrateurDAO.compter() <= 1) {
                throw new ForbiddenException(
                        "Le compte administrateur principal ne peut pas être supprimé : la plateforme doit conserver au moins un administrateur.");
            }

            if (!administrateurDAO.supprimer(admin.getId())) {
                throw new ResourceNotFoundException("Compte administrateur non trouvé.");
            }
        } else {
            throw new AuthenticationException("Session invalide : rôle inconnu.");
        }

        jwtUtil.revoquerToken(extraireToken(authHeader));
    }

    private String normaliserRole(JwtUtil.JwtClaims claims) {
        if (claims == null || claims.role() == null || claims.role().isBlank()) {
            throw new AuthenticationException("Session invalide : rôle manquant.");
        }
        return claims.role().trim().toUpperCase(Locale.ROOT);
    }

    private String resoudreNom(UpdateProfilRequest req, String nomActuel) {
        if (req.nom() == null) {
            return nomActuel;
        }
        if (req.nom().isBlank()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide.");
        }
        return req.nom().trim();
    }

    private String resoudreEmail(UpdateProfilRequest req, String emailActuel, int utilisateurId) {
        if (req.email() == null) {
            return emailActuel;
        }
        if (req.email().isBlank()) {
            throw new IllegalArgumentException("L'email ne peut pas être vide.");
        }

        String email = req.email().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Le format de l'email est invalide.");
        }

        boolean dejaUtilise = administrateurDAO.trouverParEmail(email)
                .filter(u -> u.getId() != utilisateurId).isPresent()
                || commercantDAO.trouverParEmail(email)
                .filter(u -> u.getId() != utilisateurId).isPresent()
                || clientDAO.trouverParEmail(email)
                .filter(u -> u.getId() != utilisateurId).isPresent();

        if (dejaUtilise) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà.");
        }
        return email;
    }

    private String resoudreTelephone(UpdateProfilRequest req, String telephoneActuel) {
        if (req.telephone() == null) {
            return telephoneActuel;
        }
        String telephone = req.telephone().trim();
        return telephone.isEmpty() ? null : telephone;
    }

    private String resoudreMotDePasse(UpdateProfilRequest req, String motDePasseActuelHache) {
        if (req.motDePasse() == null) {
            return motDePasseActuelHache;
        }
        if (req.motDePasse().isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe ne peut pas être vide.");
        }
        if (req.ancienMotDePasse() != null
                && !PasswordUtil.verifier(req.ancienMotDePasse(), motDePasseActuelHache)) {
            throw new IllegalArgumentException("L'ancien mot de passe est incorrect.");
        }
        return PasswordUtil.hash(req.motDePasse());
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
