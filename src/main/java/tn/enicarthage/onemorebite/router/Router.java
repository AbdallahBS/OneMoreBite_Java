package tn.enicarthage.onemorebite.router;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import tn.enicarthage.onemorebite.controller.AuthController;
import tn.enicarthage.onemorebite.controller.CommercantController;
import tn.enicarthage.onemorebite.controller.ProduitController;
import tn.enicarthage.onemorebite.dto.CommercantRegisterRequest;
import tn.enicarthage.onemorebite.dto.LoginRequest;
import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.exception.AuthenticationException;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;
import tn.enicarthage.onemorebite.security.JwtUtil;
import tn.enicarthage.onemorebite.service.AuthService;

import java.io.IOException;
import java.io.OutputStream;
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
    private final CommercantController commercantController = new CommercantController();
    private final AuthService authService = new AuthService();
    private final JwtUtil jwtUtil = new JwtUtil();

    public void dispatcher(HttpExchange exchange) throws IOException {
        // En-têtes CORS universels pour intégration web / frontend
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
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
        } catch (ResourceNotFoundException e) {
            envoyer(exchange, 404, messageErreur(e.getMessage()));
        } catch (IllegalArgumentException e) {
            envoyer(exchange, 400, messageErreur(e.getMessage()));
        } catch (Exception e) {
            envoyer(exchange, 500, messageErreur(e.getMessage()));
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
                case "me", "session", "verifier" -> {
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
        }
        throw new ResourceNotFoundException("Route auth inconnue.");
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
        exigerTokenAdmin(exchange);

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

        throw new ResourceNotFoundException("Route admin inconnue.");
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
        }
        throw new ResourceNotFoundException("Route inconnue.");
    }

    private Reponse routesProduit(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2) {
            return switch (methode) {
                case "GET" -> new Reponse(200, gson.toJson(produitController.lister()));
                case "POST" -> {
                    ProduitRequest request = lireCorps(exchange, ProduitRequest.class);
                    yield new Reponse(201, gson.toJson(produitController.creer(request)));
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        } else if (segments.length == 3) {
            int id = parseId(segments[2]);
            return switch (methode) {
                case "GET" -> new Reponse(200, gson.toJson(produitController.obtenir(id)));
                case "PUT" -> {
                    ProduitRequest request = lireCorps(exchange, ProduitRequest.class);
                    yield new Reponse(200, gson.toJson(produitController.modifier(id, request)));
                }
                case "DELETE" -> {
                    produitController.supprimer(id);
                    yield new Reponse(204, "");
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        }
        throw new ResourceNotFoundException("Route inconnue.");
    }

    private void exigerTokenAdmin(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = authService.extraireToken(authHeader);
        jwtUtil.validerTokenAdmin(token);
    }

    private String extraireParametre(String query, String paramName) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2 && parts[0].equalsIgnoreCase(paramName)) {
                return parts[1];
            }
        }
        return null;
    }

    private <T> T lireCorps(HttpExchange exchange, Class<T> clazz) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return gson.fromJson(body, clazz);
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