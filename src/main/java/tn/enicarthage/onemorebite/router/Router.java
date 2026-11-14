package tn.enicarthage.onemorebite.router;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import tn.enicarthage.onemorebite.controller.AuthController;
import tn.enicarthage.onemorebite.controller.AvisController;
import tn.enicarthage.onemorebite.controller.ClientController;
import tn.enicarthage.onemorebite.controller.CommercantController;
import tn.enicarthage.onemorebite.controller.ProduitController;
import tn.enicarthage.onemorebite.controller.ReservationController;
import tn.enicarthage.onemorebite.dto.AvisRequest;
import tn.enicarthage.onemorebite.dto.AvisUpdateRequest;
import tn.enicarthage.onemorebite.dto.ClientRegisterRequest;
import tn.enicarthage.onemorebite.dto.CommercantRegisterRequest;
import tn.enicarthage.onemorebite.dto.ImageUploadRequest;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.ProduitDisponibiliteRequest;
import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.dto.ProduitStockRequest;
import tn.enicarthage.onemorebite.dto.ReservationRequest;
import tn.enicarthage.onemorebite.dto.ReservationUpdateRequest;
import tn.enicarthage.onemorebite.dto.UpdateProfilRequest;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.exception.ForbiddenException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.exception.ServiceUnavailableException;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.service.AuthService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class Router {

    private record Reponse(int statut, String corps) {}

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, type, ctx) -> json.isJsonNull() ? null : LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> json.isJsonNull() ? null : LocalDateTime.parse(json.getAsString()))
            .create();

    private final ProduitController produitController = new ProduitController();
    private final AuthController authController = new AuthController();
    private final ClientController clientController = new ClientController();
    private final CommercantController commercantController = new CommercantController();
    private final ReservationController reservationController = new ReservationController();
    private final AvisController avisController = new AvisController();
    private final AuthService authService = new AuthService();
    private final JwtUtil jwtUtil = new JwtUtil();

    public void dispatcher(HttpExchange exchange) throws IOException {
        // En-têtes CORS universels pour intégration web / frontend
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String methode = exchange.getRequestMethod();
        String chemin = exchange.getRequestURI().getPath();
        String[] segments = chemin.split("/");

        try {
            Reponse reponse = router(methode, segments, exchange);
            envoyer(exchange, reponse.statut(), reponse.corps());
        } catch (AuthenticationException e) {
            envoyer(exchange, 401, messageErreur(e.getMessage()));
        } catch (ForbiddenException e) {
            envoyer(exchange, 403, messageErreur(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            envoyer(exchange, 404, messageErreur(e.getMessage()));
        } catch (IllegalArgumentException e) {
            envoyer(exchange, 400, messageErreur(e.getMessage()));
        } catch (ServiceUnavailableException e) {
            envoyer(exchange, 503, messageErreur(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            envoyer(exchange, 500, messageErreur("Une erreur interne est survenue."));
        }
    }

    private Reponse router(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length < 2 || segments[1].isBlank()) {
            throw new ResourceNotFoundException("Route vide ou inconnue.");
        }

        return switch (segments[1]) {
            case "produits" -> routesProduit(methode, segments, exchange);
            case "auth" -> routesAuth(methode, segments, exchange);
            case "admin" -> routesAdmin(methode, segments, exchange);
            case "commercants" -> routesCommercantPublic(methode, segments, exchange);
            case "reservations" -> routesReservation(methode, segments, exchange);
            case "avis" -> routesAvis(methode, segments, exchange);
            default -> throw new ResourceNotFoundException("Route inconnue /" + segments[1]);
        };
    }

    private Reponse routesAuth(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 3) {
            String action = segments[2];
            return switch (action) {
                case "login" -> {
                    if (!"POST".equalsIgnoreCase(methode)) {
                        throw new IllegalArgumentException("Méthode non supportée: " + methode);
                    }
                    LoginRequest request = lireCorps(exchange, LoginRequest.class);
                    yield new Reponse(200, gson.toJson(authController.login(request)));
                }
                case "logout" -> {
                    if (!"POST".equalsIgnoreCase(methode)) {
                        throw new IllegalArgumentException("Méthode non supportée: " + methode);
                    }
                    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                    yield new Reponse(200, gson.toJson(authController.logout(authHeader)));
                }
                case "me" -> routesProfil(methode, exchange);
                case "session", "verifier" -> {
                    if (!"GET".equalsIgnoreCase(methode)) {
                        throw new IllegalArgumentException("Méthode non supportée: " + methode);
                    }
                    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                    yield new Reponse(200, gson.toJson(authController.me(authHeader)));
                }
                default -> throw new ResourceNotFoundException("Action inconnue: /" + segments[1] + "/" + action);
            };
        } else if (segments.length == 4 && "commercant".equalsIgnoreCase(segments[2])) {
            String subAction = segments[3];
            return switch (subAction) {
                case "register", "inscription" -> {
                    if (!"POST".equalsIgnoreCase(methode)) {
                        throw new IllegalArgumentException("Méthode non supportée: " + methode);
                    }
                    CommercantRegisterRequest req = lireCorps(exchange, CommercantRegisterRequest.class);
                    yield new Reponse(201, gson.toJson(commercantController.inscrire(req)));
                }
                case "login" -> {
                    if (!"POST".equalsIgnoreCase(methode)) {
                        throw new IllegalArgumentException("Méthode non supportée: " + methode);
                    }
                    LoginRequest req = lireCorps(exchange, LoginRequest.class);
                    yield new Reponse(200, gson.toJson(commercantController.login(req)));
                }
                default -> throw new ResourceNotFoundException("Action inconnue: /auth/commercant/" + subAction);
            };
        } else if (segments.length == 4 && "client".equalsIgnoreCase(segments[2])) {
            String subAction = segments[3];
            return switch (subAction) {
                case "register", "inscription" -> {
                    if (!"POST".equalsIgnoreCase(methode)) {
                        throw new IllegalArgumentException("Méthode non supportée: " + methode);
                    }
                    ClientRegisterRequest req = lireCorps(exchange, ClientRegisterRequest.class);
                    yield new Reponse(201, gson.toJson(clientController.inscrire(req)));
                }
                case "login" -> {
                    if (!"POST".equalsIgnoreCase(methode)) {
                        throw new IllegalArgumentException("Méthode non supportée: " + methode);
                    }
                    LoginRequest req = lireCorps(exchange, LoginRequest.class);
                    yield new Reponse(200, gson.toJson(clientController.login(req)));
                }
                default -> throw new ResourceNotFoundException("Action inconnue: /auth/client/" + subAction);
            };
        }
        throw new ResourceNotFoundException("Route auth inconnue.");
    }

    /**
     * GET /auth/me : consultation du profil.
     * PUT|PATCH /auth/me : mise à jour partielle du profil (tout rôle connecté).
     * DELETE /auth/me : suppression du compte et révocation immédiate du token.
     */
    private Reponse routesProfil(String methode, HttpExchange exchange) throws IOException {
        return switch (methode.toUpperCase()) {
            case "GET" -> {
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                yield new Reponse(200, gson.toJson(authController.me(authHeader)));
            }
            case "PUT", "PATCH" -> {
                JwtUtil.JwtClaims claims = exigerTokenUtilisateur(exchange);
                UpdateProfilRequest request = lireCorps(exchange, UpdateProfilRequest.class);
                yield new Reponse(200, gson.toJson(authController.modifierProfil(request, claims)));
            }
            case "DELETE" -> {
                JwtUtil.JwtClaims claims = exigerTokenUtilisateur(exchange);
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                authController.supprimerCompte(claims, authHeader);
                yield new Reponse(204, "");
            }
            default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
        };
    }

    private Reponse routesAdmin(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 3 && "login".equalsIgnoreCase(segments[2])) {
            if (!"POST".equalsIgnoreCase(methode)) {
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
            }
            LoginRequest req = lireCorps(exchange, LoginRequest.class);
            return new Reponse(200, gson.toJson(authController.login(req)));
        }

        if (segments.length == 3 && "me".equalsIgnoreCase(segments[2])) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            return new Reponse(200, gson.toJson(authController.me(authHeader)));
        }

        // Toutes les autres routes sous /admin nécessitent un token ADMIN valide
        JwtUtil.JwtClaims adminClaims = exigerTokenAdmin(exchange);

        if (segments.length >= 3 && "commercants".equalsIgnoreCase(segments[2])) {
            if (segments.length == 3) {
                // GET /admin/commercants (?statut=EN_ATTENTE)
                if (!"GET".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                String statut = extraireParametre(exchange.getRequestURI().getQuery(), "statut");
                return new Reponse(200, gson.toJson(commercantController.lister(statut)));
            } else if (segments.length == 4) {
                // GET /admin/commercants/{id}
                int id = parseId(segments[3]);
                if (!"GET".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                return new Reponse(200, gson.toJson(commercantController.obtenir(id)));
            } else if (segments.length == 5) {
                int id = parseId(segments[3]);
                String action = segments[4];
                return switch (action) {
                    case "accepter", "valider", "approuver" -> {
                        if (!"PUT".equalsIgnoreCase(methode) && !"POST".equalsIgnoreCase(methode)) {
                            throw new IllegalArgumentException("Méthode non supportée: " + methode);
                        }
                        yield new Reponse(200, gson.toJson(commercantController.accepter(id)));
                    }
                    case "rejeter", "refuser" -> {
                        if (!"PUT".equalsIgnoreCase(methode) && !"POST".equalsIgnoreCase(methode)) {
                            throw new IllegalArgumentException("Méthode non supportée: " + methode);
                        }
                        yield new Reponse(200, gson.toJson(commercantController.rejeter(id)));
                    }
                    default -> throw new ResourceNotFoundException("Action admin inconnue: " + action);
                };
            }
        }

        if (segments.length >= 3 && "produits".equalsIgnoreCase(segments[2])) {
            return routesAdminProduit(methode, segments, exchange, adminClaims);
        }

        throw new ResourceNotFoundException("Route admin inconnue.");
    }

    private Reponse routesAdminProduit(String methode, String[] segments, HttpExchange exchange,
                                       JwtUtil.JwtClaims adminClaims) throws IOException {
        if (segments.length == 3) {
            // GET /admin/produits?commercantId=2&disponible=true&recherche=panier
            if ("GET".equalsIgnoreCase(methode)) {
                return new Reponse(200, gson.toJson(
                        produitController.lister(
                                parseEntierOptionnel(extraireParametre(exchange.getRequestURI().getQuery(), "commercantId")),
                                parseBooleenOptionnel(extraireParametre(exchange.getRequestURI().getQuery(), "disponible")),
                                extraireParametre(exchange.getRequestURI().getQuery(), "recherche")
                        )
                ));
            }
            // POST /admin/produits : création pour le compte d'un commerçant (commercantId obligatoire)
            if ("POST".equalsIgnoreCase(methode)) {
                ProduitRequest request = lireCorps(exchange, ProduitRequest.class);
                return new Reponse(201, gson.toJson(produitController.creer(request, adminClaims)));
            }
            throw new IllegalArgumentException("Méthode non supportée: " + methode);
        }

        if (segments.length == 4) {
            String action = segments[3];

            if ("statistiques".equalsIgnoreCase(action) || "stats".equalsIgnoreCase(action)) {
                if (!"GET".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                return new Reponse(200, gson.toJson(produitController.statistiques()));
            }

            int id = parseId(action);
            return switch (methode.toUpperCase()) {
                case "GET" -> new Reponse(200, gson.toJson(produitController.obtenir(id)));
                case "PUT", "PATCH" -> {
                    ProduitRequest request = lireCorps(exchange, ProduitRequest.class);
                    yield new Reponse(200, gson.toJson(produitController.modifier(id, request, adminClaims)));
                }
                case "DELETE" -> {
                    produitController.supprimer(id, adminClaims);
                    yield new Reponse(204, "");
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        }

        if (segments.length == 5) {
            int id = parseId(segments[3]);
            String action = segments[4];
            if (!"PUT".equalsIgnoreCase(methode) && !"PATCH".equalsIgnoreCase(methode)) {
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
            }
            return switch (action) {
                case "disponibilite" -> {
                    ProduitDisponibiliteRequest request = lireCorps(exchange, ProduitDisponibiliteRequest.class);
                    yield new Reponse(200, gson.toJson(
                            produitController.changerDisponibilite(id, request.disponibilite(), adminClaims)));
                }
                case "stock" -> {
                    ProduitStockRequest request = lireCorps(exchange, ProduitStockRequest.class);
                    yield new Reponse(200, gson.toJson(
                            produitController.changerStock(id, request.quantite(), adminClaims)));
                }
                default -> throw new ResourceNotFoundException("Action admin produit inconnue: " + action);
            };
        }

        throw new ResourceNotFoundException("Route admin produit inconnue.");
    }

    private Reponse routesCommercantPublic(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2) {
            return switch (methode) {
                case "GET" -> new Reponse(200, gson.toJson(commercantController.lister("ACTIF")));
                case "POST" -> {
                    CommercantRegisterRequest req = lireCorps(exchange, CommercantRegisterRequest.class);
                    yield new Reponse(201, gson.toJson(commercantController.inscrire(req)));
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        } else if (segments.length == 3) {
            if ("register".equalsIgnoreCase(segments[2])) {
                if (!"POST".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                CommercantRegisterRequest req = lireCorps(exchange, CommercantRegisterRequest.class);
                return new Reponse(201, gson.toJson(commercantController.inscrire(req)));
            } else {
                int id = parseId(segments[2]);
                if (!"GET".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                return new Reponse(200, gson.toJson(commercantController.obtenir(id)));
            }
        } else if (segments.length == 4 && "avis".equalsIgnoreCase(segments[3])) {
            // GET /commercants/{id}/avis : avis publics d'un commerçant + note moyenne
            int id = parseId(segments[2]);
            if (!"GET".equalsIgnoreCase(methode)) {
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
            }
            return new Reponse(200, gson.toJson(avisController.listerParCommercant(id)));
        }
        throw new ResourceNotFoundException("Route inconnue.");
    }

    private Reponse routesReservation(String methode, String[] segments, HttpExchange exchange) throws IOException {
        // POST /reservations : création par un client authentifié
        if (segments.length == 2) {
            if (!"POST".equalsIgnoreCase(methode)) {
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
            }
            JwtUtil.JwtClaims claims = exigerTokenClient(exchange);
            ReservationRequest request = lireCorps(exchange, ReservationRequest.class);
            return new Reponse(201, gson.toJson(reservationController.creer(request, claims)));
        }

        if (segments.length == 3) {
            String action = segments[2];

            // GET /reservations/mes-reservations : réservations du client connecté
            if ("mes-reservations".equalsIgnoreCase(action) || "mes".equalsIgnoreCase(action)) {
                if (!"GET".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                JwtUtil.JwtClaims claims = exigerTokenClient(exchange);
                return new Reponse(200, gson.toJson(reservationController.listerMesReservations(claims)));
            }

            // GET /reservations/recues : réservations reçues par le commerçant connecté
            if ("recues".equalsIgnoreCase(action)) {
                if (!"GET".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                JwtUtil.JwtClaims claims = exigerTokenCommercantReservation(exchange);
                return new Reponse(200, gson.toJson(reservationController.listerRecues(claims)));
            }

            int id = parseId(action);

            // GET /reservations/{id} : détail accessible au client propriétaire ou au commerçant du produit
            if ("GET".equalsIgnoreCase(methode)) {
                JwtUtil.JwtClaims claims = exigerTokenUtilisateur(exchange);
                return new Reponse(200, gson.toJson(reservationController.obtenir(id, claims)));
            }

            // PUT|PATCH /reservations/{id} : modification par le client propriétaire
            if ("PUT".equalsIgnoreCase(methode) || "PATCH".equalsIgnoreCase(methode)) {
                JwtUtil.JwtClaims claims = exigerTokenClient(exchange);
                ReservationUpdateRequest request = lireCorps(exchange, ReservationUpdateRequest.class);
                return new Reponse(200, gson.toJson(reservationController.modifier(id, request, claims)));
            }

            throw new IllegalArgumentException("Méthode non supportée: " + methode);
        }

        if (segments.length == 4) {
            int id = parseId(segments[2]);
            String action = segments[3];

            if (!"PUT".equalsIgnoreCase(methode) && !"PATCH".equalsIgnoreCase(methode)) {
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
            }

            return switch (action) {
                case "annuler" -> {
                    JwtUtil.JwtClaims claims = exigerTokenClient(exchange);
                    yield new Reponse(200, gson.toJson(reservationController.annuler(id, claims)));
                }
                case "confirmer" -> {
                    JwtUtil.JwtClaims claims = exigerTokenCommercantReservation(exchange);
                    yield new Reponse(200, gson.toJson(reservationController.confirmer(id, claims)));
                }
                case "refuser" -> {
                    JwtUtil.JwtClaims claims = exigerTokenCommercantReservation(exchange);
                    yield new Reponse(200, gson.toJson(reservationController.refuser(id, claims)));
                }
                default -> throw new ResourceNotFoundException("Action réservation inconnue: " + action);
            };
        }

        throw new ResourceNotFoundException("Route réservation inconnue.");
    }

    private Reponse routesAvis(String methode, String[] segments, HttpExchange exchange) throws IOException {
        // GET /avis?commercantId={id} : liste publique, éventuellement filtrée
        // POST /avis : création par un client authentifié (client déduit du token)
        if (segments.length == 2) {
            return switch (methode.toUpperCase()) {
                case "GET" -> {
                    Integer commercantId = parseEntierOptionnel(
                            extraireParametre(exchange.getRequestURI().getQuery(), "commercantId"));
                    yield new Reponse(200, gson.toJson(avisController.lister(commercantId)));
                }
                case "POST" -> {
                    JwtUtil.JwtClaims claims = exigerTokenClient(exchange);
                    AvisRequest request = lireCorps(exchange, AvisRequest.class);
                    yield new Reponse(201, gson.toJson(avisController.creer(request, claims)));
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        }

        // PUT /avis/{id} : client propriétaire ; DELETE /avis/{id} : client propriétaire ou admin
        if (segments.length == 3) {
            int id = parseId(segments[2]);
            return switch (methode.toUpperCase()) {
                case "PUT" -> {
                    JwtUtil.JwtClaims claims = exigerTokenClient(exchange);
                    AvisUpdateRequest request = lireCorps(exchange, AvisUpdateRequest.class);
                    yield new Reponse(200, gson.toJson(avisController.modifier(id, request, claims)));
                }
                case "DELETE" -> {
                    JwtUtil.JwtClaims claims = exigerTokenUtilisateur(exchange);
                    avisController.supprimer(id, claims);
                    yield new Reponse(204, "");
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        }

        throw new ResourceNotFoundException("Route avis inconnue.");
    }

    private Reponse routesProduit(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2) {
            return switch (methode.toUpperCase()) {
                case "GET" -> new Reponse(200, gson.toJson(
                        produitController.lister(
                                parseEntierOptionnel(extraireParametre(exchange.getRequestURI().getQuery(), "commercantId")),
                                parseBooleenOptionnel(extraireParametre(exchange.getRequestURI().getQuery(), "disponible")),
                                extraireParametre(exchange.getRequestURI().getQuery(), "recherche")
                        )
                ));
                case "POST" -> {
                    JwtUtil.JwtClaims claims = exigerTokenGestion(exchange);
                    ProduitRequest request = lireCorps(exchange, ProduitRequest.class);
                    yield new Reponse(201, gson.toJson(produitController.creer(request, claims)));
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        }

        if (segments.length == 3) {
            String action = segments[2];

            if ("mes-produits".equalsIgnoreCase(action) || "mes".equalsIgnoreCase(action)) {
                if (!"GET".equalsIgnoreCase(methode)) {
                    throw new IllegalArgumentException("Méthode non supportée: " + methode);
                }
                JwtUtil.JwtClaims claims = exigerTokenCommercant(exchange);
                return new Reponse(200, gson.toJson(produitController.listerMesProduits(claims)));
            }

            if ("images".equalsIgnoreCase(action)) {
                if ("POST".equalsIgnoreCase(methode)) {
                    exigerTokenGestion(exchange);
                    ImageUploadRequest request = lireCorps(exchange, ImageUploadRequest.class);
                    return new Reponse(201, gson.toJson(produitController.televerserImage(request)));
                }
                if ("DELETE".equalsIgnoreCase(methode)) {
                    exigerTokenGestion(exchange);
                    String publicId = extraireParametre(exchange.getRequestURI().getQuery(), "publicId");
                    if (publicId == null || publicId.isBlank()) {
                        throw new IllegalArgumentException("Le paramètre 'publicId' est obligatoire.");
                    }
                    return new Reponse(200, gson.toJson(produitController.supprimerImage(publicId)));
                }
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
            }

            int id = parseId(action);
            return switch (methode.toUpperCase()) {
                case "GET" -> new Reponse(200, gson.toJson(produitController.obtenir(id)));
                case "PUT", "PATCH" -> {
                    JwtUtil.JwtClaims claims = exigerTokenGestion(exchange);
                    ProduitRequest request = lireCorps(exchange, ProduitRequest.class);
                    yield new Reponse(200, gson.toJson(produitController.modifier(id, request, claims)));
                }
                case "DELETE" -> {
                    JwtUtil.JwtClaims claims = exigerTokenGestion(exchange);
                    produitController.supprimer(id, claims);
                    yield new Reponse(204, "");
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        }

        if (segments.length == 4) {
            int id = parseId(segments[2]);
            String action = segments[3];
            if (!"PUT".equalsIgnoreCase(methode) && !"PATCH".equalsIgnoreCase(methode)) {
                throw new IllegalArgumentException("Méthode non supportée: " + methode);
            }

            JwtUtil.JwtClaims claims = exigerTokenGestion(exchange);
            return switch (action) {
                case "disponibilite" -> {
                    ProduitDisponibiliteRequest request = lireCorps(exchange, ProduitDisponibiliteRequest.class);
                    yield new Reponse(200, gson.toJson(
                            produitController.changerDisponibilite(id, request.disponibilite(), claims)));
                }
                case "stock" -> {
                    ProduitStockRequest request = lireCorps(exchange, ProduitStockRequest.class);
                    yield new Reponse(200, gson.toJson(
                            produitController.changerStock(id, request.quantite(), claims)));
                }
                default -> throw new ResourceNotFoundException("Action produit inconnue: " + action);
            };
        }

        throw new ResourceNotFoundException("Route inconnue.");
    }

    private JwtUtil.JwtClaims exigerTokenAdmin(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = authService.extraireToken(authHeader);
        return jwtUtil.validerTokenAdmin(token);
    }

    private JwtUtil.JwtClaims exigerTokenGestion(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = authService.extraireToken(authHeader);
        return jwtUtil.validerTokenCommercant(token);
    }

    private JwtUtil.JwtClaims exigerTokenCommercant(HttpExchange exchange) {
        JwtUtil.JwtClaims claims = exigerTokenGestion(exchange);
        if (!"COMMERCANT".equalsIgnoreCase(claims.role())) {
            throw new ForbiddenException("Cette action est réservée aux commerçants.");
        }
        return claims;
    }

    private JwtUtil.JwtClaims exigerTokenClient(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = authService.extraireToken(authHeader);
        return jwtUtil.validerTokenClient(token);
    }

    private JwtUtil.JwtClaims exigerTokenCommercantReservation(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = authService.extraireToken(authHeader);
        JwtUtil.JwtClaims claims = jwtUtil.validerToken(token);
        if (!"COMMERCANT".equalsIgnoreCase(claims.role())) {
            throw new ForbiddenException("Cette action est réservée aux commerçants.");
        }
        return claims;
    }

    private JwtUtil.JwtClaims exigerTokenUtilisateur(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = authService.extraireToken(authHeader);
        return jwtUtil.validerToken(token);
    }

    private String extraireParametre(String query, String paramName) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && decoder(parts[0]).equalsIgnoreCase(paramName)) {
                return decoder(parts[1]);
            }
        }
        return null;
    }

    private String decoder(String valeur) {
        try {
            return URLDecoder.decode(valeur, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return valeur;
        }
    }

    private Integer parseEntierOptionnel(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valeur.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Paramètre entier invalide: " + valeur);
        }
    }

    private Boolean parseBooleenOptionnel(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return switch (valeur.trim().toLowerCase()) {
            case "true", "1", "oui", "yes" -> true;
            case "false", "0", "non", "no" -> false;
            default -> throw new IllegalArgumentException("Paramètre booléen invalide: " + valeur);
        };
    }

    private <T> T lireCorps(HttpExchange exchange, Class<T> clazz) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return gson.fromJson(body, clazz);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Corps de requête JSON invalide.");
        }
    }

    private int parseId(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Identifiant invalide: " + val);
        }
    }

    private String messageErreur(String msg) {
        return gson.toJson(Map.of("erreur", msg));
    }

    private void envoyer(HttpExchange exchange, int statut, String corps) throws IOException {
        byte[] bytes = (corps == null) ? new byte[0] : corps.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statut, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(statut, -1);
            exchange.close();
        }
    }
}
