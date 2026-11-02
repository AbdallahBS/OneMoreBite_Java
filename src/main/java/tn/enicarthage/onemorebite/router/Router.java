package tn.enicarthage.onemorebite.router;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import tn.enicarthage.onemorebite.controller.AvisController;
import tn.enicarthage.onemorebite.controller.ProduitController;
import tn.enicarthage.onemorebite.dto.AvisRequest;
import tn.enicarthage.onemorebite.dto.ProduitRequest;
import tn.enicarthage.onemorebite.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Router {

    private record Reponse(int statut, String corps, String contentType) {
        public Reponse(int statut, String corps) {
            this(statut, corps, "application/json; charset=utf-8");
        }
    }

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
    private final AvisController avisController = new AvisController();

    public void dispatcher(HttpExchange exchange) throws IOException {
        String methode = exchange.getRequestMethod();

        // Support pré-vol CORS pour requêtes navigateur / front-end
        if ("OPTIONS".equalsIgnoreCase(methode)) {
            ajouterEntetesCors(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String chemin = exchange.getRequestURI().getPath();
        String[] segments = chemin.split("/");

        try {
            Reponse reponse = router(methode, segments, exchange);
            envoyer(exchange, reponse.statut(), reponse.corps(), reponse.contentType());
        } catch (ResourceNotFoundException e) {
            envoyer(exchange, 404, messageErreur(e.getMessage()), "application/json; charset=utf-8");
        } catch (IllegalArgumentException e) {
            envoyer(exchange, 400, messageErreur(e.getMessage()), "application/json; charset=utf-8");
        } catch (Exception e) {
            envoyer(exchange, 500, messageErreur(e.getMessage()), "application/json; charset=utf-8");
        }
    }

    private Reponse router(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length < 2 || segments[1].isBlank()) {
            return routesAccueil(methode);
        }

        return switch (segments[1]) {
            case "produits" -> routesProduit(methode, segments, exchange);
            case "avis" -> routesAvis(methode, segments, exchange);
            case "page-avis" -> routesPageAvis(methode);
            default -> throw new ResourceNotFoundException("Route inconnue /" + segments[1]);
        };
    }

    private Reponse routesAccueil(String methode) {
        if ("GET".equalsIgnoreCase(methode)) {
            Map<String, Object> welcome = Map.of(
                    "service", "OneMoreBite API",
                    "endpoints", Map.of(
                            "produits", "/produits",
                            "avis", "/avis",
                            "avis_page_ui", "/page-avis",
                            "produit_avis", "/produits/{id}/avis"
                    )
            );
            return new Reponse(200, gson.toJson(welcome));
        }
        throw new ResourceNotFoundException("Route vide ou inconnue.");
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
        } else if (segments.length == 4 && "avis".equalsIgnoreCase(segments[3])) {
            int produitId = parseId(segments[2]);
            return switch (methode) {
                case "GET" -> new Reponse(200, gson.toJson(avisController.listerParProduit(produitId)));
                case "POST" -> {
                    AvisRequest request = lireCorps(exchange, AvisRequest.class);
                    yield new Reponse(201, gson.toJson(avisController.creerPourProduit(produitId, request)));
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        } else if (segments.length == 5 && "avis".equalsIgnoreCase(segments[3]) && "stats".equalsIgnoreCase(segments[4])) {
            int produitId = parseId(segments[2]);
            if ("GET".equalsIgnoreCase(methode)) {
                return new Reponse(200, gson.toJson(avisController.obtenirStatistiquesProduit(produitId)));
            }
            throw new IllegalArgumentException("Méthode non supportée: " + methode);
        }
        throw new ResourceNotFoundException("Route inconnue.");
    }

    private Reponse routesAvis(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2) {
            return switch (methode) {
                case "GET" -> {
                    Map<String, String> queryParams = extraireQueryParams(exchange.getRequestURI().getQuery());
                    if (queryParams.containsKey("produitId")) {
                        int produitId = parseId(queryParams.get("produitId"));
                        yield new Reponse(200, gson.toJson(avisController.listerParProduit(produitId)));
                    } else if (queryParams.containsKey("clientId")) {
                        int clientId = parseId(queryParams.get("clientId"));
                        yield new Reponse(200, gson.toJson(avisController.listerParClient(clientId)));
                    } else {
                        yield new Reponse(200, gson.toJson(avisController.lister()));
                    }
                }
                case "POST" -> {
                    AvisRequest request = lireCorps(exchange, AvisRequest.class);
                    yield new Reponse(201, gson.toJson(avisController.creer(request)));
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        } else if (segments.length == 3) {
            if ("page".equalsIgnoreCase(segments[2])) {
                return routesPageAvis(methode);
            }
            int id = parseId(segments[2]);
            return switch (methode) {
                case "GET" -> new Reponse(200, gson.toJson(avisController.obtenir(id)));
                case "PUT" -> {
                    AvisRequest request = lireCorps(exchange, AvisRequest.class);
                    yield new Reponse(200, gson.toJson(avisController.modifier(id, request)));
                }
                case "DELETE" -> {
                    avisController.supprimer(id);
                    yield new Reponse(204, "");
                }
                default -> throw new IllegalArgumentException("Méthode non supportée: " + methode);
            };
        } else if (segments.length == 4) {
            if ("produit".equalsIgnoreCase(segments[2])) {
                int produitId = parseId(segments[3]);
                if ("GET".equalsIgnoreCase(methode)) {
                    return new Reponse(200, gson.toJson(avisController.listerParProduit(produitId)));
                }
            } else if ("client".equalsIgnoreCase(segments[2])) {
                int clientId = parseId(segments[3]);
                if ("GET".equalsIgnoreCase(methode)) {
                    return new Reponse(200, gson.toJson(avisController.listerParClient(clientId)));
                }
            }
        }
        throw new ResourceNotFoundException("Route inconnue.");
    }

    private Reponse routesPageAvis(String methode) {
        if (!"GET".equalsIgnoreCase(methode)) {
            throw new IllegalArgumentException("Méthode non supportée: " + methode);
        }
        return new Reponse(200, pageAvisHtml(), "text/html; charset=utf-8");
    }

    private Map<String, String> extraireQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                params.put(pair[0].trim(), pair[1].trim());
            } else if (pair.length == 1) {
                params.put(pair[0].trim(), "");
            }
        }
        return params;
    }

    private <T> T lireCorps(HttpExchange exchange, Class<T> clazz) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return null;
        }
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
        return gson.toJson(Map.of("erreur", msg != null ? msg : "Erreur interne"));
    }

    private void ajouterEntetesCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void envoyer(HttpExchange exchange, int statut, String corps, String contentType) throws IOException {
        ajouterEntetesCors(exchange);
        byte[] bytes = (corps == null) ? new byte[0] : corps.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0) {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(statut, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(statut, -1);
            exchange.close();
        }
    }

    private String pageAvisHtml() {
        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>OneMoreBite - Donner son avis</title>
            <style>
                :root {
                    --primary: #2563eb;
                    --primary-hover: #1d4ed8;
                    --bg: #f8fafc;
                    --card: #ffffff;
                    --text: #0f172a;
                    --muted: #64748b;
                    --border: #e2e8f0;
                    --gold: #f59e0b;
                    --success: #16a34a;
                    --danger: #dc2626;
                }
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    background-color: var(--bg);
                    color: var(--text);
                    padding: 2rem 1rem;
                }
                .container {
                    max-width: 860px;
                    margin: 0 auto;
                }
                header {
                    text-align: center;
                    margin-bottom: 2rem;
                }
                header h1 {
                    font-size: 2rem;
                    color: #1e3a8a;
                    margin-bottom: 0.5rem;
                }
                header p {
                    color: var(--muted);
                    font-size: 1rem;
                }
                .card {
                    background: var(--card);
                    border-radius: 12px;
                    padding: 1.5rem;
                    border: 1px solid var(--border);
                    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.06);
                    margin-bottom: 2rem;
                }
                .card h2 {
                    font-size: 1.25rem;
                    margin-bottom: 1rem;
                    padding-bottom: 0.5rem;
                    border-bottom: 1px solid var(--border);
                }
                .form-group {
                    margin-bottom: 1.25rem;
                }
                label {
                    display: block;
                    font-weight: 600;
                    font-size: 0.9rem;
                    margin-bottom: 0.4rem;
                }
                select, input[type="number"], textarea {
                    width: 100%;
                    padding: 0.7rem 0.85rem;
                    border-radius: 8px;
                    border: 1px solid var(--border);
                    font-size: 0.95rem;
                    background: #fff;
                    color: var(--text);
                }
                select:focus, input:focus, textarea:focus {
                    outline: none;
                    border-color: var(--primary);
                    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
                }
                .stars {
                    display: flex;
                    gap: 0.4rem;
                    font-size: 1.8rem;
                    cursor: pointer;
                    user-select: none;
                }
                .star {
                    color: #cbd5e1;
                    transition: color 0.15s ease;
                }
                .star.active, .star.hover {
                    color: var(--gold);
                }
                button.btn {
                    background: var(--primary);
                    color: #fff;
                    border: none;
                    padding: 0.8rem 1.4rem;
                    font-size: 1rem;
                    font-weight: 600;
                    border-radius: 8px;
                    cursor: pointer;
                    width: 100%;
                    transition: background 0.2s;
                }
                button.btn:hover {
                    background: var(--primary-hover);
                }
                .alert {
                    padding: 0.9rem 1.2rem;
                    border-radius: 8px;
                    margin-bottom: 1.25rem;
                    font-weight: 500;
                    display: none;
                }
                .alert-success { background: #dcfce7; color: var(--success); border: 1px solid #bbf7d0; }
                .alert-danger { background: #fee2e2; color: var(--danger); border: 1px solid #fecaca; }
                .filter-bar {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    gap: 1rem;
                    margin-bottom: 1rem;
                }
                .avis-list {
                    display: flex;
                    flex-direction: column;
                    gap: 1rem;
                }
                .avis-item {
                    background: #f8fafc;
                    border: 1px solid var(--border);
                    border-radius: 10px;
                    padding: 1rem 1.25rem;
                }
                .avis-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: 0.5rem;
                }
                .badge-produit {
                    background: #e0e7ff;
                    color: #3730a3;
                    font-size: 0.8rem;
                    font-weight: 600;
                    padding: 0.25rem 0.6rem;
                    border-radius: 9999px;
                }
                .avis-stars {
                    color: var(--gold);
                    font-size: 1.1rem;
                    letter-spacing: 2px;
                }
                .avis-body {
                    margin: 0.4rem 0;
                    color: #334155;
                    line-height: 1.5;
                }
                .avis-footer {
                    font-size: 0.8rem;
                    color: var(--muted);
                }
            </style>
        </head>
        <body>
        <div class="container">
            <header>
                <h1>OneMoreBite</h1>
                <p>Espace Avis Clients - Donnez votre avis sur n'importe quel produit</p>
            </header>

            <div id="alertSuccess" class="alert alert-success"></div>
            <div id="alertDanger" class="alert alert-danger"></div>

            <section class="card">
                <h2>Rédiger un avis</h2>
                <form id="formAvis">
                    <div class="form-group">
                        <label for="selectProduit">Sélectionner un produit :</label>
                        <select id="selectProduit" required>
                            <option value="">Chargement des produits...</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label for="inputClient">ID Client :</label>
                        <input type="number" id="inputClient" value="3" min="1" required />
                    </div>

                    <div class="form-group">
                        <label>Note (de 1 à 5 étoiles) :</label>
                        <div class="stars" id="starContainer">
                            <span class="star active" data-value="1">★</span>
                            <span class="star active" data-value="2">★</span>
                            <span class="star active" data-value="3">★</span>
                            <span class="star active" data-value="4">★</span>
                            <span class="star active" data-value="5">★</span>
                        </div>
                        <input type="hidden" id="inputNote" value="5" />
                    </div>

                    <div class="form-group">
                        <label for="inputCommentaire">Votre commentaire :</label>
                        <textarea id="inputCommentaire" rows="3" placeholder="Partagez votre expérience sur la qualité, la fraîcheur..."></textarea>
                    </div>

                    <button type="submit" class="btn">Publier mon avis</button>
                </form>
            </section>

            <section class="card">
                <div class="filter-bar">
                    <h2>Tous les avis</h2>
                    <select id="filterProduit" style="max-width: 250px;">
                        <option value="">Tous les produits</option>
                    </select>
                </div>
                <div id="avisList" class="avis-list">
                    <p style="color: var(--muted);">Chargement des avis...</p>
                </div>
            </section>
        </div>

        <script>
            let produitsMap = {};
            let currentNote = 5;

            // Gestion des étoiles
            const stars = document.querySelectorAll('#starContainer .star');
            const inputNote = document.getElementById('inputNote');

            function updateStars(note) {
                currentNote = note;
                inputNote.value = note;
                stars.forEach(s => {
                    const val = parseInt(s.dataset.value);
                    if (val <= note) {
                        s.classList.add('active');
                    } else {
                        s.classList.remove('active');
                    }
                });
            }

            stars.forEach(s => {
                s.addEventListener('click', () => updateStars(parseInt(s.dataset.value)));
                s.addEventListener('mouseover', () => {
                    const val = parseInt(s.dataset.value);
                    stars.forEach(star => {
                        star.classList.toggle('hover', parseInt(star.dataset.value) <= val);
                    });
                });
                s.addEventListener('mouseout', () => {
                    stars.forEach(star => star.classList.remove('hover'));
                });
            });

            // Messages d'alerte
            function showAlert(msg, isSuccess = true) {
                const ok = document.getElementById('alertSuccess');
                const err = document.getElementById('alertDanger');
                if (isSuccess) {
                    ok.textContent = msg;
                    ok.style.display = 'block';
                    err.style.display = 'none';
                    setTimeout(() => { ok.style.display = 'none'; }, 4000);
                } else {
                    err.textContent = msg;
                    err.style.display = 'block';
                    ok.style.display = 'none';
                    setTimeout(() => { err.style.display = 'none'; }, 5000);
                }
            }

            // Charger les produits
            async function chargerProduits() {
                try {
                    const res = await fetch('/produits');
                    const produits = await res.json();
                    const select = document.getElementById('selectProduit');
                    const filter = document.getElementById('filterProduit');

                    select.innerHTML = '<option value="">-- Choisissez un produit --</option>';
                    filter.innerHTML = '<option value="">Tous les produits</option>';

                    produits.forEach(p => {
                        produitsMap[p.id] = p.nom;
                        const opt = document.createElement('option');
                        opt.value = p.id;
                        opt.textContent = `${p.nom} (#${p.id} - ${p.prix} DT)`;
                        select.appendChild(opt);

                        const filterOpt = document.createElement('option');
                        filterOpt.value = p.id;
                        filterOpt.textContent = p.nom;
                        filter.appendChild(filterOpt);
                    });
                } catch (e) {
                    console.error('Erreur chargement produits', e);
                }
            }

            // Charger les avis
            async function chargerAvis(produitId = null) {
                try {
                    const url = produitId ? `/avis?produitId=${produitId}` : '/avis';
                    const res = await fetch(url);
                    const avis = await res.json();
                    const container = document.getElementById('avisList');

                    if (!avis || avis.length === 0) {
                        container.innerHTML = '<p style="color: var(--muted); text-align: center; padding: 1.5rem;">Aucun avis pour le moment.</p>';
                        return;
                    }

                    container.innerHTML = avis.map(a => {
                        const nomProduit = produitsMap[a.produitId] || `Produit #${a.produitId}`;
                        const etoiles = '★'.repeat(a.note) + '☆'.repeat(5 - a.note);
                        const dateFormatted = a.dateAvis ? a.dateAvis.replace('T', ' ').substring(0, 16) : '';

                        return `
                            <div class="avis-item">
                                <div class="avis-header">
                                    <span class="badge-produit">${nomProduit}</span>
                                    <span class="avis-stars">${etoiles}</span>
                                </div>
                                <div class="avis-body">${a.commentaire ? a.commentaire : '<em>Aucun commentaire rédigé.</em>'}</div>
                                <div class="avis-footer">Client #${a.clientId} &bull; ${dateFormatted}</div>
                            </div>
                        `;
                    }).join('');
                } catch (e) {
                    console.error('Erreur chargement avis', e);
                }
            }

            // Soumission du formulaire
            document.getElementById('formAvis').addEventListener('submit', async (e) => {
                e.preventDefault();
                const produitId = parseInt(document.getElementById('selectProduit').value);
                const clientId = parseInt(document.getElementById('inputClient').value);
                const note = parseInt(document.getElementById('inputNote').value);
                const commentaire = document.getElementById('inputCommentaire').value.trim();

                if (!produitId) {
                    showAlert('Veuillez sélectionner un produit.', false);
                    return;
                }

                try {
                    const res = await fetch('/avis', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ produitId, clientId, note, commentaire })
                    });

                    if (!res.ok) {
                        const err = await res.json();
                        throw new Error(err.erreur || 'Erreur lors de la publication de l\\'avis');
                    }

                    showAlert('Votre avis a été publié avec succès !', true);
                    document.getElementById('inputCommentaire').value = '';
                    updateStars(5);

                    const filterValue = document.getElementById('filterProduit').value;
                    chargerAvis(filterValue ? parseInt(filterValue) : null);
                } catch (err) {
                    showAlert(err.message, false);
                }
            });

            // Filtre par produit
            document.getElementById('filterProduit').addEventListener('change', (e) => {
                const val = e.target.value;
                chargerAvis(val ? parseInt(val) : null);
            });

            // Initialisation
            (async () => {
                await chargerProduits();
                await chargerAvis();
            })();
        </script>
        </body>
        </html>
        """;
    }
}
