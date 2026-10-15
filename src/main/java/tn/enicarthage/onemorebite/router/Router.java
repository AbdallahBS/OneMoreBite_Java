package tn.enicarthage.onemorebite.router;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import tn.enicarthage.onemorebite.controller.ProduitController;
import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;

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

    public void dispatcher(HttpExchange exchange) throws IOException {
        String methode = exchange.getRequestMethod();
        String chemin = exchange.getRequestURI().getPath();
        String[] segments = chemin.split("/");

        try {
            Reponse reponse = router(methode, segments, exchange);
            envoyer(exchange, reponse.statut(), reponse.corps());
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
            default -> throw new ResourceNotFoundException("Route inconnue /" + segments[1]);
        };
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